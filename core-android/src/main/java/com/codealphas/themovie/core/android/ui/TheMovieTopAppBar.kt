package com.codealphas.themovie.core.android.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.codealphas.themovie.core.android.R

// Material3 1.4.0의 TopAppBar는 아직 실험 API라서, 앱바 한 곳에만 opt-in 적용
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TheMovieTopAppBar(
    title: String,
    onNavigateUp: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(text = title) },
        navigationIcon = {
            if (onNavigateUp != null) {
                IconButton(onClick = onNavigateUp) {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_arrow_back_24),
                        contentDescription = stringResource(R.string.common_navigate_up),
                    )
                }
            }
        },
        actions = actions,
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
            ),
    )
}
