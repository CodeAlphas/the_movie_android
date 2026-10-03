package com.codealphas.themovie.data.review.di

import com.codealphas.themovie.data.review.ReviewClock
import com.codealphas.themovie.data.review.SystemReviewClock
import com.codealphas.themovie.data.review.remote.FirebaseReviewRealtimeDataSource
import com.codealphas.themovie.data.review.remote.FirebaseReviewStorageDataSource
import com.codealphas.themovie.data.review.remote.ReviewRealtimeDataSource
import com.codealphas.themovie.data.review.remote.ReviewStorageDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// public 함수가 internal 타입을 받거나 반환하면 컴파일이 실패하므로, ReviewRemoteModule 클래스를 internal로 제한
@Module
@InstallIn(SingletonComponent::class)
internal abstract class ReviewRemoteModule {
    @Binds
    @Singleton
    abstract fun bindRealtimeDataSource(impl: FirebaseReviewRealtimeDataSource): ReviewRealtimeDataSource

    @Binds
    @Singleton
    abstract fun bindStorageDataSource(impl: FirebaseReviewStorageDataSource): ReviewStorageDataSource

    @Binds
    abstract fun bindReviewClock(impl: SystemReviewClock): ReviewClock
}
