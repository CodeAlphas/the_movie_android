package com.codealphas.themovie.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.codealphas.themovie.views.FragmentPopularMovieList
import com.codealphas.themovie.views.FragmentSearchMovie
import com.codealphas.themovie.views.FragmentTopRatedMovieList

class FragmentViewPagerAdapter(
    fragmentActivity: FragmentActivity,
    private val tabNum: Int,
) : FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int = tabNum

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> FragmentPopularMovieList()
            1 -> FragmentTopRatedMovieList()
            else -> FragmentSearchMovie()
        } // position 별로 반환될 Fragment 설정
    }
}
