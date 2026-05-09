package com.mpg99.gpstracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class TrackSummary(
    val id: Long,
    val name: String,
    val startTime: Long,
    val endTime: Long?,
    val pointCount: Int
)

@Dao
interface TrackDao {

    @Insert
    suspend fun insertTrack(track: Track): Long

    @Update
    suspend fun updateTrack(track: Track)

    @Query("DELETE FROM tracks WHERE id = :id")
    suspend fun deleteTrack(id: Long)

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getTrack(id: Long): Track?

    @Query("""
        SELECT t.id AS id, t.name AS name, t.startTime AS startTime, t.endTime AS endTime,
               COUNT(p.id) AS pointCount
        FROM tracks t
        LEFT JOIN track_points p ON p.trackId = t.id
        GROUP BY t.id
        ORDER BY t.startTime DESC
    """)
    fun observeTrackSummaries(): Flow<List<TrackSummary>>

    @Insert
    suspend fun insertPoint(point: TrackPoint): Long

    @Query("SELECT * FROM track_points WHERE trackId = :trackId ORDER BY timestamp ASC")
    suspend fun getPoints(trackId: Long): List<TrackPoint>

    @Query("SELECT * FROM track_points WHERE trackId = :trackId ORDER BY timestamp ASC")
    fun observePoints(trackId: Long): Flow<List<TrackPoint>>

    @Query("SELECT COUNT(*) FROM track_points WHERE trackId = :trackId")
    fun observePointCount(trackId: Long): Flow<Int>
}
