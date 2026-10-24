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
import com.codealphas.themovie.domain.review.ReviewDraft
import com.codealphas.themovie.domain.review.ReviewPhoto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

// 감상문 사진을 바꾸거나 지운 뒤 업로드하면, 서버 감상문이 쓰는 사진은 Storage에 남고 쓰지 않는 사진만 지워지는지 확인
// 감상문 저장(ReviewRepositoryImpl)과 업로드(ReviewUploader)가 고칠 행은 Room 쿼리 조건이 정하므로,
// 그 조건까지 확인하도록 가짜 DAO 대신 실제 Room 사용
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ReviewPhotoCleanupTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: AppDatabase
    private lateinit var dao: ReviewDao
    private val realtime = CleanupRealtimeDataSource()
    private val storage = CleanupStorageDataSource()
    private var now = FIRST_SAVE
    private val syncLock = ReviewSyncLock()

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
                .allowMainThreadQueries()
                // Room이 쿼리를 다른 스레드에서 실행하면 runTest가 실행할 코루틴이 없다고 보고 가상 시간을 서버 쓰기 시간 제한까지 넘기므로,
                // 업로드가 시간 초과로 끊기지 않도록 쿼리를 테스트 스케줄러에서 실행
                .setQueryCoroutineContext(testDispatcher)
                .build()
        dao = database.reviewDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `사진을 바꿔 저장하고 감상문을 업로드하기 전이면 서버 감상문이 가리키는 이전 사진이 Storage에 남아 있어야 한다`() =
        runTest(testDispatcher) {
            val id = uploadedReviewWithPhoto()

            save(id, ReviewPhoto.New("content://photo/b"))

            assertServerPhotoExists(id)
            assertEquals(setOf(FILE_A, fileAt(SECOND_SAVE)), storage.files)
        }

    @Test
    fun `사진을 바꾼 감상문을 업로드하면 서버는 새 사진을 가리키고 이전 사진 파일은 Storage에서 지워져야 한다`() =
        runTest(testDispatcher) {
            val id = uploadedReviewWithPhoto()
            save(id, ReviewPhoto.New("content://photo/b"))

            assertEquals(ReviewUploadResult.SUCCESS, uploader().upload())

            assertEquals(fileAt(SECOND_SAVE), realtime.remote[id]?.storageFileName)
            assertEquals(setOf(fileAt(SECOND_SAVE)), storage.files)
            assertEquals("", dao.getById(id)?.storageFileNameToDelete)
        }

    @Test
    fun `감상문을 업로드하기 전에 사진을 두 번 바꾸면 중간 사진만 바로 지우고 감상문 업로드 뒤 마지막 사진만 남아야 한다`() =
        runTest(testDispatcher) {
            val id = uploadedReviewWithPhoto()
            save(id, ReviewPhoto.New("content://photo/b"))
            save(id, ReviewPhoto.New("content://photo/c"))

            assertServerPhotoExists(id)
            assertEquals(setOf(FILE_A, fileAt(THIRD_SAVE)), storage.files)

            uploader().upload()

            assertEquals(fileAt(THIRD_SAVE), realtime.remote[id]?.storageFileName)
            assertEquals(setOf(fileAt(THIRD_SAVE)), storage.files)
        }

    @Test
    fun `한 번도 업로드하지 않은 새 감상문에서 사진을 두 번 바꾸면 감상문 업로드 뒤 마지막 사진만 남아야 한다`() =
        runTest(testDispatcher) {
            realtime.nextKey = REVIEW_ID
            assertEquals(
                Outcome.Success(Unit),
                repository().save(draft(id = null, photo = ReviewPhoto.New("content://photo/a"))),
            )
            save(REVIEW_ID, ReviewPhoto.New("content://photo/b"))
            save(REVIEW_ID, ReviewPhoto.New("content://photo/c"))

            // 처음 사진은 감상문 업로드 뒤 지우도록 남기고, 그 사이에 고른 사진만 바로 지웠는지 확인
            assertEquals(setOf(FILE_A, fileAt(THIRD_SAVE)), storage.files)

            assertEquals(ReviewUploadResult.SUCCESS, uploader().upload())

            assertEquals(fileAt(THIRD_SAVE), realtime.remote[REVIEW_ID]?.storageFileName)
            assertEquals(setOf(fileAt(THIRD_SAVE)), storage.files)
        }

    @Test
    fun `사진을 바꾼 뒤 지우고 저장하면 감상문 업로드 뒤 서버 사진이 비고 Storage에 사진이 남지 않아야 한다`() =
        runTest(testDispatcher) {
            val id = uploadedReviewWithPhoto()
            save(id, ReviewPhoto.New("content://photo/b"))
            save(id, ReviewPhoto.Removed)

            uploader().upload()

            assertEquals("", realtime.remote[id]?.storageFileName)
            assertEquals(emptySet<String>(), storage.files)
        }

    @Test
    fun `사진을 지우고 업로드하기 전에 새 사진을 고르면 업로드 뒤 Storage에 새 사진만 남아야 한다`() =
        runTest(testDispatcher) {
            val id = uploadedReviewWithPhoto()
            save(id, ReviewPhoto.Removed)
            save(id, ReviewPhoto.New("content://photo/c"))

            uploader().upload()

            assertEquals(fileAt(THIRD_SAVE), realtime.remote[id]?.storageFileName)
            assertEquals(setOf(fileAt(THIRD_SAVE)), storage.files)
        }

    @Test
    fun `사진을 바꾼 감상문을 업로드하기 전에 지우면 업로더가 처리한 뒤 서버 감상문과 두 사진 파일이 모두 없어야 한다`() =
        runTest(testDispatcher) {
            val id = uploadedReviewWithPhoto()
            save(id, ReviewPhoto.New("content://photo/b"))
            repository().delete(checkNotNull(repository().getById(id)))

            uploader().upload()

            assertEquals(null, realtime.remote[id])
            assertEquals(emptySet<String>(), storage.files)
            assertEquals(null, dao.getById(id))
        }

    @Test
    fun `사진을 바꾼 뒤 감상문을 업로드하기 전에 로그아웃하면 서버 감상문이 가리키는 이전 사진이 Storage에 남아 있어야 한다`() =
        runTest(testDispatcher) {
            val id = uploadedReviewWithPhoto()
            save(id, ReviewPhoto.New("content://photo/b"))

            repository().deleteAll()
            uploader().upload()

            assertServerPhotoExists(id)
        }

    @Test
    fun `이전 사진 파일 삭제가 네트워크 오류로 실패하면 다음 감상문 업로드에서 다시 지워야 한다`() =
        runTest(testDispatcher) {
            val id = uploadedReviewWithPhoto()
            save(id, ReviewPhoto.New("content://photo/b"))
            storage.deleteFailure = IOException("네트워크 오류")

            assertEquals(ReviewUploadResult.RETRY, uploader().upload())
            assertEquals(setOf(FILE_A, fileAt(SECOND_SAVE)), storage.files)

            storage.deleteFailure = null
            assertEquals(ReviewUploadResult.SUCCESS, uploader().upload())

            assertEquals(setOf(fileAt(SECOND_SAVE)), storage.files)
            assertEquals("", dao.getById(id)?.storageFileNameToDelete)
        }

    @Test
    fun `감상문 업로드가 이전 사진을 지우기 전에 사진을 다시 바꾸면 다음 감상문 업로드가 끝난 뒤 마지막 사진만 남아야 한다`() =
        runTest(testDispatcher) {
            // 사진 A로 업로드된 감상문을 사진 B로 바꿔 저장
            val id = uploadedReviewWithPhoto()
            save(id, ReviewPhoto.New("content://photo/b"))
            // 업로더가 B를 서버에 쓴 직후, 업로드를 마무리하기 전에 사용자가 사진을 C로 다시 바꿔 저장
            realtime.afterSave = {
                save(id, ReviewPhoto.New("content://photo/c"))
                // 두 번째 업로드 때는 사진을 다시 바꾸지 않도록 비우기
                realtime.afterSave = {}
            }

            // 첫 업로드 도중 사진이 C로 바뀌었으므로, 업로드가 끝나도 SYNCED가 아니라 아직 올릴 변경이 있는 PENDING_UPSERT로 남는지 확인
            assertEquals(ReviewUploadResult.SUCCESS, uploader().upload())
            assertEquals(ReviewSyncState.PENDING_UPSERT, dao.getById(id)?.syncState)

            // 두 번째 업로드 뒤에는 서버가 C를 가리키고 Storage에도 C만 남는지 확인
            assertEquals(ReviewUploadResult.SUCCESS, uploader().upload())
            assertEquals(fileAt(THIRD_SAVE), realtime.remote[id]?.storageFileName)
            assertEquals(setOf(fileAt(THIRD_SAVE)), storage.files)
            assertEquals("", dao.getById(id)?.storageFileNameToDelete)
        }

    // 사진 A가 있는 감상문을 저장하고 감상문 업로드까지 마쳐, 서버와 Room이 모두 사진 A를 가리키는 상태 적용
    private suspend fun uploadedReviewWithPhoto(): String {
        realtime.nextKey = REVIEW_ID
        val result = repository().save(draft(id = null, photo = ReviewPhoto.New("content://photo/a")))
        assertEquals(Outcome.Success(Unit), result)
        assertEquals(ReviewUploadResult.SUCCESS, uploader().upload())
        assertEquals(FILE_A, realtime.remote[REVIEW_ID]?.storageFileName)
        return REVIEW_ID
    }

    private suspend fun save(
        id: String,
        photo: ReviewPhoto,
    ) {
        now += SAVE_INTERVAL
        assertEquals(Outcome.Success(Unit), repository().save(draft(id = id, photo = photo)))
    }

    private fun assertServerPhotoExists(id: String) {
        val fileName = checkNotNull(realtime.remote[id]).storageFileName
        assertTrue("서버 감상문이 가리키는 사진 $fileName 이 Storage에 있어야 함", fileName in storage.files)
    }

    private fun repository(): ReviewRepositoryImpl =
        ReviewRepositoryImpl(
            reviewDao = dao,
            realtimeDataSource = realtime,
            storageDataSource = storage,
            authRepository = CleanupAuthRepository(),
            clock = { now },
            syncScheduler = NoOpSyncScheduler,
            syncLock = syncLock,
        )

    private fun uploader(): ReviewUploader =
        ReviewUploader(
            reviewDao = dao,
            realtimeDataSource = realtime,
            storageDataSource = storage,
            authRepository = CleanupAuthRepository(),
            syncLock = syncLock,
        )
}

