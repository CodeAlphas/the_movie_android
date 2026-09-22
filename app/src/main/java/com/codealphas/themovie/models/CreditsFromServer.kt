package com.example.movieapplication.models

data class CreditsFromServer(
    val id: Int,
    val cast: ArrayList<CreditItem>
) // TMDB 서버로부터 받은 영화 관계자 정보

data class CreditItem(
    val adult: Boolean,
    val gender: Int?,
    val id: Int,
    val known_for_department: String,
    val name: String,
    val original_name: String,
    val popularity: Double,
    val profile_path: String?,
    val cast_id: Int,
    val character: String,
    val credit_id: String,
    val order: Int
) // TMDB 서버로부터 받은 영화 배우 정보