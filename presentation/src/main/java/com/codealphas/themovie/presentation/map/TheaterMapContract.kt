package com.codealphas.themovie.presentation.map

import com.codealphas.themovie.domain.map.Address
import com.codealphas.themovie.domain.map.Theater
import com.codealphas.themovie.domain.result.RemoteError

data class TheaterMapUiState(
    val currentLocation: LocationLatLng? = null,
    val address: Address? = null,
    val theaters: List<Theater> = emptyList(),
    val selectedTheaterId: String? = null,
) {
    // 새 위치를 받아 목록이 바뀌면 고른 영화관이 목록에 없을 수 있으므로, 지금 목록에 있는 영화관만 시트로 표시
    val selectedTheater: Theater?
        get() = theaters.firstOrNull { it.id == selectedTheaterId }
}

sealed interface TheaterMapIntent {
    data object LocationReady : TheaterMapIntent

    data object LocationUnavailable : TheaterMapIntent

    data class TheaterClicked(
        val theaterId: String,
    ) : TheaterMapIntent

    data object TheaterSheetDismissed : TheaterMapIntent
}

sealed interface TheaterMapEffect {
    // 같은 위치를 다시 받아도 지도를 직접 옮긴 사용자가 현재 위치로 돌아올 수 있도록, 상태가 아닌 일회성 effect로 전달
    data class MoveCamera(
        val location: LocationLatLng,
    ) : TheaterMapEffect

    data object ShowLocationFailed : TheaterMapEffect

    data class ShowError(
        val error: RemoteError,
    ) : TheaterMapEffect
}
