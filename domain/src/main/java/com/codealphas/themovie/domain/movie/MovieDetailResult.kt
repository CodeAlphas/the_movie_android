package com.codealphas.themovie.domain.movie

import com.codealphas.themovie.domain.result.RemoteError

// 상세 본문은 성공하고 출연진이나 영상만 실패할 수 있어서,
// DataResult 하나로는 본문과 오류를 같이 못 실으므로 둘을 한 값으로 둠
data class MovieDetailResult(
    val detail: MovieDetail?,
    val errors: List<RemoteError>,
)
