package com.mpg99.gpstracker.gpx

import android.util.Xml
import com.mpg99.gpstracker.data.TrackPoint
import com.mpg99.gpstracker.data.TrackRepository
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

object GpxImporter {

    data class ParsedTrack(
        val name: String,
        val startTime: Long,
        val endTime: Long?,
        val points: List<TrackPoint>
    )

    class GpxParseException(message: String, cause: Throwable? = null) : Exception(message, cause)

    fun parse(input: InputStream, fallbackName: String): List<ParsedTrack> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(input, null)

        val tracks = mutableListOf<ParsedTrack>()
        var metadataTime: Long? = null
        var index = 0
        try {
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "metadata" -> metadataTime = readMetadataTime(parser)
                        "trk" -> tracks += readTrk(parser, fallbackName, index++, metadataTime)
                    }
                }
                event = parser.next()
            }
        } catch (e: Exception) {
            throw GpxParseException("No se pudo leer el archivo GPX: ${e.message}", e)
        }

        if (tracks.isEmpty()) {
            throw GpxParseException("El archivo no contiene rutas (<trk>) válidas")
        }
        return tracks
    }

    suspend fun import(repo: TrackRepository, parsed: List<ParsedTrack>): Int {
        for (t in parsed) {
            repo.importTrack(
                name = t.name,
                startTime = t.startTime,
                endTime = t.endTime,
                points = t.points
            )
        }
        return parsed.size
    }

    private fun readMetadataTime(parser: XmlPullParser): Long? {
        var time: Long? = null
        while (parser.next() != XmlPullParser.END_TAG || parser.name != "metadata") {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "time") {
                time = parseTime(parser.nextText())
            } else if (parser.eventType == XmlPullParser.END_DOCUMENT) {
                break
            }
        }
        return time
    }

    private fun readTrk(
        parser: XmlPullParser,
        fallbackName: String,
        trackIndex: Int,
        metadataTime: Long?
    ): ParsedTrack {
        var name: String? = null
        val points = mutableListOf<TrackPoint>()
        while (parser.next() != XmlPullParser.END_TAG || parser.name != "trk") {
            if (parser.eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "name" -> name = parser.nextText()?.trim()?.takeIf { it.isNotEmpty() }
                    "trkseg" -> points += readTrkSeg(parser)
                }
            } else if (parser.eventType == XmlPullParser.END_DOCUMENT) {
                break
            }
        }

        val effectiveName = name
            ?: if (trackIndex == 0) fallbackName else "$fallbackName ($trackIndex)"
        val firstTime = points.firstOrNull { it.timestamp > 0 }?.timestamp
        val lastTime = points.lastOrNull { it.timestamp > 0 }?.timestamp
        val startTime = firstTime ?: metadataTime ?: System.currentTimeMillis()
        val endTime = lastTime ?: (if (firstTime != null) firstTime else null)

        val withTimes = if (points.any { it.timestamp == 0L }) {
            points.mapIndexed { i, p ->
                if (p.timestamp == 0L) p.copy(timestamp = startTime + i) else p
            }
        } else points

        return ParsedTrack(
            name = effectiveName,
            startTime = startTime,
            endTime = endTime,
            points = withTimes
        )
    }

    private fun readTrkSeg(parser: XmlPullParser): List<TrackPoint> {
        val points = mutableListOf<TrackPoint>()
        while (parser.next() != XmlPullParser.END_TAG || parser.name != "trkseg") {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "trkpt") {
                readTrkPt(parser)?.let { points += it }
            } else if (parser.eventType == XmlPullParser.END_DOCUMENT) {
                break
            }
        }
        return points
    }

    private fun readTrkPt(parser: XmlPullParser): TrackPoint? {
        val lat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull() ?: return null
        val lon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull() ?: return null

        var ele: Double? = null
        var time: Long = 0L
        var speed: Float? = null
        var accuracy: Float? = null
        var bearing: Float? = null

        while (parser.next() != XmlPullParser.END_TAG || parser.name != "trkpt") {
            if (parser.eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "ele" -> ele = parser.nextText().toDoubleOrNull()
                    "time" -> time = parseTime(parser.nextText()) ?: 0L
                    "speed" -> speed = parser.nextText().toFloatOrNull()
                    "extensions" -> {
                        val ext = readExtensions(parser)
                        speed = speed ?: ext.speed
                        accuracy = ext.accuracy
                        bearing = ext.bearing
                    }
                }
            } else if (parser.eventType == XmlPullParser.END_DOCUMENT) {
                break
            }
        }

        return TrackPoint(
            trackId = 0,
            latitude = lat,
            longitude = lon,
            altitude = ele,
            accuracy = accuracy,
            speed = speed,
            bearing = bearing,
            timestamp = time
        )
    }

    private data class Extensions(val speed: Float?, val accuracy: Float?, val bearing: Float?)

    private fun readExtensions(parser: XmlPullParser): Extensions {
        var speed: Float? = null
        var accuracy: Float? = null
        var bearing: Float? = null
        while (parser.next() != XmlPullParser.END_TAG || parser.name != "extensions") {
            if (parser.eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "speed" -> speed = parser.nextText().toFloatOrNull()
                    "hacc", "accuracy" -> accuracy = parser.nextText().toFloatOrNull()
                    "course", "bearing" -> bearing = parser.nextText().toFloatOrNull()
                }
            } else if (parser.eventType == XmlPullParser.END_DOCUMENT) {
                break
            }
        }
        return Extensions(speed, accuracy, bearing)
    }

    private fun parseTime(raw: String?): Long? {
        val s = raw?.trim().orEmpty().ifEmpty { return null }
        return try {
            OffsetDateTime.parse(s).toInstant().toEpochMilli()
        } catch (_: DateTimeParseException) {
            try {
                java.time.Instant.parse(s).toEpochMilli()
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }
}