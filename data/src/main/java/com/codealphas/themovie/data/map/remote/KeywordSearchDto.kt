package com.codealphas.themovie.data.map.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class KeywordSearchDto(
    val documents: List<PlaceDto>,
    val meta: KeywordSearchMetaDto,
)

@Serializable
internal data class KeywordSearchMetaDto(
    @SerialName("is_end")
    val isEnd: Boolean,
)

// x, y를 Double로 선언하면 한 장소의 좌표만 빈 값이어도 검색 응답 전체를 읽지 못해 영화관이 하나도 보이지 않으므로,
// String으로 받아 TheaterMapper에서 좌표를 숫자로 바꾸지 못한 장소만 제외
@Serializable
internal data class PlaceDto(
    val id: String,
    @SerialName("place_name")
    val placeName: String,
    @SerialName("address_name")
    val addressName: String,
    @SerialName("road_address_name")
    val roadAddressName: String = "",
    val x: String,
    val y: String,
    // 검색 중심 좌표를 주지 않으면 빈 문자열이 오므로, String으로 선언
    val distance: String = "",
)
