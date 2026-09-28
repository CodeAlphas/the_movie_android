package com.codealphas.themovie.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.codealphas.themovie.domain.auth.LogoutUseCase
import com.codealphas.themovie.domain.review.Review
import com.codealphas.themovie.domain.review.ReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReviewViewModel
    @Inject
    constructor(
        private val repository: ReviewRepository,
        private val logoutUseCase: LogoutUseCase,
    ) : ViewModel() {
        val allReview: LiveData<List<Review>> = repository.observeAll().asLiveData()

        private val _maxId = MutableLiveData<Int>()
        val maxId: LiveData<Int>
            get() = _maxId

        private val _logoutCompleted = Channel<Unit>(Channel.BUFFERED)
        val logoutCompleted: Flow<Unit> = _logoutCompleted.receiveAsFlow()

        fun deleteReview(review: Review) {
            viewModelScope.launch {
                repository.delete(review)
            }
        }

        fun updateReview(review: Review) {
            viewModelScope.launch {
                repository.update(review)
            }
        }

        fun insertReview(review: Review) {
            viewModelScope.launch {
                repository.insert(review)
            }
        }

        fun insertTransaction(review: Review) {
            viewModelScope.launch {
                _maxId.value = repository.insertAndReturnId(review)
            }
        }

        fun logout() {
            viewModelScope.launch {
                logoutUseCase()
                // 삭제가 끝나기 전에 화면을 닫으면 viewModelScope가 취소되어 감상문이 남으므로, 삭제를 마친 뒤 화면 이동 이벤트 전송
                _logoutCompleted.send(Unit)
            }
        }
    }
