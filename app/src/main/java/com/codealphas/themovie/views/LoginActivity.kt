package com.example.movieapplication.views

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.movieapplication.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var id: String
    private lateinit var pw: String
    private val auth: FirebaseAuth by lazy { Firebase.auth }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initLoginbutton()
        initJoinButton()
    }

    private fun initLoginbutton() {
        binding.loginBtn.setOnClickListener {
            id = binding.idInput.text.toString()
            pw = binding.pwInput.text.toString()

            if (id.isBlank() || pw.isBlank()) {
                Toast.makeText(this, "로그인에 실패했습니다. 이메일 또는 비밀번호를 다시 확인해주세요.", Toast.LENGTH_SHORT)
                    .show()
            } else {
                auth.signInWithEmailAndPassword(id, pw)
                    .addOnCompleteListener(this) {
                        if (it.isSuccessful) {
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(
                                this,
                                "로그인에 실패했습니다. 이메일 또는 비밀번호를 다시 확인해주세요.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            } // 이메일 주소와 비밀번호로 로그인(Firebase Authentication)
        }
    }

    private fun initJoinButton() {
        binding.joinBtn.setOnClickListener {
            startActivity(Intent(this, JoinActivity::class.java))
        }
    }
}