package com.codealphas.themovie.presentation.movie

import android.content.Context
import android.util.DisplayMetrics

fun getScreenWidth(context: Context): Int {
    val metrics = context.resources.displayMetrics
    val screenWidth = metrics.widthPixels
    return dpToPx(screenWidth.toFloat(), metrics).toInt()
}

private fun dpToPx(
    px: Float,
    metrics: DisplayMetrics,
): Float = px / (metrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
