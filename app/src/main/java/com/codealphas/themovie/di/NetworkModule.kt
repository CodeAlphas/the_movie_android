package com.codealphas.themovie.di

import com.codealphas.themovie.networks.MapApiService
import com.codealphas.themovie.networks.TmdbApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Qualifier
import javax.inject.Singleton

// Retrofit 바인딩이 둘이라 구분이 없으면 Hilt가 주입 대상을 정하지 못하므로, 서버별로 표시
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TmdbRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TmapRetrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
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
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient()

    @Provides
    @Singleton
    @TmdbRetrofit
    fun provideTmdbRetrofit(
        client: OkHttpClient,
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
    @TmapRetrofit
    fun provideTmapRetrofit(
        client: OkHttpClient,
        json: Json,
    ): Retrofit =
        Retrofit
            .Builder()
            .baseUrl("https://apis.openapi.sk.com/")
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
    fun provideMapApiService(
        @TmapRetrofit retrofit: Retrofit,
    ): MapApiService = retrofit.create(MapApiService::class.java)
}
