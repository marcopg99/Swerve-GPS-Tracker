package com.mpg99.gpstracker.data

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
        dao.updateTrack(track.copy(endTime = endTime))
    }

    suspend fun addPoint(point: TrackPoint) {
        dao.insertPoint(point)
    }

    suspend fun deleteTrack(id: Long) = dao.deleteTrack(id)
}
