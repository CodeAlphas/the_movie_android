package com.codealphas.themovie.presentation.review

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.codealphas.themovie.core.android.theme.Spacing
import com.codealphas.themovie.core.android.theme.TheMovieTheme
import com.codealphas.themovie.core.android.ui.RatingStars
import com.codealphas.themovie.core.android.ui.rememberThrottledClick
import com.codealphas.themovie.core.android.ui.showToast
import com.codealphas.themovie.domain.review.Review
import com.codealphas.themovie.presentation.R
import com.codealphas.themovie.presentation.movie.MovieHeaderBackground
import com.codealphas.themovie.presentation.movie.MovieHeaderTexts

private val PanelShape = RoundedCornerShape(5.dp)
private val PanelBorderWidth = 1.dp
private val CardShape = RoundedCornerShape(10.dp)
private val CardElevation = 2.dp
private val CardBorderWidth = 1.dp
private val StarSize = 16.dp
private const val PREVIEW_REVIEW_COUNT = 3

@Composable
fun ReviewListScreen(
    onNavigateUp: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToEdit: (reviewId: Int?) -> Unit,
    viewModel: ReviewListViewModel = hiltViewModel(),
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
                    ReviewListEffect.ShowSyncFailed ->
                        context.showToast(R.string.review_list_sync_failed, Toast.LENGTH_LONG)
                    ReviewListEffect.ShowDeleted -> context.showToast(R.string.review_list_deleted, Toast.LENGTH_LONG)
                    ReviewListEffect.ShowDeleteFailed ->
                        context.showToast(R.string.review_list_delete_failed, Toast.LENGTH_LONG)
                    ReviewListEffect.NavigateToLogin -> currentOnNavigateToLogin()
                }
            }
        }
    }

    ReviewListContent(
        state = state,
        onIntent = viewModel::onIntent,
        onNavigateUp = onNavigateUp,
        onNavigateToEdit = onNavigateToEdit,
    )
}

@Composable
internal fun ReviewListContent(
    state: ReviewListUiState,
    onIntent: (ReviewListIntent) -> Unit,
    onNavigateUp: () -> Unit,
    onNavigateToEdit: (reviewId: Int?) -> Unit,
) {
    // 앱바 색이 라이트에서 어둡고 다크에서 밝아 상태 표시줄 뒤까지 칠하면 같은 계열 색의 시계와 배터리 아이콘이 묻히므로,
    // 상태 표시줄 뒤에는 창 배경이 보이도록 앱바를 상태 표시줄 높이만큼 내려 배치
    Scaffold(
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
        topBar = {
            ReviewListTopAppBar(
                onNavigateUp = onNavigateUp,
                onLogoutClick = { onIntent(ReviewListIntent.LogoutClicked) },
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            MovieHeaderBackground()
            Column(modifier = Modifier.fillMaxSize()) {
                MovieHeaderTexts(title = R.string.review_list_title, subtitle = R.string.review_list_subtitle)
                ReviewListPanel(
                    state = state,
                    onReviewClick = { review -> onNavigateToEdit(review.id) },
                    onDeleteClick = { review -> onIntent(ReviewListIntent.DeleteClicked(review)) },
                    onAddClick = { onNavigateToEdit(null) },
                    modifier = Modifier.weight(1f).padding(Spacing.small),
                )
            }
        }
    }
}

// Material3 1.4.0의 TopAppBar는 아직 실험 API라서, 앱바 한 곳에만 opt-in 적용
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewListTopAppBar(
    onNavigateUp: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    TopAppBar(
        title = { Text(text = stringResource(R.string.review_list_appbar_title)) },
        navigationIcon = {
            IconButton(onClick = onNavigateUp) {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_arrow_back_24),
                    contentDescription = stringResource(R.string.common_navigate_up),
                )
            }
        },
        actions = {
            IconButton(onClick = rememberThrottledClick(onClick = onLogoutClick)) {
                Icon(
                    painter = painterResource(android.R.drawable.ic_lock_power_off),
                    contentDescription = stringResource(R.string.common_logout),
                )
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
            ),
    )
}

