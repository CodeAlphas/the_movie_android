package com.codealphas.themovie.data.review.remote

internal interface ReviewStorageDataSource {
    /**
     * 사진을 Storage에 올린다. 사진의 MIME 타입을 찾아 contentType으로 함께 저장한다.
     * 이미지가 아니거나 형식을 알 수 없을 때, 업로드나 URL 조회가 실패할 때 예외를 던진다.
     *
     * @return 업로드한 사진을 내려받을 수 있는 URL
     */
    suspend fun upload(
        fileName: String,
        uri: String,
    ): String

    /**
     * 사진 파일을 지운다. 파일이 이미 없으면 성공으로 본다.
     * Storage가 보안 규칙으로 거부하면 [ReviewPermissionDeniedException]을, 그 밖의 실패는 다른 예외를 던진다.
     */
    suspend fun delete(fileName: String)
}
