package com.codealphas.themovie.data.auth

import com.codealphas.themovie.domain.auth.AuthRepository
import com.codealphas.themovie.domain.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

internal class AuthRepositoryImpl
    @Inject
    constructor(
        private val auth: FirebaseAuth,
    ) : AuthRepository {
        override suspend fun signIn(
            email: String,
            password: String,
        ): AuthResult = toAuthResult { auth.signInWithEmailAndPassword(email, password).await() }

        override suspend fun signUp(
            email: String,
            password: String,
        ): AuthResult = toAuthResult { auth.createUserWithEmailAndPassword(email, password).await() }

        override fun currentUserId(): String? = auth.currentUser?.uid

        override fun signOut() {
            auth.signOut()
        }
    }
