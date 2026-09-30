package com.codealphas.themovie.data.map.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CoordToAddressDto(
    val documents: List<CoordAddressDocumentDto>,
)

@Serializable
internal data class CoordAddressDocumentDto(
    @SerialName("road_address")
    val roadAddress: AddressNameDto? = null,
    val address: AddressNameDto? = null,
)

@Serializable
internal data class AddressNameDto(
    @SerialName("address_name")
    val addressName: String,
)
