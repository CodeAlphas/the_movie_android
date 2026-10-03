package com.codealphas.themovie.presentation.map

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.codealphas.themovie.core.android.theme.Spacing
import com.codealphas.themovie.core.android.theme.TheMovieTheme
import com.codealphas.themovie.core.android.ui.TheMovieTopAppBar
import com.codealphas.themovie.core.android.ui.rememberThrottledClick
import com.codealphas.themovie.core.android.ui.showToast
import com.codealphas.themovie.domain.map.Theater
import com.codealphas.themovie.presentation.R
import com.codealphas.themovie.presentation.ui.remoteErrorMessage
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraAnimation
import com.kakao.vectormap.camera.CameraUpdate
import com.kakao.vectormap.camera.CameraUpdateFactory
import kotlinx.coroutines.launch

private const val MAP_ZOOM_LEVEL = 15
private const val CAMERA_ANIMATION_MS = 200
private val LOCATION_PERMISSIONS =
    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

@Composable
fun TheaterMapScreen(
    onNavigateUp: () -> Unit,
    viewModel: TheaterMapViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    var showPermissionSettings by rememberSaveable { mutableStateOf(false) }

    val requestCurrentLocation =
        rememberCurrentLocationRequester(
            onLocationReady = { viewModel.onIntent(TheaterMapIntent.LocationReady) },
            onLocationUnavailable = { viewModel.onIntent(TheaterMapIntent.LocationUnavailable) },
            onPermissionDenied = {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = resources.getString(R.string.map_permission_denied),
                        duration = SnackbarDuration.Long,
                    )
                }
            },
            onPermissionBlocked = { showPermissionSettings = true },
        )
    val currentRequestCurrentLocation by rememberUpdatedState(requestCurrentLocation)

    // 화면이 멈춘 동안 보낸 안내를 돌아와서 받도록, 수집은 STARTED 동안만 하고 채널에 남은 effect는 다시 시작할 때 처리
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is TheaterMapEffect.MoveCamera ->
                        kakaoMap?.moveCamera(cameraUpdate(effect.location), CameraAnimation.from(CAMERA_ANIMATION_MS))
                    // showSnackbar는 안내가 사라질 때까지(약 10초) 끝나지 않아 그동안 다음 effect를 처리하지 못하므로,
                    // 안내가 떠 있는 동안 현재 위치 버튼을 다시 눌러도 카메라가 바로 움직이도록 안내는 별도 코루틴에서 표시
                    TheaterMapEffect.ShowLocationFailed ->
                        scope.launch {
                            val result =
                                snackbarHostState.showSnackbar(
                                    message = resources.getString(R.string.map_location_unavailable),
                                    actionLabel = resources.getString(R.string.map_location_retry),
                                    duration = SnackbarDuration.Long,
                                )
                            if (result == SnackbarResult.ActionPerformed) currentRequestCurrentLocation()
                        }
                    is TheaterMapEffect.ShowError ->
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = remoteErrorMessage(resources, effect.error),
                                duration = SnackbarDuration.Long,
                            )
                        }
                }
            }
        }
    }

    TheaterMapContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::onIntent,
        onNavigateUp = onNavigateUp,
        onCurrentLocationClick = requestCurrentLocation,
        onDirectionsClick = { theater ->
            state.currentLocation?.let { start -> context.openDirections(start, theater) }
        },
    ) {
        TheaterKakaoMap(
            state = state,
            kakaoMap = kakaoMap,
            onMapReady = { map ->
                kakaoMap = map
                // 지도가 준비되기 전에 채널로 온 카메라 이동은 다시 오지 않으므로, 지도가 준비된 시점에 상태의 내 위치를 읽어 카메라 이동
                viewModel.state.value.currentLocation
                    ?.let { location -> map.moveCamera(cameraUpdate(location)) }
            },
            onTheaterClick = { theaterId -> viewModel.onIntent(TheaterMapIntent.TheaterClicked(theaterId)) },
        )
    }

    if (showPermissionSettings) {
        PermissionSettingsDialog(
            onMove = {
                showPermissionSettings = false
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null),
                    ),
                )
            },
            onCancel = { showPermissionSettings = false },
        )
    }
}

// Preview가 Activity와 Kakao 지도 엔진 없이 그리도록 지도, 현재 위치 버튼 동작, 길찾기 동작을 밖에서 받아
// 인자가 detekt 한도(5개)를 넘으므로, 인자를 묶는 클래스를 만드는 대신 이 함수에만 인자 수 검사 제외
@Suppress("LongParameterList")
@Composable
internal fun TheaterMapContent(
    state: TheaterMapUiState,
    snackbarHostState: SnackbarHostState,
    onIntent: (TheaterMapIntent) -> Unit,
    onNavigateUp: () -> Unit,
    onCurrentLocationClick: () -> Unit,
    onDirectionsClick: (Theater) -> Unit,
    map: @Composable () -> Unit,
) {
    // 앱바 색이 라이트에서 어둡고 다크에서 밝아 상태 표시줄 뒤까지 칠하면 같은 계열 색의 시계와 배터리 아이콘이 묻히므로,
    // 상태 표시줄 뒤에는 창 배경이 보이도록 앱바를 상태 표시줄 높이만큼 내려 배치
    Scaffold(
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
        topBar = {
            TheMovieTopAppBar(title = stringResource(R.string.map_appbar_title), onNavigateUp = onNavigateUp)
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            map()
            FloatingActionButton(
                onClick = rememberThrottledClick(onClick = onCurrentLocationClick),
                modifier = Modifier.align(Alignment.TopEnd).padding(Spacing.medium),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_my_location_24),
                    contentDescription = stringResource(R.string.map_find_nearby_theaters),
                )
            }
        }
    }

    state.selectedTheater?.let { theater ->
        TheaterSheet(
            theater = theater,
            onDismiss = { onIntent(TheaterMapIntent.TheaterSheetDismissed) },
            onDirectionsClick = { onDirectionsClick(theater) },
        )
    }
}

