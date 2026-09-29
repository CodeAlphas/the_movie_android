package com.codealphas.themovie.movie

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
import com.codealphas.themovie.R
import com.codealphas.themovie.core.android.ui.applySystemBarInsets
import com.codealphas.themovie.core.android.ui.setupAppBar
import com.codealphas.themovie.databinding.ActivityMovieDetailBinding
import com.codealphas.themovie.domain.movie.MovieDetail
import com.codealphas.themovie.ui.observeRemoteError
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

// YouTube Android Player API는 Maven에 없어 컴파일이 막힌다. Phase 5에서 교체한다.
// import com.codealphas.themovie.R
// import com.codealphas.themovie.BuildConfig.YOUTUBE_API_KEY
// import com.google.android.youtube.player.*

@AndroidEntryPoint
class MovieDetailActivity : AppCompatActivity() {
    // private lateinit var youtubePlayerFragment: YouTubePlayerFragment
    private lateinit var creditsRecyclerViewAdapter: CreditsRecyclerViewAdapter
    private lateinit var binding: ActivityMovieDetailBinding
    private val viewModel: MovieDetailViewModel by viewModels()
    private var youtubeVideoId: ArrayList<String> = ArrayList() // TMDB 서버로부터 받은 영화관련 동영상의 ID 정보
    private var appliedDetail: MovieDetail? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySystemBarInsets()

        binding = ActivityMovieDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupAppBar(binding.toolbar, getString(R.string.app_name))

        initCredits()
        viewModel.remoteError.observeRemoteError(this, binding.root)
        observeDetail()
    }

    // 유튜브 플레이어 뷰에 동영상을 로드해주는 메소드
    // private fun loadVideo() {
    //     youtubePlayerFragment =
    //         fragmentManager.findFragmentById(R.id.youtubePlayerViewFragment) as YouTubePlayerFragment
    //     youtubePlayerFragment.initialize(YOUTUBE_API_KEY, this)
    // }
    //
    // override fun onInitializationSuccess(
    //     p0: YouTubePlayer.Provider?,
    //     p1: YouTubePlayer?,
    //     p2: Boolean
    // ) {
    //     if (!p2) {
    //         p1?.cueVideos(youtubeVideoId)
    //     }
    // }
    //
    // override fun onInitializationFailure(
    //     p0: YouTubePlayer.Provider?,
    //     p1: YouTubeInitializationResult?
    // ) {
    //     // Log.d(TAG, "에러 발생")
    // }

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
        // 영상 id가 중복되지 않도록 같은 인스턴스는 건너뜀
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
        if (detail.videos.isEmpty()) return
        detail.videos.forEach { video ->
            youtubeVideoId.add(video.key)
        }
        // loadVideo()
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
