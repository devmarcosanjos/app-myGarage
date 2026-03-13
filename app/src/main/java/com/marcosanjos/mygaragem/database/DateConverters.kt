package com.marcosanjos.mygaragem.database

import androidx.room.TypeConverter
import java.util.Date

class DateConverters {

    @TypeConverter
    fun fromTimeStamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimeStamp(date: Date?): Long? = date?.time
}
