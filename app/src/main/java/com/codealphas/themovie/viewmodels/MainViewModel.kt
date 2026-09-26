package com.codealphas.themovie.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codealphas.themovie.repository.ReviewRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        private val auth: FirebaseAuth,
        private val reviewRepository: ReviewRepository,
    ) : ViewModel() {
        private val _logoutCompleted = Channel<Unit>(Channel.BUFFERED)
        val logoutCompleted: Flow<Unit> = _logoutCompleted.receiveAsFlow()

        fun logout() {
            viewModelScope.launch {
                auth.signOut()
                reviewRepository.deleteAll()
                // 삭제가 끝나기 전에 화면을 닫으면 viewModelScope가 취소되어 감상문이 남으므로, 삭제를 마친 뒤 화면 이동 이벤트 전송
                _logoutCompleted.send(Unit)
            }
        }
    }
