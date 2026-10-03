package com.codealphas.themovie.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MovieMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // 콘솔의 알림 메시지는 notification에, 데이터 메시지는 data에 문구가 오므로, 알림 메시지에 문구가 없을 때만 data 사용
        val notification = message.notification
        val hasNotificationText = notification?.title != null || notification?.body != null
        val title = if (hasNotificationText) notification?.title else message.data[DATA_TITLE]
        val body = if (hasNotificationText) notification?.body else message.data[DATA_CONTENTS]

        MovieNotifier(applicationContext).show(
            // 알림 id가 같으면 새 알림이 이전 알림을 덮어쓰므로, 메시지마다 알림이 쌓이도록 messageId 해시를 id로 사용
            notificationId = message.messageId?.hashCode() ?: System.currentTimeMillis().toInt(),
            title = title,
            body = body,
        )
    }

    private companion object {
        const val DATA_TITLE = "title"
        const val DATA_CONTENTS = "contents"
    }
}
