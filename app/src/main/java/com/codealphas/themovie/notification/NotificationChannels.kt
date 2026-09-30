package com.codealphas.themovie.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.codealphas.themovie.R

object NotificationChannels {
    fun create(context: Context) {
        // NotificationChannel은 Android 8.0(API 26)부터 있으므로, minSdk 24 기기에서 채널 생성 제외
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel =
            NotificationChannel(
                context.getString(R.string.notification_channel_id),
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.notification_channel_description)
            }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
