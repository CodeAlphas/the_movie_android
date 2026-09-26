package com.codealphas.themovie.di

import android.content.Context
import androidx.room.Room
import com.codealphas.themovie.database.AppDatabase
import com.codealphas.themovie.database.ReviewDao
import com.codealphas.themovie.repository.ReviewRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
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
            )
            // 로그아웃이 deleteAll()을 메인 스레드에서 부르므로, 그 호출을 옮기기 전까지 메인 스레드 조회를 허용
            .allowMainThreadQueries()
            .build()

    @Provides
    @Singleton
    fun provideReviewDao(database: AppDatabase): ReviewDao = database.reviewDao()

    @Provides
    @Singleton
    fun provideReviewRepository(dao: ReviewDao): ReviewRepository = ReviewRepository(dao)
}
