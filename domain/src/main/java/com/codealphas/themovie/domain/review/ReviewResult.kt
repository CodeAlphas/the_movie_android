package com.codealphas.themovie.domain.review

import com.codealphas.themovie.domain.result.Outcome

typealias ReviewResult = Outcome<Unit, ReviewError>

enum class ReviewError {
    Unknown,
    PhotoUploadFailed,
}
