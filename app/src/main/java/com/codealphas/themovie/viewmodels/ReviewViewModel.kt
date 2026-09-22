package com.example.movieapplication.viewmodels

import android.app.Application
import androidx.lifecycle.*
import com.example.movieapplication.database.DatabaseInstance
import com.example.movieapplication.models.Review
import com.example.movieapplication.repository.ReviewRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReviewViewModel(application: Application) : AndroidViewModel(application) {

    private val _allReview: LiveData<List<Review>>
    val allReview: LiveData<List<Review>>
        get() = _allReview

    private val _maxId = MutableLiveData<Int>()
    val maxId: LiveData<Int>
        get() = _maxId

    private val repository: ReviewRepository

    init {
        val dao = DatabaseInstance.getInstance(application).reviewDao()
        repository = ReviewRepository(dao)
        _allReview = repository.allReview
    }

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