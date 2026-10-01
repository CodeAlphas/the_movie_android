package com.codealphas.themovie.presentation.review

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.codealphas.themovie.core.android.theme.TheMovieTheme
import com.codealphas.themovie.presentation.auth.LoginActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReviewMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TheMovieTheme {
                ReviewListScreen(
                    onNavigateUp = { onBackPressedDispatcher.onBackPressed() },
                    onNavigateToLogin = ::openLogin,
                    onNavigateToEdit = ::openReviewEdit,
                )
            }
        }
    }

    private fun openLogin() {
        // 감상문 화면만 닫으면 로그인 화면에서 뒤로 갈 때 로그아웃된 메인 화면이 다시 열리므로, 기존 화면을 모두 비우도록 태스크 초기화 적용
        startActivity(
            Intent(applicationContext, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        )
        finish()
    }

    private fun openReviewEdit(reviewId: Int?) {
        val intent = Intent(this, ReviewDetailActivity::class.java)
        reviewId?.let { intent.putExtra(ReviewEditViewModel.ARG_REVIEW_ID, it) }
        startActivity(intent)
    }
}
