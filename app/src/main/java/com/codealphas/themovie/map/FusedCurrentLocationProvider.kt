package com.codealphas.themovie.map

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FusedCurrentLocationProvider
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : CurrentLocationProvider {
        private val client = LocationServices.getFusedLocationProviderClient(context)

        // 주변 영화관 검색에는 수십 m 오차가 문제되지 않고 GPS는 응답이 느려서, 권한과 무관하게 균형 우선순위 적용
        @SuppressLint("MissingPermission")
        override suspend fun getCurrentLocation(): LocationLatLng? {
            val cancellation = CancellationTokenSource()
            // 요청마다 위치를 새로 측정하면 응답이 수 초 걸리므로, 1분 이내에 측정된 위치는 그대로 쓰도록 적용
            val request =
                CurrentLocationRequest
                    .Builder()
                    .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                    .setMaxUpdateAgeMillis(MAX_LOCATION_AGE_MILLIS)
                    .build()
            return try {
                // 코루틴이 취소되면 진행 중인 위치 요청도 같이 취소되도록 토큰 소스를 함께 전달
                client
                    .getCurrentLocation(request, cancellation.token)
                    .await(cancellation)
                    ?.let { LocationLatLng(it.latitude, it.longitude) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
        }

        private companion object {
            const val MAX_LOCATION_AGE_MILLIS = 60_000L
        }
    }
