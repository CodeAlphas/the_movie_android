package com.codealphas.themovie.presentation.home

import android.Manifest
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.compose.AndroidFragment
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.codealphas.themovie.core.android.theme.TheMovieTheme
import com.codealphas.themovie.core.android.ui.rememberThrottledClick
import com.codealphas.themovie.presentation.R
import com.codealphas.themovie.presentation.movie.FragmentSearchMovie
import com.codealphas.themovie.presentation.movie.MovieCategory
import com.codealphas.themovie.presentation.movie.MovieFabMenu
import com.codealphas.themovie.presentation.movie.MovieListContent
import com.codealphas.themovie.presentation.movie.MovieListScreen
import com.codealphas.themovie.presentation.movie.MovieListUiState
import com.codealphas.themovie.presentation.movie.MovieListViewModel
import com.codealphas.themovie.presentation.notification.NotificationPromptDialog
import com.codealphas.themovie.presentation.notification.notificationPermissionState
import com.codealphas.themovie.presentation.notification.openNotificationSettings
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToDetail: (movieId: Int) -> Unit,
    onNavigateToReview: () -> Unit,
    onNavigateToMap: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // 알림 권한 확인에 Activity가 필요하고 HomeScreen은 Activity 안에서만 그리므로, 없으면 바로 드러나도록 checkNotNull 적용
    val activity = checkNotNull(LocalActivity.current)
    val resources = LocalResources.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentOnNavigateToLogin by rememberUpdatedState(onNavigateToLogin)
    // 탭마다 같은 감상문, 지도 메뉴라 한 메뉴로 보이도록, 펼침 여부를 탭이 공유하고 회전 뒤에도 남도록 rememberSaveable 적용
    var fabExpanded by rememberSaveable { mutableStateOf(false) }

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = resources.getString(R.string.notification_permission_denied),
                        duration = SnackbarDuration.Long,
                    )
                }
            }
        }

    LaunchedEffect(viewModel) {
        viewModel.onIntent(HomeIntent.Entered(activity.notificationPermissionState()))
    }

    // 화면이 멈춘 동안 보낸 안내를 돌아와서 받도록, 수집은 STARTED 동안만 하고 채널에 남은 effect는 다시 시작할 때 처리
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    HomeEffect.NavigateToLogin -> currentOnNavigateToLogin()
                    HomeEffect.RequestNotificationPermission ->
                        // POST_NOTIFICATIONS는 Android 13(API 33)부터 런타임 권한이므로, 그 전 기기에서는 요청 제외
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    HomeEffect.OpenNotificationSettings -> context.openNotificationSettings()
                }
            }
        }
    }

    HomeContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::onIntent,
        onNotificationSettingsClick = {
            viewModel.onIntent(HomeIntent.NotificationSettingsClicked(activity.notificationPermissionState()))
        },
    ) { tab, isCurrentPage ->
        when (tab) {
            HomeTab.POPULAR, HomeTab.TOP_RATED -> {
                val category = if (tab == HomeTab.POPULAR) MovieCategory.POPULAR else MovieCategory.TOP_RATED
                MovieListScreen(
                    viewModel = movieListViewModel(category),
                    isCurrentPage = isCurrentPage,
                    snackbarHostState = snackbarHostState,
                    onMovieClick = onNavigateToDetail,
                ) { visible ->
                    MovieFabMenu(
                        visible = visible,
                        expanded = fabExpanded,
                        onExpandedChange = { fabExpanded = it },
                        onReviewClick = onNavigateToReview,
                        onMapClick = onNavigateToMap,
                    )
                }
            }
            // 크기를 주지 않은 AndroidFragment는 검색 화면 내용 높이만큼만 차지해 HorizontalPager 페이지 위쪽이 아니라 가운데에 놓이므로,
            // ViewPager2 페이지처럼 탭 영역을 모두 채우도록 크기 적용
            HomeTab.SEARCH -> AndroidFragment<FragmentSearchMovie>(modifier = Modifier.fillMaxSize())
        }
    }
}

