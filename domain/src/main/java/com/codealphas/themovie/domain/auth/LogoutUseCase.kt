package com.codealphas.themovie.domain.auth

import com.codealphas.themovie.domain.review.ReviewRepository
import javax.inject.Inject

class LogoutUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val reviewRepository: ReviewRepository,
    ) {
        suspend operator fun invoke() {
            // 로그아웃한 뒤 삭제가 끊기면 이전 계정의 감상문이 Room에 남아 다음 계정의 목록에 보이므로, 감상문을 먼저 지운 뒤 로그아웃
            reviewRepository.deleteAll()
            authRepository.signOut()
        }
    }
