package com.codealphas.themovie.presentation.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.codealphas.themovie.presentation.R

@Composable
internal fun LogoutConfirmDialog(
    pendingCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.logout_pending_reviews_title)) },
        text = { Text(text = stringResource(R.string.logout_pending_reviews_message, pendingCount)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(text = stringResource(R.string.common_logout)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.logout_pending_reviews_cancel)) }
        },
    )
}
