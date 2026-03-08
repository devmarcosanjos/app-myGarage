package com.marcosanjos.mygaragem.model

import java.time.Year

data class Car(
    val id: String,
    val value: CarValue
)

data class CarValue(
    val imageUrl: String,
    val year: Year,
    val name: String,
    val licence: String,
    val location: CarLocation
)

data class CarLocation(
    val lat: Double,
    val long: Double,
)
