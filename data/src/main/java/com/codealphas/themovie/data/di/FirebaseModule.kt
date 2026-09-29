package com.codealphas.themovie.data.di

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.database
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val STORAGE_RETRY_TIME_MILLIS = 30_000L

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = Firebase.auth

    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase = Firebase.database

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage =
        Firebase.storage.apply {
            // Storage는 실패한 요청을 기본 10분 동안 다시 시도해 오프라인에서 삭제가 오래 남으므로, 재시도 시간을 30초로 제한
            maxOperationRetryTimeMillis = STORAGE_RETRY_TIME_MILLIS
            maxUploadRetryTimeMillis = STORAGE_RETRY_TIME_MILLIS
        }
}
