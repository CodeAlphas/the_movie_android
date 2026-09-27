package com.codealphas.themovie.di

import javax.inject.Qualifier

// Retrofit, OkHttpClient, API 키 String이 TMDB와 TMap에 하나씩 있으므로, 구분이 없으면 Hilt가 어느 쪽을 넣을지 정하지 못해 API별 qualifier 적용
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TmdbRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TmapRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TmdbOkHttp

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TmapOkHttp

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TmdbApiKey

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TmapApiKey
