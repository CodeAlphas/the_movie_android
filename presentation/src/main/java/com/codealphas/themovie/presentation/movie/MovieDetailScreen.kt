package com.codealphas.themovie.presentation.movie

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import coil3.compose.AsyncImage
import com.codealphas.themovie.core.android.theme.Spacing
import com.codealphas.themovie.core.android.theme.TheMovieTheme
import com.codealphas.themovie.core.android.ui.RatingStars
import com.codealphas.themovie.core.android.ui.TheMovieTopAppBar
import com.codealphas.themovie.domain.movie.Cast
import com.codealphas.themovie.domain.movie.MovieDetail
import com.codealphas.themovie.domain.movie.Video
import com.codealphas.themovie.domain.result.RemoteError
import com.codealphas.themovie.presentation.R
import com.codealphas.themovie.presentation.ui.remoteErrorMessage
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.YouTubePlayerCallback
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

private val PosterWidth = 148.dp
private const val POSTER_ASPECT_RATIO = 2f / 3f
private val CastItemWidth = 125.dp
private val CastImageSize = 110.dp
private const val PLAYER_ASPECT_RATIO = 16f / 9f
private val StarSize = 18.dp
private const val PREVIEW_CAST_COUNT = 4

@Composable
fun MovieDetailScreen(
    onNavigateUp: () -> Unit,
    viewModel: MovieDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    // 화면이 멈춘 동안 보낸 안내를 돌아와서 받도록, 수집은 STARTED 동안만 하고 채널에 남은 effect는 다시 시작할 때 처리
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is MovieDetailEffect.ShowError ->
                        snackbarHostState.showSnackbar(
                            message = remoteErrorMessage(resources, effect.error),
                            duration = SnackbarDuration.Long,
                        )
                }
            }
        }
    }

    MovieDetailContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::onIntent,
        onNavigateUp = onNavigateUp,
    )
}

