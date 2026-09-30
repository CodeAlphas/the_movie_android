package com.codealphas.themovie.presentation.movie

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class FragmentViewPagerAdapter(
    fragmentActivity: FragmentActivity,
    private val tabNum: Int,
) : FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int = tabNum

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> FragmentPopularMovieList.newInstance()
            1 -> FragmentTopRatedMovieList.newInstance()
            else -> FragmentSearchMovie()
        } // position 별로 반환될 Fragment 설정
    }
}
