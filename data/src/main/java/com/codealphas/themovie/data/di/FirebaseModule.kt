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
            // 오프라인이면 Storage가 실패한 요청을 최대 10분 동안 다시 시도해 그동안 저장 중 화면이 막히므로, 오래 기다리지 않도록 재시도 시간 제한
            maxOperationRetryTimeMillis = STORAGE_RETRY_TIME_MILLIS
            maxUploadRetryTimeMillis = STORAGE_RETRY_TIME_MILLIS
            // maxDownloadRetryTimeMillis는 이름과 달리 사진 삭제 재시도에도 쓰이므로, 사진 삭제도 같은 시간 안에 끝나도록 함께 제한
            maxDownloadRetryTimeMillis = STORAGE_RETRY_TIME_MILLIS
        }
}
