package com.example.movieapplication.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.movieapplication.databinding.ReviewItemBinding
import com.example.movieapplication.models.Review
import com.example.movieapplication.utils.ReviewClickDeleteInterface
import com.example.movieapplication.utils.ReviewClickInterface

// 영화 감상문 표시화면에서 작성한 감상문을 보여주는 리싸이클러뷰를 위한 어댑터
class ReviewRecyclerViewAdapter(
    var inflater: LayoutInflater,
    var context: Context,
    val reviewClickInterface: ReviewClickInterface,
    val reviewClickDeleteInterface: ReviewClickDeleteInterface
) : RecyclerView.Adapter<ReviewRecyclerViewAdapter.ViewHolder>() {

    private val items = ArrayList<Review>()

    inner class ViewHolder(private val itemBinding: ReviewItemBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {

        fun bind(data: Review) {
            itemBinding.reviewTitle1.text = data.title
            itemBinding.reviewedTime1.text = data.time
            itemBinding.reviewRatingBar.rating = (data.rating / 2).toFloat()

            itemBinding.deleteImage1.setOnClickListener {
                reviewClickDeleteInterface.onDeleteIconClick(data)
            }

            itemBinding.reviewCardView.setOnClickListener {
                reviewClickInterface.onIconClick(data)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemBinding =
            ReviewItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items.get(position))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    // 리싸이클러뷰를 갱신해주는 메소드
    fun updateReviewList(newReview: List<Review>) {
        items.clear()
        items.addAll(newReview)
        notifyDataSetChanged()
    }
}
