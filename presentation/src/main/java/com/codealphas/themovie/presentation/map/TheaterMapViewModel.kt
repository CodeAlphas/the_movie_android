package com.codealphas.themovie.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codealphas.themovie.domain.map.TheaterRepository
import com.codealphas.themovie.domain.result.Outcome
import com.codealphas.themovie.presentation.map.location.CurrentLocationProvider
import com.codealphas.themovie.presentation.map.location.LocationLatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TheaterMapViewModel
    @Inject
    constructor(
        private val repository: TheaterRepository,
        private val locationProvider: CurrentLocationProvider,
    ) : ViewModel() {
        private val _state = MutableStateFlow(TheaterMapUiState())
        val state: StateFlow<TheaterMapUiState> = _state.asStateFlow()

        private val _effect = Channel<TheaterMapEffect>(Channel.BUFFERED)
        val effect: Flow<TheaterMapEffect> = _effect.receiveAsFlow()

        private var locationJob: Job? = null

        fun onIntent(intent: TheaterMapIntent) {
            when (intent) {
                TheaterMapIntent.LocationReady -> loadCurrentLocationAndTheaters()
                TheaterMapIntent.LocationUnavailable -> showLocationFailed()
                is TheaterMapIntent.TheaterClicked -> _state.update { it.copy(selectedTheaterId = intent.theaterId) }
                TheaterMapIntent.TheaterSheetDismissed -> _state.update { it.copy(selectedTheaterId = null) }
            }
        }

        // 위치 설정을 켜지 못한 경우도 위치를 받지 못한 경우와 같은 안내와 다시 시도를 보이도록, 같은 effect로 전달
        private fun showLocationFailed() {
            viewModelScope.launch { _effect.send(TheaterMapEffect.ShowLocationFailed) }
        }

        private fun loadCurrentLocationAndTheaters() {
            // 앞 요청의 응답이 뒤늦게 와서 새 위치의 상태를 덮지 않도록, 진행 중인 요청을 취소하고 새로 시작
            locationJob?.cancel()
            locationJob =
                viewModelScope.launch {
                    val location = locationProvider.getCurrentLocation()
                    if (location == null) {
                        _effect.send(TheaterMapEffect.ShowLocationFailed)
                        return@launch
                    }
                    // 현재 위치만 바꾸면 새 응답이 오기 전까지(요청이 실패하면 계속) 이전 위치의 주소, 영화관 마커, 열린 시트가 남으므로,
                    // 상태를 새로 만들어 셋을 함께 초기화
                    _state.value = TheaterMapUiState(currentLocation = location)
                    _effect.send(TheaterMapEffect.MoveCamera(location))
                    coroutineScope {
                        launch { loadAddress(location) }
                        launch { loadTheaters(location) }
                    }
                }
        }

        private suspend fun loadAddress(location: LocationLatLng) {
            when (val result = repository.getAddress(latitude = location.latitude, longitude = location.longitude)) {
                is Outcome.Success -> _state.update { it.copy(address = result.data) }
                is Outcome.Failure -> _effect.send(TheaterMapEffect.ShowError(result.error))
            }
        }

        private suspend fun loadTheaters(location: LocationLatLng) {
            val result = repository.getNearbyTheaters(latitude = location.latitude, longitude = location.longitude)
            when (result) {
                is Outcome.Success -> _state.update { it.copy(theaters = result.data) }
                is Outcome.Failure -> _effect.send(TheaterMapEffect.ShowError(result.error))
            }
        }
    }
