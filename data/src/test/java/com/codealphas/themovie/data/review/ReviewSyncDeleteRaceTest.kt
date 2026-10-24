package com.codealphas.themovie.data.review

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.codealphas.themovie.data.database.AppDatabase
import com.codealphas.themovie.data.review.local.ReviewDao
import com.codealphas.themovie.data.review.local.ReviewSyncState
import com.codealphas.themovie.data.review.remote.ReviewRealtimeDataSource
import com.codealphas.themovie.data.review.remote.ReviewStorageDataSource
import com.codealphas.themovie.data.review.sync.ReviewSyncLock
import com.codealphas.themovie.data.review.sync.ReviewSyncScheduler
import com.codealphas.themovie.data.review.sync.ReviewUploadResult
import com.codealphas.themovie.data.review.sync.ReviewUploader
import com.codealphas.themovie.domain.auth.AuthRepository
import com.codealphas.themovie.domain.auth.AuthResult
import com.codealphas.themovie.domain.result.Outcome
import com.codealphas.themovie.domain.review.Review
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// 감상문 목록의 동기화와 업로드가 동시에 실행돼도, 사용자가 지운 감상문이 목록에 다시 나타나지 않는지 확인
// 동기화(ReviewRepositoryImpl)와 업로드(ReviewUploader)가 고칠 행은 잠금과 Room 쿼리가 정하므로,
// 잠금과 쿼리 동작까지 확인하도록 가짜 DAO 대신 실제 Room 사용
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ReviewSyncDeleteRaceTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: AppDatabase
    private lateinit var dao: ReviewDao
    private val realtime = RaceRealtimeDataSource()
    private val syncLock = ReviewSyncLock()

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
                .allowMainThreadQueries()
                // Room이 쿼리를 다른 스레드에서 실행하면 업로더가 행을 지웠는지 테스트가 알 수 없으므로,
                // 동기화와 업로드가 멈춘 지점을 테스트가 정하도록 쿼리를 테스트 스케줄러에서 실행
                .setQueryCoroutineContext(testDispatcher)
                .build()
        dao = database.reviewDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `감상문을 지운 뒤 목록 동기화 도중에 서버 삭제가 시작되면 그 감상문이 목록에 다시 나타나지 않아야 한다`() =
        runTest(testDispatcher) {
            // 1. 서버에는 아직 있고 Room에는 삭제 대기 행으로 남은, 사용자가 방금 지운 감상문 준비
            realtime.remote[REVIEW.id] = REVIEW
            dao.insert(REVIEW.toEntity(syncState = ReviewSyncState.SYNCED, localVersion = 0))
            assertEquals(Outcome.Success(Unit), repository().delete(REVIEW))

            // 2. 지운 감상문이 든 서버 목록을 받은 직후 멈추도록 동기화 시작
            //    releaseSync는 테스트가 멈춘 동기화를 다시 움직이는 데 사용
            val releaseSync = CompletableDeferred<Unit>()
            realtime.afterGetAll = { releaseSync.await() }
            val sync = async { repository().syncFromRemote() }
            runCurrent()
            // 동기화가 서버 목록을 받기 전에 멈췄다면 업로더가 먼저 지워도 동기화 목록에 감상문이 없으므로, 목록을 받은 뒤 멈췄는지 확인
            assertEquals(1, realtime.getAllCount)

            // 3. 잠금이 빠지면 동기화가 멈춘 사이에 업로더가 감상문을 서버와 Room에서 지우므로,
            //    업로드를 시작하고 runCurrent로 업로더가 끝나거나 멈출 때까지 진행
            //    잠금이 있으면 업로더는 동기화가 잡은 잠금 앞에서 멈춘 채 4번으로 진행
            val upload = async { uploader().upload() }
            runCurrent()

            // 4. 2번에서 받은 서버 목록(지운 감상문 포함)을 Room에 넣도록 멈춘 동기화 재개
            releaseSync.complete(Unit)

            // 동기화와 업로드가 모두 끝난 뒤 지운 감상문이 서버와 Room 어디에도 없는지 확인
            assertEquals(Outcome.Success(Unit), sync.await())
            assertEquals(ReviewUploadResult.SUCCESS, upload.await())
            assertNull(realtime.remote[REVIEW.id])
            // getById는 삭제 대기 행을 돌려주지 않으므로, 대기 중인 변경 수가 0인지도 함께 확인
            assertNull(dao.getById(REVIEW.id))
            assertEquals(0, dao.countPending())
        }

    private fun repository(): ReviewRepositoryImpl =
        ReviewRepositoryImpl(
            reviewDao = dao,
            realtimeDataSource = realtime,
            storageDataSource = RaceStorageDataSource,
            authRepository = RaceAuthRepository,
            clock = { NOW_MILLIS },
            syncScheduler = RaceSyncScheduler,
            syncLock = syncLock,
        )

    private fun uploader(): ReviewUploader =
        ReviewUploader(
            reviewDao = dao,
            realtimeDataSource = realtime,
            storageDataSource = RaceStorageDataSource,
            authRepository = RaceAuthRepository,
            syncLock = syncLock,
        )
}

private const val USER_ID = "user-1"
private const val NOW_MILLIS = 1_700_000_000_000L

private val REVIEW =
    Review(
        title = "기생충",
        imageUrl = "",
        content = "잘 봤다",
        createdAt = NOW_MILLIS,
        updatedAt = NOW_MILLIS,
        rating = 9.0,
        storageFileName = "",
        id = "-review",
    )

private class RaceRealtimeDataSource : ReviewRealtimeDataSource {
    val remote = mutableMapOf<String, Review>()
    var afterGetAll: suspend () -> Unit = {}
    var getAllCount = 0

    override suspend fun getAll(userId: String): List<Review> {
        getAllCount++
        val reviews = remote.values.toList()
        afterGetAll()
        return reviews
    }

    override fun newKey(userId: String): String = error("사용하지 않음")

    override suspend fun save(
        userId: String,
        review: Review,
    ) {
        remote[review.id] = review
    }

    override suspend fun delete(
        userId: String,
        reviewId: String,
    ) {
        remote -= reviewId
    }
}

private object RaceStorageDataSource : ReviewStorageDataSource {
    override suspend fun upload(
        fileName: String,
        uri: String,
    ): String = error("사용하지 않음")

    override suspend fun delete(fileName: String) = Unit
}

private object RaceSyncScheduler : ReviewSyncScheduler {
    override fun schedule() = Unit

    override fun cancel() = Unit
}

private object RaceAuthRepository : AuthRepository {
    override suspend fun signIn(
        email: String,
        password: String,
    ): AuthResult = error("사용하지 않음")

    override suspend fun signUp(
        email: String,
        password: String,
    ): AuthResult = error("사용하지 않음")

    override fun currentUserId(): String = USER_ID

    override fun signOut() = Unit
}
