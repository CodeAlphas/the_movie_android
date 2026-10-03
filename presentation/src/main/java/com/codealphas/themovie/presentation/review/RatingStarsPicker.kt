package com.codealphas.themovie.presentation.review

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import com.codealphas.themovie.core.android.ui.RatingStars
import kotlin.math.roundToInt

private const val STAR_COUNT = 5

// 별점을 0.1 단위로 고르도록, 별 하나를 10칸으로 분할
private const val STEPS_PER_STAR = 10

@Composable
internal fun RatingStarsPicker(
    starRating: Float,
    onStarRatingChange: (Float) -> Unit,
    starSize: Dp,
    modifier: Modifier = Modifier,
) {
    val currentOnStarRatingChange by rememberUpdatedState(onStarRatingChange)
    Box(
        modifier =
            modifier
                .pointerInput(Unit) {
                    detectTapGestures { offset -> currentOnStarRatingChange(starsAt(offset.x)) }
                }.pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ ->
                        currentOnStarRatingChange(starsAt(change.position.x))
                    }
                },
    ) {
        RatingStars(filledStars = starRating, starSize = starSize)
    }
}

// 별 줄 밖으로 끌어도 0개 미만이나 5개 초과가 되지 않도록, 손가락 위치를 별 줄 폭 안의 비율로 바꿔 0.1개 단위로 반올림 적용
private fun PointerInputScope.starsAt(x: Float): Float {
    val fraction = (x / size.width).coerceIn(0f, 1f)
    return (fraction * STAR_COUNT * STEPS_PER_STAR).roundToInt() / STEPS_PER_STAR.toFloat()
}
