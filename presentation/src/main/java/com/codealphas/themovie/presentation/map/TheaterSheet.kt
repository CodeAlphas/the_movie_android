package com.codealphas.themovie.presentation.map

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.codealphas.themovie.core.android.theme.Spacing
import com.codealphas.themovie.core.android.theme.TheMovieTheme
import com.codealphas.themovie.core.android.ui.rememberThrottledClick
import com.codealphas.themovie.domain.map.Theater
import com.codealphas.themovie.presentation.R

private val SheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
private val BadgeSize = 56.dp
private val BadgePadding = 14.dp
private val BadgeShape = RoundedCornerShape(16.dp)
private val HeaderDividerTop = 20.dp
private val DirectionsButtonHeight = 56.dp
private val DirectionsButtonShape = RoundedCornerShape(16.dp)
private val AddressIconSpacing = 12.dp
private val DistanceChipEndPadding = 12.dp
private const val HAIRLINE_ALPHA = 0.24f
private const val METERS_PER_KILOMETER = 1000
private val EyebrowLetterSpacing = 0.12.em
private val DirectionsLetterSpacing = 0.02.em

// Material3 1.4.0의 ModalBottomSheet는 아직 실험 API라서, 시트 한 곳에만 opt-in 적용
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TheaterSheet(
    theater: Theater,
    onDismiss: () -> Unit,
    onDirectionsClick: () -> Unit,
) {
    val hairline = MaterialTheme.colorScheme.outline.copy(alpha = HAIRLINE_ALPHA)
    // 기본 시트는 화면 배경과 같은 색이라 지도 위에서 카드처럼 떠 보이지 않으므로, 한 단계 밝은 표면색과 큰 모서리 적용
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = SheetShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        dragHandle = { BottomSheetDefaults.DragHandle(color = hairline) },
    ) {
        TheaterSheetBody(theater = theater, hairline = hairline, onDirectionsClick = onDirectionsClick)
    }
}

@Composable
private fun TheaterSheetBody(
    theater: Theater,
    hairline: Color,
    onDirectionsClick: () -> Unit,
) {
    Column(modifier = Modifier.padding(start = Spacing.large, end = Spacing.large, bottom = Spacing.large)) {
        TheaterHeader(theater = theater)
        HorizontalDivider(modifier = Modifier.padding(top = HeaderDividerTop), color = hairline)
        Row(
            modifier = Modifier.padding(top = Spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(AddressIconSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_baseline_location_on_24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = theater.address,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DirectionsButton(
            onClick = onDirectionsClick,
            modifier = Modifier.padding(top = Spacing.large),
        )
    }
}

@Composable
private fun TheaterHeader(theater: Theater) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.medium)) {
        TheaterBadge()
        Column {
            Text(
                text = stringResource(R.string.map_theater_eyebrow),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
                letterSpacing = EyebrowLetterSpacing,
            )
            Text(
                text = theater.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            theater.distanceMeters?.let { meters ->
                DistanceChip(meters = meters, modifier = Modifier.padding(top = Spacing.small))
            }
        }
    }
}

@Composable
private fun TheaterBadge() {
    val gradient =
        Brush.linearGradient(
            colors =
                listOf(MaterialTheme.colorScheme.tertiaryContainer, TheMovieTheme.extendedColors.tertiaryContainerEnd),
            start = Offset.Zero,
            end = Offset.Infinite,
        )
    Box(
        modifier = Modifier.size(BadgeSize).background(gradient, BadgeShape).padding(BadgePadding),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_baseline_movie),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
        )
    }
}

@Composable
private fun DistanceChip(
    meters: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .background(TheMovieTheme.extendedColors.onPrimaryScrim, CircleShape)
                .padding(
                    start = Spacing.small,
                    top = Spacing.extraSmall,
                    end = DistanceChipEndPadding,
                    bottom = Spacing.extraSmall,
                ),
        horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_baseline_location_on_16),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
        )
        Text(
            text = distanceText(meters),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun distanceText(meters: Int): String =
    if (meters < METERS_PER_KILOMETER) {
        stringResource(R.string.map_theater_distance_meters, meters)
    } else {
        stringResource(R.string.map_theater_distance_kilometers, meters / METERS_PER_KILOMETER.toDouble())
    }

@Composable
private fun DirectionsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = rememberThrottledClick(onClick = onClick),
        modifier = modifier.fillMaxWidth().height(DirectionsButtonHeight),
        shape = DirectionsButtonShape,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            ),
        contentPadding = PaddingValues(horizontal = Spacing.medium),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_baseline_directions_24),
            contentDescription = null,
            modifier = Modifier.padding(end = Spacing.small),
        )
        Text(
            text = stringResource(R.string.map_theater_directions),
            style = MaterialTheme.typography.titleSmall,
            letterSpacing = DirectionsLetterSpacing,
        )
    }
}

// ModalBottomSheet는 별도 창으로 떠 Preview에 그려지지 않으므로, 시트 안 내용만 시트 배경 위에 그리도록 처리
@Preview(name = "라이트")
@Preview(name = "다크", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TheaterSheetBodyPreview() {
    TheMovieTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = SheetShape) {
            TheaterSheetBody(
                theater = previewTheater(),
                hairline = MaterialTheme.colorScheme.outline.copy(alpha = HAIRLINE_ALPHA),
                onDirectionsClick = {},
            )
        }
    }
}

private fun previewTheater(): Theater =
    Theater(
        id = "1",
        name = "Theater",
        latitude = 0.0,
        longitude = 0.0,
        address = "Address",
        distanceMeters = 430,
    )
