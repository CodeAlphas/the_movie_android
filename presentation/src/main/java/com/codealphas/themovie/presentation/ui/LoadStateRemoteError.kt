package com.codealphas.themovie.presentation.ui

import androidx.paging.LoadState
import com.codealphas.themovie.domain.result.RemoteError
import com.codealphas.themovie.domain.result.RemoteErrorException

// RemoteErrorException이 아닌 예외는 원인을 알 수 없으므로, Unknown으로 처리
internal val LoadState.Error.remoteError: RemoteError
    get() = (error as? RemoteErrorException)?.error ?: RemoteError.Unknown
