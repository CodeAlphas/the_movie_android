package com.codealphas.themovie.data.map.remote

import okhttp3.Interceptor
import okhttp3.Response

// TMap은 인증을 appKey 헤더로 받으므로, 요청에 키를 붙이도록 처리
class TmapApiKeyInterceptor(
    private val appKey: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request =
            chain
                .request()
                .newBuilder()
                .header("appKey", appKey)
                .build()
        return chain.proceed(request)
    }
}
