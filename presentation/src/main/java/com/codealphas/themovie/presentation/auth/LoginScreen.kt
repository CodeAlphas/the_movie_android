package com.codealphas.themovie.presentation.auth

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.codealphas.themovie.core.android.ui.rememberThrottledClick
import com.codealphas.themovie.core.android.ui.showToast
import com.codealphas.themovie.presentation.R

private val FormMaxWidth = 356.dp
private val LogoWidth = 260.dp
private val LogoHeight = 140.dp
private val LogoVerticalPadding = 56.dp
private val FieldShape = RoundedCornerShape(5.dp)

@Composable
fun LoginScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToJoin: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnNavigateToHome by rememberUpdatedState(onNavigateToHome)
    val currentOnNavigateToJoin by rememberUpdatedState(onNavigateToJoin)

    // 화면이 멈춘 동안 보낸 안내를 돌아와서 받도록, 수집은 STARTED 동안만 하고 채널에 남은 effect는 다시 시작할 때 처리
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    LoginEffect.NavigateToHome -> currentOnNavigateToHome()
                    LoginEffect.NavigateToJoin -> currentOnNavigateToJoin()
                    LoginEffect.ShowInvalidInput -> context.showToast(R.string.login_failed)
                    is LoginEffect.ShowError -> context.showToast(loginFailureMessage(effect.error))
                }
            }
        }
    }

    LoginContent(state = state, onIntent = viewModel::onIntent)
}

@Composable
internal fun LoginContent(
    state: LoginUiState,
    onIntent: (LoginIntent) -> Unit,
) {
    // Edge-to-Edge라 시스템이 창을 줄여 주지 않으므로, 시스템 바와 키보드에 입력란이 가리지 않도록 safeDrawing만큼 안쪽 여백 적용
    Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.the_movie),
                contentDescription = null,
                modifier =
                    Modifier
                        .padding(vertical = LogoVerticalPadding)
                        .size(width = LogoWidth, height = LogoHeight),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
            )
            LoginForm(
                state = state,
                onIntent = onIntent,
                modifier = Modifier.widthIn(max = FormMaxWidth).fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun LoginForm(
    state: LoginUiState,
    onIntent: (LoginIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
        Text(
            text = stringResource(R.string.login_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        LoginTextField(
            value = state.email,
            onValueChange = { onIntent(LoginIntent.EmailChanged(it)) },
            hint = R.string.common_id_hint,
        )
        LoginTextField(
            value = state.password,
            onValueChange = { onIntent(LoginIntent.PasswordChanged(it)) },
            hint = R.string.common_password_hint,
            isPassword = true,
            onDone = { onIntent(LoginIntent.LoginClicked) },
        )
        Button(
            onClick = rememberThrottledClick { onIntent(LoginIntent.LoginClicked) },
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
            Text(text = stringResource(R.string.common_login), style = MaterialTheme.typography.titleMedium)
        }
        TextButton(
            onClick = rememberThrottledClick { onIntent(LoginIntent.JoinClicked) },
            modifier = Modifier.align(Alignment.End),
        ) {
            Text(
                text = stringResource(R.string.common_join),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

@Composable
private fun LoginTextField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes hint: Int,
    isPassword: Boolean = false,
    onDone: () -> Unit = {},
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        textStyle = MaterialTheme.typography.bodyLarge,
        placeholder = { Text(text = stringResource(hint)) },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        // 이메일 다음 비밀번호로 이동하고 비밀번호에서 바로 로그인하도록, 칸마다 키보드 동작 적용
        keyboardOptions =
            KeyboardOptions(
                keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Email,
                imeAction = if (isPassword) ImeAction.Done else ImeAction.Next,
            ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        singleLine = true,
        shape = FieldShape,
        // TextField는 기본으로 아래에 밑줄을 그리고 포커스를 받으면 밑줄 색을 바꾸므로,
        // 밑줄 없는 채운 상자로 보이도록 포커스와 관계없이 surfaceVariant 배경에 밑줄 색을 투명으로 적용
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
private fun LoginContentPreview() {
    TheMovieTheme {
        LoginContent(state = LoginUiState(email = "user@example.com"), onIntent = {})
    }
}
