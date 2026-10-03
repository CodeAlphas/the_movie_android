package com.codealphas.themovie.domain.notification

interface NotificationPromptRepository {
    /**
     * @return `허용`이나 `나중에`를 누르거나 안내를 닫은 적이 있으면 true
     */
    suspend fun wasPromptAnswered(): Boolean

    suspend fun markPromptAnswered()

    suspend fun wasPermissionRequested(): Boolean

    suspend fun markPermissionRequested()
}
