package com.codealphas.themovie.data.review.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.codealphas.themovie.data.database.AppDatabase
import com.codealphas.themovie.data.review.local.ReviewEntity
import com.codealphas.themovie.data.review.local.ReviewSyncState
import com.codealphas.themovie.data.review.remote.ReviewPermissionDeniedException
import com.codealphas.themovie.data.review.remote.ReviewRealtimeDataSource
import com.codealphas.themovie.data.review.remote.ReviewStorageDataSource
import com.codealphas.themovie.domain.auth.AuthRepository
import com.codealphas.themovie.domain.auth.AuthResult
import com.codealphas.themovie.domain.review.Review
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class ReviewSyncWorkerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val realtime = FakeSyncWorkerRealtimeDataSource()
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `대기 중인 감상문을 모두 올리면 success를 반환해야 한다`() =
        runTest {
            database.reviewDao().insert(pendingReview())

            val result = worker().doWork()

            assertEquals(ListenableWorker.Result.success(), result)
            assertEquals(ReviewSyncState.SYNCED, database.reviewDao().getById(REVIEW_ID)?.syncState)
        }

    @Test
    fun `서버 쓰기가 네트워크 오류로 실패하면 retry를 반환해야 한다`() =
        runTest {
            database.reviewDao().insert(pendingReview())
            realtime.failure = IOException("네트워크 오류")

            val result = worker().doWork()

            assertEquals(ListenableWorker.Result.retry(), result)
        }

    @Test
    fun `서버가 권한을 거부하면 감상문을 대기 상태로 두고 success를 반환해야 한다`() =
        runTest {
            database.reviewDao().insert(pendingReview())
            realtime.failure = ReviewPermissionDeniedException(IllegalStateException("Permission denied"))

            val result = worker().doWork()

            assertEquals(ListenableWorker.Result.success(), result)
            assertEquals(ReviewSyncState.PENDING_UPSERT, database.reviewDao().getById(REVIEW_ID)?.syncState)
        }

    // HiltWorkerFactory 없이 Fake 서버를 쓰는 업로더를 넣도록 Worker를 직접 만드는 WorkerFactory 적용
    private fun worker(): ReviewSyncWorker {
        val uploader =
            ReviewUploader(
                reviewDao = database.reviewDao(),
                realtimeDataSource = realtime,
                storageDataSource = FakeSyncWorkerStorageDataSource(),
                authRepository = FakeSyncWorkerAuthRepository(),
                syncLock = ReviewSyncLock(),
            )
        val factory =
            object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ): ListenableWorker = ReviewSyncWorker(appContext, workerParameters, uploader)
            }
        return TestListenableWorkerBuilder<ReviewSyncWorker>(context).setWorkerFactory(factory).build()
    }
}

private const val REVIEW_ID = "-a"

private fun pendingReview(): ReviewEntity =
    ReviewEntity(
        title = "기생충",
        image = "",
        content = "잘 봤다",
        createdAt = 1_000L,
        updatedAt = 1_000L,
        rating = 9.0,
        storageFileName = "",
        storageFileNameToDelete = "",
        syncState = ReviewSyncState.PENDING_UPSERT,
        localVersion = 1,
        id = REVIEW_ID,
    )

private class FakeSyncWorkerRealtimeDataSource : ReviewRealtimeDataSource {
    var failure: Exception? = null

    override suspend fun save(
        userId: String,
        review: Review,
    ) {
        failure?.let { throw it }
    }

    override suspend fun getAll(userId: String): List<Review> = error("사용하지 않음")

    override fun newKey(userId: String): String = error("사용하지 않음")

    override suspend fun delete(
        userId: String,
        reviewId: String,
    ) = error("사용하지 않음")
}

private class FakeSyncWorkerStorageDataSource : ReviewStorageDataSource {
    override suspend fun upload(
        fileName: String,
        uri: String,
    ): String = error("사용하지 않음")

    override suspend fun delete(fileName: String) = error("사용하지 않음")
}

private class FakeSyncWorkerAuthRepository : AuthRepository {
    override suspend fun signIn(
        email: String,
        password: String,
    ): AuthResult = error("사용하지 않음")

    override suspend fun signUp(
        email: String,
        password: String,
    ): AuthResult = error("사용하지 않음")

    override fun currentUserId(): String = "user-1234567890"

    override fun signOut() = error("사용하지 않음")
}
