package com.codealphas.themovie.data.map.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AddressFromServer(
    val addressInfo: AddressItem,
)

@Serializable
internal data class AddressItem(
    val fullAddress: String?,
    val addressType: String?,
    @SerialName("city_do")
    val cityDo: String?,
    @SerialName("gu_gun")
    val guGun: String?,
    @SerialName("eup_myun")
    val eupMyun: String?,
    val adminDong: String?,
    val adminDongCode: String?,
    val legalDong: String?,
    val legalDongCode: String?,
    val ri: String?,
    val roadName: String?,
    val buildingIndex: String?,
    val buildingName: String?,
    val mappingDistance: String?,
    val roadCode: String?,
    val bunji: String?,
) // TMAP 서버로부터 받은 주소 정보(ReverseGeocoding)
