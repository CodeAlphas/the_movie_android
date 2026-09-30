package com.codealphas.themovie.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codealphas.themovie.domain.result.Outcome
import com.codealphas.themovie.domain.review.ReviewDraft
import com.codealphas.themovie.domain.review.ReviewError
import com.codealphas.themovie.domain.review.ReviewPhoto
import com.codealphas.themovie.domain.review.ReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// 화면의 별점은 별 5개이고 감상문 점수는 10점 만점이라 두 값의 비율
private const val RATING_SCALE = 2

data class ReviewEditUiState(
    val title: String = "",
    val content: String = "",
    // 0.0~10.0 점수
    val rating: Double = 0.0,
    val photo: ReviewPhoto = ReviewPhoto.Unchanged,
    // Unchanged일 때 화면에 보여줄, 이미 저장된 사진 URL. 사진이 없으면 빈 문자열
    val savedImageUrl: String = "",
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
) {
    // 화면마다 별점 변환을 따로 계산하지 않도록, 별 5개 기준 값을 상태에서 제공
    val starRating: Float get() = (rating / RATING_SCALE).toFloat()
}

sealed interface ReviewEditEvent {
    data class Saved(
        val isNew: Boolean,
    ) : ReviewEditEvent

    data object InputRequired : ReviewEditEvent

    data class SaveFailed(
        val error: ReviewError,
    ) : ReviewEditEvent

    data object LoadFailed : ReviewEditEvent
}

