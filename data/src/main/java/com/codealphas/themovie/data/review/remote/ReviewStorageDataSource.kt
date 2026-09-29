package com.codealphas.themovie.data.review.remote

internal interface ReviewStorageDataSource {
    // 업로드한 사진을 내려받을 수 있는 URL 반환
    suspend fun upload(
        fileName: String,
        uri: String,
    ): String

    suspend fun delete(fileName: String)
}
