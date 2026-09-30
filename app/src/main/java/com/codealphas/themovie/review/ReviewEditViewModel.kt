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

data class ReviewEditUiState(
    val title: String = "",
    val content: String = "",
    // 0.0~10.0 점수. 화면의 RatingBar(별 5개)는 이 값의 절반
    val rating: Double = 0.0,
    val photo: ReviewPhoto = ReviewPhoto.Unchanged,
    // Unchanged일 때 화면에 보여줄, 이미 저장된 사진 URL. 사진이 없으면 빈 문자열
    val savedImageUrl: String = "",
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
)

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
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        // 작성 화면은 reviewId extra 없이 열리므로 null이면 새 감상문
        private val reviewId: Int? = savedStateHandle.get<Int>(ARG_REVIEW_ID)

        private val _uiState = MutableStateFlow(ReviewEditUiState(isEditing = reviewId != null))
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
                _uiState.update {
                    it.copy(
                        title = review.title,
                        content = review.content,
                        rating = review.rating,
                        savedImageUrl = review.image,
                    )
                }
            }
        }

        fun onTitleChange(title: String) = _uiState.update { it.copy(title = title) }

        fun onContentChange(content: String) = _uiState.update { it.copy(content = content) }

        fun onRatingChange(rating: Double) = _uiState.update { it.copy(rating = rating) }

        fun onPhotoSelected(uri: String) = _uiState.update { it.copy(photo = ReviewPhoto.New(uri)) }

        fun onPhotoRemoved() = _uiState.update { it.copy(photo = ReviewPhoto.Removed) }

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
        }
    }
