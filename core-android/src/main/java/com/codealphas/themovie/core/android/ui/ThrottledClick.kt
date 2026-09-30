package com.codealphas.themovie.core.android.ui

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

private const val DEFAULT_CLICK_WINDOW_MS = 500L

@Composable
fun rememberThrottledClick(
    windowMs: Long = DEFAULT_CLICK_WINDOW_MS,
    onClick: () -> Unit,
): () -> Unit {
    // System.currentTimeMillis는 기기 시각을 과거로 바꾸면 함께 줄어 되돌린 만큼 모든 클릭이 막히므로,
    // 시각 설정과 관계없이 늘어나기만 하는 부팅 뒤 경과 시간 uptimeMillis로 간격 판단
    val throttle = remember(windowMs) { ClickThrottle(windowMs, SystemClock::uptimeMillis) }
    val currentOnClick by rememberUpdatedState(onClick)
    return remember(throttle) { { if (throttle.tryAcquire()) currentOnClick() } }
}
