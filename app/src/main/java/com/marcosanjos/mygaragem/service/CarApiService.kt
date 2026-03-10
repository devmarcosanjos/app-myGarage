package com.marcosanjos.mygaragem.service

import com.marcosanjos.mygaragem.model.Car
import com.marcosanjos.mygaragem.model.CarDetailResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface CarApiService {

    @GET("car")
    suspend fun getCars(): List<Car>

    @GET("car/{id}")
    suspend fun getCarById(@Path("id") id: String): CarDetailResponse
}