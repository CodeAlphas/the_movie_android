package com.codealphas.themovie.data.review.sync

import com.codealphas.themovie.data.review.local.ReviewDao
import com.codealphas.themovie.data.review.local.ReviewEntity
import com.codealphas.themovie.data.review.local.ReviewSyncState
import com.codealphas.themovie.data.review.remote.ReviewPermissionDeniedException
import com.codealphas.themovie.data.review.remote.ReviewRealtimeDataSource
import com.codealphas.themovie.data.review.remote.ReviewStorageDataSource
import com.codealphas.themovie.domain.auth.AuthRepository
import com.codealphas.themovie.domain.auth.AuthResult
import com.codealphas.themovie.domain.review.Review
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

class ReviewUploaderTest {
    private val calls = mutableListOf<String>()
    private val dao = FakeReviewDao(calls)
    private val realtime = FakeReviewRealtimeDataSource(calls)
    private val storage = FakeReviewStorageDataSource(calls)

    @Test
    fun `대기 중인 감상문을 서버에 쓰면 SYNCED로 바꾸고 성공을 반환해야 한다`() =
        runTest {
            dao.put(pending(id = "-a", title = "기생충", localVersion = 1))

            val result = uploader().upload()

            assertEquals(ReviewUploadResult.SUCCESS, result)
            assertEquals(listOf("-a"), realtime.savedIds)
            assertEquals(USER_ID, realtime.savedUserIds.single())
            assertEquals("기생충", realtime.remote["-a"]?.title)
            assertEquals(ReviewSyncState.SYNCED, dao.entity("-a")?.syncState)
        }

    @Test
    fun `업로드하는 동안 감상문을 다시 수정하면 PENDING_UPSERT로 남겨야 한다`() =
        runTest {
            dao.put(pending(id = "-a", title = "처음 제목", localVersion = 1))
            // 서버 쓰기가 끝나기 전에 사용자가 같은 감상문을 다시 저장한 상황을 만들도록 쓰는 도중 Room 행 교체 적용
            realtime.onSave = { dao.put(pending(id = "-a", title = "고친 제목", localVersion = 2)) }

            val result = uploader().upload()

            assertEquals(ReviewUploadResult.SUCCESS, result)
            assertEquals(ReviewSyncState.PENDING_UPSERT, dao.entity("-a")?.syncState)
            assertEquals("고친 제목", dao.entity("-a")?.title)
        }

    @Test
    fun `서버 쓰기가 네트워크 오류로 실패하면 PENDING_UPSERT로 남기고 재시도를 반환해야 한다`() =
        runTest {
            dao.put(pending(id = "-a"))
            realtime.failures["-a"] = IOException("네트워크 오류")

            val result = uploader().upload()

            assertEquals(ReviewUploadResult.RETRY, result)
            assertEquals(ReviewSyncState.PENDING_UPSERT, dao.entity("-a")?.syncState)
        }

    @Test
    fun `서버가 응답하지 않으면 30초 뒤 PENDING_UPSERT로 남기고 재시도를 반환해야 한다`() =
        runTest {
            dao.put(pending(id = "-a"))
            realtime.hangingIds += "-a"

            val result = uploader().upload()

            assertEquals(ReviewUploadResult.RETRY, result)
            assertEquals(SERVER_WRITE_TIMEOUT.inWholeMilliseconds, currentTime)
            assertEquals(ReviewSyncState.PENDING_UPSERT, dao.entity("-a")?.syncState)
        }

    @Test
    fun `서버가 권한을 거부하면 PENDING_UPSERT로 남기고 영구 실패를 반환해야 한다`() =
        runTest {
            dao.put(pending(id = "-a"))
            realtime.failures["-a"] = ReviewPermissionDeniedException(IllegalStateException("Permission denied"))

            val result = uploader().upload()

            assertEquals(ReviewUploadResult.PERMANENT_FAILURE, result)
            assertEquals(ReviewSyncState.PENDING_UPSERT, dao.entity("-a")?.syncState)
        }

