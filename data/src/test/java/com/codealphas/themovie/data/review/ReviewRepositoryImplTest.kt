package com.codealphas.themovie.data.review

import com.codealphas.themovie.data.review.local.ReviewDao
import com.codealphas.themovie.data.review.local.ReviewEntity
import com.codealphas.themovie.data.review.remote.ReviewRealtimeDataSource
import com.codealphas.themovie.data.review.remote.ReviewStorageDataSource
import com.codealphas.themovie.domain.auth.AuthRepository
import com.codealphas.themovie.domain.auth.AuthResult
import com.codealphas.themovie.domain.result.Outcome
import com.codealphas.themovie.domain.review.Review
import com.codealphas.themovie.domain.review.ReviewDraft
import com.codealphas.themovie.domain.review.ReviewError
import com.codealphas.themovie.domain.review.ReviewPhoto
import com.codealphas.themovie.domain.review.ReviewResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ReviewRepositoryImplTest {
    private val calls = mutableListOf<String>()
    private val dao = FakeReviewDao(calls)
    private val realtime = FakeReviewRealtimeDataSource(calls)
    private val storage = FakeReviewStorageDataSource(calls)

    @Test
    fun `사진을 그대로 두고 수정하면 업로드하거나 파일을 지우지 않고 기존 사진으로 Room과 서버에 써야 한다`() =
        runTest {
            dao.put(STORED)

            val result = repository().save(editDraft(ReviewPhoto.Unchanged))

            assertEquals(Outcome.Success(Unit), result)
            assertEquals(listOf("room.update", "server.save"), calls)
            val expected = STORED.toReview().copy(title = "수정한 제목")
            assertEquals(expected, dao.saved(STORED.id))
            assertEquals(expected, realtime.saved.single().withoutTime())
        }

    @Test
    fun `새 사진으로 수정하면 업로드 뒤 Room과 서버에 쓰고 마지막에 이전 파일을 지워야 한다`() =
        runTest {
            dao.put(STORED)

            val result = repository().save(editDraft(ReviewPhoto.New("content://photo/2")))

            assertEquals(Outcome.Success(Unit), result)
            assertEquals(
                listOf("storage.upload:$NEW_FILE_NAME", "room.update", "server.save", "storage.delete:old.png"),
                calls,
            )
            val expected =
                STORED.toReview().copy(
                    title = "수정한 제목",
                    image = downloadUrl(NEW_FILE_NAME),
                    storageFileName = NEW_FILE_NAME,
                )
            assertEquals(expected, dao.saved(STORED.id))
            assertEquals(expected, realtime.saved.single().withoutTime())
        }

    @Test
    fun `사진 업로드가 실패하면 Room과 서버에 쓰지 않고 이전 파일도 지우지 않아야 한다`() =
        runTest {
            dao.put(STORED)
            storage.failUpload = true

            val result = repository().save(editDraft(ReviewPhoto.New("content://photo/2")))

            assertEquals(Outcome.Failure(ReviewError.PhotoUploadFailed), result)
            assertEquals(listOf("storage.upload:$NEW_FILE_NAME"), calls)
            assertEquals(STORED.toReview(), dao.saved(STORED.id))
        }

    @Test
    fun `사진 업로드가 SDK에서 취소되면 Room과 서버에 쓰지 않고 업로드 실패를 반환해야 한다`() =
        runTest {
            dao.put(STORED)
            storage.cancelUpload = true

            val result = repository().save(editDraft(ReviewPhoto.New("content://photo/2")))

            assertEquals(Outcome.Failure(ReviewError.PhotoUploadFailed), result)
            assertEquals(listOf("storage.upload:$NEW_FILE_NAME"), calls)
            assertEquals(STORED.toReview(), dao.saved(STORED.id))
        }

    @Test
    fun `새 사진으로 수정할 때 이전 파일 삭제가 SDK에서 취소되면 저장 성공을 반환해야 한다`() =
        runTest {
            dao.put(STORED)
            storage.cancelDelete = true

            val result = repository().save(editDraft(ReviewPhoto.New("content://photo/2")))

            assertEquals(Outcome.Success(Unit), result)
            assertEquals(
                listOf("storage.upload:$NEW_FILE_NAME", "room.update", "server.save", "storage.delete:old.png"),
                calls,
            )
            assertEquals(NEW_FILE_NAME, dao.saved(STORED.id)?.storageFileName)
        }

    @Test
    fun `사진 업로드 중에 저장을 요청한 코루틴이 취소되면 실패를 반환하지 않고 취소되어야 한다`() =
        runTest {
            dao.put(STORED)
            storage.suspendUpload = true
            var result: ReviewResult? = null

            val job = launch { result = repository().save(editDraft(ReviewPhoto.New("content://photo/2"))) }
            runCurrent()
            job.cancel()
            job.join()

            assertTrue(job.isCancelled)
            assertNull(result)
            assertEquals(listOf("storage.upload:$NEW_FILE_NAME"), calls)
            assertEquals(STORED.toReview(), dao.saved(STORED.id))
        }

    @Test
    fun `사진을 지우고 수정하면 빈 URL로 저장하고 이전 파일을 지워야 한다`() =
        runTest {
            dao.put(STORED)

            val result = repository().save(editDraft(ReviewPhoto.Removed))

            assertEquals(Outcome.Success(Unit), result)
            assertEquals(listOf("room.update", "server.save", "storage.delete:old.png"), calls)
            val expected = STORED.toReview().copy(title = "수정한 제목", image = "", storageFileName = "")
            assertEquals(expected, dao.saved(STORED.id))
        }

    @Test
    fun `사진 없는 새 감상문을 저장하면 Room이 준 id로 서버에 쓰고 파일은 지우지 않아야 한다`() =
        runTest {
            val draft =
                ReviewDraft(id = null, title = "새 감상문", content = "재밌다", rating = 7.0, photo = ReviewPhoto.Unchanged)

            val result = repository().save(draft)

            assertEquals(Outcome.Success(Unit), result)
            assertEquals(listOf("room.insert", "server.save"), calls)
            val expected =
                Review(
                    title = "새 감상문",
                    image = "",
                    content = "재밌다",
                    time = "",
                    rating = 7.0,
                    storageFileName = "",
                    id = FakeReviewDao.FIRST_ID,
                )
            assertEquals(expected, dao.saved(FakeReviewDao.FIRST_ID))
            assertEquals(USER_ID, realtime.savedUserIds.single())
            assertEquals(expected, realtime.saved.single().withoutTime())
        }

    @Test
    fun `수정할 감상문이 Room에 없으면 사진을 올리거나 Room과 서버에 쓰지 않고 Unknown 실패를 반환해야 한다`() =
        runTest {
            val result = repository().save(editDraft(ReviewPhoto.New("content://photo/2")))

            assertEquals(Outcome.Failure(ReviewError.Unknown), result)
            assertEquals(emptyList<String>(), calls)
        }

    @Test
    fun `로그인하지 않은 상태면 사진을 올리거나 Room과 서버에 쓰지 않고 Unknown 실패를 반환해야 한다`() =
        runTest {
            dao.put(STORED)

            val result = repository(userId = null).save(editDraft(ReviewPhoto.New("content://photo/2")))

            assertEquals(Outcome.Failure(ReviewError.Unknown), result)
            assertEquals(emptyList<String>(), calls)
        }

    @Test
    fun `서버 감상문을 받아 오는 작업이 SDK에서 취소되면 Room에 쓰지 않고 Unknown 실패를 반환해야 한다`() =
        runTest {
            realtime.cancelGetAll = true

            val result = repository().syncFromRemote()

            assertEquals(Outcome.Failure(ReviewError.Unknown), result)
            assertEquals(listOf("server.getAll"), calls)
        }

    @Test
    fun `서버 감상문을 받아 오는 중에 동기화를 요청한 코루틴이 취소되면 실패를 반환하지 않고 취소되어야 한다`() =
        runTest {
            realtime.suspendGetAll = true
            var result: ReviewResult? = null

            val job = launch { result = repository().syncFromRemote() }
            runCurrent()
            job.cancel()
            job.join()

            assertTrue(job.isCancelled)
            assertNull(result)
            assertEquals(listOf("server.getAll"), calls)
        }

    private fun repository(userId: String? = USER_ID): ReviewRepositoryImpl =
        ReviewRepositoryImpl(
            reviewDao = dao,
            realtimeDataSource = realtime,
            storageDataSource = storage,
            authRepository = FakeReviewAuthRepository(userId),
            clock = { NOW_MILLIS },
        )
}

