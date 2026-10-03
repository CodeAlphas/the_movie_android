package com.codealphas.themovie.data.di

import com.codealphas.themovie.data.auth.AuthRepositoryImpl
import com.codealphas.themovie.data.map.TheaterRepositoryImpl
import com.codealphas.themovie.data.movie.MovieRepositoryImpl
import com.codealphas.themovie.data.notification.NotificationPromptRepositoryImpl
import com.codealphas.themovie.data.review.ReviewRepositoryImpl
import com.codealphas.themovie.domain.auth.AuthRepository
import com.codealphas.themovie.domain.map.TheaterRepository
import com.codealphas.themovie.domain.movie.MovieRepository
import com.codealphas.themovie.domain.notification.NotificationPromptRepository
import com.codealphas.themovie.domain.review.ReviewRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// public 함수가 internal 타입을 받거나 반환하면 컴파일이 실패하므로, RepositoryModule 클래스를 internal로 제한
@Module
@InstallIn(SingletonComponent::class)
internal abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindMovieRepository(impl: MovieRepositoryImpl): MovieRepository

    @Binds
    @Singleton
    abstract fun bindReviewRepository(impl: ReviewRepositoryImpl): ReviewRepository

    @Binds
    @Singleton
    abstract fun bindTheaterRepository(impl: TheaterRepositoryImpl): TheaterRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindNotificationPromptRepository(impl: NotificationPromptRepositoryImpl): NotificationPromptRepository
}
