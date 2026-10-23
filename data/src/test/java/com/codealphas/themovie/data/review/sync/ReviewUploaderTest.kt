package com.codealphas.themovie.data.review.sync

import com.codealphas.themovie.data.review.local.ReviewDao
import com.codealphas.themovie.data.review.local.ReviewEntity
import com.codealphas.themovie.data.review.local.ReviewSyncState
import com.codealphas.themovie.data.review.remote.ReviewPermissionDeniedException
import com.codealphas.themovie.data.review.remote.ReviewRealtimeDataSource
import com.codealphas.themovie.domain.auth.AuthRepository
import com.codealphas.themovie.domain.auth.AuthResult
import com.codealphas.themovie.domain.review.Review
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class ReviewUploaderTest {
    private val dao = FakeReviewDao()
    private val realtime = FakeReviewRealtimeDataSource()

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
    fun `삭제 표시된 감상문은 서버에 쓰지 않아야 한다`() =
        runTest {
            dao.put(pending(id = "-a", syncState = ReviewSyncState.PENDING_DELETE))

            val result = uploader().upload()

            assertEquals(ReviewUploadResult.SUCCESS, result)
            assertEquals(emptyList<String>(), realtime.savedIds)
            assertEquals(ReviewSyncState.PENDING_DELETE, dao.entity("-a")?.syncState)
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
            authRepository = FakeUploaderAuthRepository(userId),
        )
}

private const val USER_ID = "user-1234567890"

private fun pending(
    id: String,
    title: String = "감상문",
    updatedAt: Long = 1_000L,
    syncState: ReviewSyncState = ReviewSyncState.PENDING_UPSERT,
    localVersion: Int = 1,
): ReviewEntity =
    ReviewEntity(
        title = title,
        image = "",
        content = "잘 봤다",
        createdAt = 1_000L,
        updatedAt = updatedAt,
        rating = 8.0,
        storageFileName = "",
        syncState = syncState,
        localVersion = localVersion,
        id = id,
    )

// 상태와 버전 조건은 ReviewDaoTest가 실제 SQL로 확인하므로, 업로더 흐름만 보도록 ReviewDao 쿼리 조건을 메모리에서 같은 방식으로 적용
private class FakeReviewDao : ReviewDao {
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
        entities[id] = current.withSyncState(ReviewSyncState.SYNCED)
        return 1
    }

    override suspend fun insert(review: ReviewEntity) = error("사용하지 않음")

    override suspend fun delete(review: ReviewEntity) = error("사용하지 않음")

    override suspend fun update(review: ReviewEntity) = error("사용하지 않음")

    override suspend fun getById(id: String): ReviewEntity? = error("사용하지 않음")

    override suspend fun deleteAll() = error("사용하지 않음")

    override fun getAll(): Flow<List<ReviewEntity>> = error("사용하지 않음")

    override suspend fun deleteIfPendingDelete(id: String): Int = error("사용하지 않음")
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
        syncState = syncState,
        localVersion = localVersion,
        id = id,
    )

private class FakeReviewRealtimeDataSource : ReviewRealtimeDataSource {
    val savedIds = mutableListOf<String>()
    val savedUserIds = mutableListOf<String>()
    val remote = mutableMapOf<String, Review>()

    // 감상문 id별로 서버 쓰기가 던질 예외를 정하도록 실패 적용
    val failures = mutableMapOf<String, Exception>()

    // Realtime Database는 연결이 끊기면 쓰기를 실패시키지 않고 붙잡아 두므로, 응답 없는 서버를 만들도록 쓰기 대기 적용
    val hangingIds = mutableSetOf<String>()

    var onSave: () -> Unit = {}

    override suspend fun save(
        userId: String,
        review: Review,
    ) {
        savedIds += review.id
        savedUserIds += userId
        failures[review.id]?.let { throw it }
        if (review.id in hangingIds) awaitCancellation()
        onSave()
        remote[review.id] = review
    }

    override suspend fun getAll(userId: String): List<Review> = error("사용하지 않음")

    override fun newKey(userId: String): String = error("사용하지 않음")

    override fun delete(
        userId: String,
        reviewId: String,
    ) = error("사용하지 않음")
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
