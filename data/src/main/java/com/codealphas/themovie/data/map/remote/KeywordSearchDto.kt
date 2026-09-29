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

// x, y는 JSON에서 따옴표 친 문자열이라 Double로 바로 읽으면 파싱에 실패하므로 String으로 받음
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
)
