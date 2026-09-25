package com.codealphas.themovie.models

import com.google.gson.annotations.SerializedName

data class CreditsFromServer(
    val id: Int,
    val cast: ArrayList<CreditItem>,
) // TMDB 서버로부터 받은 영화 관계자 정보

data class CreditItem(
    val adult: Boolean,
    val gender: Int?,
    val id: Int,
    @SerializedName("known_for_department")
    val knownForDepartment: String,
    val name: String,
    @SerializedName("original_name")
    val originalName: String,
    val popularity: Double,
    @SerializedName("profile_path")
    val profilePath: String?,
    @SerializedName("cast_id")
    val castId: Int,
    val character: String,
    @SerializedName("credit_id")
    val creditId: String,
    val order: Int,
) // TMDB 서버로부터 받은 영화 배우 정보
