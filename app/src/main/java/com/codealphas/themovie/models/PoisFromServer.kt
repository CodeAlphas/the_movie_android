package com.codealphas.themovie.models

import kotlinx.serialization.Serializable

@Serializable
data class PoisFromServer(
    val searchPoiInfo: SearchPoiInfo,
)

@Serializable
data class SearchPoiInfo(
    val totalCount: Int,
    val count: Int,
    val page: Int,
    val pois: Pois,
)

@Serializable
data class Pois(
    val poi: List<PoiItem>,
)

@Serializable
data class PoiItem(
    val id: String,
    val name: String,
    val telNo: String,
    val frontLat: Double,
    val frontLon: Double,
    val noorLat: Double,
    val noorLon: Double,
    val upperAddrName: String,
    val middleAddrName: String,
    val lowerAddrName: String,
    val detailAddrName: String,
    val mlClass: String,
    val firstNo: String,
    val secondNo: String,
    val roadName: String,
    val radius: String,
    val rpFlag: String,
    val parkFlag: String,
) // TMAP 서버로부터 받은 주변 POI 정보
