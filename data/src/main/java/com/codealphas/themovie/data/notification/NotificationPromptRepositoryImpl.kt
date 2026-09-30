package com.codealphas.themovie.data.notification

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.codealphas.themovie.domain.notification.NotificationPromptRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException
import javax.inject.Inject

// 한 프로세스에 같은 파일의 DataStore가 둘 이상 있으면 읽고 쓸 때 IllegalStateException이 발생하므로,
// 인스턴스가 하나만 생기도록 최상위 위임 속성으로 선언
private val Context.notificationPromptDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "notification_prompt",
)

internal class NotificationPromptRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : NotificationPromptRepository {
        private val dataStore = context.notificationPromptDataStore

        override suspend fun wasPromptShown(): Boolean = read(PROMPT_SHOWN)

        override suspend fun markPromptShown() = markTrue(PROMPT_SHOWN)

        override suspend fun wasPermissionRequested(): Boolean = read(PERMISSION_REQUESTED)

        override suspend fun markPermissionRequested() = markTrue(PERMISSION_REQUESTED)

        private suspend fun read(key: Preferences.Key<Boolean>): Boolean =
            dataStore.data
                // 파일을 읽지 못하면 data가 IOException으로 끝나 화면 코루틴이 죽으므로, 기록이 없는 상태로 처리
                .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
                .first()[key] ?: false

        private suspend fun markTrue(key: Preferences.Key<Boolean>) {
            // 파일을 쓰지 못하면 edit가 IOException을 던져 화면 코루틴이 죽으므로, 기록 실패는 다음 진입 때 안내를 한 번 더 보는 것으로 처리
            try {
                dataStore.edit { preferences -> preferences[key] = true }
            } catch (ignored: IOException) {
            }
        }

        private companion object {
            val PROMPT_SHOWN = booleanPreferencesKey("prompt_shown")
            val PERMISSION_REQUESTED = booleanPreferencesKey("permission_requested")
        }
    }
