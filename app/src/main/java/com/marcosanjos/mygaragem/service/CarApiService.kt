package com.marcosanjos.mygaragem.service

import com.marcosanjos.mygaragem.model.Car
import com.marcosanjos.mygaragem.model.CarDetailResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface CarApiService {

    @GET("car")
    suspend fun getCars(): List<Car>

    @GET("car/{id}")
    suspend fun getCarById(@Path("id") id: String): CarDetailResponse

    @DELETE("car/{id}")
    suspend fun deleteCar(@Path("id") id: String)

    @PATCH("car/{id}")
    suspend fun updateCar(@Path("id") id: String, @Body car: Car): Response<Car>

    @POST("car")
    suspend fun addCar(@Body car: Car): Response<Car>
}