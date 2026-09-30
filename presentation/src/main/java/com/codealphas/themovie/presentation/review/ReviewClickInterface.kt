package com.codealphas.themovie.presentation.review

import com.codealphas.themovie.domain.review.Review

interface ReviewClickInterface {
    fun onIconClick(review: Review)
}

interface ReviewClickDeleteInterface {
    fun onDeleteIconClick(review: Review)
}