// 두 탭이 같은 Activity의 ViewModel 저장소를 쓰므로, 분류 이름을 key로 탭마다 다른 ViewModel을 만들고 분류를 생성 인자로 전달
@Composable
private fun movieListViewModel(category: MovieCategory): MovieListViewModel =
    hiltViewModel<MovieListViewModel, MovieListViewModel.Factory>(key = category.name) { factory ->
        factory.create(category)
    }

@Composable
internal fun HomeContent(
    state: HomeUiState,
    snackbarHostState: SnackbarHostState,
    onIntent: (HomeIntent) -> Unit,
    onNotificationSettingsClick: () -> Unit,
    page: @Composable (tab: HomeTab, isCurrentPage: Boolean) -> Unit,
) {
    val tabs = HomeTab.entries
    val pagerState = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()

    // 앱바 색이 라이트에서 어둡고 다크에서 밝아 시스템 아이콘 색과 겹치므로, XML처럼 상태 표시줄 뒤에는 창 배경이 보이도록 앱바를 그 아래에 배치하고,
    // Edge-to-Edge라 시스템이 창을 줄여 주지 않으므로 검색 키보드가 탭을 가리지 않도록 나머지는 safeDrawing만큼 안쪽 여백 적용
    Scaffold(
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
        topBar = {
            HomeTopAppBar(
                title = tabs[pagerState.currentPage].appBarTitle,
                onLogoutClick = { onIntent(HomeIntent.LogoutClicked) },
                onNotificationSettingsClick = onNotificationSettingsClick,
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Box(modifier = Modifier.weight(1f)) {
                // 검색 탭은 아직 Fragment라 페이지가 composition에서 빠지면 Fragment와 ViewModel이 사라져 검색어가 지워지므로,
                // ViewPager2처럼 모든 탭을 유지하도록 나머지 페이지를 모두 미리 구성
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = tabs.size - 1,
                ) { index ->
                    // ViewPager2는 스크롤이 멈춘 뒤 현재 탭을 RESUMED로 올렸으므로, 넘기는 중이 아니라 멈춘 페이지를 현재 페이지로 적용
                    page(tabs[index], index == pagerState.settledPage)
                }
                SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
            }
            PrimaryTabRow(
                selectedTabIndex = pagerState.currentPage,
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(text = stringResource(tab.label)) },
                        icon = { Icon(painter = painterResource(tab.icon), contentDescription = null) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (state.showNotificationPrompt) {
        NotificationPromptDialog(
            onAllow = { onIntent(HomeIntent.NotificationPromptAccepted) },
            onLater = { onIntent(HomeIntent.NotificationPromptDeclined) },
        )
    }
}

// Material3 1.4.0의 TopAppBar는 아직 실험 API라서, 앱바 한 곳에만 opt-in 적용
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopAppBar(
    @StringRes title: Int,
    onLogoutClick: () -> Unit,
    onNotificationSettingsClick: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text(text = stringResource(title)) },
        actions = {
            IconButton(onClick = rememberThrottledClick(onClick = onLogoutClick)) {
                Icon(
                    painter = painterResource(android.R.drawable.ic_lock_power_off),
                    contentDescription = stringResource(R.string.common_logout),
                )
            }
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_more_vert_24),
                    contentDescription = stringResource(R.string.common_more_menu),
                )
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.notification_settings_menu)) },
                    onClick = {
                        menuExpanded = false
                        onNotificationSettingsClick()
                    },
                )
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
            ),
    )
}

@Preview(name = "라이트")
@Preview(name = "다크", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeContentPreview() {
    HomeContentPreview(state = HomeUiState())
}

@Preview(name = "알림 권한 안내")
@Composable
private fun HomeContentNotificationPromptPreview() {
    HomeContentPreview(state = HomeUiState(showNotificationPrompt = true))
}

@Composable
private fun HomeContentPreview(state: HomeUiState) {
    TheMovieTheme {
        HomeContent(
            state = state,
            snackbarHostState = remember { SnackbarHostState() },
            onIntent = {},
            onNotificationSettingsClick = {},
            page = { tab, _ ->
                MovieListContent(
                    category = if (tab == HomeTab.TOP_RATED) MovieCategory.TOP_RATED else MovieCategory.POPULAR,
                    state = MovieListUiState(isLoading = true),
                    onIntent = {},
                    onMovieClick = {},
                    floatingActionButton = {},
                )
            },
        )
    }
}
