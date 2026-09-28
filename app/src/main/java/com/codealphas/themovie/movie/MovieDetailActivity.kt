package com.codealphas.themovie.movie

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.codealphas.themovie.R
import com.codealphas.themovie.core.android.ui.applySystemBarInsets
import com.codealphas.themovie.core.android.ui.setupAppBar
import com.codealphas.themovie.databinding.ActivityMovieDetailBinding
import com.codealphas.themovie.domain.movie.Cast
import com.codealphas.themovie.domain.movie.Movie
import com.codealphas.themovie.domain.movie.Video
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
    private var appliedCast: List<Cast>? = null
    private var appliedVideos: List<Video>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySystemBarInsets()

        binding = ActivityMovieDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupAppBar(binding.toolbar, getString(R.string.app_name))

        initViews()
        initCredits()
        viewModel.remoteError.observeRemoteError(this, binding.root)
        val movieId = intent.getIntExtra(EXTRA_MOVIE_ID, 0)
        viewModel.loadCast(movieId)
        viewModel.loadVideos(movieId)
        observeDetail()
    }

    private fun initViews() {
        // Intent의 poster는 매퍼가 붙인 w500 주소이므로, 한 번 더 붙이지 않고 그대로 로드
        val poster = intent.getStringExtra(EXTRA_POSTER)
        val title = intent.getStringExtra(EXTRA_TITLE)
        val releaseDate = intent.getStringExtra(EXTRA_RELEASE_DATE)
        val overview = intent.getStringExtra(EXTRA_OVERVIEW)
        val voteAverage = intent.getDoubleExtra(EXTRA_VOTE_AVERAGE, 0.0)

        Glide
            .with(this)
            .load(poster)
            .into(binding.imagePoster)
        binding.textTitle.text = title
        binding.textRelease.text = getString(R.string.movie_detail_release, releaseDate.toString())
        binding.textOverview.text = overview
        binding.textGrade.text = getString(R.string.movie_detail_rating, voteAverage.toString())
        binding.ratingBar.rating = voteAverage.toFloat() / 2
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
                    // 출연진과 영상이 한 StateFlow라 한쪽만 바뀌어도 상태가 한 번 더 내려오므로,
                    // 목록 객체가 바뀔 때만 출연진을 갱신하고 영상 id를 추가
                    applyCast(state.cast)
                    applyVideos(state.videos)
                }
            }
        }
    }

    private fun applyCast(cast: List<Cast>?) {
        if (cast == null || cast === appliedCast) return
        appliedCast = cast
        creditsRecyclerViewAdapter.setUpdatedData(cast)
    }

    private fun applyVideos(videos: List<Video>?) {
        if (videos == null || videos === appliedVideos) return
        appliedVideos = videos
        if (videos.isEmpty()) return
        videos.forEach { video ->
            youtubeVideoId.add(video.key)
        }
        // loadVideo()
    }

    companion object {
        private const val EXTRA_POSTER = "poster"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_RELEASE_DATE = "releaseDate"
        private const val EXTRA_OVERVIEW = "overview"
        private const val EXTRA_VOTE_AVERAGE = "voteAverage"
        private const val EXTRA_MOVIE_ID = "movieId"

        fun createIntent(
            context: Context,
            movie: Movie,
        ): Intent =
            Intent(context, MovieDetailActivity::class.java).apply {
                putExtra(EXTRA_POSTER, movie.posterUrl)
                putExtra(EXTRA_TITLE, movie.title)
                putExtra(EXTRA_RELEASE_DATE, movie.releaseDate)
                putExtra(EXTRA_OVERVIEW, movie.overview)
                putExtra(EXTRA_VOTE_AVERAGE, movie.voteAverage)
                putExtra(EXTRA_MOVIE_ID, movie.id)
            }
    }
}
