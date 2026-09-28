package com.codealphas.themovie.ui

import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.codealphas.themovie.R
import com.codealphas.themovie.domain.result.RemoteError
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

fun Flow<RemoteError>.observeRemoteError(
    owner: LifecycleOwner,
    anchor: View,
) {
    owner.lifecycleScope.launch {
        owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            collect { error ->
                Snackbar.make(anchor, error.message(anchor), Snackbar.LENGTH_LONG).show()
            }
        }
    }
}

private fun RemoteError.message(anchor: View): String {
    val resources = anchor.resources
    return when (this) {
        RemoteError.Timeout -> resources.getString(R.string.remote_error_timeout)
        RemoteError.Network -> resources.getString(R.string.remote_error_network)
        is RemoteError.Http -> resources.getString(R.string.remote_error_http, code)
        RemoteError.Serialization -> resources.getString(R.string.remote_error_serialization)
        RemoteError.Unknown -> resources.getString(R.string.remote_error_unknown)
    }
}
