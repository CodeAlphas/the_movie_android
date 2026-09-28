package com.codealphas.themovie.auth

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.codealphas.themovie.R
import com.codealphas.themovie.core.android.ui.applySystemBarInsets
import com.codealphas.themovie.databinding.ActivityJoinBinding
import com.google.android.material.color.MaterialColors
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class JoinActivity : AppCompatActivity() {
    private lateinit var binding: ActivityJoinBinding
    private var id: String = ""
    private var pw1: String = ""
    private var pw2: String = ""
    private val auth: FirebaseAuth by lazy { Firebase.auth }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySystemBarInsets()

        binding = ActivityJoinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initIdAndPwEditText()
        initJoinButton()
        initLoginbutton()
    }

    private fun initIdAndPwEditText() {
        binding.idInput.doAfterTextChanged { id = it.toString() }
        binding.pwInput1.doAfterTextChanged {
            pw1 = it.toString()
            checkPassword(pw1, pw2)
        }
        binding.pwInput2.doAfterTextChanged {
            pw2 = it.toString()
            checkPassword(pw1, pw2)
        }
    }

    private fun checkPassword(
        pw1: String,
        pw2: String,
    ) {
        if (pw1 == pw2) {
            binding.checkPwTextView.text = getString(R.string.join_password_match)
            binding.checkPwTextView.setTextColor(
                MaterialColors.getColor(binding.checkPwTextView, com.google.android.material.R.attr.colorPrimary),
            )
        } else {
            binding.checkPwTextView.text = getString(R.string.join_password_mismatch)
            binding.checkPwTextView.setTextColor(
                MaterialColors.getColor(binding.checkPwTextView, com.google.android.material.R.attr.colorOnSurface),
            )
        }
    }

    private fun initJoinButton() {
        binding.joinBtn.setOnClickListener {
            if (pw1 == pw2) {
                if (id.isBlank() || pw1.isBlank()) {
                    Toast
                        .makeText(
                            this,
                            getString(R.string.join_failed_blank),
                            Toast.LENGTH_SHORT,
                        ).show()
                } else {
                    auth
                        .createUserWithEmailAndPassword(id, pw1)
                        .addOnCompleteListener(this) {
                            if (it.isSuccessful) {
                                Toast
                                    .makeText(
                                        this,
                                        getString(R.string.join_succeeded),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                auth.signOut()
                            } else {
                                Toast
                                    .makeText(
                                        this,
                                        getString(R.string.join_failed_exists),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                            }
                        } // 이메일 주소와 비밀번호로 회원가입(Firebase Authentication)
                }
            } else {
                Toast.makeText(this, getString(R.string.join_failed_password), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initLoginbutton() {
        binding.loginBtn.setOnClickListener { finish() }
    }
}
