package com.codealphas.themovie.auth

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.codealphas.themovie.R
import com.codealphas.themovie.core.android.ui.applySystemBarInsets
import com.codealphas.themovie.databinding.ActivityJoinBinding
import com.google.android.material.color.MaterialColors
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class JoinActivity : AppCompatActivity() {
    private lateinit var binding: ActivityJoinBinding
    private val viewModel: JoinViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySystemBarInsets()

        binding = ActivityJoinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initInputs()
        initJoinButton()
        initLoginButton()
        observeJoin()
    }

    private fun initInputs() {
        binding.pwInput1.doAfterTextChanged { notifyPasswordsChanged() }
        binding.pwInput2.doAfterTextChanged { notifyPasswordsChanged() }
    }

    private fun notifyPasswordsChanged() {
        viewModel.onPasswordsChanged(
            binding.pwInput1.text.toString(),
            binding.pwInput2.text.toString(),
        )
    }

    private fun initJoinButton() {
        binding.joinBtn.setOnClickListener {
            viewModel.signUp(
                binding.idInput.text.toString(),
                binding.pwInput1.text.toString(),
                binding.pwInput2.text.toString(),
            )
        }
    }

    private fun initLoginButton() {
        binding.loginBtn.setOnClickListener { finish() }
    }

    private fun observeJoin() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        binding.joinBtn.isEnabled = !state.isLoading
                        showPasswordMatch(state.passwordsMatch)
                    }
                }
                launch {
                    viewModel.events.collect { event ->
                        val message =
                            when (event) {
                                JoinEvent.ShowLoginPrompt -> R.string.join_succeeded
                                JoinEvent.ShowInvalidInput -> R.string.join_failed_blank
                                JoinEvent.ShowPasswordMismatch -> R.string.join_failed_password
                                is JoinEvent.ShowError -> joinFailureMessage(event.error)
                            }
                        Toast.makeText(this@JoinActivity, getString(message), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun showPasswordMatch(passwordsMatch: Boolean?) {
        when (passwordsMatch) {
            true -> {
                binding.checkPwTextView.text = getString(R.string.join_password_match)
                binding.checkPwTextView.setTextColor(
                    MaterialColors.getColor(binding.checkPwTextView, com.google.android.material.R.attr.colorPrimary),
                )
            }

            false -> {
                binding.checkPwTextView.text = getString(R.string.join_password_mismatch)
                binding.checkPwTextView.setTextColor(
                    MaterialColors.getColor(binding.checkPwTextView, com.google.android.material.R.attr.colorOnSurface),
                )
            }

            null -> binding.checkPwTextView.text = ""
        }
    }
}
