package com.codealphas.themovie.data.review.di

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

// public 함수가 internal 타입을 받으면 컴파일이 실패하므로, DatabaseModule 클래스를 internal로 제한
// 함수에 internal을 붙이면 컴파일된 메서드 이름 뒤에 $와 이 모듈 이름 data가 붙어 Hilt 팩토리가 $data가 붙은 이름을 호출하므로,
// 선언된 함수 이름을 호출하도록 @Provides는 public으로 유지
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
