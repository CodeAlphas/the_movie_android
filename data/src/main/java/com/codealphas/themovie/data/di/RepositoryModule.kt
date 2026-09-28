package com.codealphas.themovie.data.di

import com.codealphas.themovie.data.auth.AuthRepositoryImpl
import com.codealphas.themovie.data.map.TheaterRepositoryImpl
import com.codealphas.themovie.data.movie.MovieRepositoryImpl
import com.codealphas.themovie.data.review.ReviewRepositoryImpl
import com.codealphas.themovie.domain.auth.AuthRepository
import com.codealphas.themovie.domain.map.TheaterRepository
import com.codealphas.themovie.domain.movie.MovieRepository
import com.codealphas.themovie.domain.review.ReviewRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
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
}
