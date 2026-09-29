package com.codealphas.themovie.review

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codealphas.themovie.domain.review.Review
import com.codealphas.themovie.domain.review.ReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReviewViewModel
    @Inject
    constructor(
        private val repository: ReviewRepository,
    ) : ViewModel() {
        private val _maxId = MutableLiveData<Int>()
        val maxId: LiveData<Int>
            get() = _maxId

        fun updateReview(review: Review) {
            viewModelScope.launch {
                repository.update(review)
            }
        }

        fun insertTransaction(review: Review) {
            viewModelScope.launch {
                _maxId.value = repository.insertAndReturnId(review)
            }
        }
    }
