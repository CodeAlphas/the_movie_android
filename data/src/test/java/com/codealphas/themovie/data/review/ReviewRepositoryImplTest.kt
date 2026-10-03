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
                    imageUrl = downloadUrl(NEW_FILE_NAME),
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
            val expected = STORED.toReview().copy(title = "수정한 제목", imageUrl = "", storageFileName = "")
            assertEquals(expected, dao.saved(STORED.id))
        }

    @Test
    fun `사진 없는 새 감상문을 저장하면 새로 만든 키로 Room과 서버에 쓰고 파일은 지우지 않아야 한다`() =
        runTest {
            val result = repository().save(newDraft(ReviewPhoto.Unchanged))

            assertEquals(Outcome.Success(Unit), result)
            assertEquals(listOf("server.newKey", "room.insert", "server.save"), calls)
            val expected =
                Review(
                    title = "새 감상문",
                    imageUrl = "",
                    content = "재밌다",
                    time = "",
                    rating = 7.0,
                    storageFileName = "",
                    id = NEW_KEY,
                )
            assertEquals(expected, dao.saved(NEW_KEY))
            assertEquals(USER_ID, realtime.savedUserIds.single())
            assertEquals(expected, realtime.saved.single().withoutTime())
        }

    @Test
    fun `다시 설치한 뒤 서버 감상문을 받아 오기 전에 새 감상문을 저장하면 서버에 있던 감상문을 덮어쓰지 않아야 한다`() =
        runTest {
            // Room 자동 증가 id를 서버 키로 쓰면 빈 Room에서 새 감상문이 서버의 1번 감상문을 덮어쓰므로, 서버에 1번 키 감상문을 둔 상태 적용
            val serverReview = STORED.toReview().copy(id = "1")
            realtime.remote[serverReview.id] = serverReview

            repository().save(newDraft(ReviewPhoto.Unchanged))

            assertEquals(serverReview, realtime.remote["1"])
            assertEquals("새 감상문", realtime.remote[NEW_KEY]?.title)
        }

    @Test
    fun `새 감상문의 키를 만들지 못하면 사진을 올리거나 Room과 서버에 쓰지 않고 Unknown 실패를 반환해야 한다`() =
        runTest {
            realtime.failNewKey = true

            val result = repository().save(newDraft(ReviewPhoto.New("content://photo/2")))

            assertEquals(Outcome.Failure(ReviewError.Unknown), result)
            assertEquals(listOf("server.newKey"), calls)
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
    fun `서버에서 받아 온 감상문이 Room에 없으면 서버 키를 id로 Room에 추가해야 한다`() =
        runTest {
            val serverReview = STORED.toReview().copy(id = "-server")
            realtime.remote[serverReview.id] = serverReview

            val result = repository().syncFromRemote()

            assertEquals(Outcome.Success(Unit), result)
            assertEquals(serverReview, dao.saved("-server"))
        }

    @Test
    fun `서버에서 받아 온 감상문과 같은 키의 감상문이 Room에 있으면 Room 감상문을 그대로 두어야 한다`() =
        runTest {
            dao.put(STORED)
            realtime.remote[STORED.id] = STORED.toReview().copy(title = "서버에서 고친 제목")

            val result = repository().syncFromRemote()

            assertEquals(Outcome.Success(Unit), result)
            assertEquals(STORED.toReview(), dao.saved(STORED.id))
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

    @Test
    fun `감상문을 지우면 Room, 서버, 사진 파일 순서로 지우고 성공을 반환해야 한다`() =
        runTest {
            dao.put(STORED)

            val result = repository().delete(STORED.toReview())

            assertEquals(Outcome.Success(Unit), result)
            assertEquals(listOf("room.delete", "server.delete:${STORED.id}", "storage.delete:old.png"), calls)
            assertNull(dao.saved(STORED.id))
        }

    @Test
    fun `Room에서 감상문을 지우지 못하면 서버와 사진 파일을 지우지 않고 Unknown 실패를 반환해야 한다`() =
        runTest {
            dao.put(STORED)
            dao.failDelete = true

            val result = repository().delete(STORED.toReview())

            assertEquals(Outcome.Failure(ReviewError.Unknown), result)
            assertEquals(listOf("room.delete"), calls)
            assertEquals(STORED.toReview(), dao.saved(STORED.id))
        }

    @Test
    fun `감상문 사진 파일을 지우지 못해도 Room에서 지웠으면 성공을 반환해야 한다`() =
        runTest {
            dao.put(STORED)
            storage.failDelete = true

            val result = repository().delete(STORED.toReview())

            assertEquals(Outcome.Success(Unit), result)
            assertEquals(listOf("room.delete", "server.delete:${STORED.id}", "storage.delete:old.png"), calls)
            assertNull(dao.saved(STORED.id))
        }

    @Test
    fun `서버 삭제 요청이 바로 실패하면 사진 파일은 지우지 않고 성공을 반환해야 한다`() =
        runTest {
            dao.put(STORED)
            realtime.failDelete = true

            val result = repository().delete(STORED.toReview())

            assertEquals(Outcome.Success(Unit), result)
            assertEquals(listOf("room.delete", "server.delete:${STORED.id}"), calls)
            assertNull(dao.saved(STORED.id))
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
private const val NEW_KEY = "-new-key"

private val STORED =
    ReviewEntity(
        title = "기생충",
        image = downloadUrl("old.png"),
        content = "잘 봤다",
        time = "",
        rating = 9.0,
        storageFileName = "old.png",
        id = "-stored",
    )

private fun editDraft(photo: ReviewPhoto): ReviewDraft =
    ReviewDraft(id = STORED.id, title = "수정한 제목", content = "잘 봤다", rating = 9.0, photo = photo)

private fun newDraft(photo: ReviewPhoto): ReviewDraft =
    ReviewDraft(id = null, title = "새 감상문", content = "재밌다", rating = 7.0, photo = photo)

private fun downloadUrl(fileName: String): String = "https://storage.example.com/$fileName"

// 작성 시각은 기기 시간대에 따라 문자열이 달라지므로, 시각을 뺀 필드만 비교하도록 빈 문자열로 적용
private fun Review.withoutTime(): Review = copy(time = "")

private class FakeReviewDao(
    private val calls: MutableList<String>,
) : ReviewDao {
    private val entities = mutableMapOf<String, ReviewEntity>()

    var failDelete: Boolean = false

    fun put(entity: ReviewEntity) {
        entities[entity.id] = entity
    }

    fun saved(id: String): Review? = entities[id]?.toReview()?.withoutTime()

    // Room의 OnConflictStrategy.IGNORE처럼 같은 id가 있으면 기존 행을 두도록, 없는 id만 추가 적용
    override suspend fun insert(review: ReviewEntity) {
        calls += "room.insert"
        if (review.id !in entities) entities[review.id] = review
    }

    override suspend fun delete(review: ReviewEntity) {
        calls += "room.delete"
        if (failDelete) throw IllegalStateException("Room 삭제 실패")
        entities -= review.id
    }

    override suspend fun update(review: ReviewEntity) {
        calls += "room.update"
        entities[review.id] = review
    }

    override suspend fun getById(id: String): ReviewEntity? = entities[id]

    override suspend fun deleteAll() = error("사용하지 않음")

    override fun getAll(): Flow<List<ReviewEntity>> = error("사용하지 않음")
}

private class FakeReviewRealtimeDataSource(
    private val calls: MutableList<String>,
) : ReviewRealtimeDataSource {
    val saved = mutableListOf<Review>()
    val savedUserIds = mutableListOf<String>()

    // 서버에 쓰인 감상문을 키별로 남겨, 덮어쓰기 여부와 동기화로 받아 올 감상문을 확인하도록 서버 상태 적용
    val remote = mutableMapOf<String, Review>()

    var failNewKey: Boolean = false

    // Firebase Realtime Database SDK가 서버 감상문 읽기를 취소하면 동기화를 요청한 코루틴이 취소되지 않아도 취소 예외가 올라오므로,
    // 코루틴 취소 없이 읽기에서 CancellationException 발생 적용
    var cancelGetAll: Boolean = false

    // awaitCancellation()은 코루틴이 취소될 때까지 멈추므로,
    // 동기화를 요청한 코루틴이 서버 감상문을 받아 오는 도중에 취소되는 상황을 만들도록 읽기 대기 적용
    var suspendGetAll: Boolean = false

    var failDelete: Boolean = false

    override suspend fun getAll(userId: String): List<Review> {
        calls += "server.getAll"
        if (cancelGetAll) throw CancellationException("SDK에서 취소된 읽기")
        if (suspendGetAll) awaitCancellation()
        return remote.values.toList()
    }

    override fun newKey(userId: String): String {
        calls += "server.newKey"
        if (failNewKey) throw IllegalStateException("키 생성 실패")
        return NEW_KEY
    }

    override fun save(
        userId: String,
        review: Review,
    ) {
        calls += "server.save"
        savedUserIds += userId
        saved += review
        remote[review.id] = review
    }

    override fun delete(
        userId: String,
        reviewId: String,
    ) {
        calls += "server.delete:$reviewId"
        if (failDelete) throw IllegalStateException("서버 삭제 요청 실패")
    }
}

private class FakeReviewStorageDataSource(
    private val calls: MutableList<String>,
) : ReviewStorageDataSource {
    var failUpload: Boolean = false
    var failDelete: Boolean = false

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
        if (failDelete) throw IOException("삭제 실패")
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
