package com.codealphas.themovie.domain.review

import com.codealphas.themovie.domain.result.Outcome

typealias ReviewResult = Outcome<Unit, ReviewError>

sealed interface ReviewError {
    data object Unknown : ReviewError

    data object PhotoUploadFailed : ReviewError
}