    @Test
    fun `세 감상문 중 가운데를 서버가 거부하면 나머지 두 감상문은 SYNCED로 바꾸고 영구 실패를 반환해야 한다`() =
        runTest {
            dao.put(pending(id = "-a", updatedAt = 1_000L))
            dao.put(pending(id = "-b", updatedAt = 2_000L))
            dao.put(pending(id = "-c", updatedAt = 3_000L))
            realtime.failures["-b"] = ReviewPermissionDeniedException(IllegalStateException("Permission denied"))

            val result = uploader().upload()

            assertEquals(ReviewUploadResult.PERMANENT_FAILURE, result)
            assertEquals(listOf("-a", "-b", "-c"), realtime.savedIds)
            assertEquals(ReviewSyncState.SYNCED, dao.entity("-a")?.syncState)
            assertEquals(ReviewSyncState.PENDING_UPSERT, dao.entity("-b")?.syncState)
            assertEquals(ReviewSyncState.SYNCED, dao.entity("-c")?.syncState)
        }

    @Test
    fun `세 감상문 중 가운데의 서버 쓰기가 네트워크 오류로 실패하면 세 번째 감상문은 올리지 않고 재시도를 반환해야 한다`() =
        runTest {
            dao.put(pending(id = "-a", updatedAt = 1_000L))
            dao.put(pending(id = "-b", updatedAt = 2_000L))
            dao.put(pending(id = "-c", updatedAt = 3_000L))
            realtime.failures["-b"] = IOException("네트워크 오류")

            val result = uploader().upload()

            assertEquals(ReviewUploadResult.RETRY, result)
            assertEquals(listOf("-a", "-b"), realtime.savedIds)
            assertEquals(ReviewSyncState.SYNCED, dao.entity("-a")?.syncState)
            assertEquals(ReviewSyncState.PENDING_UPSERT, dao.entity("-c")?.syncState)
        }

    @Test
    fun `삭제 표시된 감상문은 서버에서 지운 뒤 사진 파일과 행을 지우고 성공을 반환해야 한다`() =
        runTest {
            dao.put(pending(id = "-a", syncState = ReviewSyncState.PENDING_DELETE, storageFileName = PHOTO))

            val result = uploader().upload()

            assertEquals(ReviewUploadResult.SUCCESS, result)
            assertEquals(listOf("server.delete:-a", "storage.delete:$PHOTO", "room.delete:-a"), calls)
            assertEquals(USER_ID, realtime.deletedUserIds.single())
            assertNull(dao.entity("-a"))
        }

    @Test
    fun `사진 없는 감상문을 지우면 서버에 쓰지 않고 Storage도 부르지 않은 채 서버에서 지운 뒤 행을 지워야 한다`() =
        runTest {
            dao.put(pending(id = "-a", syncState = ReviewSyncState.PENDING_DELETE))

            val result = uploader().upload()

            assertEquals(ReviewUploadResult.SUCCESS, result)
            assertEquals(emptyList<String>(), realtime.savedIds)
            assertEquals(listOf("server.delete:-a", "room.delete:-a"), calls)
            assertNull(dao.entity("-a"))
        }

    @Test
    fun `업로드하는 동안 감상문을 지우면 SYNCED로 바꾸지 않고 PENDING_DELETE로 남겨야 한다`() =
        runTest {
            dao.put(pending(id = "-a", localVersion = 1))
            // 서버 쓰기가 끝나기 전에 사용자가 같은 감상문을 지운 상황을 만들도록 쓰는 도중 삭제 표시 적용
            realtime.onSave = {
                dao.put(pending(id = "-a", syncState = ReviewSyncState.PENDING_DELETE, localVersion = 1))
            }

            val result = uploader().upload()

            assertEquals(ReviewUploadResult.SUCCESS, result)
            assertEquals(ReviewSyncState.PENDING_DELETE, dao.entity("-a")?.syncState)
        }

    @Test
    fun `서버가 삭제에 응답하지 않으면 30초 뒤 행과 사진 파일을 남기고 재시도를 반환해야 한다`() =
        runTest {
            dao.put(pending(id = "-a", syncState = ReviewSyncState.PENDING_DELETE, storageFileName = PHOTO))
            realtime.hangingIds += "-a"

            val result = uploader().upload()

            assertEquals(ReviewUploadResult.RETRY, result)
            assertEquals(SERVER_WRITE_TIMEOUT.inWholeMilliseconds, currentTime)
            assertEquals(listOf("server.delete:-a"), calls)
            assertEquals(ReviewSyncState.PENDING_DELETE, dao.entity("-a")?.syncState)
        }

