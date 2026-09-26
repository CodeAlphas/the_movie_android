package com.codealphas.themovie.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codealphas.themovie.models.Review
import com.codealphas.themovie.repository.ReviewRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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
        private val auth: FirebaseAuth,
    ) : ViewModel() {
        private val _allReview: LiveData<List<Review>> = repository.allReview
        val allReview: LiveData<List<Review>>
            get() = _allReview

        private val _maxId = MutableLiveData<Int>()
        val maxId: LiveData<Int>
            get() = _maxId

        private val _logoutCompleted = Channel<Unit>(Channel.BUFFERED)
        val logoutCompleted: Flow<Unit> = _logoutCompleted.receiveAsFlow()

        fun deleteReview(review: Review) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.delete(review)
            }
        } // 사용자의 영화 감상문을 삭제하는 메소드

        fun updateReview(review: Review) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.update(review)
            }
        } // 사용자의 영화 감상문을 수정하는 메소드

        fun insertReview(review: Review) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.insert(review)
            }
        } // 사용자의 영화 감상문을 추가하는 메소드

        fun insertTransaction(review: Review) {
            viewModelScope.launch(Dispatchers.IO) {
                _maxId.postValue(repository.insertTransaction(review))
            }
        } // 사용자의 영화 감상문을 추가하고 해당 감상문의 id 정보를 받아오는 메소드

        fun logout() {
            viewModelScope.launch {
                auth.signOut()
                repository.deleteAll()
                // 삭제가 끝나기 전에 화면을 닫으면 viewModelScope가 취소되어 감상문이 남으므로, 삭제를 마친 뒤 화면 이동 이벤트 전송
                _logoutCompleted.send(Unit)
            }
        }
    }
