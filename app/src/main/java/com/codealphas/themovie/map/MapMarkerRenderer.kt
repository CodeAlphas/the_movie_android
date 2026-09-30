package com.codealphas.themovie.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.DrawableCompat
import com.codealphas.themovie.R
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.label.LabelLayer
import com.kakao.vectormap.label.LabelLayerOptions
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.label.LabelTextBuilder
import com.kakao.vectormap.label.LabelTextStyle

class MapMarkerRenderer(
    private val context: Context,
    map: KakaoMap,
) {
    private val currentStyles: LabelStyles
    private val theaterStyles: LabelStyles
    private val currentLayer: LabelLayer
    private val theaterLayer: LabelLayer
    private var renderedState: MapUiState? = null

    init {
        val labelManager = checkNotNull(map.labelManager)
        // styleId가 둘 다 빈 문자열이면 SDK가 같은 스타일로 보고 두 번째를 등록하지 않으므로, 스타일마다 id를 부여
        val currentStyle = markerStyle(context.getColor(R.color.map_marker_current), textLineCount = 2)
        val theaterStyle = markerStyle(context.getColor(R.color.map_marker_theater), textLineCount = 1)
        currentStyles = checkNotNull(labelManager.addLabelStyles(LabelStyles.from(CURRENT_STYLE_ID, currentStyle)))
        theaterStyles = checkNotNull(labelManager.addLabelStyles(LabelStyles.from(THEATER_STYLE_ID, theaterStyle)))
        currentLayer = checkNotNull(labelManager.addLayer(LabelLayerOptions.from(CURRENT_LAYER_ID)))
        theaterLayer = checkNotNull(labelManager.addLayer(LabelLayerOptions.from(THEATER_LAYER_ID)))
    }

    // 한쪽 응답에 다른 쪽 Label이 지워졌다 다시 생기지 않도록, 이전에 그린 상태와 달라진 레이어만 다시 그리도록 적용
    fun render(state: MapUiState) {
        val previous = renderedState
        val currentChanged = previous?.currentLocation != state.currentLocation || previous?.address != state.address
        if (previous == null || currentChanged) {
            renderCurrent(state)
        }
        if (previous == null || previous.theaters != state.theaters) {
            renderTheaters(state)
        }
        renderedState = state
    }

    private fun renderCurrent(state: MapUiState) {
        currentLayer.removeAll()
        state.currentLocation?.let { location ->
            currentLayer.addLabel(
                LabelOptions
                    .from(LatLng.from(location.latitude, location.longitude))
                    .setStyles(currentStyles)
                    .setTexts(currentLabelText(state)),
            )
        }
    }

    private fun renderTheaters(state: MapUiState) {
        theaterLayer.removeAll()
        state.theaters.forEach { theater ->
            theaterLayer.addLabel(
                LabelOptions
                    .from(LatLng.from(theater.latitude, theater.longitude))
                    .setStyles(theaterStyles)
                    .setTexts(LabelTextBuilder().setTexts(theater.name)),
            )
        }
    }

    // 텍스트 줄 수와 스타일 수가 다르면 네이티브에서 앱이 종료되므로, 주소가 없어도 빈 둘째 줄을 두어 줄 수 유지
    private fun currentLabelText(state: MapUiState): LabelTextBuilder {
        val title = context.getString(R.string.map_current_location)
        return LabelTextBuilder().setTexts(title, state.address?.fullAddress.orEmpty())
    }

    // 텍스트 줄 수보다 스타일이 적으면 네이티브에서 앱이 종료되므로, 줄 수만큼 스타일 생성
    private fun markerStyle(
        @ColorInt color: Int,
        textLineCount: Int,
    ): LabelStyle {
        val resources = context.resources
        val textSize = resources.getDimensionPixelSize(R.dimen.map_label_text_size)
        val textStroke = resources.getDimensionPixelSize(R.dimen.map_label_text_stroke)
        val textColor = context.getColor(R.color.map_label_text)
        val strokeColor = context.getColor(R.color.map_label_stroke)
        val textStyles = Array(textLineCount) { LabelTextStyle.from(textSize, textColor, textStroke, strokeColor) }
        return LabelStyle
            .from(markerBitmap(color))
            .setAnchorPoint(MARKER_ANCHOR_X, MARKER_ANCHOR_Y)
            .setTextStyles(*textStyles)
    }

    // 벡터 리소스는 Label 아이콘으로 바로 쓸 수 없으므로, 색을 입힌 Bitmap으로 변환해 적용
    private fun markerBitmap(
        @ColorInt color: Int,
    ): Bitmap {
        val icon = checkNotNull(ContextCompat.getDrawable(context, R.drawable.ic_baseline_location_on_24))
        val drawable = DrawableCompat.wrap(icon.mutate())
        DrawableCompat.setTint(drawable, color)
        val iconSize = context.resources.getDimensionPixelSize(R.dimen.map_marker_icon_size)
        val bitmap = createBitmap(iconSize, iconSize)
        drawable.setBounds(0, 0, iconSize, iconSize)
        drawable.draw(Canvas(bitmap))
        return bitmap
    }

    private companion object {
        const val CURRENT_LAYER_ID = "current"
        const val THEATER_LAYER_ID = "theater"
        const val CURRENT_STYLE_ID = "currentStyle"
        const val THEATER_STYLE_ID = "theaterStyle"
        const val MARKER_ANCHOR_X = 0.5f
        const val MARKER_ANCHOR_Y = 1.0f
    }
}
