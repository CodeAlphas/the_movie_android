package com.codealphas.themovie.domain.notification

interface NotificationPromptRepository {
    /**
     * 메인 화면에 처음 들어왔을 때 띄우는 알림 안내에 사용자가 답한 적이 있는지 확인한다.
     *
     * @return `허용`이나 `나중에`를 누르거나 안내를 닫은 적이 있으면 true
     */
    suspend fun wasPromptShown(): Boolean

    suspend fun markPromptShown()

    /**
     * 알림 권한 시스템 창을 띄운 적이 있는지 확인한다.
     * 권한이 없고 rationale도 false일 때 한 번도 묻지 않은 상태와 다시 묻지 않음 상태를 구분하는 데 쓴다.
     *
     * @return 시스템 창을 한 번이라도 띄웠으면 true
     */
    suspend fun wasPermissionRequested(): Boolean

    suspend fun markPermissionRequested()
}
