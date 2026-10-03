package com.codealphas.themovie.presentation.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codealphas.themovie.domain.result.Outcome
import com.codealphas.themovie.domain.review.ReviewDraft
import com.codealphas.themovie.domain.review.ReviewPhoto
import com.codealphas.themovie.domain.review.ReviewRepository
import com.codealphas.themovie.presentation.navigation.ReviewEdit
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

@HiltViewModel
class ReviewEditViewModel
    @Inject
    constructor(
        private val repository: ReviewRepository,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        // toRoute()는 Bundle이 필요해 JVM 단위 테스트에서 실패하므로, Navigation이 속성 이름을 키로 넣어 둔 값 직접 조회
        // 작성 화면은 reviewId 없이 열리므로, 값이 없으면 새 감상문으로 처리
        private val reviewId: String? = savedStateHandle.get<String>(ReviewEdit::reviewId.name)

        // 카메라 앱을 쓰는 동안 프로세스가 종료되면 입력이 사라지므로, SavedStateHandle에 남긴 입력으로 초기 상태 복원
        private val _state =
            MutableStateFlow(
                ReviewEditUiState(
                    title = savedStateHandle[KEY_TITLE] ?: "",
                    content = savedStateHandle[KEY_CONTENT] ?: "",
                    rating = savedStateHandle[KEY_RATING] ?: 0.0,
                    photo = restoredPhoto(),
                    isEditing = reviewId != null,
                ),
            )
        val state: StateFlow<ReviewEditUiState> = _state.asStateFlow()

        private val _effect = Channel<ReviewEditEffect>(Channel.BUFFERED)
        val effect: Flow<ReviewEditEffect> = _effect.receiveAsFlow()

        init {
            // 회전할 때마다 Room을 다시 읽으면 고치던 입력이 Room 값으로 덮이므로, 회전에도 남는 ViewModel이 만들어질 때 한 번만 읽기 적용
            reviewId?.let(::loadReview)
        }

        private fun loadReview(id: String) {
            viewModelScope.launch {
                val review = repository.getById(id)
                if (review == null) {
                    _effect.send(ReviewEditEffect.LoadFailed)
                    return@launch
                }
                // 복원한 입력을 Room 값으로 덮으면 프로세스 종료 전에 고친 내용이 사라지므로, 남긴 입력이 없는 항목만 Room 값 반영
                _state.update {
                    it.copy(
                        title = if (KEY_TITLE in savedStateHandle) it.title else review.title,
                        content = if (KEY_CONTENT in savedStateHandle) it.content else review.content,
                        rating = if (KEY_RATING in savedStateHandle) it.rating else review.rating,
                        savedImageUrl = review.imageUrl,
                    )
                }
            }
        }

        fun onIntent(intent: ReviewEditIntent) {
            when (intent) {
                is ReviewEditIntent.TitleChanged -> changeTitle(intent.title)
                is ReviewEditIntent.ContentChanged -> changeContent(intent.content)
                is ReviewEditIntent.StarRatingChanged -> changeStarRating(intent.stars)
                is ReviewEditIntent.PhotoSelected -> selectPhoto(intent.uri)
                ReviewEditIntent.PhotoRemoved -> removePhoto()
                is ReviewEditIntent.CameraStarted -> startCamera(intent.fileUri)
                is ReviewEditIntent.CameraFinished -> finishCamera(intent.isSaved)
                ReviewEditIntent.SaveClicked -> save()
            }
        }

        private fun changeTitle(title: String) {
            savedStateHandle[KEY_TITLE] = title
            _state.update { it.copy(title = title) }
        }

        private fun changeContent(content: String) {
            savedStateHandle[KEY_CONTENT] = content
            _state.update { it.copy(content = content) }
        }

        private fun changeStarRating(stars: Float) {
            val rating = stars.toDouble() * RATING_SCALE
            savedStateHandle[KEY_RATING] = rating
            _state.update { it.copy(rating = rating) }
        }

        // Photo Picker uri의 읽기 권한은 프로세스가 끝나면 사라지므로, 복원하지 않도록 남긴 사진 기록 삭제
        private fun selectPhoto(uri: String) {
            storePhoto(cameraUri = null, isRemoved = false)
            _state.update { it.copy(photo = ReviewPhoto.New(uri)) }
        }

        private fun removePhoto() {
            storePhoto(cameraUri = null, isRemoved = true)
            _state.update { it.copy(photo = ReviewPhoto.Removed) }
        }

        // 카메라 앱을 쓰는 동안 프로세스가 종료되면 촬영 파일 위치를 잃어 결과를 받지 못하므로, 여는 순간 파일 uri 보관
        private fun startCamera(fileUri: String) {
            savedStateHandle[KEY_PENDING_CAMERA_URI] = fileUri
        }

        private fun finishCamera(isSaved: Boolean) {
            val uri = savedStateHandle.remove<String>(KEY_PENDING_CAMERA_URI)
            // 촬영이 끝나지 않으면 파일에 사진이 없으므로, 이전 사진 유지
            if (!isSaved || uri == null) return
            storePhoto(cameraUri = uri, isRemoved = false)
            _state.update { it.copy(photo = ReviewPhoto.New(uri)) }
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

        private fun save() {
            val state = _state.value
            // 저장 중에 저장 버튼을 다시 누르면 같은 감상문이 한 번 더 등록되므로, 첫 저장이 끝나기 전의 요청은 무시
            if (state.isSaving) return
            // 입력이 비었는데 사진부터 올리면 감상문은 저장되지 않고 사진만 바뀌므로, 입력 확인을 가장 먼저 처리
            if (state.title.isBlank() || state.content.isBlank()) {
                _effect.trySend(ReviewEditEffect.InputRequired)
                return
            }
            _state.update { it.copy(isSaving = true) }
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
                    // 성공 뒤 화면이 닫히기 전에 저장 버튼을 다시 누르면 같은 감상문이 한 번 더 저장되므로, isSaving 유지
                    is Outcome.Success -> _effect.send(ReviewEditEffect.Saved(isNew = reviewId == null))
                    is Outcome.Failure -> {
                        _state.update { it.copy(isSaving = false) }
                        _effect.send(ReviewEditEffect.SaveFailed(result.error))
                    }
                }
            }
        }

        companion object {
            private const val KEY_TITLE = "title"
            private const val KEY_CONTENT = "content"
            private const val KEY_RATING = "rating"
            private const val KEY_CAMERA_PHOTO_URI = "cameraPhotoUri"
            private const val KEY_PHOTO_REMOVED = "photoRemoved"
            private const val KEY_PENDING_CAMERA_URI = "pendingCameraUri"
        }
    }
