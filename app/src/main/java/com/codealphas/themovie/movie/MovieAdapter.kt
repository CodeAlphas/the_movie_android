package com.codealphas.themovie.movie

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.codealphas.themovie.databinding.MovieItemBinding
import com.codealphas.themovie.domain.movie.Movie

class MovieAdapter(
    private val onMovieClick: (Movie) -> Unit,
) : ListAdapter<Movie, MovieAdapter.ViewHolder>(MovieDiff) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val itemBinding = MovieItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(itemBinding, onMovieClick)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val itemBinding: MovieItemBinding,
        private val onMovieClick: (Movie) -> Unit,
    ) : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(movie: Movie) {
            Glide
                .with(itemBinding.imageViewMoviePoster)
                .load(movie.posterUrl)
                .centerCrop()
                .into(itemBinding.imageViewMoviePoster)
            itemBinding.textViewMovieTitle.text = movie.title
            itemBinding.cardView.setOnClickListener { onMovieClick(movie) }
        }
    }
}

private object MovieDiff : DiffUtil.ItemCallback<Movie>() {
    override fun areItemsTheSame(
        oldItem: Movie,
        newItem: Movie,
    ): Boolean = oldItem.id == newItem.id

    override fun areContentsTheSame(
        oldItem: Movie,
        newItem: Movie,
    ): Boolean = oldItem == newItem
}
