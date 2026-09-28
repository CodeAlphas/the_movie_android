package com.codealphas.themovie.data.movie.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CreditsDto(
    val id: Int,
    val cast: List<CreditDto>,
) // TMDB 서버로부터 받은 영화 관계자 정보

@Serializable
internal data class CreditDto(
    val adult: Boolean,
    val gender: Int?,
    val id: Int,
    @SerialName("known_for_department")
    val knownForDepartment: String,
    val name: String,
    @SerialName("original_name")
    val originalName: String,
    val popularity: Double,
    @SerialName("profile_path")
    val profilePath: String?,
    @SerialName("cast_id")
    val castId: Int,
    val character: String,
    @SerialName("credit_id")
    val creditId: String,
    val order: Int,
) // TMDB 서버로부터 받은 영화 배우 정보
