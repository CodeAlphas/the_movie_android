package com.codealphas.themovie.data.map

import com.codealphas.themovie.data.map.remote.AddressDto
import com.codealphas.themovie.data.map.remote.PoiDto
import com.codealphas.themovie.data.map.remote.PoisDto
import com.codealphas.themovie.domain.map.Address
import com.codealphas.themovie.domain.map.Theater

internal fun PoisDto.toTheaters(): List<Theater> = searchPoiInfo.pois.poi.map(PoiDto::toTheater)

internal fun PoiDto.toTheater(): Theater =
    Theater(
        name = name,
        // MapActivity가 noorLat와 noorLon으로 마커를 찍고 있었으므로,
        // 마커 위치가 바뀌지 않도록 noor 좌표를 latitude와 longitude로 매핑
        latitude = noorLat,
        longitude = noorLon,
        address = "$upperAddrName $middleAddrName $lowerAddrName $detailAddrName",
    )

internal fun AddressDto.toAddress(): Address = Address(fullAddress = addressInfo.fullAddress)
