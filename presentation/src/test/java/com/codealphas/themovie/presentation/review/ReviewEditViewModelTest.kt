package com.codealphas.themovie.presentation.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.codealphas.themovie.domain.result.Outcome
import com.codealphas.themovie.domain.review.Review
import com.codealphas.themovie.domain.review.ReviewDraft
import com.codealphas.themovie.domain.review.ReviewError
import com.codealphas.themovie.domain.review.ReviewPhoto
import com.codealphas.themovie.domain.review.ReviewRepository
import com.codealphas.themovie.domain.review.ReviewResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewEditViewModelTest {
    @Test
    fun `제목이나 내용이 비어 있거나 공백뿐이면 입력 안내를 보내야 한다`() {
        val repository = FakeReviewEditRepository()
        runReviewEditTest(repository, SavedStateHandle()) { viewModel ->
            val events = collectReviewEditEvents(viewModel)

            viewModel.onTitleChange(" ")
            viewModel.onContentChange("잘 봤다")
            viewModel.save()
            viewModel.onTitleChange("기생충")
            viewModel.onContentChange("")
            viewModel.save()
            runCurrent()

            assertEquals(emptyList<ReviewDraft>(), repository.savedDrafts)
            assertEquals(listOf(ReviewEditEvent.InputRequired, ReviewEditEvent.InputRequired), events)
            assertFalse(viewModel.uiState.value.isSaving)
        }
    }

    @Test
    fun `저장 중에 저장 버튼을 다시 누르면 저장을 한 번만 요청해야 한다`() {
        val result = CompletableDeferred<ReviewResult>()
        val repository = FakeReviewEditRepository(saveResult = result)
        runReviewEditTest(repository, SavedStateHandle()) { viewModel ->
            fillInput(viewModel)

            viewModel.save()
            runCurrent()
            viewModel.save()
            runCurrent()

            assertEquals(1, repository.savedDrafts.size)
            assertTrue(viewModel.uiState.value.isSaving)
            result.complete(Outcome.Success(Unit))
        }
    }

    @Test
    fun `새 감상문을 저장하면 Repository에 id 없이 입력과 고른 사진을 넘기고 새 감상문 저장 완료를 보내야 한다`() {
        val repository = FakeReviewEditRepository(saveResult = CompletableDeferred(Outcome.Success(Unit)))
        runReviewEditTest(repository, SavedStateHandle()) { viewModel ->
            val events = collectReviewEditEvents(viewModel)
            fillInput(viewModel)
            viewModel.onPhotoSelected("content://photo/1")

            viewModel.save()
            runCurrent()

            val expected =
                ReviewDraft(
                    id = null,
                    title = "기생충",
                    content = "잘 봤다",
                    rating = 8.0,
                    photo = ReviewPhoto.New("content://photo/1"),
                )
            assertEquals(listOf(expected), repository.savedDrafts)
            assertEquals(listOf<ReviewEditEvent>(ReviewEditEvent.Saved(isNew = true)), events)
            // 성공 뒤 화면이 닫히기 전의 재클릭을 막으려고 isSaving을 유지하므로, 저장 중 상태가 그대로인지 확인
            assertTrue(viewModel.uiState.value.isSaving)
        }
    }

    @Test
    fun `저장에 실패하면 저장 중 상태를 풀고 실패 이유를 보내야 한다`() {
        val failure = Outcome.Failure(ReviewError.PhotoUploadFailed)
        val repository = FakeReviewEditRepository(saveResult = CompletableDeferred(failure))
        runReviewEditTest(repository, SavedStateHandle()) { viewModel ->
            val events = collectReviewEditEvents(viewModel)
            fillInput(viewModel)

            viewModel.save()
            runCurrent()

            assertEquals(listOf<ReviewEditEvent>(ReviewEditEvent.SaveFailed(ReviewError.PhotoUploadFailed)), events)
            assertFalse(viewModel.uiState.value.isSaving)
        }
    }

    @Test
    fun `수정 모드로 열면 Room의 감상문으로 입력 초기값을 채워야 한다`() {
        val repository = FakeReviewEditRepository(stored = STORED_REVIEW)
        runReviewEditTest(repository, editHandle(STORED_REVIEW.id)) { viewModel ->
            runCurrent()

            val expected =
                ReviewEditUiState(
                    title = STORED_REVIEW.title,
                    content = STORED_REVIEW.content,
                    rating = STORED_REVIEW.rating,
                    photo = ReviewPhoto.Unchanged,
                    savedImageUrl = STORED_REVIEW.image,
                    isEditing = true,
                )
            assertEquals(expected, viewModel.uiState.value)
            assertEquals(listOf(STORED_REVIEW.id), repository.requestedIds)
        }
    }

    @Test
    fun `수정 모드에서 저장하면 Repository에 같은 id와 그대로 둔 사진을 넘기고 수정 완료를 보내야 한다`() {
        val repository =
            FakeReviewEditRepository(stored = STORED_REVIEW, saveResult = CompletableDeferred(Outcome.Success(Unit)))
        runReviewEditTest(repository, editHandle(STORED_REVIEW.id)) { viewModel ->
            val events = collectReviewEditEvents(viewModel)
            runCurrent()

            viewModel.onTitleChange("기생충 다시 보기")
            viewModel.save()
            runCurrent()

            val expected =
                ReviewDraft(
                    id = STORED_REVIEW.id,
                    title = "기생충 다시 보기",
                    content = STORED_REVIEW.content,
                    rating = STORED_REVIEW.rating,
                    photo = ReviewPhoto.Unchanged,
                )
            assertEquals(listOf(expected), repository.savedDrafts)
            assertEquals(listOf<ReviewEditEvent>(ReviewEditEvent.Saved(isNew = false)), events)
        }
    }

    @Test
    fun `수정할 감상문이 Room에 없으면 불러오기 실패를 보내야 한다`() {
        val repository = FakeReviewEditRepository(stored = null)
        runReviewEditTest(repository, editHandle(STORED_REVIEW.id)) { viewModel ->
            val events = collectReviewEditEvents(viewModel)

            runCurrent()

            assertEquals(listOf<ReviewEditEvent>(ReviewEditEvent.LoadFailed), events)
            assertEquals("", viewModel.uiState.value.title)
        }
    }

    @Test
    fun `사진을 지우고 저장하면 Repository에 사진 삭제를 넘겨야 한다`() {
        val repository =
            FakeReviewEditRepository(stored = STORED_REVIEW, saveResult = CompletableDeferred(Outcome.Success(Unit)))
        runReviewEditTest(repository, editHandle(STORED_REVIEW.id)) { viewModel ->
            runCurrent()

            viewModel.onPhotoRemoved()
            viewModel.save()
            runCurrent()

            assertEquals(ReviewPhoto.Removed, viewModel.uiState.value.photo)
            assertEquals(listOf<ReviewPhoto>(ReviewPhoto.Removed), repository.savedDrafts.map(ReviewDraft::photo))
        }
    }

    @Test
    fun `별점을 별 개수로 바꾸면 10점 만점 점수로 저장하고 같은 별 개수를 보여야 한다`() {
        runReviewEditTest(FakeReviewEditRepository(), SavedStateHandle()) { viewModel ->
            viewModel.onStarRatingChange(3.5f)

            assertEquals(7.0, viewModel.uiState.value.rating, 0.0)
            assertEquals(3.5f, viewModel.uiState.value.starRating)
        }
    }

    @Test
    fun `프로세스가 종료된 뒤 다시 열면 입력과 촬영한 사진을 복원해야 한다`() {
        val savedStateHandle = SavedStateHandle()
        runReviewEditTest(FakeReviewEditRepository(), savedStateHandle) { viewModel ->
            fillInput(viewModel)
            viewModel.onCameraStarted(CAMERA_URI)
            viewModel.onCameraResult(isSaved = true)

            val restored = ReviewEditViewModel(FakeReviewEditRepository(), savedStateHandle)

            val expected =
                ReviewEditUiState(
                    title = "기생충",
                    content = "잘 봤다",
                    rating = 8.0,
                    photo = ReviewPhoto.New(CAMERA_URI),
                )
            assertEquals(expected, restored.uiState.value)
        }
    }

    @Test
    fun `Photo Picker로 고른 사진은 프로세스가 종료된 뒤 다시 열면 고르기 전 사진으로 돌아가야 한다`() {
        val savedStateHandle = SavedStateHandle()
        runReviewEditTest(FakeReviewEditRepository(), savedStateHandle) { viewModel ->
            viewModel.onCameraStarted(CAMERA_URI)
            viewModel.onCameraResult(isSaved = true)
            viewModel.onPhotoSelected("content://photo/1")

            val restored = ReviewEditViewModel(FakeReviewEditRepository(), savedStateHandle)

            assertEquals(ReviewPhoto.Unchanged, restored.uiState.value.photo)
        }
    }

    @Test
    fun `카메라를 쓰는 동안 프로세스가 종료돼도 촬영에 성공하면 카메라에 넘긴 파일을 새 사진으로 골라야 한다`() {
        val savedStateHandle = SavedStateHandle()
        runReviewEditTest(FakeReviewEditRepository(), savedStateHandle) { viewModel ->
            viewModel.onCameraStarted(CAMERA_URI)

            val restored = ReviewEditViewModel(FakeReviewEditRepository(), savedStateHandle)
            restored.onCameraResult(isSaved = true)

            assertEquals(ReviewPhoto.New(CAMERA_URI), restored.uiState.value.photo)
        }
    }

    @Test
    fun `촬영에 실패하면 이전에 고른 사진을 그대로 둬야 한다`() {
        runReviewEditTest(FakeReviewEditRepository(), SavedStateHandle()) { viewModel ->
            viewModel.onPhotoSelected("content://photo/1")
            viewModel.onCameraStarted(CAMERA_URI)

            viewModel.onCameraResult(isSaved = false)

            assertEquals(ReviewPhoto.New("content://photo/1"), viewModel.uiState.value.photo)
        }
    }

    @Test
    fun `수정 모드에서 프로세스가 종료된 뒤 다시 열면 고친 입력은 Room 값으로 덮지 않아야 한다`() {
        val savedStateHandle = editHandle(STORED_REVIEW.id)
        val repository = FakeReviewEditRepository(stored = STORED_REVIEW)
        runReviewEditTest(repository, savedStateHandle) { viewModel ->
            runCurrent()
            viewModel.onTitleChange("기생충 다시 보기")

            val restored = ReviewEditViewModel(repository, savedStateHandle)
            runCurrent()

            val expected =
                ReviewEditUiState(
                    title = "기생충 다시 보기",
                    content = STORED_REVIEW.content,
                    rating = STORED_REVIEW.rating,
                    savedImageUrl = STORED_REVIEW.image,
                    isEditing = true,
                )
            assertEquals(expected, restored.uiState.value)
        }
    }
}

