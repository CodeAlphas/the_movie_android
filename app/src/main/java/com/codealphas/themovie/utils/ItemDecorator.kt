package com.example.movieapplication.utils

import android.content.Context
import android.graphics.Rect
import android.util.TypedValue
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ItemDecorator(var divWidth: Int, val context: Context) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)

        val divWidthPx = dpToPx(context, divWidth)
        val ch: GridLayoutManager.LayoutParams = view.layoutParams as GridLayoutManager.LayoutParams
        val index: Int = ch.spanIndex

        if (index == 0) {
            outRect.left = divWidthPx
            outRect.right = divWidthPx / 2
        } // (GridLayout)의 왼쪽에 위치할 아이템
        else {
            outRect.left = divWidthPx / 2
            outRect.right = divWidthPx
        } // (GridLayout)의 오른쪽에 위치할 아이템
    }

    // dp를 px로 변환해주는 메소드
    private fun dpToPx(context: Context, dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}