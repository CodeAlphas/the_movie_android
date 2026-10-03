package com.codealphas.themovie.domain.movie

import com.codealphas.themovie.domain.result.RemoteError

// 본문은 성공하고 출연진이나 영상만 실패하면 DataResult 하나에 본문과 오류를 함께 담지 못하므로, 상세와 오류 목록을 한 값에 담는 결과 타입 사용
data class MovieDetailResult(
    val detail: MovieDetail?,
    val errors: List<RemoteError>,
)
