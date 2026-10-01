package com.codealphas.themovie.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
internal data object AuthGraph

@Serializable
internal data object Login

@Serializable
internal data object Join

@Serializable
internal data object Home

@Serializable
internal data class MovieDetail(
    val movieId: Int,
)

@Serializable
internal data object ReviewList

@Serializable
internal data class ReviewEdit(
    val reviewId: Int? = null,
)

@Serializable
internal data object TheaterMap