// 사진 파일 이름은 사용자 id 앞 10자에 현재 시각을 붙이므로, 10자보다 긴 id에서 앞 10자만 쓰는지 확인하도록 15자 id 적용
private const val USER_ID = "user-1234567890"
private const val NOW_MILLIS = 1_700_000_000_000L
private const val NEW_FILE_NAME = "user-12345$NOW_MILLIS.png"

private val STORED =
    ReviewEntity(
        title = "기생충",
        image = downloadUrl("old.png"),
        content = "잘 봤다",
        time = "",
        rating = 9.0,
        storageFileName = "old.png",
        id = 3,
    )

private fun editDraft(photo: ReviewPhoto): ReviewDraft =
    ReviewDraft(id = STORED.id, title = "수정한 제목", content = "잘 봤다", rating = 9.0, photo = photo)

private fun downloadUrl(fileName: String): String = "https://storage.example.com/$fileName"

// 작성 시각은 기기 시간대에 따라 문자열이 달라지므로, 시각을 뺀 필드만 비교하도록 빈 문자열로 적용
private fun Review.withoutTime(): Review = copy(time = "")

private class FakeReviewDao(
    private val calls: MutableList<String>,
) : ReviewDao {
    private val entities = mutableMapOf<Int, ReviewEntity>()

    fun put(entity: ReviewEntity) {
        entities[entity.id] = entity
    }

    fun saved(id: Int): Review? = entities[id]?.toReview()?.withoutTime()

    override suspend fun insert(review: ReviewEntity): Long {
        calls += "room.insert"
        val id = if (review.id == 0) (entities.keys.maxOrNull() ?: 0) + FIRST_ID else review.id
        if (id in entities) return IGNORED_ROW_ID
        entities[id] = review.toReview().copy(id = id).toEntity()
        return id.toLong()
    }

    override suspend fun delete(review: ReviewEntity) = error("사용하지 않음")

    override suspend fun update(review: ReviewEntity) {
        calls += "room.update"
        entities[review.id] = review
    }

    override suspend fun getById(id: Int): ReviewEntity? = entities[id]

    override suspend fun deleteAll() = error("사용하지 않음")

    override fun getAll(): Flow<List<ReviewEntity>> = error("사용하지 않음")

    companion object {
        const val FIRST_ID = 1
        const val IGNORED_ROW_ID = -1L
    }
}

