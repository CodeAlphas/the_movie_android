package com.codealphas.themovie.map

// android.net.Uri는 로컬 단위 테스트에서 호출하면 "not mocked" 예외를 던지므로,
// 좌표 순서와 쿼리 인자를 테스트에서 문자열로 비교하도록 Uri.Builder 대신 주소를 문자열로 조립
object KakaoMapDirections {
    private const val APP_ROUTE = "kakaomap://route"

    // URL Scheme 가이드는 모바일 웹 주소를 http로 적지만 http 요청은 302로 https에 넘기므로,
    // 리다이렉트를 거치지 않도록 처음부터 https 적용
    private const val WEB_ROUTE = "https://m.map.kakao.com/scheme/route"

    // 도심 영화관은 주차보다 대중교통으로 가는 경우가 많으므로, 이동 수단 기본값을 대중교통으로 지정
    private const val BY_PUBLIC_TRANSIT = "publictransit"

    fun appUrl(
        start: LocationLatLng,
        end: LocationLatLng,
    ): String = "$APP_ROUTE?${routeQuery(start, end)}"

    fun webUrl(
        start: LocationLatLng,
        end: LocationLatLng,
    ): String = "$WEB_ROUTE?${routeQuery(start, end)}"

    private fun routeQuery(
        start: LocationLatLng,
        end: LocationLatLng,
    ): String = "sp=${start.toParam()}&ep=${end.toParam()}&by=$BY_PUBLIC_TRANSIT"

    // 카카오맵 길찾기는 경도, 위도가 아니라 위도, 경도 순서로 받으므로 순서 고정
    private fun LocationLatLng.toParam(): String = "$latitude,$longitude"
}
