package com.marcosanjos.mygaragem.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Car(
    @SerializedName("id")
    val id: String? = null,
    
    @SerializedName("imageUrl", alternate = ["image", "url", "foto"])
    val imageUrl: String? = null,
    
    @SerializedName("year", alternate = ["ano", "modelYear"])
    val year: String? = null,
    
    @SerializedName("name", alternate = ["nome", "title"])
    val name: String? = null,
    
    @SerializedName("licence", alternate = ["placa", "plate"])
    val licence: String? = null,
    
    @SerializedName("place")
    val place: CarLocation? = null
) : Serializable

data class CarLocation(
    val lat: Double? = null,
    val long: Double? = null,
) : Serializable

// Nova classe para lidar com o detalhe aninhado da sua API
data class CarDetailResponse(
    val id: String? = null,
    val value: Car? = null
)