private class FakeReviewRealtimeDataSource(
    private val calls: MutableList<String>,
) : ReviewRealtimeDataSource {
    val saved = mutableListOf<Review>()
    val savedUserIds = mutableListOf<String>()

    // Firebase Realtime Database SDK가 서버 감상문 읽기를 취소하면 동기화를 요청한 코루틴이 취소되지 않아도 취소 예외가 올라오므로,
    // 코루틴 취소 없이 읽기에서 CancellationException 발생 적용
    var cancelGetAll: Boolean = false

    // awaitCancellation()은 코루틴이 취소될 때까지 멈추므로,
    // 동기화를 요청한 코루틴이 서버 감상문을 받아 오는 도중에 취소되는 상황을 만들도록 읽기 대기 적용
    var suspendGetAll: Boolean = false

    override suspend fun getAll(userId: String): List<Review> {
        calls += "server.getAll"
        if (cancelGetAll) throw CancellationException("SDK에서 취소된 읽기")
        if (suspendGetAll) awaitCancellation()
        return emptyList()
    }

    override fun save(
        userId: String,
        review: Review,
    ) {
        calls += "server.save"
        savedUserIds += userId
        saved += review
    }

    override fun delete(
        userId: String,
        reviewId: Int,
    ) = error("사용하지 않음")
}

private class FakeReviewStorageDataSource(
    private val calls: MutableList<String>,
) : ReviewStorageDataSource {
    var failUpload: Boolean = false

    // Firebase Storage SDK가 업로드나 삭제를 취소하면 저장을 요청한 코루틴이 취소되지 않아도 취소 예외가 올라오므로,
    // 코루틴 취소 없이 업로드나 삭제에서 CancellationException 발생 적용
    var cancelUpload: Boolean = false
    var cancelDelete: Boolean = false

    // awaitCancellation()은 코루틴이 취소될 때까지 멈추므로,
    // 저장을 요청한 코루틴이 업로드 도중에 취소되는 상황을 만들도록 업로드 대기 적용
    var suspendUpload: Boolean = false

    override suspend fun upload(
        fileName: String,
        uri: String,
    ): String {
        calls += "storage.upload:$fileName"
        if (failUpload) throw IOException("업로드 실패")
        if (cancelUpload) throw CancellationException("SDK에서 취소된 업로드")
        if (suspendUpload) awaitCancellation()
        return downloadUrl(fileName)
    }

    override suspend fun delete(fileName: String) {
        calls += "storage.delete:$fileName"
        if (cancelDelete) throw CancellationException("SDK에서 취소된 삭제")
    }
}

private class FakeReviewAuthRepository(
    private val userId: String?,
) : AuthRepository {
    override suspend fun signIn(
        email: String,
        password: String,
    ): AuthResult = error("사용하지 않음")

    override suspend fun signUp(
        email: String,
        password: String,
    ): AuthResult = error("사용하지 않음")

    override fun currentUserId(): String? = userId

    override fun signOut() = error("사용하지 않음")
}
