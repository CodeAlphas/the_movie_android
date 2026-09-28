package com.codealphas.themovie.movie

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.codealphas.themovie.databinding.ActivityMovieDetailBinding
import com.codealphas.themovie.domain.movie.Cast
import com.codealphas.themovie.domain.movie.Video
import com.codealphas.themovie.ui.observeRemoteError
import com.codealphas.themovie.utils.applySystemBarInsets
import com.codealphas.themovie.utils.setupAppBar
import dagger.hilt.android.AndroidEntryPoint

// YouTube Android Player API는 Maven에 없어 컴파일이 막힌다. Phase 5에서 교체한다.
// import com.codealphas.themovie.R
// import com.codealphas.themovie.BuildConfig.YOUTUBE_API_KEY
// import com.google.android.youtube.player.*

@AndroidEntryPoint
class MovieDetailActivity : AppCompatActivity() {
    // private lateinit var youtubePlayerFragment: YouTubePlayerFragment
    private lateinit var creditsRecyclerViewAdapter: CreditsRecyclerViewAdapter
    private lateinit var binding: ActivityMovieDetailBinding
    private val viewModel: MovieViewModel by viewModels()
    private var youtubeVideoId: ArrayList<String> = ArrayList() // TMDB 서버로부터 받은 영화관련 동영상의 ID 정보

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySystemBarInsets()

        binding = ActivityMovieDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupAppBar(binding.toolbar, "더 무비")

        initViews()
        viewModel.remoteError.observeRemoteError(this, binding.root)
        val movieId = intent.getIntExtra("movieId", 0) // 영화 id
        getCredits(movieId)
        checkVideo(movieId)
    }

    private fun initViews() {
        // Intent의 poster는 매퍼가 붙인 w500 주소이므로, 한 번 더 붙이지 않고 그대로 로드
        val poster = intent.getStringExtra("poster")
        val title = intent.getStringExtra("title") // 영화 제목
        val releaseDate = intent.getStringExtra("releaseDate") // 영화 개봉일자
        val overview = intent.getStringExtra("overview") // 영화 개요
        val voteAverage = intent.getDoubleExtra("voteAverage", 0.0) // 영화 평점

        Glide
            .with(this)
            .load(poster)
            .into(binding.imagePoster)
        binding.textTitle.text = title
        binding.textRelease.text = "$releaseDate 개봉"
        binding.textOverview.text = overview
        binding.textGrade.text = "평점 : $voteAverage"
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

    // 영화 관계자 정보를 TMDB 서버로부터 가져와 리싸이클러뷰에 넣어주는 메소드
    private fun getCredits(movieId: Int) {
        binding.creditsRecyclerView.layoutManager =
            LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false,
            ) // 리싸이클러 뷰의 아이템들을 가로 방향으로 배열
        creditsRecyclerViewAdapter = CreditsRecyclerViewAdapter(this)
        binding.creditsRecyclerView.adapter = creditsRecyclerViewAdapter

        viewModel.makeCreditApiCall(movieId)
        viewModel.allCredits
            .observe(
                this,
                Observer<List<Cast>> { cast ->
                    creditsRecyclerViewAdapter.setUpdatedData(cast)
                },
            )
    }

    // 영화와 관련된 동영상 정보가 있는지 TMDB 서버에 확인하고 있으면 해당 정보로 유튜브 플레이어를 로드하는 메소드
    private fun checkVideo(movieId: Int) {
        viewModel.makeVideoApiCall(movieId)
        viewModel.allVideos
            .observe(
                this,
                Observer<List<Video>> { videos ->
                    if (videos.size > 0) {
                        videos.forEach { video ->
                            youtubeVideoId.add(video.key)
                        }
                        // loadVideo()
                    }
                },
            )
    }
}
