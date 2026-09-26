package com.codealphas.themovie.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codealphas.themovie.models.Review
import com.codealphas.themovie.repository.ReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReviewViewModel
    @Inject
    constructor(
        private val repository: ReviewRepository,
    ) : ViewModel() {
        private val _allReview: LiveData<List<Review>> = repository.allReview
        val allReview: LiveData<List<Review>>
            get() = _allReview

        private val _maxId = MutableLiveData<Int>()
        val maxId: LiveData<Int>
            get() = _maxId

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
    }
