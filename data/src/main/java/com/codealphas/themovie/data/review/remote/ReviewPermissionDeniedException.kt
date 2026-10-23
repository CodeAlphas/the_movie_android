package com.codealphas.themovie.data.review.remote

// 보안 규칙이 거부한 요청은 다시 보내도 같은 결과라 재시도하면 업로드가 끝나지 않으므로,
// 네트워크 오류와 구분하도록 권한 거부를 별도 예외로 분리
internal class ReviewPermissionDeniedException(
    cause: Throwable,
) : Exception(cause)
