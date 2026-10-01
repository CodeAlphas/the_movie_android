package com.codealphas.themovie.presentation.ui

import android.content.res.Resources
import com.codealphas.themovie.domain.result.RemoteError
import com.codealphas.themovie.presentation.R

internal fun remoteErrorMessage(
    resources: Resources,
    error: RemoteError,
): String =
    when (error) {
        RemoteError.Timeout -> resources.getString(R.string.remote_error_timeout)
        RemoteError.Network -> resources.getString(R.string.remote_error_network)
        is RemoteError.Http -> resources.getString(R.string.remote_error_http, error.code)
        RemoteError.Serialization -> resources.getString(R.string.remote_error_serialization)
        RemoteError.Unknown -> resources.getString(R.string.remote_error_unknown)
    }
