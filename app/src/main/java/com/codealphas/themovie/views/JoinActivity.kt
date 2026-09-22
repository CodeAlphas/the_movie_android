package com.example.movieapplication.views

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import com.example.movieapplication.databinding.ActivityJoinBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class JoinActivity : AppCompatActivity() {

    private lateinit var binding: ActivityJoinBinding
    private var id: String = ""
    private var pw1: String = ""
    private var pw2: String = ""
    private val auth: FirebaseAuth by lazy { Firebase.auth }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

    private fun checkPassword(pw1: String, pw2: String) {
        if (pw1 == pw2) {
            binding.checkPwTextView.text = "비밀번호와 일치합니다."
            binding.checkPwTextView.setTextColor(Color.GREEN)
        } else {
            binding.checkPwTextView.text = "비밀번호와 일치하지 않습니다."
            binding.checkPwTextView.setTextColor(Color.WHITE)
        }
    }

    private fun initJoinButton() {
        binding.joinBtn.setOnClickListener {
            if (pw1 == pw2) {
                if (id.isBlank() || pw1.isBlank()) {
                    Toast.makeText(
                        this,
                        "회원가입에 실패했습니다. 이메일 또는 비밀번호를 다시 확인해주세요.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    auth.createUserWithEmailAndPassword(id, pw1)
                        .addOnCompleteListener(this) {
                            if (it.isSuccessful) {
                                Toast.makeText(
                                    this,
                                    "회원가입에 성공했습니다. 로그인하기 버튼을 눌러 로그인해주세요.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                auth.signOut()
                            } else {
                                Toast.makeText(
                                    this,
                                    "이미 가입한 이메일이거나 회원가입에 실패했습니다.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } // 이메일 주소와 비밀번호로 회원가입(Firebase Authentication)
                }
            } else {
                Toast.makeText(this, "회원가입에 실패했습니다. 비밀번호를 다시 확인해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initLoginbutton() {
        binding.loginBtn.setOnClickListener { finish() }
    }
}