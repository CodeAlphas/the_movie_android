package com.codealphas.themovie.presentation.movie

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.codealphas.themovie.core.android.theme.Spacing
import com.codealphas.themovie.core.android.ui.rememberThrottledClick
import com.codealphas.themovie.presentation.R

private val MainButtonSize = 60.dp
private val MenuButtonSize = 55.dp
private const val MOVE_DURATION_MS = 300
private const val FADE_IN_DURATION_MS = 800
private const val FADE_OUT_DURATION_MS = 150
private const val EXPANDED_ROTATION = 45f

@Composable
fun MovieFabMenu(
    visible: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onReviewClick: () -> Unit,
    onMapClick: () -> Unit,
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) EXPANDED_ROTATION else 0f,
        animationSpec = tween(MOVE_DURATION_MS),
        label = "fabRotation",
    )

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        MenuButton(
            visible = visible && expanded,
            icon = R.drawable.ic_baseline_location_on_24,
            description = R.string.map_appbar_title,
            onClick = onMapClick,
        )
        MenuButton(
            visible = visible && expanded,
            icon = R.drawable.ic_baseline_edit_24,
            description = R.string.review_list_appbar_title,
            onClick = onReviewClick,
        )
        // visible이 바뀔 때 FAB가 갑자기 사라지지 않도록, 크기를 줄이며 흐려지는 전환 적용
        AnimatedVisibility(visible = visible, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
            FloatingActionButton(
                onClick = { onExpandedChange(!expanded) },
                modifier = Modifier.size(MainButtonSize),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_add_24),
                    contentDescription = null,
                    modifier = Modifier.rotate(rotation),
                )
            }
        }
    }
}

@Composable
private fun MenuButton(
    visible: Boolean,
    @DrawableRes icon: Int,
    @StringRes description: Int,
    onClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(tween(MOVE_DURATION_MS)) { it } + fadeIn(tween(FADE_IN_DURATION_MS)),
        exit = slideOutVertically(tween(MOVE_DURATION_MS)) { it } + fadeOut(tween(FADE_OUT_DURATION_MS)),
    ) {
        FloatingActionButton(
            onClick = rememberThrottledClick(onClick = onClick),
            modifier = Modifier.size(MenuButtonSize),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
        ) {
            Icon(painter = painterResource(icon), contentDescription = stringResource(description))
        }
    }
}
