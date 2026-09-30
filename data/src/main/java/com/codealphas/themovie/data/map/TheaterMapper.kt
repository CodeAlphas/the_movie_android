package com.codealphas.themovie.data.map

import com.codealphas.themovie.data.map.remote.CoordToAddressDto
import com.codealphas.themovie.data.map.remote.PlaceDto
import com.codealphas.themovie.domain.map.Address
import com.codealphas.themovie.domain.map.Theater

// x, y가 숫자가 아닌 장소는 지도에 찍을 수 없으므로 제외
internal fun List<PlaceDto>.toTheaters(): List<Theater> = mapNotNull(PlaceDto::toTheaterOrNull)

internal fun PlaceDto.toTheaterOrNull(): Theater? {
    val latitude = y.toDoubleOrNull()
    val longitude = x.toDoubleOrNull()
    return if (latitude == null || longitude == null) {
        null
    } else {
        Theater(
            id = id,
            name = placeName,
            latitude = latitude,
            longitude = longitude,
            address = roadAddressName.ifBlank { addressName },
            distanceMeters = distance.toIntOrNull(),
        )
    }
}

// 도로명 주소가 없는 좌표도 있고 바다처럼 주소 자체가 없는 좌표도 있으므로, 지번 주소로 대체하고 그마저 없으면 null로 처리
internal fun CoordToAddressDto.toAddress(): Address {
    val document = documents.firstOrNull()
    return Address(fullAddress = (document?.roadAddress ?: document?.address)?.addressName)
}
