package com.codealphas.themovie.domain.map

data class Theater(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    // 검색 중심에서의 직선거리이며, 응답에 없으면 null
    val distanceMeters: Int?,
)
