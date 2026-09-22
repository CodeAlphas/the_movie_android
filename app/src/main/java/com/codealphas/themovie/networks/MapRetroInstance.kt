package com.example.movieapplication.networks

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MapRetroInstance {

    companion object {
        private const val BaseURL: String = "https://apis.openapi.sk.com/"

        fun getMapRetrofitInstance(): Retrofit {
            return Retrofit.Builder()
                .baseUrl(BaseURL)
                .addConverterFactory(GsonConverterFactory.create())
                .build() // TMAP 서버와의 통신을 위한 Retrofit 객체
        }
    }
}