package com.codealphas.themovie.presentation.movie

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import com.codealphas.themovie.domain.movie.Cast
import com.codealphas.themovie.presentation.databinding.ActorItemBinding

// 영화 상세화면에서 등장인물 정보를 보여주는 리싸이클러뷰를 위한 어댑터
class CreditsRecyclerViewAdapter : RecyclerView.Adapter<CreditsRecyclerViewAdapter.ViewHolder>() {
    private var items: List<Cast> = emptyList()

    inner class ViewHolder(
        private val itemBinding: ActorItemBinding,
    ) : RecyclerView.ViewHolder(itemBinding.root) {
        // 뷰와 데이터를 연결해주는 메소드
        fun bind(data: Cast) {
            itemBinding.actorImageView.load(data.profileUrl)
            itemBinding.actorCharacterNameTextView.text = data.character
            itemBinding.actorRealNameTextView.text = data.name
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val itemBinding =
            ActorItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(itemBinding)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        holder.bind(items.get(position))
    }

    override fun getItemCount(): Int = items.size

    // 리싸이클러뷰를 갱신해주는 메소드
    fun setUpdatedData(items: List<Cast>) {
        this.items = items
        notifyDataSetChanged()
    }
}
