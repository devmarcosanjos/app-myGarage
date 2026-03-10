package com.marcosanjos.mygaragem.model

import java.io.Serializable

data class Car(
    val id: String? = null,
    val imageUrl: String? = null,
    val year: String? = null,
    val name: String? = null,
    val licence: String? = null,
    val place: CarLocation? = null
) : Serializable

data class CarLocation(
    val lat: Double? = null,
    val long: Double? = null,
) : Serializable