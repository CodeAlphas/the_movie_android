package com.codealphas.themovie.presentation.ui

import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
                Snackbar.make(anchor, remoteErrorMessage(anchor.resources, error), Snackbar.LENGTH_LONG).show()
            }
        }
    }
}
