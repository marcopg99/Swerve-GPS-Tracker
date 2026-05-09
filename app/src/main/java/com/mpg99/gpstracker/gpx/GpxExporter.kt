package com.mpg99.gpstracker.gpx

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.mpg99.gpstracker.data.Track
import com.mpg99.gpstracker.data.TrackPoint
import com.mpg99.gpstracker.util.formatGpxTime
import java.io.File
import java.util.Locale

object GpxExporter {

    private fun fmt(pattern: String, vararg args: Any?): String =
        String.format(Locale.US, pattern, *args)

    fun writeGpx(context: Context, track: Track, points: List<TrackPoint>): File {
        val dir = File(context.filesDir, "gpx").apply { mkdirs() }
        val safeName = track.name.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val file = File(dir, "${safeName}_${track.id}.gpx")
        file.writer(Charsets.UTF_8).use { w ->
            w.append("""<?xml version="1.0" encoding="UTF-8"?>""").append('\n')
            w.append(
                """<gpx version="1.1" creator="GPS Tracker" """ +
                """xmlns="http://www.topografix.com/GPX/1/1" """ +
                """xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" """ +
                """xsi:schemaLocation="http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd">"""
            ).append('\n')
            w.append("  <metadata>\n")
            w.append("    <name>").append(escape(track.name)).append("</name>\n")
            w.append("    <time>").append(formatGpxTime(track.startTime)).append("</time>\n")
            w.append("  </metadata>\n")
            w.append("  <trk>\n")
            w.append("    <name>").append(escape(track.name)).append("</name>\n")
            w.append("    <trkseg>\n")
            for (p in points) {
                w.append("      <trkpt lat=\"").append(fmt("%.7f", p.latitude))
                    .append("\" lon=\"").append(fmt("%.7f", p.longitude)).append("\">\n")
                p.altitude?.let { w.append("        <ele>").append(fmt("%.2f", it)).append("</ele>\n") }
                w.append("        <time>").append(formatGpxTime(p.timestamp)).append("</time>\n")
                if (p.speed != null || p.accuracy != null || p.bearing != null) {
                    w.append("        <extensions>\n")
                    p.speed?.let { w.append("          <speed>").append(fmt("%.3f", it)).append("</speed>\n") }
                    p.accuracy?.let { w.append("          <hacc>").append(fmt("%.2f", it)).append("</hacc>\n") }
                    p.bearing?.let { w.append("          <course>").append(fmt("%.2f", it)).append("</course>\n") }
                    w.append("        </extensions>\n")
                }
                w.append("      </trkpt>\n")
            }
            w.append("    </trkseg>\n")
            w.append("  </trk>\n")
            w.append("</gpx>\n")
        }
        return file
    }

    fun shareIntent(context: Context, file: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/gpx+xml"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun escape(s: String): String = buildString {
        for (c in s) when (c) {
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '&' -> append("&amp;")
            '"' -> append("&quot;")
            '\'' -> append("&apos;")
            else -> append(c)
        }
    }
}
