package com.codealphas.themovie.data.di

import android.content.Context
import androidx.room.Room
import com.codealphas.themovie.data.review.local.AppDatabase
import com.codealphas.themovie.data.review.local.ReviewDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// public 함수가 internal 타입을 받거나 반환하면 컴파일이 실패하므로, DatabaseModule 클래스를 internal로 제한
@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase =
        Room
            .databaseBuilder(
                context,
                AppDatabase::class.java,
                "review_database",
            ).build()

    @Provides
    @Singleton
    fun provideReviewDao(database: AppDatabase): ReviewDao = database.reviewDao()
}