@HiltViewModel
class ReviewEditViewModel
    @Inject
    constructor(
        private val repository: ReviewRepository,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        // 작성 화면은 reviewId extra 없이 열리므로 null이면 새 감상문
        private val reviewId: Int? = savedStateHandle.get<Int>(ARG_REVIEW_ID)

        // 카메라 앱을 쓰는 동안 프로세스가 종료되면 입력이 사라지므로, SavedStateHandle에 남긴 입력으로 초기 상태 복원
        private val _uiState =
            MutableStateFlow(
                ReviewEditUiState(
                    title = savedStateHandle[KEY_TITLE] ?: "",
                    content = savedStateHandle[KEY_CONTENT] ?: "",
                    rating = savedStateHandle[KEY_RATING] ?: 0.0,
                    photo = restoredPhoto(),
                    isEditing = reviewId != null,
                ),
            )
        val uiState: StateFlow<ReviewEditUiState> = _uiState.asStateFlow()

        private val _events = Channel<ReviewEditEvent>(Channel.BUFFERED)
        val events: Flow<ReviewEditEvent> = _events.receiveAsFlow()

        init {
            // 회전하면 Activity onCreate가 다시 실행되어 Room을 다시 읽고 입력값을 덮어쓰므로, ViewModel을 만들 때 한 번만 읽음
            reviewId?.let(::loadReview)
        }

        private fun loadReview(id: Int) {
            viewModelScope.launch {
                val review = repository.getById(id)
                if (review == null) {
                    _events.send(ReviewEditEvent.LoadFailed)
                    return@launch
                }
                // 복원한 입력을 Room 값으로 덮으면 프로세스 종료 전에 고친 내용이 사라지므로, 남긴 입력이 없는 항목만 Room 값 반영
                _uiState.update {
                    it.copy(
                        title = if (KEY_TITLE in savedStateHandle) it.title else review.title,
                        content = if (KEY_CONTENT in savedStateHandle) it.content else review.content,
                        rating = if (KEY_RATING in savedStateHandle) it.rating else review.rating,
                        savedImageUrl = review.image,
                    )
                }
            }
        }

        fun onTitleChange(title: String) {
            savedStateHandle[KEY_TITLE] = title
            _uiState.update { it.copy(title = title) }
        }

        fun onContentChange(content: String) {
            savedStateHandle[KEY_CONTENT] = content
            _uiState.update { it.copy(content = content) }
        }

        fun onStarRatingChange(stars: Float) {
            val rating = stars.toDouble() * RATING_SCALE
            savedStateHandle[KEY_RATING] = rating
            _uiState.update { it.copy(rating = rating) }
        }

        // Photo Picker uri의 읽기 권한은 프로세스가 끝나면 사라지므로, 복원하지 않도록 남긴 사진 기록 삭제
        fun onPhotoSelected(uri: String) {
            storePhoto(cameraUri = null, isRemoved = false)
            _uiState.update { it.copy(photo = ReviewPhoto.New(uri)) }
        }

        fun onPhotoRemoved() {
            storePhoto(cameraUri = null, isRemoved = true)
            _uiState.update { it.copy(photo = ReviewPhoto.Removed) }
        }

        // 카메라 앱을 쓰는 동안 프로세스가 종료되면 촬영 파일 위치를 잃어 결과를 받지 못하므로, 여는 순간 파일 uri 보관
        fun onCameraStarted(fileUri: String) {
            savedStateHandle[KEY_PENDING_CAMERA_URI] = fileUri
        }

        fun onCameraResult(isSaved: Boolean) {
            val uri = savedStateHandle.remove<String>(KEY_PENDING_CAMERA_URI)
            // 촬영이 끝나지 않으면 파일에 사진이 없으므로, 이전 사진 유지
            if (!isSaved || uri == null) return
            storePhoto(cameraUri = uri, isRemoved = false)
            _uiState.update { it.copy(photo = ReviewPhoto.New(uri)) }
        }

        private fun storePhoto(
            cameraUri: String?,
            isRemoved: Boolean,
        ) {
            savedStateHandle[KEY_CAMERA_PHOTO_URI] = cameraUri
            savedStateHandle[KEY_PHOTO_REMOVED] = isRemoved
        }

        // 촬영 사진은 앱 캐시 파일이라 복원 뒤에도 읽히므로, 촬영 사진과 사진 삭제만 복원
        private fun restoredPhoto(): ReviewPhoto {
            val cameraUri = savedStateHandle.get<String>(KEY_CAMERA_PHOTO_URI)
            return when {
                cameraUri != null -> ReviewPhoto.New(cameraUri)
                savedStateHandle.get<Boolean>(KEY_PHOTO_REMOVED) == true -> ReviewPhoto.Removed
                else -> ReviewPhoto.Unchanged
            }
        }

        fun save() {
            val state = _uiState.value
            // 저장 중에 저장 버튼을 다시 누르면 같은 감상문이 한 번 더 등록되므로, 첫 저장이 끝나기 전의 요청은 무시
            if (state.isSaving) return
            // 입력이 비었는데 사진부터 올리면 감상문은 저장되지 않고 사진만 바뀌므로, 입력 확인을 가장 먼저 처리
            if (state.title.isBlank() || state.content.isBlank()) {
                _events.trySend(ReviewEditEvent.InputRequired)
                return
            }
            _uiState.update { it.copy(isSaving = true) }
            viewModelScope.launch {
                val draft =
                    ReviewDraft(
                        id = reviewId,
                        title = state.title,
                        content = state.content,
                        rating = state.rating,
                        photo = state.photo,
                    )
                when (val result = repository.save(draft)) {
                    // 성공 뒤 화면이 닫히기 전에 저장 버튼을 다시 누르는 것을 막기 위해 isSaving은 그대로 유지
                    is Outcome.Success -> _events.send(ReviewEditEvent.Saved(isNew = reviewId == null))
                    is Outcome.Failure -> {
                        _uiState.update { it.copy(isSaving = false) }
                        _events.send(ReviewEditEvent.SaveFailed(result.error))
                    }
                }
            }
        }

        companion object {
            const val ARG_REVIEW_ID = "reviewId"
            private const val KEY_TITLE = "title"
            private const val KEY_CONTENT = "content"
            private const val KEY_RATING = "rating"
            private const val KEY_CAMERA_PHOTO_URI = "cameraPhotoUri"
            private const val KEY_PHOTO_REMOVED = "photoRemoved"
            private const val KEY_PENDING_CAMERA_URI = "pendingCameraUri"
        }
    }