@Composable
private fun ReviewListPanel(
    state: ReviewListUiState,
    onReviewClick: (Review) -> Unit,
    onDeleteClick: (Review) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, PanelShape)
                .border(PanelBorderWidth, MaterialTheme.colorScheme.outline, PanelShape),
    ) {
        val reviews = state.reviews
        if (reviews != null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(Spacing.small),
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                items(items = reviews, key = { it.id }) { review ->
                    ReviewCard(
                        review = review,
                        onClick = { onReviewClick(review) },
                        onDeleteClick = { onDeleteClick(review) },
                    )
                }
            }
        }
        // Room에 감상문이 이미 있으면 동기화가 그 위에 덮어쓰므로, 목록을 가리지 않도록 보여 줄 감상문이 없을 때만 로딩 표시
        if (reviews.isNullOrEmpty() && state.isSyncing) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        // 동기화 중에는 서버 감상문이 아직 Room에 들어오지 않았으므로,
        // 불러오는 중을 감상문 없음으로 안내하지 않도록 동기화가 끝난 뒤에만 빈 목록 안내 표시
        if (reviews != null && reviews.isEmpty() && !state.isSyncing) {
            Text(
                text = stringResource(R.string.review_list_empty),
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FloatingActionButton(
            onClick = rememberThrottledClick(onClick = onAddClick),
            modifier = Modifier.align(Alignment.BottomEnd).padding(Spacing.medium),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_baseline_add_24),
                contentDescription = stringResource(R.string.review_list_add),
            )
        }
    }
}

@Composable
private fun ReviewCard(
    review: Review,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val gradient =
        Brush.horizontalGradient(
            listOf(MaterialTheme.colorScheme.tertiaryContainer, TheMovieTheme.extendedColors.tertiaryContainerEnd),
        )
    val contentColor = MaterialTheme.colorScheme.onTertiaryContainer
    Card(
        onClick = rememberThrottledClick(onClick = onClick),
        shape = CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = CardElevation),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(gradient, CardShape)
                    .border(CardBorderWidth, MaterialTheme.colorScheme.tertiary, CardShape)
                    .padding(Spacing.extraSmall),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = review.title,
                    modifier = Modifier.weight(1f).padding(Spacing.extraSmall),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(onClick = rememberThrottledClick(onClick = onDeleteClick)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_delete_24),
                        contentDescription = stringResource(R.string.review_list_delete),
                        tint = contentColor,
                    )
                }
            }
            ReviewCardFooter(review = review)
        }
    }
}

@Composable
private fun ReviewCardFooter(review: Review) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(Spacing.extraSmall),
        horizontalArrangement = Arrangement.spacedBy(Spacing.small, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.review_list_rating_label),
            style = MaterialTheme.typography.bodyLarge,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        // 감상문 평점은 10점 만점이고 별은 5개라서, 별 하나가 2점이 되도록 절반으로 줄여 표시
        RatingStars(filledStars = (review.rating / 2).toFloat(), starSize = StarSize)
        Text(
            text = review.time,
            style = MaterialTheme.typography.bodyLarge,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

@Preview(name = "라이트")
@Preview(name = "다크", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ReviewListContentPreview() {
    ReviewListContentPreview(
        state = ReviewListUiState(reviews = List(PREVIEW_REVIEW_COUNT) { previewReview(it) }, isSyncing = false),
    )
}

@Preview(name = "빈 목록")
@Composable
private fun ReviewListContentEmptyPreview() {
    ReviewListContentPreview(state = ReviewListUiState(reviews = emptyList(), isSyncing = false))
}

@Preview(name = "불러오는 중")
@Composable
private fun ReviewListContentSyncingPreview() {
    ReviewListContentPreview(state = ReviewListUiState())
}

@Composable
private fun ReviewListContentPreview(state: ReviewListUiState) {
    TheMovieTheme {
        ReviewListContent(
            state = state,
            onIntent = {},
            onNavigateUp = {},
            onNavigateToEdit = {},
        )
    }
}

private fun previewReview(index: Int): Review =
    Review(
        title = "Review $index",
        image = "",
        content = "",
        time = "2026/10/02 12:00",
        rating = 7.0,
        storageFileName = "",
        id = index,
    )