    @Test
    fun `보안 규칙이 서버 삭제를 거부하면 행과 사진 파일을 남기고 영구 실패를 반환해야 한다`() =
        runTest {
            dao.put(pending(id = "-a", syncState = ReviewSyncState.PENDING_DELETE, storageFileName = PHOTO))
            realtime.failures["-a"] = ReviewPermissionDeniedException(IllegalStateException("Permission denied"))

            val result = uploader().upload()

            assertEquals(ReviewUploadResult.PERMANENT_FAILURE, result)
            assertEquals(listOf("server.delete:-a"), calls)
            assertEquals(ReviewSyncState.PENDING_DELETE, dao.entity("-a")?.syncState)
        }

    @Test
    fun `사진 파일 삭제가 네트워크 오류로 실패하면 행을 남기고 재시도를 반환해야 한다`() =
        runTest {
            dao.put(pending(id = "-a", syncState = ReviewSyncState.PENDING_DELETE, storageFileName = PHOTO))
            storage.failure = IOException("네트워크 오류")

            val result = uploader().upload()

            assertEquals(ReviewUploadResult.RETRY, result)
            assertEquals(listOf("server.delete:-a", "storage.delete:$PHOTO"), calls)
            assertEquals(ReviewSyncState.PENDING_DELETE, dao.entity("-a")?.syncState)
        }

    @Test
    fun `Storage가 사진 파일 삭제에 응답하지 않으면 30초 뒤 행을 남기고 재시도를 반환해야 한다`() =
        runTest {
            dao.put(pending(id = "-a", syncState = ReviewSyncState.PENDING_DELETE, storageFileName = PHOTO))
            storage.hanging = true

            val result = uploader().upload()

            assertEquals(ReviewUploadResult.RETRY, result)
            assertEquals(SERVER_WRITE_TIMEOUT.inWholeMilliseconds, currentTime)
            assertEquals(listOf("server.delete:-a", "storage.delete:$PHOTO"), calls)
            assertEquals(ReviewSyncState.PENDING_DELETE, dao.entity("-a")?.syncState)
        }

    @Test
    fun `보안 규칙이 사진 파일 삭제를 거부하면 사진 파일은 두고 행을 지운 뒤 성공을 반환해야 한다`() =
        runTest {
            dao.put(pending(id = "-a", syncState = ReviewSyncState.PENDING_DELETE, storageFileName = PHOTO))
            storage.failure = ReviewPermissionDeniedException(IllegalStateException("Permission denied"))

            val result = uploader().upload()

            assertEquals(ReviewUploadResult.SUCCESS, result)
            assertEquals(listOf("server.delete:-a", "storage.delete:$PHOTO", "room.delete:-a"), calls)
            assertNull(dao.entity("-a"))
        }

    @Test
    fun `사진을 바꾼 감상문을 서버에 쓰면 그 뒤 이전 사진 파일을 지우고 이름을 빈 문자열로 만든 다음 SYNCED로 바꿔야 한다`() =
        runTest {
            dao.put(pending(id = "-a", storageFileName = PHOTO, storageFileNameToDelete = OLD_PHOTO))

            val result = uploader().upload()

            assertEquals(ReviewUploadResult.SUCCESS, result)
            assertEquals(listOf("server.save:-a", "storage.delete:$OLD_PHOTO"), calls)
            assertEquals("", dao.entity("-a")?.storageFileNameToDelete)
            assertEquals(ReviewSyncState.SYNCED, dao.entity("-a")?.syncState)
        }

    @Test
    fun `사진을 바꾼 감상문의 서버 쓰기가 네트워크 오류로 실패하면 이전 사진 파일을 지우지 않고 이름을 남겨야 한다`() =
        runTest {
            dao.put(pending(id = "-a", storageFileName = PHOTO, storageFileNameToDelete = OLD_PHOTO))
            realtime.failures["-a"] = IOException("네트워크 오류")

            val result = uploader().upload()

            assertEquals(ReviewUploadResult.RETRY, result)
            assertEquals(listOf("server.save:-a"), calls)
            assertEquals(OLD_PHOTO, dao.entity("-a")?.storageFileNameToDelete)
            assertEquals(ReviewSyncState.PENDING_UPSERT, dao.entity("-a")?.syncState)
        }

