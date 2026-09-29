package com.codealphas.themovie.review

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.codealphas.themovie.R
import com.codealphas.themovie.auth.LoginActivity
import com.codealphas.themovie.core.android.ui.applySystemBarInsets
import com.codealphas.themovie.core.android.ui.setupAppBar
import com.codealphas.themovie.databinding.ActivityReviewMainBinding
import com.codealphas.themovie.domain.review.Review
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ReviewMainActivity :
    AppCompatActivity(),
    ReviewClickInterface,
    ReviewClickDeleteInterface {
    private lateinit var binding: ActivityReviewMainBinding
    private val reviewViewModel: ReviewListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySystemBarInsets()

        binding = ActivityReviewMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupAppBar(binding.toolbar, getString(R.string.review_list_appbar_title))

        initRecyclerView()
        initReviewAddFloatingButton()
        observeEvents()
    }

    private fun observeEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    reviewViewModel.syncFailed.collect {
                        Toast
                            .makeText(
                                this@ReviewMainActivity,
                                R.string.review_list_sync_failed,
                                Toast.LENGTH_LONG,
                            ).show()
                    }
                }
                launch {
                    reviewViewModel.logoutCompleted.collect {
                        startActivity(Intent(applicationContext, LoginActivity::class.java))
                        finish()
                    }
                }
            }
        }
    }

    private fun initRecyclerView() {
        binding.reviewRecyclerView.layoutManager = LinearLayoutManager(this)
        val reviewRecyclerViewAdapter =
            ReviewRecyclerViewAdapter(LayoutInflater.from(this), this, this, this)
        binding.reviewRecyclerView.adapter = reviewRecyclerViewAdapter
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                reviewViewModel.uiState.collect { state ->
                    // 목록을 아직 읽지 못한 상태를 빈 목록으로 보면 빈 목록 안내가 먼저 깜빡이므로, 읽은 뒤에만 반영
                    val reviews = state.reviews ?: return@collect
                    reviewRecyclerViewAdapter.updateReviewList(reviews)
                    binding.textViewCenter.isVisible = reviews.isEmpty()
                }
            }
        }
    }

    private fun initReviewAddFloatingButton() {
        binding.reviewAddFloatingButton.setOnClickListener {
            startActivity(Intent(this@ReviewMainActivity, ReviewDetailActivity::class.java))
        }
    }

    override fun onDeleteIconClick(review: Review) {
        reviewViewModel.deleteReview(review)
        Toast.makeText(this, getString(R.string.review_list_deleted), Toast.LENGTH_LONG).show()
    } // 작성한 감상문 아이템에서 X 이미지를 누르면 발생하는 이벤트 처리를 위한 메소드

    override fun onIconClick(review: Review) {
        val intent = Intent(this@ReviewMainActivity, ReviewDetailActivity::class.java)
        intent.putExtra(ReviewEditViewModel.ARG_REVIEW_ID, review.id)
        startActivity(intent)
    } // 작성한 감상문 아이템(X 이미지를 제외한 부분)을 누르면 발생하는 이벤트 처리를 위한 메소드

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.application_menu, menu)
        return true
    } // 앱바에 로그아웃 버튼 추가

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.logout_action -> {
                reviewViewModel.logout()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    } // 앱바의 로그아웃 버튼에 로그아웃 이벤트 추가
}
