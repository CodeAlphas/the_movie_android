package com.codealphas.themovie.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.codealphas.themovie.MainActivity
import com.codealphas.themovie.R
import com.codealphas.themovie.presentation.R as PresentationR

class MovieNotifier(
    private val context: Context,
) {
    fun show(
        notificationId: Int,
        title: String?,
        body: String?,
    ) {
        // Android 13부터 알림 권한이 없으면 notify가 SecurityException을 던지므로, 허용된 경우에만 표시
        val isPermitted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        if (!isPermitted) return

        val notification =
            NotificationCompat
                .Builder(context, context.getString(R.string.notification_channel_id))
                .setSmallIcon(PresentationR.drawable.ic_baseline_movie)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(createMainPendingIntent())
                .setAutoCancel(true)
                .build()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun createMainPendingIntent(): PendingIntent {
        val intent =
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            // targetSdk 31 이상은 가변성 플래그가 없으면 getActivity가 IllegalArgumentException을 던지므로,
            // 메시지를 받아 알림을 만들 때 앱이 죽지 않도록 FLAG_IMMUTABLE 적용
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
