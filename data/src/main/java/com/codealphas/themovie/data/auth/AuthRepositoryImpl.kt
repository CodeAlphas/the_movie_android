package com.codealphas.themovie.data.auth

import com.codealphas.themovie.domain.auth.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

internal class AuthRepositoryImpl
    @Inject
    constructor(
        private val auth: FirebaseAuth,
    ) : AuthRepository {
        override fun signOut() {
            auth.signOut()
        }
    }
