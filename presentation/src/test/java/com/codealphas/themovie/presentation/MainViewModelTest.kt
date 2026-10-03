package com.codealphas.themovie.presentation

import com.codealphas.themovie.domain.auth.AuthRepository
import com.codealphas.themovie.domain.auth.AuthResult
import com.codealphas.themovie.presentation.navigation.AuthGraph
import com.codealphas.themovie.presentation.navigation.Home
import org.junit.Assert.assertEquals
import org.junit.Test

class MainViewModelTest {
    @Test
    fun `로그인한 상태로 앱을 열면 홈에서 시작해야 한다`() {
        val viewModel = MainViewModel(FakeMainAuthRepository(userId = "uid"))

        assertEquals(Home, viewModel.startDestination)
    }

    @Test
    fun `로그인하지 않은 상태로 앱을 열면 로그인 화면에서 시작해야 한다`() {
        val viewModel = MainViewModel(FakeMainAuthRepository(userId = null))

        assertEquals(AuthGraph, viewModel.startDestination)
    }
}

private class FakeMainAuthRepository(
    private val userId: String?,
) : AuthRepository {
    override suspend fun signIn(
        email: String,
        password: String,
    ): AuthResult = error("사용하지 않음")

    override suspend fun signUp(
        email: String,
        password: String,
    ): AuthResult = error("사용하지 않음")

    override fun currentUserId(): String? = userId

    override fun signOut() = error("사용하지 않음")
}
