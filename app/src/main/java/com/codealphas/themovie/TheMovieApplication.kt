package com.codealphas.themovie

import android.app.Application
import android.os.Build
import com.kakao.vectormap.KakaoMapSdk
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TheMovieApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Kakao Map SDK 네이티브 라이브러리가 arm 계열만 있어 x86 에뮬레이터에서 초기화하면 앱 시작이 죽으므로,
        // 지원 ABI에서만 초기화 적용
        if (Build.SUPPORTED_ABIS.any { it in KAKAO_MAP_ABIS }) {
            KakaoMapSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
        }
    }

    private companion object {
        val KAKAO_MAP_ABIS = setOf("arm64-v8a", "armeabi-v7a")
    }
}
