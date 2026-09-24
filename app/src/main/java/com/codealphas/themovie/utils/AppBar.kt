package com.codealphas.themovie.utils

import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

fun AppCompatActivity.setupAppBar(
    toolbar: MaterialToolbar,
    title: String,
    showBack: Boolean = true,
) {
    setSupportActionBar(toolbar)
    supportActionBar?.title = title
    if (!showBack) return
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    // parentActivityName이 없으면 화살표를 눌러도 아무 동작도 하지 않으므로, 뒤로 가기처럼 현재 화면을 닫도록 처리
    toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
}
