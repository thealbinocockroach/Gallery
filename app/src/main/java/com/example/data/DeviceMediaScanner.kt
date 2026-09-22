package com.example.data

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore

object DeviceMediaScanner {

    /**
     * Column that exposes the folder a file lives in. RELATIVE_PATH is the modern
     * (API 29+) value; DATA is the legacy absolute path used on older devices.
     */
    private fun folderColumn(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.MediaColumns.RELATIVE_PATH
        } else {
            @Suppress("DEPRECATION")
            MediaStore.MediaColumns.DATA
        }

    /**
     * Resolves the album a file belongs to. Files anywhere under DCIM/Camera
     * (burst "multiShoot" folders, FrontCamera, Portrait, Video, ...) all roll up
     * into a single "Camera" album so the Camera folder is never split.
     */
    private fun resolveAlbumName(bucket: String?, folderPath: String?): String {
        val path = folderPath?.replace('\\', '/').orEmpty()
        val isCamera = path == "DCIM/Camera" ||
            path.startsWith("DCIM/Camera/") ||
            path.contains("/DCIM/Camera/") ||
            path.endsWith("/DCIM/Camera")
        if (isCamera) return "Camera"
        return bucket?.takeIf { it.isNotBlank() } ?: "Camera"
    }

    fun scanDeviceMedia(context: Context): List<MediaItem> {
        val items = mutableListOf<MediaItem>()

        scanImages(context, items)
        scanVideos(context, items)

        return collapseBursts(items)
    }

    private fun isBurstTitle(title: String): Boolean {
        return title.contains("burst", ignoreCase = true)
    }

    /**
     * Burst shots come as many near-identical frames within ~2 seconds.
     * Show them as a single representative — the latest / largest file in each burst group.
     * This is display deduplication, not deletion; one tile per burst in every grid.
     */
    fun collapseBursts(items: List<MediaItem>): List<MediaItem> {
        if (items.isEmpty()) return items
        val sorted = items.sortedByDescending { it.dateTaken }
        val result = mutableListOf<MediaItem>()
        var burstGroupLast: MediaItem? = null
        for (item in sorted) {
            if (item.isVideo) {
                result.add(item)
                burstGroupLast = null
                continue
            }
            val isBurst = isBurstTitle(item.title)
            if (isBurst && burstGroupLast != null &&
                burstGroupLast.albumName == item.albumName &&
                kotlin.math.abs(burstGroupLast.dateTaken - item.dateTaken) < 2500 &&
                isBurstTitle(burstGroupLast.title)
            ) {
                // Same burst group — keep the larger file as representative (usually sharpest).
                if (item.sizeBytes > burstGroupLast.sizeBytes) {
                    result[result.lastIndex] = item
                    burstGroupLast = item
                }
                continue
            }
            result.add(item)
            burstGroupLast = if (isBurst) item else null
        }
        return result
    }

    private fun scanImages(context: Context, items: MutableList<MediaItem>) {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            folderColumn()
        )
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_TAKEN} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val addedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val modifiedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val folderCol = cursor.getColumnIndexOrThrow(folderColumn())

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                ).toString()
                items.add(
                    MediaItem(
                        title = cursor.getString(nameCol) ?: "Photo $id",
                        uri = uri,
                        isVideo = false,
                        durationMs = 0,
                        sizeBytes = cursor.getLong(sizeCol),
                        dateTaken = resolveDate(
                            takenMs = cursor.getLong(dateCol),
                            addedSec = cursor.getLong(addedCol),
                            modifiedSec = cursor.getLong(modifiedCol)
                        ),
                        width = cursor.getInt(widthCol).coerceAtLeast(1),
                        height = cursor.getInt(heightCol).coerceAtLeast(1),
                        albumName = resolveAlbumName(
                            bucket = cursor.getString(bucketCol),
                            folderPath = cursor.getString(folderCol)
                        ),
                        isFavorite = false,
                        isInTrash = false,
                        filterName = "Normal",
                        rotationDegrees = 0f,
                        flipHorizontal = false,
                        flipVertical = false
                    )
                )
            }
        }
    }

    private fun scanVideos(context: Context, items: MutableList<MediaItem>) {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            folderColumn()
        )
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_TAKEN} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN)
            val addedCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val modifiedCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            val folderCol = cursor.getColumnIndexOrThrow(folderColumn())

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    id
                ).toString()
                items.add(
                    MediaItem(
                        title = cursor.getString(nameCol) ?: "Video $id",
                        uri = uri,
                        isVideo = true,
                        durationMs = cursor.getLong(durationCol),
                        sizeBytes = cursor.getLong(sizeCol),
                        dateTaken = resolveDate(
                            takenMs = cursor.getLong(dateCol),
                            addedSec = cursor.getLong(addedCol),
                            modifiedSec = cursor.getLong(modifiedCol)
                        ),
                        width = cursor.getInt(widthCol).coerceAtLeast(1),
                        height = cursor.getInt(heightCol).coerceAtLeast(1),
                        albumName = resolveAlbumName(
                            bucket = cursor.getString(bucketCol),
                            folderPath = cursor.getString(folderCol)
                        ),
                        isFavorite = false,
                        isInTrash = false,
                        filterName = "Normal",
                        rotationDegrees = 0f,
                        flipHorizontal = false,
                        flipVertical = false
                    )
                )
            }
        }
    }

    /**
     * Resolves the real capture timestamp. MediaStore frequently leaves DATE_TAKEN at 0,
     * so we fall back to DATE_ADDED (seconds) then DATE_MODIFIED (seconds) before giving up.
     */
    private fun resolveDate(takenMs: Long, addedSec: Long, modifiedSec: Long): Long {
        if (takenMs > 0) return takenMs
        if (addedSec > 0) return addedSec * 1000L
        if (modifiedSec > 0) return modifiedSec * 1000L
        return System.currentTimeMillis()
    }
}