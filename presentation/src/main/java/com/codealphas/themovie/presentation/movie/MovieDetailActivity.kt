package com.codealphas.themovie.presentation.movie

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.codealphas.themovie.core.android.ui.applySystemBarInsets
import com.codealphas.themovie.core.android.ui.setupAppBar
import com.codealphas.themovie.domain.movie.MovieDetail
import com.codealphas.themovie.domain.movie.Video
import com.codealphas.themovie.presentation.R
import com.codealphas.themovie.presentation.databinding.ActivityMovieDetailBinding
import com.codealphas.themovie.presentation.ui.observeRemoteError
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MovieDetailActivity : AppCompatActivity() {
    private lateinit var creditsRecyclerViewAdapter: CreditsRecyclerViewAdapter
    private lateinit var binding: ActivityMovieDetailBinding
    private val viewModel: MovieDetailViewModel by viewModels()
    private var appliedDetail: MovieDetail? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySystemBarInsets()

        binding = ActivityMovieDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupAppBar(binding.toolbar, getString(R.string.app_name))

        initCredits()
        // 화면을 나가도 영상 소리가 계속 나지 않도록, 화면 생명주기에 맞춰 멈추고 해제하는 observer 등록
        lifecycle.addObserver(binding.youTubePlayerView)
        viewModel.remoteError.observeRemoteError(this, binding.root)
        observeDetail()
    }

    private fun initCredits() {
        binding.creditsRecyclerView.layoutManager =
            LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false,
            )
        creditsRecyclerViewAdapter = CreditsRecyclerViewAdapter(this)
        binding.creditsRecyclerView.adapter = creditsRecyclerViewAdapter
    }

    private fun observeDetail() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state.isLoading
                    renderDetail(state.detail)
                }
            }
        }
    }

    private fun renderDetail(detail: MovieDetail?) {
        // StateFlow는 화면이 다시 STARTED가 되면 같은 detail을 한 번 더 주므로,
        // 이미 초기화한 플레이어를 다시 초기화하지 않도록 같은 인스턴스는 건너뜀
        if (detail == null || detail === appliedDetail) return
        appliedDetail = detail
        Glide
            .with(this)
            .load(detail.posterUrl)
            .into(binding.imagePoster)
        binding.textTitle.text = detail.title
        binding.textRelease.text = getString(R.string.movie_detail_release, detail.releaseDate)
        binding.textOverview.text = detail.overview
        binding.textGrade.text = getString(R.string.movie_detail_rating, detail.voteAverage.toString())
        binding.ratingBar.rating = detail.voteAverage.toFloat() / 2
        creditsRecyclerViewAdapter.setUpdatedData(detail.cast)
        renderTrailer(detail.videos.firstOrNull())
    }

    private fun renderTrailer(video: Video?) {
        binding.textViewVideo.isVisible = video != null
        binding.youTubePlayerView.isVisible = video != null
        if (video == null) return
        binding.youTubePlayerView.initialize(
            object : AbstractYouTubePlayerListener() {
                // 상세에 들어오자마자 소리가 나지 않도록, 자동 재생 없이 썸네일과 재생 버튼만 보이게 영상 로드
                override fun onReady(youTubePlayer: YouTubePlayer) {
                    youTubePlayer.cueVideo(video.key, 0f)
                }
            },
        )
    }

    companion object {
        fun createIntent(
            context: Context,
            movieId: Int,
        ): Intent =
            Intent(context, MovieDetailActivity::class.java).apply {
                putExtra(MovieDetailViewModel.ARG_MOVIE_ID, movieId)
            }
    }
}
