package com.codealphas.themovie.data.review

import com.codealphas.themovie.data.review.local.ReviewDao
import com.codealphas.themovie.data.review.local.ReviewEntity
import com.codealphas.themovie.data.review.local.ReviewSyncState
import com.codealphas.themovie.data.review.remote.ReviewRealtimeDataSource
import com.codealphas.themovie.data.review.remote.ReviewStorageDataSource
import com.codealphas.themovie.data.review.sync.ReviewSyncLock
import com.codealphas.themovie.data.review.sync.ReviewSyncScheduler
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
import kotlinx.coroutines.test.currentTime
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
    private val scheduler = FakeReviewSyncScheduler(calls)

    @Test
    fun `사진을 그대로 두고 수정하면 사진을 올리거나 지우지 않고 기존 사진으로 Room에 저장한 뒤 서버 업로드를 예약해야 한다`() =
        runTest {
            dao.put(STORED)

            val result = repository().save(editDraft(ReviewPhoto.Unchanged))

            assertEquals(Outcome.Success(Unit), result)
            assertEquals(listOf("room.update", "sync.schedule"), calls)
            val expected = STORED.toReview().copy(title = "수정한 제목", updatedAt = NOW_MILLIS)
            assertEquals(expected, dao.saved(STORED.id))
        }

    @Test
    fun `새 사진으로 수정하면 사진을 올린 뒤 Room에 저장하고 서버 업로드를 예약하며 이전 사진 파일은 지우지 않고 이름을 남겨야 한다`() =
        runTest {
            dao.put(STORED)

            val result = repository().save(editDraft(ReviewPhoto.New("content://photo/2")))

            assertEquals(Outcome.Success(Unit), result)
            assertEquals(listOf("storage.upload:$NEW_FILE_NAME", "room.update", "sync.schedule"), calls)
            assertEquals("old.png", dao.entity(STORED.id)?.storageFileNameToDelete)
            val expected =
                STORED.toReview().copy(
                    title = "수정한 제목",
                    imageUrl = downloadUrl(NEW_FILE_NAME),
                    updatedAt = NOW_MILLIS,
                    storageFileName = NEW_FILE_NAME,
                )
            assertEquals(expected, dao.saved(STORED.id))
        }

    @Test
    fun `사진 업로드가 실패하면 Room에 저장하거나 서버 업로드를 예약하지 않고 이전 사진 파일도 지우지 않아야 한다`() =
        runTest {
            dao.put(STORED)
            storage.failUpload = true

            val result = repository().save(editDraft(ReviewPhoto.New("content://photo/2")))

            assertEquals(Outcome.Failure(ReviewError.PhotoUploadFailed), result)
            assertEquals(listOf("storage.upload:$NEW_FILE_NAME"), calls)
            assertEquals(STORED.toReview(), dao.saved(STORED.id))
        }

    @Test
    fun `사진 업로드가 SDK에서 취소되면 Room에 저장하지 않고 PhotoUploadFailed를 반환해야 한다`() =
        runTest {
            dao.put(STORED)
            storage.cancelUpload = true

            val result = repository().save(editDraft(ReviewPhoto.New("content://photo/2")))

            assertEquals(Outcome.Failure(ReviewError.PhotoUploadFailed), result)
            assertEquals(listOf("storage.upload:$NEW_FILE_NAME"), calls)
            assertEquals(STORED.toReview(), dao.saved(STORED.id))
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
    fun `사진을 지우고 수정하면 Room에 사진 없이 저장하고 이전 사진 파일은 업로드 뒤 지우도록 기록해야 한다`() =
        runTest {
            dao.put(STORED)

            val result = repository().save(editDraft(ReviewPhoto.Removed))

            assertEquals(Outcome.Success(Unit), result)
            assertEquals(listOf("room.update", "sync.schedule"), calls)
            assertEquals("old.png", dao.entity(STORED.id)?.storageFileNameToDelete)
            val expected =
                STORED.toReview().copy(title = "수정한 제목", imageUrl = "", updatedAt = NOW_MILLIS, storageFileName = "")
            assertEquals(expected, dao.saved(STORED.id))
        }

    @Test
    fun `업로드 전에 사진을 두 번 바꾸면 중간에 고른 사진 파일만 바로 지우고 서버에 있는 사진 파일은 업로드 뒤 지우도록 남겨야 한다`() =
        runTest {
            dao.put(STORED_WITH_PHOTO_TO_DELETE)

            val result = repository().save(editDraft(ReviewPhoto.New("content://photo/3")))

            assertEquals(Outcome.Success(Unit), result)
            assertEquals(
                listOf("storage.upload:$NEW_FILE_NAME", "room.update", "sync.schedule", "storage.delete:middle.png"),
                calls,
            )
            assertEquals("old.png", dao.entity(STORED.id)?.storageFileNameToDelete)
        }

    @Test
    fun `업로드 전에 사진을 바꾼 뒤 글만 수정하면 사진 파일을 지우지 않고 서버에 있는 사진 파일은 업로드 뒤 지우도록 남겨야 한다`() =
        runTest {
            dao.put(STORED_WITH_PHOTO_TO_DELETE)

            val result = repository().save(editDraft(ReviewPhoto.Unchanged))

            assertEquals(Outcome.Success(Unit), result)
            assertEquals(listOf("room.update", "sync.schedule"), calls)
            assertEquals("old.png", dao.entity(STORED.id)?.storageFileNameToDelete)
        }

    @Test
    fun `사진 없는 새 감상문을 저장하면 새로 만든 키로 Room에 저장하고 서버 업로드를 예약해야 한다`() =
        runTest {
            val result = repository().save(newDraft(ReviewPhoto.Unchanged))

            assertEquals(Outcome.Success(Unit), result)
            assertEquals(listOf("server.newKey", "room.insert", "sync.schedule"), calls)
            val expected =
                Review(
                    title = "새 감상문",
                    imageUrl = "",
                    content = "재밌다",
                    createdAt = NOW_MILLIS,
                    updatedAt = NOW_MILLIS,
                    rating = 7.0,
                    storageFileName = "",
                    id = NEW_KEY,
                )
            assertEquals(expected, dao.saved(NEW_KEY))
        }

    @Test
    fun `새 감상문을 저장하면 Room에 syncState는 PENDING_UPSERT, localVersion은 1로 넣어야 한다`() =
        runTest {
            repository().save(newDraft(ReviewPhoto.Unchanged))

            assertEquals(ReviewSyncState.PENDING_UPSERT, dao.entity(NEW_KEY)?.syncState)
            assertEquals(1, dao.entity(NEW_KEY)?.localVersion)
        }

    @Test
    fun `감상문을 수정하면 Room에서 syncState를 PENDING_UPSERT로 바꾸고 localVersion을 1 올려야 한다`() =
        runTest {
            dao.put(STORED)

            repository().save(editDraft(ReviewPhoto.Unchanged))

            assertEquals(ReviewSyncState.PENDING_UPSERT, dao.entity(STORED.id)?.syncState)
            assertEquals(STORED.localVersion + 1, dao.entity(STORED.id)?.localVersion)
        }

    @Test
    fun `감상문을 수정하면 createdAt은 그대로 두고 updatedAt만 지금 시각으로 바꿔야 한다`() =
        runTest {
            dao.put(STORED)

            repository().save(editDraft(ReviewPhoto.Unchanged))

            val saved = dao.saved(STORED.id)
            assertEquals(STORED.createdAt, saved?.createdAt)
            assertEquals(NOW_MILLIS, saved?.updatedAt)
        }

    @Test
    fun `새 감상문을 저장하면 createdAt과 updatedAt을 모두 지금 시각으로 저장해야 한다`() =
        runTest {
            repository().save(newDraft(ReviewPhoto.Unchanged))

            val saved = dao.saved(NEW_KEY)
            assertEquals(NOW_MILLIS, saved?.createdAt)
            assertEquals(NOW_MILLIS, saved?.updatedAt)
        }

    @Test
    fun `다시 설치한 뒤 서버 감상문을 받아 오기 전에 새 감상문을 저장하면 서버에 있던 감상문과 다른 키로 저장해야 한다`() =
        runTest {
            // Room 자동 증가 id를 서버 키로 쓰면 빈 Room에서 새 감상문이 서버의 1번 감상문과 같은 키가 되므로, 서버에 1번 키 감상문을 둔 상태 적용
            val serverReview = STORED.toReview().copy(id = "1")
            realtime.remote[serverReview.id] = serverReview

            repository().save(newDraft(ReviewPhoto.Unchanged))
            repository().syncFromRemote()

            assertEquals(serverReview, dao.saved("1"))
            assertEquals("새 감상문", dao.saved(NEW_KEY)?.title)
        }

    @Test
    fun `새 감상문의 키를 만들지 못하면 사진을 올리거나 Room에 저장하지 않고 Unknown 실패를 반환해야 한다`() =
        runTest {
            realtime.failNewKey = true

            val result = repository().save(newDraft(ReviewPhoto.New("content://photo/2")))

            assertEquals(Outcome.Failure(ReviewError.Unknown), result)
            assertEquals(listOf("server.newKey"), calls)
        }

    @Test
    fun `수정할 감상문이 Room에 없으면 사진을 올리거나 서버 업로드를 예약하지 않고 Unknown 실패를 반환해야 한다`() =
        runTest {
            val result = repository().save(editDraft(ReviewPhoto.New("content://photo/2")))

            assertEquals(Outcome.Failure(ReviewError.Unknown), result)
            assertEquals(emptyList<String>(), calls)
        }

    @Test
    fun `로그인하지 않은 상태면 사진을 올리거나 Room에 저장하지 않고 Unknown 실패를 반환해야 한다`() =
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
    fun `서버에서 받아 온 감상문을 Room에 추가하면 syncState는 SYNCED, localVersion은 0으로 저장해야 한다`() =
        runTest {
            realtime.remote["-server"] = STORED.toReview().copy(id = "-server")

            repository().syncFromRemote()

            assertEquals(ReviewSyncState.SYNCED, dao.entity("-server")?.syncState)
            assertEquals(0, dao.entity("-server")?.localVersion)
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
    fun `서버 감상문을 받아 온 뒤 서버에 올리지 않은 감상문이 Room에 남아 있으면 서버 업로드를 예약해야 한다`() =
        runTest {
            dao.put(
                ReviewEntity(
                    title = "올리지 못한 감상문",
                    image = "",
                    content = "서버가 거부함",
                    createdAt = STORED_CREATED_AT,
                    updatedAt = STORED_UPDATED_AT,
                    rating = 5.0,
                    storageFileName = "",
                    storageFileNameToDelete = "",
                    syncState = ReviewSyncState.PENDING_UPSERT,
                    localVersion = 1,
                    id = "-pending",
                ),
            )

            repository().syncFromRemote()

            assertEquals(listOf("server.getAll", "sync.schedule"), calls)
        }

    @Test
    fun `서버 감상문을 받아 온 뒤 서버에 올리지 않은 감상문이 Room에 없으면 서버 업로드를 예약하지 않아야 한다`() =
        runTest {
            dao.put(STORED)

            repository().syncFromRemote()

            assertEquals(listOf("server.getAll"), calls)
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
    fun `서버 감상문 응답이 시간 제한까지 오지 않으면 Room에 쓰지 않고 시간 제한이 지난 뒤 Unknown 실패를 반환해야 한다`() =
        runTest {
            realtime.suspendGetAll = true

            val result = repository().syncFromRemote()

            assertEquals(Outcome.Failure(ReviewError.Unknown), result)
            assertEquals(SERVER_READ_TIMEOUT.inWholeMilliseconds, currentTime)
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
    fun `감상문을 지우면 서버와 사진 파일은 지우지 않고 Room에 삭제 표시를 한 뒤 서버 업로드를 예약해야 한다`() =
        runTest {
            dao.put(STORED)

            val result = repository().delete(STORED.toReview())

            assertEquals(Outcome.Success(Unit), result)
            assertEquals(listOf("room.markPendingDelete", "sync.schedule"), calls)
            assertEquals(ReviewSyncState.PENDING_DELETE, dao.entity(STORED.id)?.syncState)
            assertEquals(STORED.localVersion, dao.entity(STORED.id)?.localVersion)
        }

    @Test
    fun `Room에 삭제 표시를 하지 못하면 서버 업로드를 예약하지 않고 Unknown 실패를 반환해야 한다`() =
        runTest {
            dao.put(STORED)
            dao.failMarkPendingDelete = true

            val result = repository().delete(STORED.toReview())

            assertEquals(Outcome.Failure(ReviewError.Unknown), result)
            assertEquals(listOf("room.markPendingDelete"), calls)
            assertEquals(ReviewSyncState.SYNCED, dao.entity(STORED.id)?.syncState)
        }

    @Test
    fun `지운 감상문을 수정하면 저장하거나 서버 업로드를 예약하지 않고 Unknown 실패를 반환해야 한다`() =
        runTest {
            dao.put(STORED)
            repository().delete(STORED.toReview())
            calls.clear()

            val result = repository().save(editDraft(ReviewPhoto.New("content://photo/2")))

            assertEquals(Outcome.Failure(ReviewError.Unknown), result)
            assertEquals(emptyList<String>(), calls)
            assertEquals(ReviewSyncState.PENDING_DELETE, dao.entity(STORED.id)?.syncState)
        }

    @Test
    fun `지운 감상문이 서버에 남아 있어도 서버 감상문을 받아 오면 다시 추가하지 않아야 한다`() =
        runTest {
            dao.put(STORED)
            realtime.remote[STORED.id] = STORED.toReview()
            repository().delete(STORED.toReview())

            repository().syncFromRemote()

            assertEquals(ReviewSyncState.PENDING_DELETE, dao.entity(STORED.id)?.syncState)
        }

    @Test
    fun `업로드 작업을 취소하면 Room, 서버, Storage를 건드리지 않고 대기 중인 감상문을 그대로 두어야 한다`() =
        runTest {
            dao.put(STORED)
            repository().delete(STORED.toReview())
            calls.clear()

            repository().cancelPendingSync()

            assertEquals(listOf("sync.cancel"), calls)
            assertEquals(ReviewSyncState.PENDING_DELETE, dao.entity(STORED.id)?.syncState)
        }

    private fun repository(userId: String? = USER_ID): ReviewRepositoryImpl =
        ReviewRepositoryImpl(
            reviewDao = dao,
            realtimeDataSource = realtime,
            storageDataSource = storage,
            authRepository = FakeReviewAuthRepository(userId),
            clock = { NOW_MILLIS },
            syncScheduler = scheduler,
            syncLock = ReviewSyncLock(),
        )
}

private const val USER_ID = "user-1234567890"
private const val NOW_MILLIS = 1_700_000_000_000L
private const val NEW_FILE_NAME = "$USER_ID/$NOW_MILLIS"
private const val NEW_KEY = "-new-key"

// 저장된 createdAt이 지금 시각이나 저장된 updatedAt과 같으면,
// 수정할 때 createdAt을 지금 시각이나 기존 updatedAt으로 잘못 덮어써도 기대값과 같아져 테스트가 통과하므로,
// 저장된 감상문의 createdAt과 updatedAt을 지금 시각보다 이전의 서로 다른 값으로 적용
private const val STORED_CREATED_AT = 1_600_000_000_000L
private const val STORED_UPDATED_AT = 1_650_000_000_000L

private val STORED =
    ReviewEntity(
        title = "기생충",
        image = downloadUrl("old.png"),
        content = "잘 봤다",
        createdAt = STORED_CREATED_AT,
        updatedAt = STORED_UPDATED_AT,
        rating = 9.0,
        storageFileName = "old.png",
        storageFileNameToDelete = "",
        syncState = ReviewSyncState.SYNCED,
        // 저장된 localVersion이 0이면 수정한 뒤 기대하는 localVersion이 새 감상문과 같은 1이라 수정할 때도 1로 저장하는 잘못된 코드가 통과하므로,
        // 그 코드를 잡아내도록 0이 아닌 값 적용
        localVersion = 1,
        id = "-stored",
    )

// 사진을 old.png에서 middle.png로 바꾼 수정이 아직 서버에 올라가지 않아, 서버는 old.png를 가리키고 Room은 middle.png를 가리키는 감상문
private val STORED_WITH_PHOTO_TO_DELETE =
    ReviewEntity(
        title = STORED.title,
        image = downloadUrl("middle.png"),
        content = STORED.content,
        createdAt = STORED_CREATED_AT,
        updatedAt = STORED_UPDATED_AT,
        rating = STORED.rating,
        storageFileName = "middle.png",
        storageFileNameToDelete = "old.png",
        syncState = ReviewSyncState.PENDING_UPSERT,
        localVersion = 2,
        id = STORED.id,
    )

private fun editDraft(photo: ReviewPhoto): ReviewDraft =
    ReviewDraft(id = STORED.id, title = "수정한 제목", content = "잘 봤다", rating = 9.0, photo = photo)

private fun newDraft(photo: ReviewPhoto): ReviewDraft =
    ReviewDraft(id = null, title = "새 감상문", content = "재밌다", rating = 7.0, photo = photo)

private fun downloadUrl(fileName: String): String = "https://storage.example.com/$fileName"

private class FakeReviewDao(
    private val calls: MutableList<String>,
) : ReviewDao {
    private val entities = mutableMapOf<String, ReviewEntity>()

    var failMarkPendingDelete: Boolean = false

    fun put(entity: ReviewEntity) {
        entities[entity.id] = entity
    }

    fun saved(id: String): Review? = entities[id]?.toReview()

    fun entity(id: String): ReviewEntity? = entities[id]

    // Room의 OnConflictStrategy.IGNORE처럼 같은 id가 있으면 기존 행을 두도록, 없는 id만 추가 적용
    override suspend fun insert(review: ReviewEntity) {
        calls += "room.insert"
        if (review.id !in entities) entities[review.id] = review
    }

    override suspend fun markPendingDelete(id: String) {
        calls += "room.markPendingDelete"
        if (failMarkPendingDelete) throw IllegalStateException("Room 삭제 표시 실패")
        entities[id]?.let { entities[id] = it.withSyncState(ReviewSyncState.PENDING_DELETE) }
    }

    override suspend fun update(review: ReviewEntity) {
        calls += "room.update"
        entities[review.id] = review
    }

    // 실제 쿼리처럼 지운 감상문을 수정하려는 저장을 막도록, 삭제 표시된 감상문 제외 적용
    override suspend fun getById(id: String): ReviewEntity? =
        entities[id]?.takeIf { it.syncState != ReviewSyncState.PENDING_DELETE }

    override suspend fun deleteAll() = error("사용하지 않음")

    override fun getAll(): Flow<List<ReviewEntity>> = error("사용하지 않음")

    override suspend fun getPending(): List<ReviewEntity> = error("사용하지 않음")

    override suspend fun countPending(): Int = entities.values.count { it.syncState != ReviewSyncState.SYNCED }

    override suspend fun markSynced(
        id: String,
        localVersion: Int,
    ): Int = error("사용하지 않음")

    override suspend fun clearStorageFileNameToDelete(
        id: String,
        fileName: String,
    ): Int = error("사용하지 않음")

    override suspend fun deleteIfPendingDelete(id: String): Int = error("사용하지 않음")
}

private class FakeReviewRealtimeDataSource(
    private val calls: MutableList<String>,
) : ReviewRealtimeDataSource {
    // 동기화로 받아 올 서버 감상문을 키별로 두도록 서버 상태 적용
    val remote = mutableMapOf<String, Review>()

    var failNewKey: Boolean = false

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
        return remote.values.toList()
    }

    override fun newKey(userId: String): String {
        calls += "server.newKey"
        if (failNewKey) throw IllegalStateException("키 생성 실패")
        return NEW_KEY
    }

    override suspend fun save(
        userId: String,
        review: Review,
    ) {
        calls += "server.save"
        remote[review.id] = review
    }

    override suspend fun delete(
        userId: String,
        reviewId: String,
    ) = error("사용하지 않음")
}

private class FakeReviewStorageDataSource(
    private val calls: MutableList<String>,
) : ReviewStorageDataSource {
    var failUpload: Boolean = false

    // Firebase Storage SDK가 업로드를 취소하면 저장을 요청한 코루틴이 취소되지 않아도 취소 예외가 올라오므로,
    // 코루틴 취소 없이 업로드에서 CancellationException 발생 적용
    var cancelUpload: Boolean = false

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
    }
}

private fun ReviewEntity.withSyncState(syncState: ReviewSyncState): ReviewEntity =
    ReviewEntity(
        title = title,
        image = image,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt,
        rating = rating,
        storageFileName = storageFileName,
        storageFileNameToDelete = storageFileNameToDelete,
        syncState = syncState,
        localVersion = localVersion,
        id = id,
    )

private class FakeReviewSyncScheduler(
    private val calls: MutableList<String>,
) : ReviewSyncScheduler {
    override fun schedule() {
        calls += "sync.schedule"
    }

    override fun cancel() {
        calls += "sync.cancel"
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
