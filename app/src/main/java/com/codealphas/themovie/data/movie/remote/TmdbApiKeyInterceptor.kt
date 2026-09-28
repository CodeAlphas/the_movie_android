package com.codealphas.themovie.data.movie.remote

import okhttp3.Interceptor
import okhttp3.Response

// TMDB는 인증을 api_key 쿼리로 받으므로, 요청 URL에 키를 붙이도록 처리
class TmdbApiKeyInterceptor(
    private val apiKey: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val url =
            original.url
                .newBuilder()
                .setQueryParameter("api_key", apiKey)
                .build()
        return chain.proceed(
            original
                .newBuilder()
                .url(url)
                .build(),
        )
    }
}
