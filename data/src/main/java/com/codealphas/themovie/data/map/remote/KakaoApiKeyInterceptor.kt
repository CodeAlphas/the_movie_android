package com.codealphas.themovie.data.map.remote

import okhttp3.Interceptor
import okhttp3.Response

// Kakao Local은 인증을 Authorization 헤더의 KakaoAK 접두사로 받으므로, 요청에 REST API 키를 붙이도록 처리
internal class KakaoApiKeyInterceptor(
    private val restApiKey: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request =
            chain
                .request()
                .newBuilder()
                .header("Authorization", "KakaoAK $restApiKey")
                .build()
        return chain.proceed(request)
    }
}
