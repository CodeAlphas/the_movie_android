package com.codealphas.themovie

import android.app.Application
import android.os.Build
import com.codealphas.themovie.notification.NotificationChannels
import com.kakao.vectormap.KakaoMapSdk
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TheMovieApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // FCM이 백그라운드 알림 메시지를 표시하기 전에 채널이 있어야 Manifest의 기본 채널로 들어가므로, 프로세스 시작 시 생성 적용
        NotificationChannels.create(this)
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
