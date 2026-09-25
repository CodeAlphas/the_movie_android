package com.codealphas.themovie.networks

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetroInstance {
    companion object {
        private const val BASE_URL: String = "https://api.themoviedb.org/3/"
        private var instance: Retrofit? = null

        fun getRetrofitInstance(): Retrofit =
            instance ?: synchronized(this) {
                val instance =
                    Retrofit
                        .Builder()
                        .baseUrl(BASE_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build() // TMDB 서버와의 통신을 위한 Retrofit 객체
                this.instance = instance
                instance
            }
    }
}
