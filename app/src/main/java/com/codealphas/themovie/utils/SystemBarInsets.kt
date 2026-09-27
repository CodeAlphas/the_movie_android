package com.codealphas.themovie.utils

import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

// targetSdk 35부터 Edge-to-Edge가 기본이라, 콘텐츠가 상태 표시줄·내비게이션 바·카메라 컷아웃에 가려지지 않도록 그 높이만큼 패딩 적용
fun AppCompatActivity.applySystemBarInsets() {
    enableEdgeToEdge()
    val content = findViewById<ViewGroup>(android.R.id.content)
    ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
        val bars =
            insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
            )
        // Edge-to-Edge에서는 adjustResize가 무시되므로, 키보드가 입력란을 가리지 않도록 키보드 높이를 하단 패딩에 반영
        val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
        // content(레이아웃을 감싸는 시스템 프레임)에 패딩을 주면 여백에 윈도우 배경이 보이므로, 패딩은 레이아웃 루트에 적용
        val root =
            (view as ViewGroup).getChildAt(0)
                ?: return@setOnApplyWindowInsetsListener insets
        root.updatePadding(
            left = bars.left,
            top = bars.top,
            right = bars.right,
            bottom = maxOf(bars.bottom, ime.bottom),
        )
        // 이 리스너가 CONSUMED를 반환하면 안쪽 뷰에는 insets가 전달되지 않으므로, 검색창이 키보드 표시 여부를 받도록 레이아웃 루트에 원래 insets를 직접 전달
        ViewCompat.dispatchApplyWindowInsets(root, insets)
        WindowInsetsCompat.CONSUMED
    }
}
