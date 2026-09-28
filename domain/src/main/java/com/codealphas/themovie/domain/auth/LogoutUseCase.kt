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
            authRepository.signOut()
            reviewRepository.deleteAll()
        }
    }
