package com.codealphas.themovie.presentation.movie

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.codealphas.themovie.core.android.theme.Spacing
import com.codealphas.themovie.core.android.theme.TheMovieTheme
import com.codealphas.themovie.presentation.R
import com.codealphas.themovie.presentation.ui.remoteErrorMessage

private const val PREVIEW_MOVIE_COUNT = 4

@Composable
fun SearchMovieScreen(
    snackbarHostState: SnackbarHostState,
    onMovieClick: (Int) -> Unit,
    floatingActionButton: @Composable (visible: Boolean) -> Unit,
    viewModel: SearchMovieViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 화면이 멈춘 동안 보낸 안내를 돌아와서 받도록, 수집은 STARTED 동안만 하고 채널에 남은 effect는 다시 시작할 때 처리
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is SearchMovieEffect.ShowError ->
                        snackbarHostState.showSnackbar(
                            message = remoteErrorMessage(resources, effect.error),
                            duration = SnackbarDuration.Long,
                        )
                }
            }
        }
    }

    SearchMovieContent(
        state = state,
        onIntent = viewModel::onIntent,
        onMovieClick = onMovieClick,
        floatingActionButton = floatingActionButton,
    )
}

@Composable
internal fun SearchMovieContent(
    state: SearchMovieUiState,
    onIntent: (SearchMovieIntent) -> Unit,
    onMovieClick: (Int) -> Unit,
    floatingActionButton: @Composable (visible: Boolean) -> Unit,
) {
    val gridState = rememberLazyGridState()
    ClearFocusWhenImeHides()

    Box(modifier = Modifier.fillMaxSize()) {
        MovieHeaderBackground()
        Column(modifier = Modifier.fillMaxSize()) {
            SearchField(query = state.query, onQueryChange = { onIntent(SearchMovieIntent.QueryChanged(it)) })
            MovieHeaderTexts(title = R.string.search_result_title, subtitle = R.string.search_result_subtitle)
            Box(modifier = Modifier.fillMaxSize()) {
                state.movies?.let { movies ->
                    MovieGrid(movies = movies, gridState = gridState, onMovieClick = onMovieClick)
                }
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                // 검색이 실패하면 목록이 앞 검색어의 결과로 남으므로,
                // 앞 검색어가 0건이었을 때 실패한 검색어까지 결과 없음으로 보이지 않도록 오류가 있으면 문구 표시 제외
                val isEmptyResult = state.movies?.isEmpty() == true && !state.isLoading
                if (isEmptyResult && state.loadError == null) {
                    Text(
                        text = stringResource(R.string.search_empty_result),
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        // XML 검색 탭은 검색 전에도 FAB를 보여 줬으므로, 목록 유무와 관계없이 스크롤하는 동안에만 숨김 적용
        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(Spacing.medium)) {
            floatingActionButton(!gridState.isScrollInProgress)
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(start = Spacing.medium, top = Spacing.medium, end = Spacing.medium),
        textStyle = MaterialTheme.typography.bodyLarge,
        placeholder = { Text(text = stringResource(R.string.search_query_hint)) },
        leadingIcon = { Icon(painter = painterResource(R.drawable.ic_baseline_search_24), contentDescription = null) },
        trailingIcon =
            if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_close_24),
                            contentDescription = stringResource(R.string.search_clear_query),
                        )
                    }
                }
            } else {
                null
            },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        // 키보드의 검색 키는 기본 동작이 없어 눌러도 키보드가 결과 목록을 가린 채 남으므로, 키보드가 내려가도록 포커스 해제
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        singleLine = true,
        shape = CircleShape,
        // XML 검색창의 colorSurfaceInverse가 라이트와 다크 모두 surfaceContainerHigh와 같은 색이므로,
        // 밑줄 없는 같은 색 상자로 보이도록 포커스와 관계없이 surfaceContainerHigh 배경에 밑줄 색을 투명으로 적용
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
    )
}

@Composable
private fun ClearFocusWhenImeHides() {
    val focusManager = LocalFocusManager.current
    val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    // 뒤로 가기는 키보드만 숨기고 포커스는 남겨 상세에서 돌아오면 키보드가 다시 뜨므로, 키보드가 내려가면 검색창 포커스 해제
    LaunchedEffect(isImeVisible) {
        if (!isImeVisible) focusManager.clearFocus()
    }
}

@Preview(name = "라이트")
@Preview(name = "다크", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SearchMovieContentPreview() {
    TheMovieTheme {
        Surface {
            SearchMovieContent(
                state =
                    SearchMovieUiState(
                        query = "Movie",
                        movies = List(PREVIEW_MOVIE_COUNT) { previewMovie(it) },
                    ),
                onIntent = {},
                onMovieClick = {},
                floatingActionButton = { visible ->
                    MovieFabMenu(
                        visible = visible,
                        expanded = false,
                        onExpandedChange = {},
                        onReviewClick = {},
                        onMapClick = {},
                    )
                },
            )
        }
    }
}

@Preview(name = "결과 없음")
@Composable
private fun SearchMovieContentEmptyPreview() {
    TheMovieTheme {
        Surface {
            SearchMovieContent(
                state = SearchMovieUiState(query = "zzz", movies = emptyList()),
                onIntent = {},
                onMovieClick = {},
                floatingActionButton = {},
            )
        }
    }
}
