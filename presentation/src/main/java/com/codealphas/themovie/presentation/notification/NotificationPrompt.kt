package com.codealphas.themovie.presentation.notification

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.codealphas.themovie.presentation.R
import kotlinx.coroutines.launch

internal fun Activity.notificationPermissionState(): NotificationPermissionState {
    val permission = Manifest.permission.POST_NOTIFICATIONS
    return NotificationPermissionState(
        sdkInt = Build.VERSION.SDK_INT,
        isGranted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED,
        shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, permission),
    )
}

internal fun Context.openNotificationSettings() {
    // 앱 알림 설정 화면은 Android 8.0(API 26)부터 있으므로, minSdk 24 기기에서는 앱 정보 화면으로 이동
    val intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        }
    startActivity(intent)
}

@Composable
internal fun rememberNotificationPermissionRequester(snackbarHostState: SnackbarHostState): () -> Unit {
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val contract = remember { ActivityResultContracts.RequestPermission() }
    val launcher =
        rememberLauncherForActivityResult(contract) { isGranted ->
            if (!isGranted) {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = resources.getString(R.string.notification_permission_denied),
                        duration = SnackbarDuration.Long,
                    )
                }
            }
        }
    return remember(launcher) {
        {
            // POST_NOTIFICATIONS는 Android 13(API 33)부터 런타임 권한이므로, 그 전 기기에서는 요청 제외
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
internal fun NotificationPromptDialog(
    onAllow: () -> Unit,
    onLater: () -> Unit,
) {
    AlertDialog(
        // 뒤로 가기나 바깥 터치로 닫아도 나중에와 같은 뜻이므로, 다음 실행에 자동 안내가 다시 뜨지 않도록 나중에와 같게 기록
        onDismissRequest = onLater,
        title = { Text(text = stringResource(R.string.notification_prompt_title)) },
        text = { Text(text = stringResource(R.string.notification_prompt_message)) },
        confirmButton = {
            TextButton(onClick = onAllow) { Text(text = stringResource(R.string.notification_prompt_allow)) }
        },
        dismissButton = {
            TextButton(onClick = onLater) { Text(text = stringResource(R.string.notification_prompt_later)) }
        },
    )
}
