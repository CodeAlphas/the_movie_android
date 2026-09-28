package com.codealphas.themovie.movie

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codealphas.themovie.R
import com.codealphas.themovie.databinding.TopRatedMovieListFragmentBinding
import com.codealphas.themovie.domain.movie.Movie
import com.codealphas.themovie.map.MapActivity
import com.codealphas.themovie.review.ReviewMainActivity
import com.codealphas.themovie.ui.observeRemoteError
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FragmentTopRatedMovieList : Fragment() {
    companion object {
        private const val GRID_SPAN_COUNT = 2
        private const val POSTER_WIDTH_DP = 180

        // 왼쪽 여백, 포스터 사이, 오른쪽 여백의 너비가 같으므로, 포스터를 뺀 화면 너비를 3으로 나눔
        private const val ROW_GAP_UNIT_COUNT = 3

        // ViewModel을 처음 만들 때 arguments가 SavedStateHandle에 복사되므로, 분류를 읽도록 생성 전에 arguments에 넣음
        fun newInstance(): FragmentTopRatedMovieList =
            FragmentTopRatedMovieList().apply {
                arguments = bundleOf(MovieListViewModel.ARG_CATEGORY to MovieCategory.TOP_RATED)
            }
    }

    private var _binding: TopRatedMovieListFragmentBinding? = null
    val binding get() = _binding!!
    private var buttonClicked = false
    private lateinit var movieAdapter: MovieAdapter
    private val viewModel: MovieListViewModel by viewModels()
    private val rotateOpen: Animation by lazy { AnimationUtils.loadAnimation(context, R.anim.rotate_open_anim) }
    private val rotateClose: Animation by lazy { AnimationUtils.loadAnimation(context, R.anim.rotate_close_anim) }
    private val fromBottom: Animation by lazy { AnimationUtils.loadAnimation(context, R.anim.from_bottom_anim) }
    private val toBottom: Animation by lazy { AnimationUtils.loadAnimation(context, R.anim.to_bottom_anim) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = TopRatedMovieListFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        initRecyclerView()
        initViewModel()
        initFloatingActionButton()
    }

    override fun onResume() {
        super.onResume()
        // ViewPager2는 화면에 없는 탭도 STARTED로 유지해 탭을 옮길 때 onResume만 다시 호출하므로, 실패로 비어 있는 목록을 탭이 다시 보일 때 재요청
        if (viewModel.uiState.value.movies == null) viewModel.loadMovies()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initRecyclerView() {
        // 리싸이클러 뷰의 아이템들을 GridLayout 방식으로 배치
        binding.topRatedMovieRecyclerView.layoutManager = GridLayoutManager(requireContext(), GRID_SPAN_COUNT)
        // ItemDecorator 클래스를 이용하여 리싸이클러뷰 아이템들 사이의 간격 조정
        binding.topRatedMovieRecyclerView.addItemDecoration(
            ItemDecorator(
                (getScreenWidth(requireContext()) - GRID_SPAN_COUNT * POSTER_WIDTH_DP) /
                    ROW_GAP_UNIT_COUNT,
                requireContext(),
            ),
        )
        movieAdapter = MovieAdapter { movie -> openMovieDetail(movie) }
        binding.topRatedMovieRecyclerView.adapter = movieAdapter
        binding.topRatedMovieRecyclerView.addOnScrollListener(
            object :
                RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(
                    recyclerView: RecyclerView,
                    newState: Int,
                ) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        binding.floatingButton.show() // 리싸이클러뷰 스크롤이 정지되어 있으면 플로팅 액션 버튼을 보이게 한다
                        if (buttonClicked) {
                            binding.writeFloatingButton.show()
                            binding.locationFloatingButton.show()
                        }
                    } else {
                        binding.floatingButton.hide() // 리싸이클러뷰 스크롤이 움직이면 플로팅 액션 버튼을 숨긴다
                        if (buttonClicked) {
                            binding.writeFloatingButton.hide()
                            binding.locationFloatingButton.hide()
                        }
                    }
                }
            },
        )
    }

    private fun initViewModel() {
        viewModel.remoteError.observeRemoteError(viewLifecycleOwner, binding.root)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val movies = state.movies ?: return@collect
                    movieAdapter.submitList(movies)
                    binding.floatingButton.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun openMovieDetail(movie: Movie) {
        startActivity(MovieDetailActivity.createIntent(requireContext(), movie))
    }

    private fun initFloatingActionButton() {
        binding.floatingButton.setOnClickListener {
            addButtonClicked()
        }
        binding.writeFloatingButton.setOnClickListener {
            startActivity(Intent(requireContext(), ReviewMainActivity::class.java))
        }
        binding.locationFloatingButton.setOnClickListener {
            startActivity(Intent(requireContext(), MapActivity::class.java))
        }
    }

    private fun addButtonClicked() {
        setVisibility()
        setAnimation()
        buttonClicked = !buttonClicked
    }

    private fun setVisibility() {
        if (!buttonClicked) {
            binding.writeFloatingButton.isVisible = true
            binding.locationFloatingButton.isVisible = true
        } else {
            binding.writeFloatingButton.isVisible = false
            binding.locationFloatingButton.isVisible = false
        }
    }

    private fun setAnimation() {
        if (!buttonClicked) {
            binding.writeFloatingButton.startAnimation(fromBottom)
            binding.locationFloatingButton.startAnimation(fromBottom)
            binding.floatingButton.startAnimation(rotateOpen)
        } else {
            binding.writeFloatingButton.startAnimation(toBottom)
            binding.locationFloatingButton.startAnimation(toBottom)
            binding.floatingButton.startAnimation(rotateClose)
        }
    }
}
