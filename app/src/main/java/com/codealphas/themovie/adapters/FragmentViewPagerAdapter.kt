package com.example.movieapplication.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.movieapplication.views.FragmentPopularMovieList
import com.example.movieapplication.views.FragmentSearchMovie
import com.example.movieapplication.views.FragmentTopRatedMovieList

class FragmentViewPagerAdapter(
    fragmentActivity: FragmentActivity,
    private val tabNum: Int
) : FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int {
        return tabNum
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> FragmentPopularMovieList()
            1 -> FragmentTopRatedMovieList()
            else -> FragmentSearchMovie()
        } // position 별로 반환될 Fragment 설정
    }
}