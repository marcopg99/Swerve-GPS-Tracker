package com.mpg99.gpstracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class TrackSummary(
    val id: Long,
    val name: String,
    val startTime: Long,
    val endTime: Long?,
    val coverUri: String?,
    val distanceMeters: Double,
    val pointCount: Int
)

/** Bare coordinate pair, used to draw route shapes without loading full [TrackPoint] rows. */
data class LatLon(
    val latitude: Double,
    val longitude: Double
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

    @Query("SELECT * FROM tracks WHERE distanceMeters = 0")
    suspend fun tracksWithoutDistance(): List<Track>

    @Query("""
        SELECT t.id AS id, t.name AS name, t.startTime AS startTime, t.endTime AS endTime,
               t.coverUri AS coverUri, t.distanceMeters AS distanceMeters,
               COUNT(p.id) AS pointCount
        FROM tracks t
        LEFT JOIN track_points p ON p.trackId = t.id
        GROUP BY t.id
        ORDER BY t.startTime DESC
    """)
    fun observeTrackSummaries(): Flow<List<TrackSummary>>

    @Query("UPDATE tracks SET coverUri = :coverUri WHERE id = :id")
    suspend fun setCover(id: Long, coverUri: String?)

    @Query("UPDATE tracks SET distanceMeters = :meters WHERE id = :id")
    suspend fun updateDistance(id: Long, meters: Double)

    @Query("UPDATE tracks SET name = :name WHERE id = :id")
    suspend fun renameTrack(id: Long, name: String)

    @Insert
    suspend fun insertPoint(point: TrackPoint): Long

    @Insert
    suspend fun insertPoints(points: List<TrackPoint>): List<Long>

    @Query("SELECT * FROM track_points WHERE trackId = :trackId ORDER BY timestamp ASC")
    suspend fun getPoints(trackId: Long): List<TrackPoint>

    @Query("SELECT latitude, longitude FROM track_points WHERE trackId = :trackId ORDER BY timestamp ASC")
    suspend fun getPointCoords(trackId: Long): List<LatLon>

    @Query("SELECT * FROM track_points WHERE trackId = :trackId ORDER BY timestamp ASC")
    fun observePoints(trackId: Long): Flow<List<TrackPoint>>

    @Query("SELECT COUNT(*) FROM track_points WHERE trackId = :trackId")
    fun observePointCount(trackId: Long): Flow<Int>

    @Query("DELETE FROM track_points WHERE trackId = :trackId AND (timestamp < :from OR timestamp > :to)")
    suspend fun deletePointsOutside(trackId: Long, from: Long, to: Long)

    @Query("UPDATE tracks SET startTime = :startTime, endTime = :endTime, distanceMeters = :meters WHERE id = :id")
    suspend fun updateBounds(id: Long, startTime: Long, endTime: Long, meters: Double)

    @Transaction
    suspend fun trim(trackId: Long, from: Long, to: Long, meters: Double) {
        deletePointsOutside(trackId, from, to)
        updateBounds(trackId, from, to, meters)
    }

    @Transaction
    suspend fun insertTrackWithPoints(track: Track, points: (Long) -> List<TrackPoint>): Long {
        val id = insertTrack(track)
        val resolved = points(id)
        if (resolved.isNotEmpty()) insertPoints(resolved)
        return id
    }
}
