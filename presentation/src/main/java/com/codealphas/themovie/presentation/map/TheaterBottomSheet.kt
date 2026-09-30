package com.codealphas.themovie.presentation.map

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.codealphas.themovie.domain.map.Theater
import com.codealphas.themovie.presentation.R
import com.codealphas.themovie.presentation.databinding.MapTheaterBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

// BottomSheetDialog를 직접 띄우면 화면 회전으로 Activity가 다시 만들어질 때 시트가 사라지므로,
// 회전 뒤에도 FragmentManager가 시트를 다시 띄우도록 BottomSheetDialogFragment로 구현
class TheaterBottomSheet : BottomSheetDialogFragment() {
    private var binding: MapTheaterBottomSheetBinding? = null

    // 시트가 MapActivity와 같은 영화관 목록과 현재 위치를 보여 줘야 하므로, MapActivity의 MapViewModel을 공유하도록 적용
    private val viewModel: MapViewModel by activityViewModels()

    // Theater는 Android 의존성이 없는 :domain 모델이라 Parcelable을 구현할 수 없으므로,
    // 회전 뒤에도 MapViewModel의 최신 목록에서 영화관을 찾도록 id만 arguments에서 조회
    private val theaterId: String by lazy { requireArguments().getString(ARG_THEATER_ID).orEmpty() }

    override fun getTheme(): Int = R.style.ThemeOverlay_TheMovie_BottomSheetDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = MapTheaterBottomSheetBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::bind)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    // 새 위치를 받아 목록이 바뀌거나 프로세스 종료로 상태가 비면 가리킬 영화관이 없으므로 시트를 닫도록 적용
    private fun bind(state: MapUiState) {
        val binding = binding ?: return
        val theater = state.theaters.firstOrNull { it.id == theaterId }
        val start = state.currentLocation
        if (theater == null || start == null) {
            dismissAllowingStateLoss()
            return
        }
        binding.theaterNameTextView.text = theater.name
        binding.theaterAddressTextView.text = theater.address
        binding.theaterDistanceTextView.isVisible = theater.distanceMeters != null
        binding.theaterDistanceTextView.text = theater.distanceMeters?.let(::formatDistance)
        binding.directionsButton.setOnClickListener { openDirections(start, theater) }
    }

    private fun formatDistance(meters: Int): String =
        if (meters < METERS_PER_KILOMETER) {
            getString(R.string.map_theater_distance_meters, meters)
        } else {
            getString(R.string.map_theater_distance_kilometers, meters / METERS_PER_KILOMETER.toDouble())
        }

    // Android 11부터 resolveActivity는 <queries> 선언 없이는 설치된 카카오맵도 찾지 못하므로,
    // 설치 여부를 미리 확인하지 않고 앱 실행이 실패하면 웹 길찾기로 대체
    private fun openDirections(
        start: LocationLatLng,
        theater: Theater,
    ) {
        val end = LocationLatLng(theater.latitude, theater.longitude)
        try {
            startActivity(Intent(Intent.ACTION_VIEW, KakaoMapDirections.appUrl(start, end).toUri()))
        } catch (_: ActivityNotFoundException) {
            openWebDirections(start, end)
        }
    }

    private fun openWebDirections(
        start: LocationLatLng,
        end: LocationLatLng,
    ) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, KakaoMapDirections.webUrl(start, end).toUri()))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(requireContext(), R.string.map_directions_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val TAG = "TheaterBottomSheet"
        private const val ARG_THEATER_ID = "theaterId"
        private const val METERS_PER_KILOMETER = 1000

        // FragmentManager는 회전 뒤 Fragment를 인자 없는 생성자로 다시 만들어 생성자 인자가 사라지므로,
        // 회전 뒤에도 영화관 id가 남도록 arguments에 저장
        fun newInstance(theaterId: String): TheaterBottomSheet =
            TheaterBottomSheet().apply { arguments = bundleOf(ARG_THEATER_ID to theaterId) }
    }
}