@Composable
internal fun MovieDetailContent(
    state: MovieDetailUiState,
    snackbarHostState: SnackbarHostState,
    onIntent: (MovieDetailIntent) -> Unit,
    onNavigateUp: () -> Unit,
) {
    // 앱바 색이 라이트에서 어둡고 다크에서 밝아 상태 표시줄 뒤까지 칠하면 같은 계열 색의 시계와 배터리 아이콘이 묻히므로,
    // 상태 표시줄 뒤에는 창 배경이 보이도록 앱바를 상태 표시줄 높이만큼 내려 배치
    Scaffold(
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
        topBar = {
            TheMovieTopAppBar(title = stringResource(R.string.app_name), onNavigateUp = onNavigateUp)
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            state.detail?.let { detail -> MovieDetailBody(detail = detail) }
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            val loadError = state.loadError
            if (loadError != null && state.detail == null && !state.isLoading) {
                LoadErrorContent(
                    error = loadError,
                    onRetry = { onIntent(MovieDetailIntent.RetryClicked) },
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

@Composable
private fun MovieDetailBody(detail: MovieDetail) {
    // 포스터와 제목을 스크롤 밖에 고정하면 배우 정보와 줄거리가 보이는 영역이 그만큼 좁아지므로,
    // 포스터까지 함께 올라가도록 화면 전체에 스크롤 적용
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        MovieSummary(detail = detail)
        SectionTitle(title = R.string.movie_detail_cast_title)
        CastRow(cast = detail.cast)
        detail.videos.firstOrNull()?.let { video ->
            SectionTitle(title = R.string.movie_detail_video_title)
            TrailerPlayer(videoKey = video.key)
        }
        SectionTitle(title = R.string.movie_detail_overview_title)
        Text(
            text = detail.overview,
            modifier = Modifier.padding(horizontal = Spacing.medium),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun MovieSummary(detail: MovieDetail) {
    Row(
        modifier = Modifier.padding(start = Spacing.medium, top = Spacing.medium, end = Spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val placeholder = painterResource(R.drawable.poster_placeholder)
        AsyncImage(
            model = detail.posterUrl,
            contentDescription = null,
            modifier = Modifier.width(PosterWidth).aspectRatio(POSTER_ASPECT_RATIO),
            placeholder = placeholder,
            error = placeholder,
            fallback = placeholder,
            contentScale = ContentScale.Crop,
        )
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
            Text(
                text = detail.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.movie_detail_release, detail.releaseDate),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.movie_detail_rating, detail.voteAverage),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // TMDB 평점은 10점 만점이고 별은 5개라서, 별 하나가 2점이 되도록 절반으로 줄여 표시
            RatingStars(filledStars = (detail.voteAverage / 2).toFloat(), starSize = StarSize)
        }
    }
}

@Composable
private fun SectionTitle(
    @StringRes title: Int,
) {
    Text(
        text = stringResource(title),
        modifier = Modifier.padding(start = Spacing.medium, top = Spacing.small, end = Spacing.medium),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun CastRow(cast: List<Cast>) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Spacing.small),
    ) {
        items(cast) { person -> CastItem(cast = person) }
    }
}

@Composable
private fun CastItem(cast: Cast) {
    Column(
        modifier = Modifier.width(CastItemWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // AsyncImage 대체 이미지로 넘긴 drawable은 Compose 테마 색을 읽지 못하고 colorFilter는 불러온 사진까지 칠하므로,
        // 사진 아래에 테마 색 틀을 깔아 로딩 중이거나 사진이 없을 때 노출
        Box(
            modifier =
                Modifier
                    .padding(Spacing.extraSmall)
                    .size(CastImageSize)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Icon(
                painter = painterResource(R.drawable.cast_profile_silhouette),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AsyncImage(
                model = cast.profileUrl,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Text(
            text = cast.character,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = cast.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TrailerPlayer(videoKey: String) {
    Box(modifier = Modifier.fillMaxWidth().aspectRatio(PLAYER_ASPECT_RATIO)) {
        // Preview는 WebView를 띄우지 못해 플레이어 자리가 그려지지 않으므로, Preview에서는 준비 중 화면만 그리도록 처리
        if (LocalInspectionMode.current) {
            PlayerLoading()
        } else {
            YouTubeVideoPlayer(videoKey = videoKey)
        }
    }
}

@Composable
private fun YouTubeVideoPlayer(videoKey: String) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var isReady by remember(videoKey) { mutableStateOf(false) }
    // 코드로 만든 YouTubePlayerView는 생성자에서 IFrame 초기화를 시작해 initialize를 한 번 더 부르면 예외가 나므로,
    // initialize 대신 준비 콜백에서 썸네일과 재생 버튼만 보이도록 자동 재생 없이 영상 로드
    val playerView =
        remember(videoKey) {
            YouTubePlayerView(context).apply {
                getYouTubePlayerWhenReady(
                    object : YouTubePlayerCallback {
                        override fun onYouTubePlayer(youTubePlayer: YouTubePlayer) {
                            youTubePlayer.cueVideo(videoKey, 0f)
                            isReady = true
                        }
                    },
                )
            }
        }

    // 플레이어가 화면 생명주기를 모르면 화면을 나가도 영상 소리가 계속 나므로, 재개와 멈춤을 플레이어에 전달
    DisposableEffect(lifecycle, playerView) {
        val observer =
            LifecycleEventObserver { owner, event ->
                // ON_DESTROY를 넘기면 YouTubePlayerView가 release를 부르고 아래 DisposableEffect도 한 번 더 불러
                // 네트워크 콜백 해제에서 예외가 나므로, ON_DESTROY 제외
                if (event != Lifecycle.Event.ON_DESTROY) playerView.onStateChanged(owner, event)
            }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    // 화면을 나가거나 videoKey가 바뀌어 새 플레이어를 만들 때 이전 플레이어가 남지 않도록, 컴포지션에서 빠질 때 한 번만 해제
    DisposableEffect(playerView) {
        onDispose { playerView.release() }
    }

    AndroidView(factory = { playerView }, modifier = Modifier.fillMaxSize())
    // IFrame을 불러오는 동안 플레이어 자리가 흰 빈칸으로 보이므로, 준비 콜백이 오기 전까지 로딩 표시를 겹침 적용
    if (!isReady) PlayerLoading()
}

@Composable
private fun PlayerLoading() {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Preview(name = "라이트")
@Preview(name = "다크", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MovieDetailContentPreview() {
    TheMovieTheme {
        MovieDetailContent(
            state = MovieDetailUiState(detail = previewMovieDetail(), isLoading = false),
            snackbarHostState = remember { SnackbarHostState() },
            onIntent = {},
            onNavigateUp = {},
        )
    }
}

@Preview(name = "불러오는 중")
@Composable
private fun MovieDetailContentLoadingPreview() {
    TheMovieTheme {
        MovieDetailContent(
            state = MovieDetailUiState(),
            snackbarHostState = remember { SnackbarHostState() },
            onIntent = {},
            onNavigateUp = {},
        )
    }
}

@Preview(name = "실패")
@Composable
private fun MovieDetailContentErrorPreview() {
    TheMovieTheme {
        MovieDetailContent(
            state = MovieDetailUiState(isLoading = false, loadError = RemoteError.Network),
            snackbarHostState = remember { SnackbarHostState() },
            onIntent = {},
            onNavigateUp = {},
        )
    }
}

private fun previewMovieDetail(): MovieDetail =
    MovieDetail(
        id = 0,
        title = "Movie",
        posterUrl = null,
        releaseDate = "2019-05-30",
        overview = "Overview",
        voteAverage = 7.3,
        cast = List(PREVIEW_CAST_COUNT) { Cast(name = "Actor $it", character = "Character $it", profileUrl = null) },
        videos = listOf(Video(key = "key", name = "Trailer", type = "Trailer")),
    )