private const val USER_ID = "user-1"
private const val REVIEW_ID = "-review"
private const val FIRST_SAVE = 1_000L
private const val SAVE_INTERVAL = 1_000L
private const val SECOND_SAVE = FIRST_SAVE + SAVE_INTERVAL
private const val THIRD_SAVE = SECOND_SAVE + SAVE_INTERVAL
private val FILE_A = fileAt(FIRST_SAVE)

// 그 시각에 저장한 사진의 파일 이름을 ReviewRepositoryImpl과 같은 규칙("사용자 id/저장 시각.png")으로 생성
private fun fileAt(millis: Long): String = "$USER_ID/$millis.png"

private fun draft(
    id: String?,
    photo: ReviewPhoto,
): ReviewDraft = ReviewDraft(id = id, title = "기생충", content = "잘 봤다", rating = 9.0, photo = photo)

private class CleanupRealtimeDataSource : ReviewRealtimeDataSource {
    val remote = mutableMapOf<String, Review>()
    var nextKey = REVIEW_ID
    var afterSave: suspend () -> Unit = {}

    override suspend fun getAll(userId: String): List<Review> = remote.values.toList()

    override fun newKey(userId: String): String = nextKey

    override suspend fun save(
        userId: String,
        review: Review,
    ) {
        remote[review.id] = review
        afterSave()
    }

    override suspend fun delete(
        userId: String,
        reviewId: String,
    ) {
        remote -= reviewId
    }
}

// 테스트가 Storage에 남은 사진 파일을 확인할 수 있도록, 올린 파일 이름을 집합에 넣고 지우면 빼는 가짜 Storage
private class CleanupStorageDataSource : ReviewStorageDataSource {
    val files = mutableSetOf<String>()
    var deleteFailure: Exception? = null

    override suspend fun upload(
        fileName: String,
        uri: String,
    ): String {
        files += fileName
        return "https://storage.example.com/$fileName"
    }

    override suspend fun delete(fileName: String) {
        deleteFailure?.let { throw it }
        // 실제 Storage 데이터 소스도 없는 파일 삭제를 성공으로 처리하므로, 집합에 없는 이름이어도 예외 없이 진행
        files -= fileName
    }
}

private object NoOpSyncScheduler : ReviewSyncScheduler {
    override fun schedule() = Unit

    override fun cancel() = Unit
}

private class CleanupAuthRepository : AuthRepository {
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
