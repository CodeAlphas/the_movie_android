package com.codealphas.themovie.data.map

import com.codealphas.themovie.data.map.remote.AddressFromServer
import com.codealphas.themovie.data.map.remote.PoiItem
import com.codealphas.themovie.data.map.remote.PoisFromServer
import com.codealphas.themovie.domain.map.Address
import com.codealphas.themovie.domain.map.Theater

internal fun PoisFromServer.toTheaters(): List<Theater> = searchPoiInfo.pois.poi.map(PoiItem::toTheater)

internal fun PoiItem.toTheater(): Theater =
    Theater(
        name = name,
        // MapActivity가 noorLat와 noorLon으로 마커를 찍고 있었으므로,
        // 마커 위치가 바뀌지 않도록 noor 좌표를 latitude와 longitude로 매핑
        latitude = noorLat,
        longitude = noorLon,
        address = "$upperAddrName $middleAddrName $lowerAddrName $detailAddrName",
    )

internal fun AddressFromServer.toAddress(): Address = Address(fullAddress = addressInfo.fullAddress)
