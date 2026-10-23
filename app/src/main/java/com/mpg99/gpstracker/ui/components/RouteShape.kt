package com.mpg99.gpstracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mpg99.gpstracker.data.LatLon
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

/**
 * Draws only the *shape* of a route (no map behind it), scaled to fit and centered in the
 * available space. Longitude is compressed by cos(latitude) so the shape is not stretched.
 * Marks the start with a hollow dot and the end with a filled one.
 */
@Composable
fun RouteShape(
    points: List<LatLon>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    holeColor: Color = MaterialTheme.colorScheme.background,
    strokeWidth: Dp = 3.dp,
    padding: Dp = 14.dp
) {
    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas

        val pad = padding.toPx()
        val minLat = points.minOf { it.latitude }
        val maxLat = points.maxOf { it.latitude }
        val minLon = points.minOf { it.longitude }
        val maxLon = points.maxOf { it.longitude }
        val lonScale = cos(Math.toRadians((minLat + maxLat) / 2))

        val geoW = (maxLon - minLon) * lonScale
        val geoH = maxLat - minLat
        val availW = (size.width - 2 * pad).toDouble()
        val availH = (size.height - 2 * pad).toDouble()
        val scale = min(
            if (geoW > 0) availW / geoW else Double.MAX_VALUE,
            if (geoH > 0) availH / geoH else Double.MAX_VALUE
        ).let { if (it == Double.MAX_VALUE) 1.0 else it }

        val offsetX = pad + (availW - geoW * scale) / 2
        val offsetY = pad + (availH - geoH * scale) / 2

        fun project(p: LatLon) = Offset(
            x = (offsetX + (p.longitude - minLon) * lonScale * scale).toFloat(),
            y = (offsetY + (maxLat - p.latitude) * scale).toFloat()
        )

        val path = Path()
        points.forEachIndexed { i, p ->
            val o = project(p)
            if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
        }

        val width = strokeWidth.toPx()
        // Soft halo underneath, then the line itself.
        drawPath(
            path,
            color = lineColor.copy(alpha = 0.25f),
            style = Stroke(width = width * 3.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawPath(
            path,
            color = lineColor,
            style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        val dot = max(width * 1.6f, 4.dp.toPx())
        val start = project(points.first())
        drawCircle(holeColor, radius = dot, center = start)
        drawCircle(lineColor, radius = dot, center = start, style = Stroke(width = width))
        drawCircle(lineColor, radius = dot, center = project(points.last()))
    }
}
