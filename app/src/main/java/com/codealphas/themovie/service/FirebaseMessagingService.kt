package com.example.movieapplication.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.movieapplication.R
import com.example.movieapplication.views.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_NAME = "The Movie"
        private const val CHANNEL_DESCRIPTION = "The Movie 채널"
        private const val CHANNEL_ID = "The Movie Channel Id"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Log.d("로그", token)
    } // 토큰이 새로 만들어질때나 갱신될 때, 등록된 토큰을 애플리케이션 서버로보내는 메소드

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        createNotificationChannel()

        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT // 동일한 pendingIntent가 이미 존재하면 extra data만 갱신해서 사용
        ) // Notifictaion Manager에 intent를 다룰 수 있는 권한을 줌, Notifictaion Manager가 해당 intent를 수행해야겠다고 판단할 때 수행

        // 알림 메시지
        var title = message.notification?.title
        var receivedMessage = message.notification?.body

        // 데이터 메시지
        if (title == null && receivedMessage == null) {
            title = message.data["title"]
            receivedMessage = message.data["contents"]
        }

        val buildNotification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_movie)
            .setContentTitle(title)
            .setContentText(receivedMessage)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // 사용자가 알림을 탭하면 자동으로 알림 삭제

        NotificationManagerCompat.from(this).notify(0, buildNotification.build())
    } // 메시지를 수신할 때마다 호출되는 메소드

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = CHANNEL_DESCRIPTION

            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                channel
            )
        } // 안드로이드 8.0(오레오, API 수준 26)부터는 모든 알림을 채널에 할당해야 함
    }
}