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
    // :data는 :app을 의존하지 않아 secrets 플러그인이 만든 :app의 BuildConfig 키를 읽지 못하므로, :app에서 꺼내 키마다 qualifier를 붙여 제공
    @Provides
    @TmdbApiKey
    fun provideTmdbApiKey(): String = BuildConfig.TMDB_API_KEY

    @Provides
    @KakaoRestApiKey
    fun provideKakaoRestApiKey(): String = BuildConfig.KAKAO_REST_API_KEY
}
