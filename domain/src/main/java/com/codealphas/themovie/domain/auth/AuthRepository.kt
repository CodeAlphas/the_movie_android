package com.codealphas.themovie.domain.auth

interface AuthRepository {
    suspend fun signIn(
        email: String,
        password: String,
    ): AuthResult

    suspend fun signUp(
        email: String,
        password: String,
    ): AuthResult

    fun currentUserId(): String?

    fun signOut()
}
