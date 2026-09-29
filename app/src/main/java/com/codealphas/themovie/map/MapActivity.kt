package com.codealphas.themovie.map

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.location.LocationManagerCompat
import com.codealphas.themovie.R
import com.codealphas.themovie.core.android.ui.applySystemBarInsets
import com.codealphas.themovie.core.android.ui.setupAppBar
import com.codealphas.themovie.databinding.ActivityMapBinding
import com.codealphas.themovie.ui.observeRemoteError
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelLayer
import com.kakao.vectormap.label.LabelLayerOptions
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.label.LabelTextBuilder
import com.kakao.vectormap.label.LabelTextStyle
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MapActivity : AppCompatActivity() {
    private companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        const val MAP_ZOOM_LEVEL = 15
        const val CURRENT_LAYER_ID = "current"
        const val THEATER_LAYER_ID = "theater"
        const val CURRENT_STYLE_ID = "currentStyle"
        const val THEATER_STYLE_ID = "theaterStyle"
        const val MARKER_ANCHOR_X = 0.5f
        const val MARKER_ANCHOR_Y = 1.0f
    }

    private lateinit var binding: ActivityMapBinding
    private var kakaoMap: KakaoMap? = null
    private var currentLayer: LabelLayer? = null
    private var theaterLayer: LabelLayer? = null
    private var currentStyles: LabelStyles? = null
    private var theaterStyles: LabelStyles? = null
    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private var locationCancellation: CancellationTokenSource? = null
    private var currentLocationPos: LocationLatLng? = null
    private val viewModel: MapViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySystemBarInsets()

        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupAppBar(binding.toolbar, getString(R.string.map_appbar_title))

        initKakaoMap()
        initCurrentLocationButton()
        // 위치 갱신마다 구독을 추가하면 결과 한 번에 마커가 여러 번 그려지므로, 구독은 여기서 한 번만 등록
        viewModel.remoteError.observeRemoteError(this, binding.root)
        observeCurrentAddress()
        observeNearTheaters()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.resume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 화면이 사라진 뒤 위치 콜백이 도착하지 않도록 진행 중인 요청 취소 적용
        locationCancellation?.cancel()
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
        val labelManager = checkNotNull(map.labelManager)
        // styleId가 둘 다 빈 문자열이면 SDK가 같은 스타일로 보고 두 번째를 등록하지 않으므로, 스타일마다 id를 부여
        val currentStyle = markerStyle(getColor(R.color.map_marker_current), textLineCount = 2)
        val theaterStyle = markerStyle(getColor(R.color.map_marker_theater), textLineCount = 1)
        currentStyles = labelManager.addLabelStyles(LabelStyles.from(CURRENT_STYLE_ID, currentStyle))
        theaterStyles = labelManager.addLabelStyles(LabelStyles.from(THEATER_STYLE_ID, theaterStyle))
        currentLayer = labelManager.addLayer(LabelLayerOptions.from(CURRENT_LAYER_ID))
        theaterLayer = labelManager.addLayer(LabelLayerOptions.from(THEATER_LAYER_ID))
    }

    // 텍스트 줄 수보다 스타일이 적으면 네이티브에서 앱이 종료되므로, 줄 수만큼 스타일 생성
    private fun markerStyle(
        @ColorInt color: Int,
        textLineCount: Int,
    ): LabelStyle {
        val textSize = resources.getDimensionPixelSize(R.dimen.map_label_text_size)
        val textStroke = resources.getDimensionPixelSize(R.dimen.map_label_text_stroke)
        val textColor = getColor(R.color.map_label_text)
        val strokeColor = getColor(R.color.map_label_stroke)
        val textStyles = Array(textLineCount) { LabelTextStyle.from(textSize, textColor, textStroke, strokeColor) }
        return LabelStyle
            .from(markerBitmap(color))
            .setAnchorPoint(MARKER_ANCHOR_X, MARKER_ANCHOR_Y)
            .setTextStyles(*textStyles)
    }

    // 벡터 리소스는 Label 아이콘으로 바로 쓸 수 없으므로, 색을 입힌 Bitmap으로 변환해 적용
    private fun markerBitmap(
        @ColorInt color: Int,
    ): Bitmap {
        val icon = checkNotNull(ContextCompat.getDrawable(this, R.drawable.ic_baseline_location_on_24))
        val drawable = DrawableCompat.wrap(icon.mutate())
        DrawableCompat.setTint(drawable, color)
        val iconSize = resources.getDimensionPixelSize(R.dimen.map_marker_icon_size)
        val bitmap = createBitmap(iconSize, iconSize)
        drawable.setBounds(0, 0, iconSize, iconSize)
        drawable.draw(Canvas(bitmap))
        return bitmap
    }

    private fun initCurrentLocationButton() {
        binding.currentLocationButton.setOnClickListener {
            getCurrentLocation()
        }
    }

    private fun getCurrentLocation() {
        val locationManager = getSystemService(LocationManager::class.java)
        if (!LocationManagerCompat.isLocationEnabled(locationManager)) {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            return
        }

        if (hasLocationPermission()) {
            requestCurrentLocation()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
                LOCATION_PERMISSION_REQUEST_CODE,
            )
        }
    }

    private fun hasLocationPermission(): Boolean =
        isPermissionGranted(android.Manifest.permission.ACCESS_FINE_LOCATION) ||
            isPermissionGranted(android.Manifest.permission.ACCESS_COARSE_LOCATION)

    private fun isPermissionGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            // Android 12 이상에서 대략적인 위치만 허용해도 위치 조회가 가능하므로, 둘 중 하나만 허용돼도 성공 처리
            if (grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
                requestCurrentLocation()
            } else if (isLocationPermissionBlocked()) {
                showPermissionSettingsDialog()
            } else {
                Toast.makeText(this, getString(R.string.map_permission_denied), Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 두 번 거부하면 시스템이 권한 요청 창을 더 이상 띄우지 않으므로, 근거 표시 여부로 영구 거부를 판별해 설정 이동 안내 적용
    private fun isLocationPermissionBlocked(): Boolean =
        listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
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

    // 주변 영화관 검색에는 수십 m 오차가 문제되지 않고 GPS는 응답이 느려서, 권한과 무관하게 균형 우선순위 적용
    @SuppressLint("MissingPermission")
    private fun requestCurrentLocation() {
        // 버튼을 연속으로 누르면 이전 요청의 결과가 뒤늦게 덮어쓰므로, 이전 요청 취소 후 새로 요청
        locationCancellation?.cancel()
        val cancellation = CancellationTokenSource().also { locationCancellation = it }
        fusedLocationClient
            .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellation.token)
            .addOnSuccessListener { location ->
                if (location == null) {
                    showLocationUnavailable()
                } else {
                    onCurrentLocationChanged(LocationLatLng(location.latitude.toFloat(), location.longitude.toFloat()))
                }
            }.addOnFailureListener {
                showLocationUnavailable()
            }
    }

    private fun showLocationUnavailable() {
        Toast.makeText(this, getString(R.string.map_location_unavailable), Toast.LENGTH_SHORT).show()
    }

    private fun onCurrentLocationChanged(locationPos: LocationLatLng) {
        // 지도 준비 전에 위치가 오면 카메라와 Label을 그릴 곳이 없어 크래시하므로 준비된 뒤의 위치만 처리
        val map = kakaoMap ?: return
        map.moveCamera(
            CameraUpdateFactory.newCenterPosition(
                LatLng.from(locationPos.latitude.toDouble(), locationPos.longitude.toDouble()),
                MAP_ZOOM_LEVEL,
            ),
        )

        // 주소 응답이 뒤늦게 와도 가장 최근 위치에 마커를 그리도록 위치 보관
        currentLocationPos = locationPos
        viewModel.makeCurrentAddressApiCall(locationPos.latitude.toString(), locationPos.longitude.toString())
        viewModel.makeTheaterListApiCall(locationPos.latitude.toDouble(), locationPos.longitude.toDouble())
    }

    private fun observeCurrentAddress() {
        viewModel.currentAddress.observe(this) { address ->
            val pos = currentLocationPos ?: return@observe
            val layer = currentLayer ?: return@observe
            val styles = currentStyles ?: return@observe
            layer.removeAll()
            val options =
                LabelOptions
                    .from(LatLng.from(pos.latitude.toDouble(), pos.longitude.toDouble()))
                    .setStyles(styles)
                    .setTexts(
                        LabelTextBuilder().setTexts(
                            getString(R.string.map_current_location),
                            address.fullAddress,
                        ),
                    )
            layer.addLabel(options)
        }
    }

    private fun observeNearTheaters() {
        viewModel.allTheater.observe(this) { theaters ->
            val layer = theaterLayer ?: return@observe
            val styles = theaterStyles ?: return@observe
            layer.removeAll()
            theaters.forEach { theater ->
                layer.addLabel(
                    LabelOptions
                        .from(LatLng.from(theater.latitude, theater.longitude))
                        .setStyles(styles)
                        .setTexts(LabelTextBuilder().setTexts(theater.name)),
                )
            }
        }
    }
}
