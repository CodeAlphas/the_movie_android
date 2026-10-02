package com.codealphas.themovie.core.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.clearAndSetSemantics

@Composable
fun BlockingProgressBox(
    isBlocking: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        // TalkBack은 화면 좌표를 누르지 않고 선택한 버튼을 직접 실행해 덮개가 터치를 막아도 앱바 뒤로 버튼이 눌리므로,
        // 막는 동안 TalkBack이 아래 화면의 버튼과 입력란을 선택하지 못하도록 접근성 트리에서 제외
        Box(
            modifier = if (isBlocking) Modifier.clearAndSetSemantics {} else Modifier,
            propagateMinConstraints = true,
        ) {
            content()
        }
        if (isBlocking) {
            // 시스템 뒤로 가기는 터치가 아니라 덮개로 막을 수 없으므로, 빈 BackHandler로 뒤로 가기와 예측형 뒤로 가기 미리보기 차단
            BackHandler {}
            val focusManager = LocalFocusManager.current
            // 키보드는 별도 창이라 덮개가 터치를 막아도 입력이 들어가므로, 막기 시작할 때 포커스를 해제해 키보드 닫기 적용
            LaunchedEffect(Unit) { focusManager.clearFocus() }
            // 덮개 아래의 입력란과 앱바 버튼을 누르지 못하도록, 화면 전체를 덮어 모든 터치 소비
            Box(
                modifier =
                    Modifier.matchParentSize().pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent().changes.forEach { it.consume() }
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
