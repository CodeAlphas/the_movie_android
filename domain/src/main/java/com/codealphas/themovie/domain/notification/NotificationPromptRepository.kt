package com.codealphas.themovie.domain.notification

interface NotificationPromptRepository {
    /**
     * 홈 화면에 처음 들어왔을 때 띄우는 알림 안내에 사용자가 답한 적이 있는지 확인한다.
     *
     * @return `허용`이나 `나중에`를 누르거나 안내를 닫은 적이 있으면 true
     */
    suspend fun wasPromptShown(): Boolean

    suspend fun markPromptShown()

    suspend fun wasPermissionRequested(): Boolean

    suspend fun markPermissionRequested()
}