@Composable
private fun TheaterKakaoMap(
    state: TheaterMapUiState,
    kakaoMap: KakaoMap?,
    onMapReady: (KakaoMap) -> Unit,
    onTheaterClick: (theaterId: String) -> Unit,
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentOnMapReady by rememberUpdatedState(onMapReady)
    val currentOnTheaterClick by rememberUpdatedState(onTheaterClick)
    val mapView =
        remember {
            MapView(context).apply {
                start(
                    object : MapLifeCycleCallback() {
                        override fun onMapDestroy() = Unit

                        override fun onMapError(error: Exception) {
                            context.showToast(R.string.map_load_failed)
                        }
                    },
                    object : KakaoMapReadyCallback() {
                        override fun onMapReady(map: KakaoMap) {
                            currentOnMapReady(map)
                        }
                    },
                )
            }
        }
    // Label 스타일과 레이어는 지도마다 한 번만 등록해야 하므로, 지도가 바뀔 때만 렌더러 생성
    val markerRenderer =
        remember(kakaoMap) {
            kakaoMap?.let { map -> MapMarkerRenderer(context, map) { theaterId -> currentOnTheaterClick(theaterId) } }
        }

    LaunchedEffect(markerRenderer, state) {
        markerRenderer?.render(state)
    }
    DisposableEffect(lifecycle, mapView) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> mapView.resume()
                    Lifecycle.Event.ON_PAUSE -> mapView.pause()
                    else -> Unit
                }
            }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    DisposableEffect(mapView) {
        onDispose {
            // SDK는 Activity가 끝나는 중이 아니면 지도가 창에서 분리돼도 엔진을 멈춰 두기만 하므로,
            // 회전이나 화면 이동 뒤 엔진이 남지 않도록 지도 종료
            // Activity가 끝나는 중이면 지도가 창에서 분리될 때 SDK가 엔진을 종료하므로, 종료를 두 번 요청하지 않도록 이때는 제외
            if (activity?.isFinishing != true) mapView.finish()
        }
    }

    AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
}

private fun cameraUpdate(location: LocationLatLng): CameraUpdate =
    CameraUpdateFactory.newCenterPosition(LatLng.from(location.latitude, location.longitude), MAP_ZOOM_LEVEL)

@Composable
private fun rememberCurrentLocationRequester(
    onLocationReady: () -> Unit,
    onLocationUnavailable: () -> Unit,
    onPermissionDenied: () -> Unit,
    onPermissionBlocked: () -> Unit,
): () -> Unit {
    // 위치 권한 확인과 위치 설정 대화상자에 Activity가 필요하고 지도 화면은 Activity 안에서만 그리므로,
    // 없으면 바로 드러나도록 checkNotNull 적용
    val activity = checkNotNull(LocalActivity.current)
    val currentOnLocationReady by rememberUpdatedState(onLocationReady)
    val currentOnLocationUnavailable by rememberUpdatedState(onLocationUnavailable)
    val currentOnPermissionDenied by rememberUpdatedState(onPermissionDenied)
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
                else -> currentOnPermissionDenied()
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

private fun Context.hasLocationPermission(): Boolean =
    LOCATION_PERMISSIONS.any { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }

// 두 번 거부하면 시스템이 권한 요청 창을 더 이상 띄우지 않으므로, 근거 표시 여부로 영구 거부를 판별해 설정 이동 안내 적용
private fun Activity.isLocationPermissionBlocked(): Boolean =
    LOCATION_PERMISSIONS.none { ActivityCompat.shouldShowRequestPermissionRationale(this, it) }

// Android 11부터 resolveActivity는 <queries> 선언 없이는 설치된 카카오맵도 찾지 못하므로,
// 설치 여부를 미리 확인하지 않고 앱 실행이 실패하면 웹 길찾기로 대체
private fun Context.openDirections(
    start: LocationLatLng,
    theater: Theater,
) {
    val end = LocationLatLng(theater.latitude, theater.longitude)
    try {
        startActivity(Intent(Intent.ACTION_VIEW, KakaoMapDirections.appUrl(start, end).toUri()))
    } catch (_: ActivityNotFoundException) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, KakaoMapDirections.webUrl(start, end).toUri()))
        } catch (_: ActivityNotFoundException) {
            showToast(R.string.map_directions_unavailable)
        }
    }
}

@Composable
private fun PermissionSettingsDialog(
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

@Preview(name = "라이트")
@Preview(name = "다크", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TheaterMapContentPreview() {
    TheMovieTheme {
        TheaterMapContent(
            state = TheaterMapUiState(),
            snackbarHostState = remember { SnackbarHostState() },
            onIntent = {},
            onNavigateUp = {},
            onCurrentLocationClick = {},
            onDirectionsClick = {},
        ) {
            // Preview는 Kakao 지도 엔진을 띄우지 못하므로, 지도 자리에 표면색 영역만 그리도록 처리
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh))
        }
    }
}
