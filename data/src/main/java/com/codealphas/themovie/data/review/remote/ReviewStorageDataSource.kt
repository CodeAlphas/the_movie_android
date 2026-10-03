package com.codealphas.themovie.data.review.remote

internal interface ReviewStorageDataSource {
    /**
     * 사진을 Storage에 올린다. 업로드나 URL 조회가 실패하면 예외를 던진다.
     *
     * @return 업로드한 사진을 내려받을 수 있는 URL
     */
    suspend fun upload(
        fileName: String,
        uri: String,
    ): String

    suspend fun delete(fileName: String)
}
