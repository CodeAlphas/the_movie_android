package com.codealphas.themovie.presentation.map

import com.codealphas.themovie.presentation.map.location.LocationLatLng
import org.junit.Assert.assertEquals
import org.junit.Test

class KakaoMapDirectionsTest {
    private val start = LocationLatLng(latitude = 37.4979, longitude = 127.0276)
    private val end = LocationLatLng(latitude = 37.5009, longitude = 127.0264)

    @Test
    fun `앱 주소는 출발지와 도착지를 위도, 경도 순서로 넣고 대중교통으로 요청해야 한다`() {
        assertEquals(
            "kakaomap://route?sp=37.4979,127.0276&ep=37.5009,127.0264&by=publictransit",
            KakaoMapDirections.appUrl(start, end),
        )
    }

    @Test
    fun `웹 주소는 앱 주소와 같은 인자를 https 모바일 웹 주소에 붙여야 한다`() {
        assertEquals(
            "https://m.map.kakao.com/scheme/route?sp=37.4979,127.0276&ep=37.5009,127.0264&by=publictransit",
            KakaoMapDirections.webUrl(start, end),
        )
    }
}
