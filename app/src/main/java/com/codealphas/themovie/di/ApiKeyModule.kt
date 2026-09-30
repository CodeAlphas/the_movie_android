package com.codealphas.themovie.di

import com.codealphas.themovie.BuildConfig
import com.codealphas.themovie.data.di.KakaoRestApiKey
import com.codealphas.themovie.data.di.TmdbApiKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ApiKeyModule {
    // 네트워크 코드가 BuildConfig를 참조하면 모듈 분리 때 키가 따라가므로, app에서 키만 꺼내 주입
    @Provides
    @TmdbApiKey
    fun provideTmdbApiKey(): String = BuildConfig.TMDB_API_KEY

    @Provides
    @KakaoRestApiKey
    fun provideKakaoRestApiKey(): String = BuildConfig.KAKAO_REST_API_KEY
}
