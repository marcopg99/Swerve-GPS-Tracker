package com.mpg99.gpstracker.util

/**
 * Evenly thins [points] down to at most [maxPoints] entries, always keeping the first and last
 * so the shape of a route stays anchored at both ends.
 */
fun <T> downsample(points: List<T>, maxPoints: Int): List<T> {
    if (points.size <= maxPoints || maxPoints < 2) return points
    val step = (points.size - 1).toDouble() / (maxPoints - 1)
    return List(maxPoints) { i -> points[(i * step).toInt().coerceAtMost(points.lastIndex)] }
}
