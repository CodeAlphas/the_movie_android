package com.codealphas.themovie.movie

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codealphas.themovie.R
import com.codealphas.themovie.databinding.SearchMovieFragmentBinding
import com.codealphas.themovie.domain.movie.Movie
import com.codealphas.themovie.map.MapActivity
import com.codealphas.themovie.review.ReviewMainActivity
import com.codealphas.themovie.ui.observeRemoteError
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FragmentSearchMovie : Fragment() {
    private companion object {
        const val GRID_SPAN_COUNT = 2
        const val POSTER_WIDTH_DP = 180

        // 왼쪽 여백, 포스터 사이, 오른쪽 여백의 너비가 같으므로, 포스터를 뺀 화면 너비를 3으로 나눔
        const val ROW_GAP_UNIT_COUNT = 3
    }

    private var _binding: SearchMovieFragmentBinding? = null
    val binding get() = _binding!!
    private var buttonClicked = false
    private lateinit var movieAdapter: MovieAdapter
    private val viewModel: SearchMovieViewModel by viewModels()
    private val rotateOpen: Animation by lazy { AnimationUtils.loadAnimation(context, R.anim.rotate_open_anim) }
    private val rotateClose: Animation by lazy { AnimationUtils.loadAnimation(context, R.anim.rotate_close_anim) }
    private val fromBottom: Animation by lazy { AnimationUtils.loadAnimation(context, R.anim.from_bottom_anim) }
    private val toBottom: Animation by lazy { AnimationUtils.loadAnimation(context, R.anim.to_bottom_anim) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = SearchMovieFragmentBinding.inflate(inflater, container, false)
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
        initSearchViews()
        clearFocusWhenImeHides()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initRecyclerView() {
        // 리싸이클러 뷰의 아이템들을 GridLayout 방식으로 배치
        binding.searchMovieRecyclerView.layoutManager = GridLayoutManager(requireContext(), GRID_SPAN_COUNT)
        // ItemDecorator 클래스를 이용하여 리싸이클러뷰 아이템들 사이의 간격 조정
        binding.searchMovieRecyclerView.addItemDecoration(
            ItemDecorator(
                (getScreenWidth(requireContext()) - GRID_SPAN_COUNT * POSTER_WIDTH_DP) /
                    ROW_GAP_UNIT_COUNT,
                requireContext(),
            ),
        )
        movieAdapter = MovieAdapter { movie -> openMovieDetail(movie) }
        binding.searchMovieRecyclerView.adapter = movieAdapter
        binding.searchMovieRecyclerView.addOnScrollListener(
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

    private fun initViewModel() {
        viewModel.remoteError.observeRemoteError(viewLifecycleOwner, binding.root)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val movies = state.movies ?: return@collect
                    movieAdapter.submitList(movies)
                }
            }
        }
    }

    private fun initSearchViews() {
        binding.searchView.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    // onQueryTextSubmit이 true를 반환하면 SearchView는 제출이 처리된 것으로 보고 키보드를 숨기지 않으므로,
                    // 검색 뒤에 키보드가 화면에 남아 있지 않도록 포커스를 해제
                    binding.searchView.clearFocus()
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    // 검색창을 비울 때 빈 문자열을 queryText에 넣지 않으면 이전 검색어가 남고 StateFlow는 observeQuery에 같은 값을 다시 넘기지 않으므로,
                    // 비운 뒤 같은 검색어를 다시 입력해도 요청이 나가도록 빈 문자열도 반영
                    viewModel.onQueryChange(newText.orEmpty())
                    return true
                }
            },
        )
    }

    private fun clearFocusWhenImeHides() {
        var imeVisible = false
        ViewCompat.setOnApplyWindowInsetsListener(binding.searchView) { _, insets ->
            val visible = insets.isVisible(WindowInsetsCompat.Type.ime())
            // 뒤로 가기는 키보드만 숨기고 포커스는 남기므로, 상세에서 돌아와도 키보드가 다시 뜨지 않도록 포커스를 해제
            if (imeVisible && !visible) binding.searchView.clearFocus()
            imeVisible = visible
            insets
        }
    }

    private fun openMovieDetail(movie: Movie) {
        startActivity(MovieDetailActivity.createIntent(requireContext(), movie.id))
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
