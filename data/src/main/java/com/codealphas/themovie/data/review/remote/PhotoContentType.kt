package com.codealphas.themovie.data.review.remote

import android.content.ContentResolver
import android.net.Uri
import android.webkit.MimeTypeMap

/**
 * 업로드할 사진의 MIME 타입을 찾는다. 이미지가 아니거나 형식을 알 수 없으면 [IllegalArgumentException]을 던진다.
 *
 * Photo Picker 사진(content uri)은 [ContentResolver]로, 카메라 촬영 사진(file uri)은 파일 확장자로 찾는다.
 */
internal fun photoContentType(
    uri: Uri,
    contentResolver: ContentResolver,
): String {
    // 촬영 사진은 파일 경로(file uri)라 ContentResolver에 물어도 형식을 알 수 없으므로, 파일 확장자로 형식 조회
    val type =
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            contentResolver.getType(uri)
        } else {
            val extension = uri.lastPathSegment?.substringAfterLast('.', missingDelimiterValue = "")?.lowercase()
            extension?.let(MimeTypeMap.getSingleton()::getMimeTypeFromExtension)
        }
    // Storage 규칙이 이미지가 아닌 업로드를 거부하므로, 사진 파일을 다 올린 뒤 거부되지 않도록 업로드 전에 실패 처리
    return requireNotNull(type?.takeIf { it.startsWith("image/") }) { "사진 형식을 알 수 없음: $uri" }
}
