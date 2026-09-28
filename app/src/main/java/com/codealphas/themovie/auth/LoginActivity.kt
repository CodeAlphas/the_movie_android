package com.codealphas.themovie.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.codealphas.themovie.R
import com.codealphas.themovie.core.android.ui.applySystemBarInsets
import com.codealphas.themovie.databinding.ActivityLoginBinding
import com.codealphas.themovie.movie.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySystemBarInsets()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initLoginButton()
        initJoinButton()
        observeLogin()
    }

    private fun initLoginButton() {
        binding.loginBtn.setOnClickListener {
            viewModel.signIn(binding.idInput.text.toString(), binding.pwInput.text.toString())
        }
    }

    private fun initJoinButton() {
        binding.joinBtn.setOnClickListener {
            startActivity(Intent(this, JoinActivity::class.java))
        }
    }

    private fun observeLogin() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        binding.loginBtn.isEnabled = !state.isLoading
                    }
                }
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            LoginEvent.NavigateToMain -> openMain()
                            LoginEvent.ShowInvalidInput -> showMessage(R.string.login_failed)
                            is LoginEvent.ShowError -> showMessage(loginFailureMessage(event.error))
                        }
                    }
                }
            }
        }
    }

    private fun openMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showMessage(message: Int) {
        Toast.makeText(this, getString(message), Toast.LENGTH_SHORT).show()
    }
}
