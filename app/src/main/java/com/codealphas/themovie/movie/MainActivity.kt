package com.codealphas.themovie.movie

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.codealphas.themovie.R
import com.codealphas.themovie.auth.LoginActivity
import com.codealphas.themovie.core.android.ui.applySystemBarInsets
import com.codealphas.themovie.core.android.ui.setupAppBar
import com.codealphas.themovie.databinding.ActivityMainBinding
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private companion object {
        const val TAB_COUNT = 3
    }

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val tabTitleResIds =
        intArrayOf(
            R.string.movie_list_tab_popular,
            R.string.movie_list_tab_top_rated,
            R.string.movie_list_tab_search,
        )
    private val tabIcons =
        arrayListOf(
            R.drawable.ic_baseline_movie_popular_24,
            R.drawable.ic_baseline_top_movies_24,
            R.drawable.ic_baseline_search_24,
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        applySystemBarInsets()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupAppBar(binding.toolbar, getString(R.string.movie_list_appbar_popular), showBack = false)
        initViewPager()
        initTabLayout()
        linkViewPagerAndTabLayout()
        observeLogout()
    }

    private fun observeLogout() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.logoutCompleted.collect {
                    startActivity(Intent(applicationContext, LoginActivity::class.java))
                    finish()
                }
            }
        }
    }

    private fun initViewPager() {
        binding.viewPager.adapter = FragmentViewPagerAdapter(this, TAB_COUNT) // 뷰페이저에 Adapter 장착
    }

    private fun initTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    val position = tab!!.position
                    binding.viewPager.currentItem = position

                    when (position) {
                        0 -> supportActionBar?.title = getString(R.string.movie_list_appbar_popular)
                        1 -> supportActionBar?.title = getString(R.string.movie_list_top_rated_subtitle)
                        else -> supportActionBar?.title = getString(R.string.movie_list_appbar_search)
                    } // 탭 클릭시 해당 탭에 맞게 앱바(액션바)의 텍스트 변경
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) = Unit

                override fun onTabReselected(tab: TabLayout.Tab?) = Unit
            },
        ) // 탭 레이아웃 설정
    }

    private fun linkViewPagerAndTabLayout() {
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = getString(tabTitleResIds[position])
            tab.icon = getDrawable(tabIcons[position])
        }.attach() // 탭 레이아웃과 뷰페이저 연결
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.application_menu, menu)
        return true
    } // 앱바에 로그아웃 버튼 추가

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.logout_action -> {
                viewModel.logout()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    } // 앱바의 로그아웃 버튼에 로그아웃 이벤트 추가
}
