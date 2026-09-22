package com.example.movieapplication.models

import com.google.gson.annotations.SerializedName

data class AddressFromServer(
    val addressInfo: AddressItem
)

data class AddressItem(
    @SerializedName("fullAddress")
    val fullAddress: String?,
    @SerializedName("addressType")
    val addressType: String?,
    @SerializedName("city_do")
    val cityDo: String?,
    @SerializedName("gu_gun")
    val guGun: String?,
    @SerializedName("eup_myun")
    val eupMyun: String?,
    @SerializedName("adminDong")
    val adminDong: String?,
    @SerializedName("adminDongCode")
    val adminDongCode: String?,
    @SerializedName("legalDong")
    val legalDong: String?,
    @SerializedName("legalDongCode")
    val legalDongCode: String?,
    @SerializedName("ri")
    val ri: String?,
    @SerializedName("roadName")
    val roadName: String?,
    @SerializedName("buildingIndex")
    val buildingIndex: String?,
    @SerializedName("buildingName")
    val buildingName: String?,
    @SerializedName("mappingDistance")
    val mappingDistance: String?,
    @SerializedName("roadCode")
    val roadCode: String?,
    @SerializedName("bunji")
    val bunji: String?
) // TMAP 서버로부터 받은 주소 정보(ReverseGeocoding)