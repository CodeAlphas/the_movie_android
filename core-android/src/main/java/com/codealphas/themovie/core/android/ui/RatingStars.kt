package com.codealphas.themovie.core.android.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.codealphas.themovie.core.android.R

private const val STAR_COUNT = 5

@Composable
fun RatingStars(
    filledStars: Float,
    starSize: Dp,
) {
    val star = painterResource(R.drawable.ic_baseline_star_24)
    Row {
        repeat(STAR_COUNT) { index ->
            val fill = (filledStars - index).coerceIn(0f, 1f)
            Box(modifier = Modifier.size(starSize)) {
                Icon(
                    painter = star,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    tint = MaterialTheme.colorScheme.outline,
                )
                // Material3에는 RatingBar가 없어서, 3.5개처럼 일부만 찬 별을 그리도록 채운 별을 점수 비율만큼 잘라 겹침 적용
                Icon(
                    painter = star,
                    contentDescription = null,
                    modifier =
                        Modifier.fillMaxSize().drawWithContent {
                            clipRect(right = size.width * fill) { this@drawWithContent.drawContent() }
                        },
                    tint = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
    }
}
