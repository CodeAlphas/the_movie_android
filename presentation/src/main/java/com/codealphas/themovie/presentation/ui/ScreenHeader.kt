package com.codealphas.themovie.presentation.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codealphas.themovie.core.android.theme.Spacing
import com.codealphas.themovie.core.android.theme.TheMovieTheme

private val HeaderBackgroundHeight = 210.dp
private val HeaderOvalOverflowX = 90.dp
private val HeaderOvalOverflowTop = 100.dp

@Composable
internal fun ScreenHeaderBackground() {
    val gradientStart = TheMovieTheme.extendedColors.primaryGradientStart
    val gradientEnd = TheMovieTheme.extendedColors.primaryGradientEnd
    // Canvas는 영역 밖에 그린 부분을 자르지 않아 넘친 타원이 옆 탭 페이지에도 보이므로, 영역 밖을 잘라 내도록 clipToBounds 적용
    Canvas(modifier = Modifier.fillMaxWidth().height(HeaderBackgroundHeight).clipToBounds()) {
        val overflowX = HeaderOvalOverflowX.toPx()
        val overflowTop = HeaderOvalOverflowTop.toPx()
        drawOval(
            brush =
                Brush.verticalGradient(
                    // primaryGradientStart는 헤더 아래쪽, primaryGradientEnd는 위쪽 색이므로, 위에서 아래로 칠하는 verticalGradient에는 끝색부터 적용
                    colors = listOf(gradientEnd, gradientStart),
                    // 보이는 부분에만 그라데이션을 걸면 헤더 위끝이 끝색 그대로 칠해지므로, 타원 위끝부터 아래끝까지 범위 적용
                    startY = -overflowTop,
                    endY = size.height,
                ),
            // 목록 위 헤더 배경의 아래 가장자리만 둥글게 보이도록, 타원을 위와 양옆으로 화면 밖까지 크게 배치
            topLeft = Offset(-overflowX, -overflowTop),
            size = Size(size.width + overflowX * 2, size.height + overflowTop),
        )
    }
}

@Composable
internal fun ScreenHeaderTexts(
    @StringRes title: Int,
    @StringRes subtitle: Int,
) {
    Column(
        modifier = Modifier.padding(start = Spacing.medium, top = Spacing.medium, end = Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(subtitle),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