    @Test
    fun `이전 사진 파일 삭제가 네트워크 오류로 실패하면 이름을 남기고 PENDING_UPSERT로 둔 채 재시도를 반환해야 한다`() =
        runTest {
            dao.put(pending(id = "-a", storageFileName = PHOTO, storageFileNameToDelete = OLD_PHOTO))
            storage.failure = IOException("네트워크 오류")

            val result = uploader().upload()

            assertEquals(ReviewUploadResult.RETRY, result)
            assertEquals(listOf("server.save:-a", "storage.delete:$OLD_PHOTO"), calls)
            assertEquals(OLD_PHOTO, dao.entity("-a")?.storageFileNameToDelete)
            assertEquals(ReviewSyncState.PENDING_UPSERT, dao.entity("-a")?.syncState)
        }

    @Test
    fun `보안 규칙이 이전 사진 파일 삭제를 거부하면 파일은 두고 이름을 빈 문자열로 만든 뒤 SYNCED로 바꿔야 한다`() =
        runTest {
            dao.put(pending(id = "-a", storageFileName = PHOTO, storageFileNameToDelete = OLD_PHOTO))
            storage.failure = ReviewPermissionDeniedException(IllegalStateException("Permission denied"))

            val result = uploader().upload()

            assertEquals(ReviewUploadResult.SUCCESS, result)
            assertEquals("", dao.entity("-a")?.storageFileNameToDelete)
            assertEquals(ReviewSyncState.SYNCED, dao.entity("-a")?.syncState)
        }

    @Test
    fun `사진을 바꾼 수정을 올리기 전에 지운 감상문은 서버에서 지운 뒤 지금 사진과 이전 사진 파일을 모두 지워야 한다`() =
        runTest {
            dao.put(
                pending(
                    id = "-a",
                    syncState = ReviewSyncState.PENDING_DELETE,
                    storageFileName = PHOTO,
                    storageFileNameToDelete = OLD_PHOTO,
                ),
            )

            val result = uploader().upload()

            assertEquals(ReviewUploadResult.SUCCESS, result)
            assertEquals(
                listOf("server.delete:-a", "storage.delete:$PHOTO", "storage.delete:$OLD_PHOTO", "room.delete:-a"),
                calls,
            )
            assertNull(dao.entity("-a"))
        }

    @Test
    fun `로그인하지 않은 상태면 서버에 쓰지 않고 성공을 반환해야 한다`() =
        runTest {
            dao.put(pending(id = "-a"))

            val result = uploader(userId = null).upload()

            assertEquals(ReviewUploadResult.SUCCESS, result)
            assertEquals(emptyList<String>(), realtime.savedIds)
            assertEquals(ReviewSyncState.PENDING_UPSERT, dao.entity("-a")?.syncState)
        }

    private fun uploader(userId: String? = USER_ID): ReviewUploader =
        ReviewUploader(
            reviewDao = dao,
            realtimeDataSource = realtime,
            storageDataSource = storage,
            authRepository = FakeUploaderAuthRepository(userId),
            syncLock = ReviewSyncLock(),
        )
}

private const val USER_ID = "user-1234567890"
private const val PHOTO = "$USER_ID/1000.png"
private const val OLD_PHOTO = "$USER_ID/500.png"

private fun pending(
    id: String,
    title: String = "감상문",
    updatedAt: Long = 1_000L,
    syncState: ReviewSyncState = ReviewSyncState.PENDING_UPSERT,
    localVersion: Int = 1,
    storageFileName: String = "",
    storageFileNameToDelete: String = "",
): ReviewEntity =
    ReviewEntity(
        title = title,
        image = "",
        content = "잘 봤다",
        createdAt = 1_000L,
        updatedAt = updatedAt,
        rating = 8.0,
        storageFileName = storageFileName,
        storageFileNameToDelete = storageFileNameToDelete,
        syncState = syncState,
        localVersion = localVersion,
        id = id,
    )

