package com.marcosanjos.mygaragem.service

import com.marcosanjos.mygaragem.model.Car
import retrofit2.http.GET

interface ApiService {

    @GET("car")
    suspend fun getCars(): List<Car>
}