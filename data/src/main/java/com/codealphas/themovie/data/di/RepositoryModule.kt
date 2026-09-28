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

// public 함수가 internal 타입을 받으면 컴파일이 실패하므로, RepositoryModule 클래스를 internal로 제한
// 함수에 internal을 붙이면 컴파일된 메서드 이름 뒤에 $와 이 모듈 이름 data가 붙어 Hilt 팩토리가 $data가 붙은 이름을 호출하므로,
// 선언된 함수 이름을 호출하도록 @Binds는 public으로 유지
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
}