private const val CAMERA_URI = "file:///cache/review_photo_1.jpg"

private val STORED_REVIEW =
    Review(
        title = "기생충",
        image = "https://example.com/photo.png",
        content = "잘 봤다",
        time = "2024/01/02 03:04",
        rating = 9.0,
        storageFileName = "photo.png",
        id = 3,
    )

private fun editHandle(reviewId: Int): SavedStateHandle =
    SavedStateHandle(mapOf(ReviewEditViewModel.ARG_REVIEW_ID to reviewId))

private fun fillInput(viewModel: ReviewEditViewModel) {
    viewModel.onTitleChange("기생충")
    viewModel.onContentChange("잘 봤다")
    viewModel.onStarRatingChange(4f)
}

private fun runReviewEditTest(
    repository: FakeReviewEditRepository,
    savedStateHandle: SavedStateHandle,
    body: suspend TestScope.(ReviewEditViewModel) -> Unit,
) = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    val viewModel = ReviewEditViewModel(repository, savedStateHandle)
    try {
        body(viewModel)
    } finally {
        viewModel.viewModelScope.cancel()
        Dispatchers.resetMain()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun TestScope.collectReviewEditEvents(viewModel: ReviewEditViewModel): List<ReviewEditEvent> {
    val events = mutableListOf<ReviewEditEvent>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        viewModel.events.collect { events += it }
    }
    return events
}

private class FakeReviewEditRepository(
    private val stored: Review? = null,
    private val saveResult: CompletableDeferred<ReviewResult> = CompletableDeferred(),
) : ReviewRepository {
    val requestedIds = mutableListOf<Int>()
    val savedDrafts = mutableListOf<ReviewDraft>()

    override fun observeAll(): Flow<List<Review>> = error("사용하지 않음")

    override suspend fun syncFromRemote(): ReviewResult = error("사용하지 않음")

    override suspend fun getById(id: Int): Review? {
        requestedIds += id
        return stored
    }

    override suspend fun save(draft: ReviewDraft): ReviewResult {
        savedDrafts += draft
        return saveResult.await()
    }

    override suspend fun delete(review: Review) = error("사용하지 않음")

    override suspend fun deleteAll() = error("사용하지 않음")
}