// 상태와 버전 조건은 ReviewDaoTest가 실제 SQL로 확인하므로, 업로더 흐름만 보도록 ReviewDao 쿼리 조건을 메모리에서 같은 방식으로 적용
private class FakeReviewDao(
    private val calls: MutableList<String>,
) : ReviewDao {
    private val entities = mutableMapOf<String, ReviewEntity>()

    fun put(entity: ReviewEntity) {
        entities[entity.id] = entity
    }

    fun entity(id: String): ReviewEntity? = entities[id]

    override suspend fun getPending(): List<ReviewEntity> =
        entities.values
            .filter { it.syncState != ReviewSyncState.SYNCED }
            .sortedWith(compareBy(ReviewEntity::updatedAt, ReviewEntity::id))

    override suspend fun markSynced(
        id: String,
        localVersion: Int,
    ): Int {
        val current = entities[id]
        if (current == null ||
            current.localVersion != localVersion ||
            current.syncState != ReviewSyncState.PENDING_UPSERT
        ) {
            return 0
        }
        entities[id] = current.copy(syncState = ReviewSyncState.SYNCED)
        return 1
    }

    override suspend fun clearStorageFileNameToDelete(
        id: String,
        fileName: String,
    ): Int {
        val current = entities[id]
        if (current == null || current.storageFileNameToDelete != fileName) return 0
        entities[id] = current.copy(storageFileNameToDelete = "")
        return 1
    }

    override suspend fun deleteIfPendingDelete(id: String): Int {
        calls += "room.delete:$id"
        if (entities[id]?.syncState != ReviewSyncState.PENDING_DELETE) return 0
        entities -= id
        return 1
    }

    override suspend fun insert(review: ReviewEntity) = error("사용하지 않음")

    override suspend fun markPendingDelete(id: String) = error("사용하지 않음")

    override suspend fun update(review: ReviewEntity) = error("사용하지 않음")

    override suspend fun getById(id: String): ReviewEntity? = error("사용하지 않음")

    override suspend fun deleteAll() = error("사용하지 않음")

    override fun getAll(): Flow<List<ReviewEntity>> = error("사용하지 않음")

    override suspend fun countPending(): Int = error("사용하지 않음")
}

// ReviewEntity는 data class가 아니라 copy가 없으므로, 업로더가 바꾸는 두 컬럼만 바꾼 행을 만들도록 같은 이름의 함수 적용
private fun ReviewEntity.copy(
    syncState: ReviewSyncState = this.syncState,
    storageFileNameToDelete: String = this.storageFileNameToDelete,
): ReviewEntity =
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

private class FakeReviewRealtimeDataSource(
    private val calls: MutableList<String>,
) : ReviewRealtimeDataSource {
    val savedIds = mutableListOf<String>()
    val savedUserIds = mutableListOf<String>()
    val deletedUserIds = mutableListOf<String>()
    val remote = mutableMapOf<String, Review>()

    // 감상문 id별로 서버 쓰기와 삭제가 던질 예외를 정하도록 실패 적용
    val failures = mutableMapOf<String, Exception>()

    // Realtime Database는 연결이 끊기면 쓰기와 삭제를 실패시키지 않고 붙잡아 두므로, 응답 없는 서버를 만들도록 요청 대기 적용
    val hangingIds = mutableSetOf<String>()

    var onSave: () -> Unit = {}

    override suspend fun save(
        userId: String,
        review: Review,
    ) {
        calls += "server.save:${review.id}"
        savedIds += review.id
        savedUserIds += userId
        failures[review.id]?.let { throw it }
        if (review.id in hangingIds) awaitCancellation()
        onSave()
        remote[review.id] = review
    }

    override suspend fun delete(
        userId: String,
        reviewId: String,
    ) {
        calls += "server.delete:$reviewId"
        deletedUserIds += userId
        failures[reviewId]?.let { throw it }
        if (reviewId in hangingIds) awaitCancellation()
        remote -= reviewId
    }

    override suspend fun getAll(userId: String): List<Review> = error("사용하지 않음")

    override fun newKey(userId: String): String = error("사용하지 않음")
}

private class FakeReviewStorageDataSource(
    private val calls: MutableList<String>,
) : ReviewStorageDataSource {
    var failure: Exception? = null
    var hanging = false

    override suspend fun delete(fileName: String) {
        calls += "storage.delete:$fileName"
        failure?.let { throw it }
        if (hanging) awaitCancellation()
    }

    override suspend fun upload(
        fileName: String,
        uri: String,
    ): String = error("사용하지 않음")
}

private class FakeUploaderAuthRepository(
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
