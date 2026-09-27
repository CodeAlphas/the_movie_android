package com.codealphas.themovie.data

import com.codealphas.themovie.data.movie.MovieRepositoryImpl
import com.codealphas.themovie.data.review.ReviewRepositoryImpl
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
}
