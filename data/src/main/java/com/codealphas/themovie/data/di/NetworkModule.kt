package com.codealphas.themovie.data.di

import com.codealphas.themovie.data.map.remote.KakaoApiKeyInterceptor
import com.codealphas.themovie.data.map.remote.KakaoLocalApiService
import com.codealphas.themovie.data.movie.remote.TmdbApiKeyInterceptor
import com.codealphas.themovie.data.movie.remote.TmdbApiService
import com.codealphas.themovie.data.network.RetryInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

// public 함수가 internal 타입을 받거나 반환하면 컴파일이 실패하므로, NetworkModule 클래스를 internal로 제한
@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {
    @Provides
    @Singleton
    fun provideJson(): Json =
        Json {
            // TMDB 응답의 total_pages, crew처럼 모델에 없는 키가 있으면 응답 전체가 파싱에 실패하므로, 모르는 키는 건너뛰도록 설정
            ignoreUnknownKeys = true
            // nullable 프로퍼티라도 키가 응답에 없으면 파싱에 실패하므로, 키가 없을 때 null로 읽도록 설정
            explicitNulls = false
        }

    @Provides
    @Singleton
    fun provideRetryInterceptor(): RetryInterceptor = RetryInterceptor()

    // TMDB와 Kakao는 붙이는 인증 키가 다르므로, 클라이언트도 API별로 분리
    @Provides
    @Singleton
    @TmdbOkHttp
    fun provideTmdbOkHttpClient(
        @TmdbApiKey apiKey: String,
        retryInterceptor: RetryInterceptor,
    ): OkHttpClient =
        OkHttpClient
            .Builder()
            .addInterceptor(retryInterceptor)
            .addInterceptor(TmdbApiKeyInterceptor(apiKey))
            .build()

    @Provides
    @Singleton
    @KakaoOkHttp
    fun provideKakaoOkHttpClient(
        @KakaoRestApiKey apiKey: String,
        retryInterceptor: RetryInterceptor,
    ): OkHttpClient =
        OkHttpClient
            .Builder()
            .addInterceptor(retryInterceptor)
            .addInterceptor(KakaoApiKeyInterceptor(apiKey))
            .build()

    @Provides
    @Singleton
    @TmdbRetrofit
    fun provideTmdbRetrofit(
        @TmdbOkHttp client: OkHttpClient,
        json: Json,
    ): Retrofit =
        Retrofit
            .Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    @KakaoRetrofit
    fun provideKakaoRetrofit(
        @KakaoOkHttp client: OkHttpClient,
        json: Json,
    ): Retrofit =
        Retrofit
            .Builder()
            .baseUrl("https://dapi.kakao.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideTmdbApiService(
        @TmdbRetrofit retrofit: Retrofit,
    ): TmdbApiService = retrofit.create(TmdbApiService::class.java)

    @Provides
    @Singleton
    fun provideKakaoLocalApiService(
        @KakaoRetrofit retrofit: Retrofit,
    ): KakaoLocalApiService = retrofit.create(KakaoLocalApiService::class.java)
}
