package com.codealphas.themovie.networks

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object MapRetroInstance {
    private const val BASE_URL: String = "https://apis.openapi.sk.com/"

    fun getMapRetrofitInstance(): Retrofit {
        return Retrofit
            .Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build() // TMAP 서버와의 통신을 위한 Retrofit 객체
    }
}
