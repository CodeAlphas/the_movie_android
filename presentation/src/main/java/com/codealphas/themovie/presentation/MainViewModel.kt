package com.codealphas.themovie.presentation

import androidx.lifecycle.ViewModel
import com.codealphas.themovie.domain.auth.AuthRepository
import com.codealphas.themovie.presentation.navigation.AuthGraph
import com.codealphas.themovie.presentation.navigation.Home
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        authRepository: AuthRepository,
    ) : ViewModel() {
        // currentUserId는 저장된 로그인 정보를 바로 읽으므로, 첫 화면부터 로그인 여부에 맞게 열리도록 생성 시점에 시작 목적지 결정
        internal val startDestination: Any = if (authRepository.currentUserId() != null) Home else AuthGraph
    }
