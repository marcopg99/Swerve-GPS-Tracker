package com.mpg99.gpstracker.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
private val gpxTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)

fun formatDateTime(epochMs: Long): String = dateFormat.format(Date(epochMs))

fun formatGpxTime(epochMs: Long): String = gpxTimeFormat.format(Date(epochMs))

fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

fun formatDistance(meters: Double): String =
    if (meters >= 1000) "%.2f km".format(meters / 1000.0)
    else "%.0f m".format(meters)

private const val EARTH_RADIUS_M = 6_371_000.0

fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).let { it * it } +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2).let { it * it }
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return EARTH_RADIUS_M * c
}
