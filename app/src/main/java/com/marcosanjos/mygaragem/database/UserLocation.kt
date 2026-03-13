package com.marcosanjos.mygaragem.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "user_location_table")
data class UserLocation(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var createdAt: Date = Date()
)
