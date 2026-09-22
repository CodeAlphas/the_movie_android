package com.example.movieapplication.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.movieapplication.views.MovieDetailActivity
import com.example.movieapplication.databinding.MovieItemBinding
import com.example.movieapplication.models.MovieItem

// 영화 검색화면에서 자신이 검색한 영화를 보여주는 리싸이클러뷰를 위한 어댑터
class SearchMoviesRecyclerViewAdapter(
    var context: Context
) : RecyclerView.Adapter<SearchMoviesRecyclerViewAdapter.ViewHolder>() {

    private var items = ArrayList<MovieItem>()

    inner class ViewHolder(private val itemBinding: MovieItemBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {
        // 뷰와 데이터를 연결해주는 메소드
        fun bind(data: MovieItem) {
            val imageUrl = "https://image.tmdb.org/t/p/w500" + data.poster_path

            // 뷰에 Glide 라이브러리를 이용하여 이미지 로드
            Glide
                .with(context)
                .load(imageUrl)
                .centerCrop()
                .into(itemBinding.imageViewMoviePoster)
            itemBinding.textViewMovieTitle.text = data.title

            // 리싸이클러 뷰 각각의 아이템에 클릭 리스너 장착
            itemBinding.cardView.setOnClickListener {
                val position: Int = adapterPosition
                val movie = items.get(position)
                // 인텐트를 통하여 영화 상세화면에 영화 정보 전달
                val intent = Intent(context, MovieDetailActivity::class.java)
                intent.putExtra("poster", movie.poster_path)
                intent.putExtra("title", movie.title)
                intent.putExtra("releaseDate", movie.release_date)
                intent.putExtra("overview", movie.overview)
                intent.putExtra("voteAverage", movie.vote_average)
                intent.putExtra("movieId", movie.id)
                context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemBinding =
            MovieItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items.get(position))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    // 리싸이클러뷰를 갱신해주는 메소드
    fun setUpdatedData(items: ArrayList<MovieItem>) {
        this.items = items
        notifyDataSetChanged()
    }
}