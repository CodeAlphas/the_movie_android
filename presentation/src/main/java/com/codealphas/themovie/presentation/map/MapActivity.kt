package com.codealphas.themovie.presentation.map

import android.Manifest
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.codealphas.themovie.core.android.ui.applySystemBarInsets
import com.codealphas.themovie.core.android.ui.setupAppBar
import com.codealphas.themovie.presentation.R
import com.codealphas.themovie.presentation.databinding.ActivityMapBinding
import com.codealphas.themovie.presentation.ui.observeRemoteError
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.material.snackbar.Snackbar
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.camera.CameraAnimation
import com.kakao.vectormap.camera.CameraUpdateFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MapActivity : AppCompatActivity() {
    private companion object {
        const val MAP_ZOOM_LEVEL = 15
        const val CAMERA_ANIMATION_MS = 200
    }

    private lateinit var binding: ActivityMapBinding
    private var kakaoMap: KakaoMap? = null
    private var markerRenderer: MapMarkerRenderer? = null
    private val viewModel: MapViewModel by viewModels()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            // Android 12 이상에서 대략적인 위치만 허용해도 위치 조회가 가능하므로, 둘 중 하나만 허용돼도 성공 처리
            if (grants.values.any { it }) {
                checkLocationSettings()
            } else if (isLocationPermissionBlocked()) {
                showPermissionSettingsDialog()
            } else {
                Snackbar.make(binding.root, R.string.map_permission_denied, Snackbar.LENGTH_LONG).show()
            }
        }

    private val locationSettingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                viewModel.loadCurrentLocationAndTheaters()
            } else {
                showLocationUnavailable()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySystemBarInsets()

        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupAppBar(binding.toolbar, getString(R.string.map_appbar_title))

        initKakaoMap()
        binding.currentLocationButton.setOnClickListener { requestCurrentLocation() }
        viewModel.remoteError.observeRemoteError(this, binding.root)
        observeLocationFailed()
        observeUiState()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.resume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.pause()
    }

    private fun initKakaoMap() {
        binding.mapView.start(
            object : MapLifeCycleCallback() {
                override fun onMapDestroy() = Unit

                override fun onMapError(error: Exception) {
                    Toast.makeText(this@MapActivity, getString(R.string.map_load_failed), Toast.LENGTH_SHORT).show()
                }
            },
            object : KakaoMapReadyCallback() {
                override fun onMapReady(map: KakaoMap) {
                    onKakaoMapReady(map)
                }
            },
        )
    }

    private fun onKakaoMapReady(map: KakaoMap) {
        kakaoMap = map
        markerRenderer = MapMarkerRenderer(this, map, ::showTheaterBottomSheet)
        // 회전으로 화면이 다시 만들어지면 상태 구독이 지도 준비보다 먼저 값을 받아 그리지 못하므로, 준비 직후 저장된 상태 재적용
        val state = viewModel.uiState.value
        markerRenderer?.render(state)
        state.currentLocation?.let { location ->
            map.moveCamera(cameraUpdate(location))
        }
    }

    // 같은 Label을 빠르게 두 번 누르면 시트가 겹쳐 뜨므로, 이미 떠 있는 시트가 있으면 무시하도록 적용
    private fun showTheaterBottomSheet(theaterId: String) {
        if (supportFragmentManager.findFragmentByTag(TheaterBottomSheet.TAG) != null) return
        TheaterBottomSheet.newInstance(theaterId).show(supportFragmentManager, TheaterBottomSheet.TAG)
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state -> markerRenderer?.render(state) }
                }
                launch {
                    viewModel.locationReceived.collect { location ->
                        kakaoMap?.moveCamera(cameraUpdate(location), CameraAnimation.from(CAMERA_ANIMATION_MS))
                    }
                }
            }
        }
    }

    private fun cameraUpdate(location: LocationLatLng) =
        CameraUpdateFactory.newCenterPosition(LatLng.from(location.latitude, location.longitude), MAP_ZOOM_LEVEL)

    private fun observeLocationFailed() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.locationFailed.collect { showLocationUnavailable() }
            }
        }
    }

    private fun showLocationUnavailable() {
        Snackbar
            .make(binding.root, R.string.map_location_unavailable, Snackbar.LENGTH_LONG)
            .setAction(R.string.map_location_retry) { requestCurrentLocation() }
            .show()
    }

    private fun requestCurrentLocation() {
        if (hasLocationPermission()) {
            checkLocationSettings()
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            )
        }
    }

    // 위치가 꺼져 있으면 설정 화면으로 보내지 않고, 시스템 대화상자로 앱 안에서 켜도록 적용
    private fun checkLocationSettings() {
        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 0L).build()
        val settingsRequest = LocationSettingsRequest.Builder().addLocationRequest(request).build()
        LocationServices
            .getSettingsClient(this)
            .checkLocationSettings(settingsRequest)
            .addOnSuccessListener { viewModel.loadCurrentLocationAndTheaters() }
            .addOnFailureListener { error ->
                if (error is ResolvableApiException) {
                    launchLocationSettingsResolution(error)
                } else {
                    showLocationUnavailable()
                }
            }
    }

    private fun launchLocationSettingsResolution(error: ResolvableApiException) {
        try {
            locationSettingsLauncher.launch(IntentSenderRequest.Builder(error.resolution).build())
        } catch (_: IntentSender.SendIntentException) {
            showLocationUnavailable()
        }
    }

    private fun hasLocationPermission(): Boolean =
        isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION) ||
            isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION)

    private fun isPermissionGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    // 두 번 거부하면 시스템이 권한 요청 창을 더 이상 띄우지 않으므로, 근거 표시 여부로 영구 거부를 판별해 설정 이동 안내 적용
    private fun isLocationPermissionBlocked(): Boolean =
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ).none { ActivityCompat.shouldShowRequestPermissionRationale(this, it) }

    private fun showPermissionSettingsDialog() {
        AlertDialog
            .Builder(this)
            .setTitle(getString(R.string.map_permission_settings_title))
            .setMessage(getString(R.string.map_permission_settings_message))
            .setPositiveButton(getString(R.string.map_permission_settings_move)) { _, _ ->
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)),
                )
            }.setNegativeButton(getString(R.string.map_permission_settings_cancel), null)
            .show()
    }
}
