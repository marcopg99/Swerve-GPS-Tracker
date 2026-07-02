package com.mpg99.gpstracker.data

import com.mpg99.gpstracker.util.haversineMeters
import kotlinx.coroutines.flow.Flow

class TrackRepository(private val dao: TrackDao) {

    fun observeTracks(): Flow<List<TrackSummary>> = dao.observeTrackSummaries()

    fun observePoints(trackId: Long): Flow<List<TrackPoint>> = dao.observePoints(trackId)

    fun observePointCount(trackId: Long): Flow<Int> = dao.observePointCount(trackId)

    suspend fun getTrack(id: Long): Track? = dao.getTrack(id)

    suspend fun getPoints(trackId: Long): List<TrackPoint> = dao.getPoints(trackId)

    suspend fun startTrack(name: String, startTime: Long): Long =
        dao.insertTrack(Track(name = name, startTime = startTime))

    suspend fun finishTrack(trackId: Long, endTime: Long) {
        val track = dao.getTrack(trackId) ?: return
        val distance = totalDistanceMeters(dao.getPoints(trackId))
        dao.updateTrack(track.copy(endTime = endTime, distanceMeters = distance))
    }

    suspend fun setDistance(trackId: Long, meters: Double) {
        dao.updateDistance(trackId, meters)
    }

    /** Recomputes distance for tracks stored before distances were tracked (value still 0). */
    suspend fun backfillDistances() {
        dao.tracksWithoutDistance().forEach { track ->
            val distance = totalDistanceMeters(dao.getPoints(track.id))
            if (distance > 0) dao.updateDistance(track.id, distance)
        }
    }

    suspend fun renameTrack(id: Long, name: String) {
        dao.renameTrack(id, name.trim())
    }

    suspend fun setCover(id: Long, coverUri: String?) {
        dao.setCover(id, coverUri)
    }

    suspend fun addPoint(point: TrackPoint) {
        dao.insertPoint(point)
    }

    suspend fun deleteTrack(id: Long) = dao.deleteTrack(id)

    suspend fun importTrack(
        name: String,
        startTime: Long,
        endTime: Long?,
        points: List<TrackPoint>
    ): Long = dao.insertTrackWithPoints(
        Track(
            name = name,
            startTime = startTime,
            endTime = endTime,
            distanceMeters = totalDistanceMeters(points)
        )
    ) { trackId -> points.map { it.copy(trackId = trackId) } }

    private fun totalDistanceMeters(points: List<TrackPoint>): Double {
        if (points.size < 2) return 0.0
        var total = 0.0
        for (i in 1 until points.size) {
            val a = points[i - 1]
            val b = points[i]
            total += haversineMeters(a.latitude, a.longitude, b.latitude, b.longitude)
        }
        return total
    }
}
