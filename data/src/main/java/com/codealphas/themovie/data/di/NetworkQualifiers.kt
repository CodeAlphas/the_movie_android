package com.codealphas.themovie.data.di

import javax.inject.Qualifier

// Retrofit, OkHttpClient, API 키 String이 TMDB와 Kakao에 하나씩 있으므로, 구분이 없으면 Hilt가 어느 쪽을 넣을지 정하지 못해 API별 qualifier 적용
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class TmdbRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class KakaoRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class TmdbOkHttp

@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class KakaoOkHttp

// :app의 ApiKeyModule이 BuildConfig에서 꺼낸 키를 TmdbApiKey, KakaoRestApiKey로 넣으므로, data 모듈 밖에서도 참조하도록 public으로 유지
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TmdbApiKey

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class KakaoRestApiKey
