package com.codealphas.themovie.domain.result

// PagingSource는 실패를 예외로만 넘길 수 있으므로, 결과 타입의 RemoteError를 담아 화면까지 전달하는 예외
class RemoteErrorException(
    val error: RemoteError,
) : Exception(error.toString())
