package com.codealphas.themovie.di

import com.codealphas.themovie.BuildConfig
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
    @TmapApiKey
    fun provideTmapApiKey(): String = BuildConfig.TMAP_API_KEY
}
