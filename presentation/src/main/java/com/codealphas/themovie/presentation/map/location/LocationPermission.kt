package com.codealphas.themovie.presentation.map.location

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.codealphas.themovie.presentation.R
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch

private val LOCATION_PERMISSIONS =
    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

private fun Context.hasLocationPermission(): Boolean =
    LOCATION_PERMISSIONS.any { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }

// 두 번 거부하면 시스템이 권한 요청 창을 더 이상 띄우지 않으므로, 근거 표시 여부로 영구 거부를 판별해 설정 이동 안내 적용
private fun Activity.isLocationPermissionBlocked(): Boolean =
    LOCATION_PERMISSIONS.none { ActivityCompat.shouldShowRequestPermissionRationale(this, it) }

internal fun Context.openLocationPermissionSettings() {
    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
}

@Composable
internal fun rememberCurrentLocationRequester(
    snackbarHostState: SnackbarHostState,
    onLocationReady: () -> Unit,
    onLocationUnavailable: () -> Unit,
    onPermissionBlocked: () -> Unit,
): () -> Unit {
    // 위치 권한 확인과 위치 설정 대화상자에 Activity가 필요하고 지도 화면은 Activity 안에서만 그리므로,
    // 없으면 바로 드러나도록 checkNotNull 적용
    val activity = checkNotNull(LocalActivity.current)
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val currentOnLocationReady by rememberUpdatedState(onLocationReady)
    val currentOnLocationUnavailable by rememberUpdatedState(onLocationUnavailable)
    val currentOnPermissionBlocked by rememberUpdatedState(onPermissionBlocked)

    val locationSettingsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) currentOnLocationReady() else currentOnLocationUnavailable()
        }
    // 위치가 꺼져 있으면 설정 화면으로 보내지 않고, 시스템 대화상자로 앱 안에서 켜도록 적용
    val checkLocationSettings = {
        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 0L).build()
        val settingsRequest = LocationSettingsRequest.Builder().addLocationRequest(request).build()
        LocationServices
            .getSettingsClient(activity)
            .checkLocationSettings(settingsRequest)
            .addOnSuccessListener { currentOnLocationReady() }
            .addOnFailureListener { error ->
                if (error is ResolvableApiException) {
                    try {
                        locationSettingsLauncher.launch(IntentSenderRequest.Builder(error.resolution).build())
                    } catch (_: IntentSender.SendIntentException) {
                        currentOnLocationUnavailable()
                    }
                } else {
                    currentOnLocationUnavailable()
                }
            }
    }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            when {
                // Android 12 이상에서 대략적인 위치만 허용해도 위치 조회가 가능하므로, 둘 중 하나만 허용돼도 성공 처리
                grants.values.any { it } -> checkLocationSettings()
                activity.isLocationPermissionBlocked() -> currentOnPermissionBlocked()
                else ->
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = resources.getString(R.string.map_permission_denied),
                            duration = SnackbarDuration.Long,
                        )
                    }
            }
        }

    return {
        if (activity.hasLocationPermission()) {
            checkLocationSettings()
        } else {
            permissionLauncher.launch(
                LOCATION_PERMISSIONS,
            )
        }
    }
}

@Composable
internal fun LocationPermissionSettingsDialog(
    onMove: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(text = stringResource(R.string.map_permission_settings_title)) },
        text = { Text(text = stringResource(R.string.map_permission_settings_message)) },
        confirmButton = {
            TextButton(onClick = onMove) { Text(text = stringResource(R.string.map_permission_settings_move)) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(text = stringResource(R.string.map_permission_settings_cancel)) }
        },
    )
}
