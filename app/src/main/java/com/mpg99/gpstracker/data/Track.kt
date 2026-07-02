package com.mpg99.gpstracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class Track(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val startTime: Long,
    val endTime: Long? = null,
    /** Absolute path to the cover image copied into internal storage, or null. */
    val coverUri: String? = null,
    /** Total recorded distance in meters, maintained while tracking and on import. */
    val distanceMeters: Double = 0.0
)
