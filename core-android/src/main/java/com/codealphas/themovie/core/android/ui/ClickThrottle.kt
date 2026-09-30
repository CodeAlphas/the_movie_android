package com.codealphas.themovie.core.android.ui

internal class ClickThrottle(
    private val windowMs: Long,
    private val now: () -> Long,
) {
    private var lastAcceptedAt: Long? = null

    fun tryAcquire(): Boolean {
        val currentTime = now()
        val last = lastAcceptedAt
        if (last != null && currentTime - last < windowMs) return false
        lastAcceptedAt = currentTime
        return true
    }
}
