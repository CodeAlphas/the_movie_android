package com.codealphas.themovie.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codealphas.themovie.domain.map.Address
import com.codealphas.themovie.domain.map.Theater
import com.codealphas.themovie.domain.map.TheaterRepository
import com.codealphas.themovie.domain.result.DataResult
import com.codealphas.themovie.domain.result.RemoteError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapUiState(
    val currentLocation: LocationLatLng? = null,
    val address: Address? = null,
    val theaters: List<Theater> = emptyList(),
)

@HiltViewModel
class MapViewModel
    @Inject
    constructor(
        private val repository: TheaterRepository,
        private val locationProvider: CurrentLocationProvider,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(MapUiState())
        val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

        private val _remoteError = MutableSharedFlow<RemoteError>()
        val remoteError: SharedFlow<RemoteError> = _remoteError

        private val _locationFailed = MutableSharedFlow<Unit>()
        val locationFailed: SharedFlow<Unit> = _locationFailed

        // 같은 위치를 다시 받아도 지도를 직접 옮긴 사용자가 현재 위치로 돌아올 수 있도록, 상태가 아닌 일회성 이벤트로 전달
        private val _locationReceived = MutableSharedFlow<LocationLatLng>()
        val locationReceived: SharedFlow<LocationLatLng> = _locationReceived

        private var locationJob: Job? = null

        fun loadCurrentLocationAndTheaters() {
            // 앞 요청의 응답이 뒤늦게 와서 새 위치의 상태를 덮지 않도록, 진행 중인 요청을 취소하고 새로 시작
            locationJob?.cancel()
            locationJob =
                viewModelScope.launch {
                    val location = locationProvider.getCurrentLocation()
                    if (location == null) {
                        _locationFailed.emit(Unit)
                        return@launch
                    }
                    // 이전 위치의 주소와 영화관이 새 위치에 남아 보이지 않도록 함께 비움
                    _uiState.value = MapUiState(currentLocation = location)
                    _locationReceived.emit(location)
                    coroutineScope {
                        launch { loadAddress(location) }
                        launch { loadTheaters(location) }
                    }
                }
        }

        private suspend fun loadAddress(location: LocationLatLng) {
            when (val result = repository.getAddress(latitude = location.latitude, longitude = location.longitude)) {
                is DataResult.Success -> _uiState.update { it.copy(address = result.data) }
                is DataResult.Failure -> _remoteError.emit(result.error)
            }
        }

        private suspend fun loadTheaters(location: LocationLatLng) {
            val result = repository.getNearbyTheaters(latitude = location.latitude, longitude = location.longitude)
            when (result) {
                is DataResult.Success -> _uiState.update { it.copy(theaters = result.data) }
                is DataResult.Failure -> _remoteError.emit(result.error)
            }
        }
    }
