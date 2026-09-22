package com.example.movieapplication.views

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.movieapplication.R
import com.example.movieapplication.adapters.PopularMoviesRecyclerViewAdapter
import com.example.movieapplication.databinding.PopularMovieListFragmentBinding
import com.example.movieapplication.models.MoviesFromServer
import com.example.movieapplication.utils.ItemDecorator
import com.example.movieapplication.utils.Utils
import com.example.movieapplication.viewmodels.MovieViewModel

class FragmentPopularMovieList : Fragment() {

    private var _binding: PopularMovieListFragmentBinding? = null
    private val binding get() = _binding!!
    private var buttonClicked = false
    private lateinit var popularMoviesRecyclerViewAdapter: PopularMoviesRecyclerViewAdapter
    private lateinit var viewModel: MovieViewModel
    private val rotateOpen: Animation by lazy { AnimationUtils.loadAnimation(context, R.anim.rotate_open_anim) }
    private val rotateClose: Animation by lazy { AnimationUtils.loadAnimation(context, R.anim.rotate_close_anim) }
    private val fromBottom: Animation by lazy { AnimationUtils.loadAnimation(context, R.anim.from_bottom_anim) }
    private val toBottom: Animation by lazy { AnimationUtils.loadAnimation(context, R.anim.to_bottom_anim) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = PopularMovieListFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initRecyclerView()
        initViewModel()
        initFloatingActionButton()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initRecyclerView() {
        // 리싸이클러 뷰의 아이템들을 GridLayout 방식으로 배치
        binding.popularMovieRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        // ItemDecorator 클래스를 이용하여 리싸이클러뷰 아이템들 사이의 간격 조정
        binding.popularMovieRecyclerView.addItemDecoration(
            ItemDecorator((Utils.getScreenWidth(requireContext()) - 360) / 3, requireContext())
        )
        popularMoviesRecyclerViewAdapter = PopularMoviesRecyclerViewAdapter(requireContext())
        binding.popularMovieRecyclerView.adapter = popularMoviesRecyclerViewAdapter
        binding.popularMovieRecyclerView.addOnScrollListener(object :
            RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    binding.floatingButton.show() // 리싸이클러뷰 스크롤이 정지되어 있으면 플로팅 액션 버튼을 보이게 한다
                    if (buttonClicked) {
                        binding.writeFloatingButton.show()
                        binding.locationFloatingButton.show()
                    }
                } else {
                    binding.floatingButton.hide() // 리싸이클러뷰 스크롤이 움직이면 플로팅 액션 버튼을 숨긴다
                    if (buttonClicked) {
                        binding.writeFloatingButton.hide()
                        binding.locationFloatingButton.hide()
                    }
                }
            }
        })
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(this).get(MovieViewModel::class.java)
        viewModel.makePopMovieListApiCall()
        viewModel.allPopMovies
            .observe(viewLifecycleOwner, Observer<MoviesFromServer> {
                if (it != null) {
                    popularMoviesRecyclerViewAdapter.setUpdatedData(it.results)
                    binding.floatingButton.visibility = View.VISIBLE
                } else {
                    // Log.d(TAG, "에러 발생")
                }
            })
    }

    private fun initFloatingActionButton() {
        binding.floatingButton.setOnClickListener {
            addButtonClicked()
        }
        binding.writeFloatingButton.setOnClickListener {
            startActivity(Intent(requireContext(), ReviewMainActivity::class.java))
        }
        binding.locationFloatingButton.setOnClickListener {
            startActivity(Intent(requireContext(), MapActivity::class.java))
        }
    }

    private fun addButtonClicked() {
        setVisibility()
        setAnimation()
        buttonClicked = !buttonClicked
    }

    private fun setVisibility() {
        if (!buttonClicked) {
            binding.writeFloatingButton.isVisible = true
            binding.locationFloatingButton.isVisible = true
        } else {
            binding.writeFloatingButton.isVisible = false
            binding.locationFloatingButton.isVisible = false
        }
    }

    private fun setAnimation() {
        if (!buttonClicked) {
            binding.writeFloatingButton.startAnimation(fromBottom)
            binding.locationFloatingButton.startAnimation(fromBottom)
            binding.floatingButton.startAnimation(rotateOpen)
        } else {
            binding.writeFloatingButton.startAnimation(toBottom)
            binding.locationFloatingButton.startAnimation(toBottom)
            binding.floatingButton.startAnimation(rotateClose)
        }
    }
}