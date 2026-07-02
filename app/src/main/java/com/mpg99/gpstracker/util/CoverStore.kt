package com.mpg99.gpstracker.util

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Stores track cover images inside the app's internal storage (`filesDir/covers/`).
 *
 * The picked image (a transient content URI) is copied into a stable file we fully own, so the
 * cover survives across reboots and after the source app revokes access. The absolute path is what
 * gets persisted in [com.mpg99.gpstracker.data.Track.coverUri].
 */
object CoverStore {

    private fun dir(context: Context): File =
        File(context.filesDir, "covers").apply { mkdirs() }

    /**
     * Copies [source] into internal storage as the cover for [trackId], returning the absolute
     * path, or null if the image could not be read. A fresh timestamped filename is used every
     * time (and any [previousPath] deleted) so image caches never show a stale bitmap.
     */
    fun saveCover(
        context: Context,
        trackId: Long,
        source: Uri,
        previousPath: String? = null
    ): String? {
        val target = File(dir(context), "cover_${trackId}_${System.currentTimeMillis()}.jpg")
        val copied = runCatching {
            context.contentResolver.openInputStream(source)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
        }
        if (copied.isFailure) {
            target.delete()
            return null
        }
        deleteCover(previousPath)
        return target.absolutePath
    }

    /** Deletes the cover file at [path] if present. */
    fun deleteCover(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).delete() }
    }
}
