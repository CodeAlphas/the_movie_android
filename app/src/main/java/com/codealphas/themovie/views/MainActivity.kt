package com.codealphas.themovie.views

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity

import com.codealphas.themovie.R
import com.codealphas.themovie.adapters.FragmentViewPagerAdapter
import com.codealphas.themovie.database.DatabaseInstance
import com.codealphas.themovie.databinding.ActivityMainBinding
import com.codealphas.themovie.repository.ReviewRepository
import com.codealphas.themovie.utils.applySystemBarInsets
import com.codealphas.themovie.utils.setupAppBar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val tabTitles = arrayListOf("인기", "높은 평점", "검색")
    private val tabIcons = arrayListOf(
        R.drawable.ic_baseline_movie_popular_24,
        R.drawable.ic_baseline_top_movies_24,
        R.drawable.ic_baseline_search_24
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
        setupAppBar(binding.toolbar, "오늘의 인기 영화", showBack = false)
        initViewPager()
        initTabLayout()
        linkViewPagerAndTabLayout()
    }

    private fun initViewPager() {
        binding.viewPager.adapter = FragmentViewPagerAdapter(this, 3) // 뷰페이저에 Adapter 장착
    }

    private fun initTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val position = tab!!.position
                binding.viewPager.currentItem = position

                when (position) {
                    0 -> supportActionBar?.title = "오늘의 인기 영화"
                    1 -> supportActionBar?.title = "높은 평점 영화"
                    else -> supportActionBar?.title = "영화 검색"
                } // 탭 클릭시 해당 탭에 맞게 앱바(액션바)의 텍스트 변경
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        }) // 탭 레이아웃 설정
    }

    private fun linkViewPagerAndTabLayout() {
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = tabTitles[position]
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
                auth.signOut() // 로그아웃(Firebase Authentication)

                val dao = DatabaseInstance.getInstance(application).reviewDao()
                val repository = ReviewRepository(dao)
                repository.deleteAll() // 로그아웃시 reviewTable의 데이터 삭제

                startActivity(Intent(applicationContext, LoginActivity::class.java))
                this.finish()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    } // 앱바의 로그아웃 버튼에 로그아웃 이벤트 추가
}
