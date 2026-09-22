package com.example.movieapplication.utils

import android.content.Context
import android.util.DisplayMetrics
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import com.example.movieapplication.models.Review
import java.text.SimpleDateFormat
import java.util.*

class Utils {

    companion object {

        const val TAG: String = "로그"

        // 기기의 가로 px을 반환해주는 메소드
        fun getScreenWidth(context: Context): Int {
            val metrics = context.resources.displayMetrics
            val screenWidth = metrics.widthPixels
            return dpToPx(screenWidth.toFloat(), metrics).toInt()
        }

        // dp를 px로 변환해주는 메소드
        fun dpToPx(px: Float, metrics: DisplayMetrics): Float {
            return px / (metrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
        }

        // 소프트 키보드를 화면에서 내려가게하는 메소드
        fun hideKeyboard(context: Context, view: View) {
            val inputMethodManager =
                context.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
        }

        // 현재 시간을 주어진 형식으로 반환해주는 메소드
        fun getCurrentDate(): String {
            val time = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.KOREA)
            return time.format(Date())
        }
    }
}

interface ReviewClickDeleteInterface {
    fun onDeleteIconClick(review: Review)
}

interface ReviewClickInterface {
    fun onIconClick(review: Review)
}