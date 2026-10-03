package com.codealphas.themovie.presentation.home

import android.content.res.Configuration
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.codealphas.themovie.core.android.theme.TheMovieTheme
import com.codealphas.themovie.core.android.ui.TheMovieTopAppBar
import com.codealphas.themovie.core.android.ui.rememberThrottledClick
import com.codealphas.themovie.presentation.R
import com.codealphas.themovie.presentation.home.navigation.HomeNavHost
import com.codealphas.themovie.presentation.home.navigation.HomeTab
import com.codealphas.themovie.presentation.home.navigation.navigateToTab
import com.codealphas.themovie.presentation.home.navigation.toHomeTab
import com.codealphas.themovie.presentation.movie.MovieCategory
import com.codealphas.themovie.presentation.movie.MovieFabMenu
import com.codealphas.themovie.presentation.movie.MovieListContent
import com.codealphas.themovie.presentation.movie.MovieListUiState
import com.codealphas.themovie.presentation.notification.NotificationPromptDialog
import com.codealphas.themovie.presentation.notification.notificationPermissionState
import com.codealphas.themovie.presentation.notification.openNotificationSettings
import com.codealphas.themovie.presentation.notification.rememberNotificationPermissionRequester

@Composable
fun HomeScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToDetail: (movieId: Int) -> Unit,
    onNavigateToReview: () -> Unit,
    onNavigateToMap: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // 알림 권한 확인에 Activity가 필요하고 HomeScreen은 Activity 안에서만 그리므로, 없으면 바로 드러나도록 checkNotNull 적용
    val activity = checkNotNull(LocalActivity.current)
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val requestNotificationPermission = rememberNotificationPermissionRequester(snackbarHostState)
    val currentOnNavigateToLogin by rememberUpdatedState(onNavigateToLogin)
    // 탭마다 같은 감상문, 지도 메뉴라 한 메뉴로 보이도록, 펼침 여부를 탭이 공유하고 회전 뒤에도 남도록 rememberSaveable 적용
    var fabExpanded by rememberSaveable { mutableStateOf(false) }
    val fabMenu: @Composable (visible: Boolean) -> Unit = { visible ->
        MovieFabMenu(
            visible = visible,
            expanded = fabExpanded,
            onExpandedChange = { fabExpanded = it },
            onReviewClick = onNavigateToReview,
            onMapClick = onNavigateToMap,
        )
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
                    HomeEffect.RequestNotificationPermission -> requestNotificationPermission()
                    HomeEffect.OpenNotificationSettings -> activity.openNotificationSettings()
                }
            }
        }
    }

    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()

    HomeContent(
        state = state,
        selectedTab = currentEntry?.destination.toHomeTab(),
        onTabSelected = navController::navigateToTab,
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::onIntent,
        onNotificationSettingsClick = {
            viewModel.onIntent(HomeIntent.NotificationSettingsClicked(activity.notificationPermissionState()))
        },
    ) {
        HomeNavHost(
            navController = navController,
            snackbarHostState = snackbarHostState,
            onNavigateToDetail = onNavigateToDetail,
            floatingActionButton = fabMenu,
        )
    }
}

// Activity와 NavController 없이 Preview를 그려야 해서 선택 탭, 탭 이동, 탭 내용을 따로 받으므로,
// 인자를 묶는 클래스를 새로 만들지 않도록 이 함수에만 인자 수 한도 예외 적용
@Suppress("LongParameterList")
@Composable
internal fun HomeContent(
    state: HomeUiState,
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    snackbarHostState: SnackbarHostState,
    onIntent: (HomeIntent) -> Unit,
    onNotificationSettingsClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    // 앱바 색이 라이트에서 어둡고 다크에서 밝아 상태 표시줄 뒤까지 칠하면 같은 계열 색의 시계와 배터리 아이콘이 묻히므로,
    // 상태 표시줄 뒤에는 창 배경이 보이도록 앱바를 상태 표시줄 높이만큼 내려 배치
    Scaffold(
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
        topBar = {
            TheMovieTopAppBar(
                title = stringResource(selectedTab.appBarTitle),
                actions = {
                    HomeAppBarActions(
                        onLogoutClick = { onIntent(HomeIntent.LogoutClicked) },
                        onNotificationSettingsClick = onNotificationSettingsClick,
                    )
                },
            )
        },
        // Edge-to-Edge라 시스템이 창을 줄여 주지 않으므로, 검색 키보드가 탭을 가리지 않도록 나머지는 safeDrawing만큼 안쪽 여백 적용
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Box(modifier = Modifier.weight(1f)) {
                content()
                SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
            }
            // Scaffold의 safeDrawing 여백 안에 두어 키보드 위로 올라가므로, 내비게이션 바 여백이 두 번 들어가지 않도록 자체 inset 제거
            NavigationBar(windowInsets = WindowInsets(0)) {
                HomeTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == selectedTab,
                        onClick = { onTabSelected(tab) },
                        icon = { Icon(painter = painterResource(tab.icon), contentDescription = null) },
                        label = { Text(text = stringResource(tab.label)) },
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

@Composable
private fun RowScope.HomeAppBarActions(
    onLogoutClick: () -> Unit,
    onNotificationSettingsClick: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
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
            selectedTab = HomeTab.POPULAR,
            onTabSelected = {},
            snackbarHostState = remember { SnackbarHostState() },
            onIntent = {},
            onNotificationSettingsClick = {},
        ) {
            MovieListContent(
                category = MovieCategory.POPULAR,
                state = MovieListUiState(isLoading = true),
                onIntent = {},
                onMovieClick = {},
                floatingActionButton = {},
            )
        }
    }
}
