package com.codealphas.themovie.presentation.auth

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.codealphas.themovie.core.android.theme.Spacing
import com.codealphas.themovie.core.android.theme.TheMovieTheme
import com.codealphas.themovie.core.android.ui.BlockingProgressBox
import com.codealphas.themovie.core.android.ui.rememberThrottledClick
import com.codealphas.themovie.core.android.ui.showToast
import com.codealphas.themovie.presentation.R

private val FormMaxWidth = 356.dp
private val FieldShape = RoundedCornerShape(5.dp)

@Composable
fun JoinScreen(
    onNavigateToLogin: () -> Unit,
    viewModel: JoinViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnNavigateToLogin by rememberUpdatedState(onNavigateToLogin)

    // 화면이 멈춘 동안 보낸 안내를 돌아와서 받도록, 수집은 STARTED 동안만 하고 채널에 남은 effect는 다시 시작할 때 처리
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    JoinEffect.NavigateToLogin -> currentOnNavigateToLogin()
                    JoinEffect.ShowLoginPrompt -> context.showToast(R.string.join_succeeded)
                    JoinEffect.ShowInvalidInput -> context.showToast(R.string.join_failed_blank)
                    JoinEffect.ShowPasswordMismatch -> context.showToast(R.string.join_failed_password)
                    is JoinEffect.ShowError -> context.showToast(joinFailureMessage(effect.error))
                }
            }
        }
    }

    JoinContent(state = state, onIntent = viewModel::onIntent)
}

@Composable
internal fun JoinContent(
    state: JoinUiState,
    onIntent: (JoinIntent) -> Unit,
) {
    // 가입 응답 전에 화면을 닫으면 viewModelScope가 취소되어 가입 뒤 로그아웃이 빠지므로,
    // 가입한 계정으로 로그인된 채 남지 않도록 가입이 끝날 때까지 터치와 뒤로 가기 차단
    BlockingProgressBox(isBlocking = state.isLoading, modifier = Modifier.fillMaxSize()) {
        // Edge-to-Edge라 시스템이 창을 줄여 주지 않으므로, 시스템 바와 키보드에 입력란이 가리지 않도록 safeDrawing만큼 안쪽 여백 적용
        Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { innerPadding ->
            BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                Column(
                    // verticalScroll 안은 높이 제한이 없어 Arrangement.Center만으로는 폼이 맨 위에 붙으므로,
                    // 최소 높이를 보이는 영역 높이로 잡아 폼을 세로 가운데 배치
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .heightIn(min = maxHeight)
                            .padding(horizontal = Spacing.large, vertical = Spacing.large),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    JoinForm(
                        state = state,
                        onIntent = onIntent,
                        modifier = Modifier.widthIn(max = FormMaxWidth).fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun JoinForm(
    state: JoinUiState,
    onIntent: (JoinIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
        Text(
            text = stringResource(R.string.join_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        JoinTextField(
            value = state.email,
            onValueChange = { onIntent(JoinIntent.EmailChanged(it)) },
            hint = R.string.common_id_hint,
        )
        JoinTextField(
            value = state.password,
            onValueChange = { onIntent(JoinIntent.PasswordChanged(it)) },
            hint = R.string.common_password_hint,
            isPassword = true,
        )
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)) {
            JoinTextField(
                value = state.confirmPassword,
                onValueChange = { onIntent(JoinIntent.ConfirmPasswordChanged(it)) },
                hint = R.string.join_password_confirm_hint,
                isPassword = true,
                onDone = { onIntent(JoinIntent.JoinClicked) },
            )
            PasswordMatchText(passwordsMatch = state.passwordsMatch)
        }
        Button(
            onClick = rememberThrottledClick { onIntent(JoinIntent.JoinClicked) },
            enabled = !state.isLoading,
            // 버튼 기본 높이가 입력란보다 낮아 폼이 들쭉날쭉하므로, 입력란과 같은 최소 높이 적용
            modifier = Modifier.fillMaxWidth().heightIn(min = TextFieldDefaults.MinHeight),
            shape = FieldShape,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ),
        ) {
            Text(text = stringResource(R.string.common_join), style = MaterialTheme.typography.titleMedium)
        }
        TextButton(
            onClick = rememberThrottledClick { onIntent(JoinIntent.LoginClicked) },
            modifier = Modifier.align(Alignment.End),
        ) {
            Text(
                text = stringResource(R.string.common_login),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

@Composable
private fun PasswordMatchText(passwordsMatch: Boolean?) {
    val message =
        when (passwordsMatch) {
            true -> stringResource(R.string.join_password_match)
            false -> stringResource(R.string.join_password_mismatch)
            null -> ""
        }
    // 두 칸이 비어 문구가 없을 때 줄을 없애면 입력할 때마다 아래 버튼이 위아래로 밀리므로, 빈 문자열로 한 줄 높이 유지
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = if (passwordsMatch == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun JoinTextField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes hint: Int,
    isPassword: Boolean = false,
    onDone: (() -> Unit)? = null,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        textStyle = MaterialTheme.typography.bodyLarge,
        placeholder = { Text(text = stringResource(hint)) },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        // 마지막 비밀번호 확인 칸에서 바로 가입하도록, onDone이 있는 칸만 완료 동작이고 나머지는 다음 칸으로 이동 적용
        keyboardOptions =
            KeyboardOptions(
                keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Email,
                imeAction = if (onDone != null) ImeAction.Done else ImeAction.Next,
            ),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
        singleLine = true,
        shape = FieldShape,
        // XML 입력란처럼 밑줄 없는 채운 상자로 보이도록, 포커스와 관계없이 surfaceVariant 배경에 밑줄 색을 투명으로 적용
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
    )
}

@Preview(name = "라이트")
@Preview(name = "다크", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun JoinContentPreview() {
    TheMovieTheme {
        JoinContent(
            state =
                JoinUiState(
                    email = "user@example.com",
                    password = "secret",
                    confirmPassword = "secre",
                    passwordsMatch = false,
                ),
            onIntent = {},
        )
    }
}
