package com.codealphas.themovie.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.codealphas.themovie.R
import com.codealphas.themovie.databinding.ActivityMapBinding
import com.codealphas.themovie.models.AddressFromServer
import com.codealphas.themovie.models.LocationLatLng
import com.codealphas.themovie.models.PoisFromServer
import com.codealphas.themovie.utils.applySystemBarInsets
import com.codealphas.themovie.utils.setupAppBar
import com.codealphas.themovie.viewmodels.MapViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MapActivity :
    AppCompatActivity(),
    OnMapReadyCallback {
    private companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        const val MAP_ZOOM = 17.0f
    }

    private lateinit var binding: ActivityMapBinding
    private lateinit var googleMap: GoogleMap
    private lateinit var locationManager: LocationManager
    private lateinit var myLocationListener: MyLocationListener
    private val viewModel: MapViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySystemBarInsets()

        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupAppBar(binding.toolbar, "내 근처 영화관")

        initGoogleMap()
        initCurrentLocationButton()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::locationManager.isInitialized) {
            locationManager.removeUpdates(myLocationListener)
        }
    }

    private fun initGoogleMap() {
        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isCompassEnabled = true
    }

    private fun initCurrentLocationButton() {
        binding.currentLocationButton.setOnClickListener {
            getCurrentLocation()
        }
    }

    private fun getCurrentLocation() {
        if (::locationManager.isInitialized.not()) {
            locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        }

        val isGpsAvailable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (isGpsAvailable) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                ) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    ),
                    LOCATION_PERMISSION_REQUEST_CODE,
                )
            } else {
                setMyLocationListener()
            }
        } else {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                grantResults[1] == PackageManager.PERMISSION_GRANTED
            ) {
                setMyLocationListener()
            } else {
                Toast.makeText(this, "권한을 부여받지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun setMyLocationListener() {
        val minTime = 1500L // 설정된 시간 간격으로 이벤트 전달, 1.5s
        val minDistance = 100f // 설정된 거리만큼의 변화가 있을 때 이벤트를 전달, 100m

        if (::myLocationListener.isInitialized.not()) {
            myLocationListener = MyLocationListener()
        }

        with(locationManager) {
            requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                minTime,
                minDistance,
                myLocationListener,
            )
            requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                minTime,
                minDistance,
                myLocationListener,
            )
        }
    }

    inner class MyLocationListener : LocationListener {
        override fun onLocationChanged(location: Location) {
            try {
                location.latitude
                val currentLocationPos =
                    LocationLatLng(
                        location.latitude.toFloat(),
                        location.longitude.toFloat(),
                    )
                onCurrentLocationChanged(currentLocationPos)
            } catch (e: Exception) {
                Log.e("MapActivity", "위치 정보 갱신 불가", e)
                Toast.makeText(this@MapActivity, "위치 정보 갱신 불가", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onStatusChanged(
            provider: String?,
            status: Int,
            extras: Bundle?,
        ) = Unit

        override fun onProviderEnabled(provider: String) = Unit

        override fun onProviderDisabled(provider: String) = Unit
        // https://stackoverflow.com/questions/64638260/android-locationlistener-abstractmethoderror-on-onstatuschanged-and-onproviderd
    }

    private fun onCurrentLocationChanged(currentLocationPos: LocationLatLng) {
        googleMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(
                    currentLocationPos.latitude.toDouble(),
                    currentLocationPos.longitude.toDouble(),
                ),
                MAP_ZOOM,
            ),
        )

        showCurrentLocationMarker(currentLocationPos)
        showNearTheater(currentLocationPos)
    }

    private fun showCurrentLocationMarker(currentLocationPos: LocationLatLng) {
        viewModel.makeCurrentAddressApiCall(
            currentLocationPos.latitude.toString(),
            currentLocationPos.longitude.toString(),
        )
        viewModel.currentAddress
            .observe(
                this,
                Observer<AddressFromServer> {
                    if (it != null) {
                        googleMap.clear()
                        it.addressInfo.apply {
                            val currentMarker = MarkerOptions()

                            currentMarker.position(
                                LatLng(
                                    currentLocationPos.latitude.toDouble(),
                                    currentLocationPos.longitude.toDouble(),
                                ),
                            )
                            currentMarker.title("현재 위치")
                            currentMarker.snippet(this.fullAddress)
                            googleMap.addMarker(currentMarker)
                        }
                    } else {
                        // Log.d(TAG, "에러 발생")
                    }
                },
            )
    }

    private fun showNearTheater(currentLocationPos: LocationLatLng) {
        val categories = "영화관"
        val centerLat = currentLocationPos.latitude.toDouble()
        val centerLon = currentLocationPos.longitude.toDouble()

        viewModel.makeTheaterListApiCall(categories, centerLat, centerLon)
        viewModel.allTheater
            .observe(
                this,
                Observer<PoisFromServer> {
                    if (it != null) {
                        it.searchPoiInfo.pois.apply {
                            this.poi.forEach {
                                val poiMarker = MarkerOptions()
                                val bmDescriptor =
                                    BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)

                                poiMarker.icon(bmDescriptor)
                                poiMarker.position(LatLng(it.noorLat, it.noorLon))
                                poiMarker.title(it.name)
                                poiMarker.snippet(
                                    "${it.upperAddrName} ${it.middleAddrName} ${it.lowerAddrName} ${it.detailAddrName}",
                                )
                                googleMap.addMarker(poiMarker)
                            }
                        }
                    } else {
                        // Log.d(TAG, "에러 발생")
                    }
                },
            )
    }
}
