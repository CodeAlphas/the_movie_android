package com.codealphas.themovie.presentation.movie.search

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.codealphas.themovie.core.android.theme.Spacing
import com.codealphas.themovie.core.android.theme.TheMovieTheme
import com.codealphas.themovie.domain.movie.Movie
import com.codealphas.themovie.domain.result.RemoteError
import com.codealphas.themovie.domain.result.RemoteErrorException
import com.codealphas.themovie.presentation.R
import com.codealphas.themovie.presentation.movie.LoadErrorContent
import com.codealphas.themovie.presentation.movie.MovieFabMenu
import com.codealphas.themovie.presentation.movie.MovieGrid
import com.codealphas.themovie.presentation.movie.previewMovie
import com.codealphas.themovie.presentation.ui.ScreenHeaderBackground
import com.codealphas.themovie.presentation.ui.ScreenHeaderTexts
import com.codealphas.themovie.presentation.ui.remoteError
import kotlinx.coroutines.flow.flowOf

private const val PREVIEW_MOVIE_COUNT = 4

@Composable
fun SearchMovieScreen(
    onMovieClick: (Int) -> Unit,
    floatingActionButton: @Composable (visible: Boolean) -> Unit,
    viewModel: SearchMovieViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val movies = viewModel.movies.collectAsLazyPagingItems()

    SearchMovieContent(
        state = state,
        movies = movies,
        onIntent = viewModel::onIntent,
        onMovieClick = onMovieClick,
        floatingActionButton = floatingActionButton,
    )
}

@Composable
internal fun SearchMovieContent(
    state: SearchMovieUiState,
    movies: LazyPagingItems<Movie>,
    onIntent: (SearchMovieIntent) -> Unit,
    onMovieClick: (Int) -> Unit,
    floatingActionButton: @Composable (visible: Boolean) -> Unit,
) {
    val gridState = rememberLazyGridState()
    ClearFocusWhenImeHides()
    ScrollToTopOnNewResults(
        query = state.query,
        isRefreshing = movies.loadState.refresh is LoadState.Loading,
        gridState = gridState,
    )

    Box(modifier = Modifier.fillMaxSize()) {
        ScreenHeaderBackground()
        Column(modifier = Modifier.fillMaxSize()) {
            SearchField(query = state.query, onQueryChange = { onIntent(SearchMovieIntent.QueryChanged(it)) })
            ScreenHeaderTexts(title = R.string.search_result_title, subtitle = R.string.search_result_subtitle)
            Box(modifier = Modifier.fillMaxSize()) {
                val refresh = movies.loadState.refresh
                // 새 검색어의 첫 로드가 실패해도 Paging은 앞 검색어의 목록을 남겨 실패한 검색어의 결과처럼 보이므로,
                // 실패하면 목록 대신 실패 화면만 표시
                if (refresh is LoadState.Error) {
                    LoadErrorContent(
                        error = refresh.remoteError,
                        onRetry = movies::retry,
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else if (movies.itemCount > 0) {
                    MovieGrid(movies = movies, gridState = gridState, onMovieClick = onMovieClick)
                }
                if (refresh is LoadState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                // 검색 전의 빈 목록도 로드가 끝난 0건과 같은 상태로 보이므로, 끝에 닿았는지까지 확인해 검색 결과가 0건일 때만 문구 표시
                val isEmptyResult =
                    refresh is LoadState.NotLoading &&
                        movies.itemCount == 0 &&
                        movies.loadState.append.endOfPaginationReached
                if (isEmptyResult) {
                    Text(
                        text = stringResource(R.string.search_empty_result),
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        // FAB가 영화 목록을 가리지 않도록, 스크롤하는 동안 숨김 적용
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
        // TextField는 기본으로 아래에 밑줄을 그리고 포커스를 받으면 밑줄 색을 바꾸므로,
        // 검색창이 밑줄 없는 둥근 상자로 보이도록 포커스와 관계없이 surfaceContainerHigh 배경에 밑줄 색을 투명으로 적용
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
private fun ScrollToTopOnNewResults(
    query: String,
    isRefreshing: Boolean,
    gridState: LazyGridState,
) {
    var lastQuery by remember { mutableStateOf(query) }
    var scrollToTopPending by remember { mutableStateOf(false) }
    // 입력 중에는 목록이 튀지 않도록, 검색어가 바뀌면 바로 이동하지 않고 이동할 일이 있다고만 기록
    LaunchedEffect(query) {
        if (query != lastQuery) {
            lastQuery = query
            scrollToTopPending = true
        }
    }
    // 앞 검색어의 스크롤 위치가 남아 새 결과가 맨 위가 아닌 곳부터 보일 수 있으므로, 첫 로드가 끝나면 맨 위로 이동 적용
    LaunchedEffect(isRefreshing) {
        if (!isRefreshing && scrollToTopPending) {
            scrollToTopPending = false
            gridState.scrollToItem(0)
        }
    }
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
                state = SearchMovieUiState(query = "Movie"),
                movies =
                    flowOf(PagingData.from(List(PREVIEW_MOVIE_COUNT) { previewMovie(it) })).collectAsLazyPagingItems(),
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
    val finished = LoadState.NotLoading(endOfPaginationReached = true)
    val emptyStates = LoadStates(refresh = finished, prepend = finished, append = finished)
    TheMovieTheme {
        Surface {
            SearchMovieContent(
                state = SearchMovieUiState(query = "zzz"),
                movies = flowOf(PagingData.empty<Movie>(sourceLoadStates = emptyStates)).collectAsLazyPagingItems(),
                onIntent = {},
                onMovieClick = {},
                floatingActionButton = {},
            )
        }
    }
}

@Preview(name = "실패")
@Composable
private fun SearchMovieContentErrorPreview() {
    val notLoading = LoadState.NotLoading(endOfPaginationReached = false)
    val failedStates =
        LoadStates(
            refresh = LoadState.Error(RemoteErrorException(RemoteError.Network)),
            prepend = notLoading,
            append = notLoading,
        )
    TheMovieTheme {
        Surface {
            SearchMovieContent(
                state = SearchMovieUiState(query = "bad"),
                movies = flowOf(PagingData.empty<Movie>(sourceLoadStates = failedStates)).collectAsLazyPagingItems(),
                onIntent = {},
                onMovieClick = {},
                floatingActionButton = {},
            )
        }
    }
}
