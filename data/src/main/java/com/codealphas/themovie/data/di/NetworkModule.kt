package com.codealphas.themovie.data.di

import com.codealphas.themovie.data.map.remote.KakaoApiKeyInterceptor
import com.codealphas.themovie.data.map.remote.KakaoLocalService
import com.codealphas.themovie.data.movie.remote.TmdbApiKeyInterceptor
import com.codealphas.themovie.data.movie.remote.TmdbApiService
import com.codealphas.themovie.data.remote.RetryInterceptor
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

// public 함수가 internal 타입을 받으면 컴파일이 실패하므로, NetworkModule 클래스를 internal로 제한
// 함수에 internal을 붙이면 컴파일된 메서드 이름 뒤에 $와 이 모듈 이름 data가 붙어 Hilt 팩토리가 $data가 붙은 이름을 호출하므로,
// 선언된 함수 이름을 호출하도록 @Provides는 public으로 유지
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

    // TMDB와 Kakao는 붙이는 인증 키가 다르므로, 클라이언트도 API별로 나눔
    @Provides
    @Singleton
    @TmdbOkHttp
    fun provideTmdbOkHttpClient(
        @TmdbApiKey apiKey: String,
        retryInterceptor: RetryInterceptor,
    ): OkHttpClient =
        OkHttpClient
            .Builder()
            // proceed()는 다음 인터셉터로 들어가므로, 재시도마다 인증 키를 다시 붙이도록 키 인터셉터보다 먼저 등록
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
            // proceed()는 다음 인터셉터로 들어가므로, 재시도마다 인증 키를 다시 붙이도록 키 인터셉터보다 먼저 등록
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
    fun provideKakaoLocalService(
        @KakaoRetrofit retrofit: Retrofit,
    ): KakaoLocalService = retrofit.create(KakaoLocalService::class.java)
}
