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
            // 재시도를 기다리던 업로드 작업이 남으면 다음 계정이 저장한 감상문이 그 작업 뒤에 이어 붙어 재시도 대기 시간만큼 늦게 올라가므로,
            // 남은 업로드 작업 취소
            reviewRepository.cancelPendingSync()
            // 로그아웃한 뒤 삭제가 끊기면 이전 계정의 감상문이 Room에 남아 다음 계정의 목록에 보이므로, 감상문을 먼저 지운 뒤 로그아웃
            reviewRepository.deleteAll()
            authRepository.signOut()
        }
    }
