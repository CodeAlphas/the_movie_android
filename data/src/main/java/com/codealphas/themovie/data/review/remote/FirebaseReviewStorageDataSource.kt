package com.codealphas.themovie.data.review.remote

import android.content.Context
import androidx.core.net.toUri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

internal class FirebaseReviewStorageDataSource
    @Inject
    constructor(
        private val storage: FirebaseStorage,
        @param:ApplicationContext private val context: Context,
    ) : ReviewStorageDataSource {
        override suspend fun upload(
            fileName: String,
            uri: String,
        ): String {
            val photoUri = uri.toUri()
            // 파일 이름으로는 사진의 실제 형식을 알 수 없으므로,
            // Storage 규칙의 이미지 형식 검사를 통과하도록 사진에서 찾은 형식을 업로드 메타데이터로 직접 지정
            val metadata =
                StorageMetadata
                    .Builder()
                    .setContentType(photoContentType(photoUri, context.contentResolver))
                    .build()
            return photoReference(fileName)
                .putFile(photoUri, metadata)
                .await()
                .storage.downloadUrl
                .await()
                .toString()
        }

        override suspend fun delete(fileName: String) {
            try {
                photoReference(fileName).delete().await()
            } catch (e: StorageException) {
                when (e.errorCode) {
                    // 감상문 업로드 Worker가 사진을 지운 뒤 감상문 행을 지우기 전에 멈추면 다음 실행이 같은 사진을 다시 지우는데,
                    // 파일 없음을 실패로 보면 재시도만 반복되어 뒤에 대기 중인 감상문도 올라가지 않으므로, 파일 없음은 성공으로 처리
                    StorageException.ERROR_OBJECT_NOT_FOUND -> Unit
                    // 보안 규칙이 거부한 삭제는 다시 보내도 같은 결과라 재시도하면 업로드가 끝나지 않으므로, 권한 거부로 변환
                    StorageException.ERROR_NOT_AUTHORIZED -> throw ReviewPermissionDeniedException(e)
                    else -> throw e
                }
            }
        }

        private fun photoReference(fileName: String) =
            storage.reference
                .child("review/photo")
                .child(fileName)
    }
