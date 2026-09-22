package com.example.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.FileOutputStream

class GalleryRepository(private val mediaDao: MediaDao) {

    val allMedia: Flow<List<MediaItem>> = mediaDao.getAllActiveMedia()
    val favorites: Flow<List<MediaItem>> = mediaDao.getFavorites()
    val videos: Flow<List<MediaItem>> = mediaDao.getVideos()
    val trashMedia: Flow<List<MediaItem>> = mediaDao.getTrashMedia()
    val albums: Flow<List<Album>> = mediaDao.getAllAlbumsByLastModified()
    val fonts: Flow<List<FontItem>> = mediaDao.getAllFonts()

    /**
     * Scans the device photo & video store and imports anything not already tracked.
     * Existing rows also get their metadata refreshed (especially the real capture date).
     */
    suspend fun syncDeviceMedia(context: Context): Int {
        val deviceItems = DeviceMediaScanner.scanDeviceMedia(context)
        val existingItems = mediaDao.getAllMediaOnce()
        val existingByUri = existingItems.associateBy { it.uri }

        val newItems = deviceItems.filter { it.uri !in existingByUri }
        if (newItems.isNotEmpty()) {
            mediaDao.insertAllMedia(newItems)
        }

        // Refresh already-tracked items in a single batch, and only when something
        // actually changed. Avoids thousands of per-row writes on every launch.
        val systemAlbumNames = mediaDao.getSystemAlbumNames().toHashSet()
        val changed = ArrayList<MediaItem>()
        deviceItems.forEach { scanned ->
            val old = existingByUri[scanned.uri] ?: return@forEach
            val albumChanged = old.albumName != scanned.albumName && old.albumName in systemAlbumNames
            val metaChanged = old.dateTaken != scanned.dateTaken ||
                old.sizeBytes != scanned.sizeBytes ||
                old.width != scanned.width ||
                old.height != scanned.height
            if (albumChanged || metaChanged) {
                changed.add(
                    old.copy(
                        dateTaken = scanned.dateTaken,
                        sizeBytes = scanned.sizeBytes,
                        width = scanned.width,
                        height = scanned.height,
                        albumName = if (albumChanged) scanned.albumName else old.albumName
                    )
                )
            }
        }
        if (changed.isNotEmpty()) {
            mediaDao.updateMediaList(changed)
        }

        // Ensure an album row exists for every folder seen on the device.
        val albumNames = deviceItems.map { it.albumName }.filter { it.isNotBlank() }.distinct()
        albumNames.forEach { name ->
            if (mediaDao.getAlbumByName(name) == null) {
                val cover = deviceItems.filter { it.albumName == name }.maxByOrNull { it.dateTaken }?.uri.orEmpty()
                mediaDao.insertAlbum(Album(name = name, coverUri = cover, isSystem = true))
            } else {
                // Keep cover fresh — point it to the latest image in the album.
                val latestUri = deviceItems.filter { it.albumName == name }.maxByOrNull { it.dateTaken }?.uri
                if (latestUri != null) {
                    val existing = mediaDao.getAlbumByName(name)
                    if (existing != null && existing.coverUri != latestUri) {
                        mediaDao.insertAlbum(existing.copy(coverUri = latestUri))
                    }
                }
            }
        }

        // Drop system albums that no longer contain any media (e.g. the burst
        // sub-folders that were just merged back into Camera).
        mediaDao.deleteEmptySystemAlbums()
        return newItems.size
    }

    fun getMediaByAlbum(albumName: String): Flow<List<MediaItem>> {
        return mediaDao.getMediaByAlbum(albumName)
    }

    suspend fun getMediaById(id: Long): MediaItem? {
        return mediaDao.getMediaById(id)
    }

    suspend fun getMediaByIds(ids: List<Long>): List<MediaItem> {
        return mediaDao.getMediaByIds(ids)
    }

    suspend fun insertMedia(item: MediaItem): Long {
        return mediaDao.insertMedia(item)
    }

    suspend fun insertMultipleMedia(items: List<MediaItem>): List<Long> {
        return mediaDao.insertMediaReturningIds(items)
    }

    suspend fun moveMediaToAlbumBulk(ids: List<Long>, newAlbum: String) {
        mediaDao.moveMediaToAlbumBulk(ids, newAlbum)
    }

    suspend fun deleteMediaByIds(ids: List<Long>) {
        mediaDao.deleteMediaByIds(ids)
    }

    /**
     * Physically copy the byte data of [items] into the app's own storage and
     * return new rows pointing at the copied files (metadata-only copy in the DB).
     */
    suspend fun copyMediaFiles(context: Context, items: List<MediaItem>, targetAlbum: String): List<MediaItem> {
        val copied = mutableListOf<MediaItem>()
        val appDir = context.getExternalFilesDir(null) ?: context.filesDir
        val copyDir = File(appDir, "copies").apply { mkdirs() }
        items.forEach { item ->
            try {
                val resolver = context.contentResolver
                val source = Uri.parse(item.uri)
                val ext = if (item.isVideo) ".mp4" else ".jpg"
                val fileName = "copy_${System.currentTimeMillis()}_${item.id}$ext"
                val dest = File(copyDir, fileName)
                resolver.openInputStream(source)?.use { input ->
                    FileOutputStream(dest).use { output -> input.copyTo(output) }
                } ?: return@forEach
                copied.add(
                    item.copy(
                        id = 0,
                        uri = Uri.fromFile(dest).toString(),
                        albumName = targetAlbum,
                        title = "Copy_${item.title}",
                        isFavorite = false,
                        isInTrash = false,
                        trashTimestamp = 0,
                        filterName = "Normal",
                        rotationDegrees = 0f,
                        flipHorizontal = false,
                        flipVertical = false
                    )
                )
            } catch (_: Exception) {
                // Skip files that could not be copied.
            }
        }
        return copied
    }

    suspend fun updateMedia(item: MediaItem) {
        mediaDao.updateMedia(item)
    }

    suspend fun moveToTrash(id: Long) {
        mediaDao.moveToTrash(id)
    }

    suspend fun restoreFromTrash(id: Long) {
        mediaDao.restoreFromTrash(id)
    }

    /**
     * Permanent delete removes the DB row AND the underlying file, so a later
     * device sync can't resurrect the item back into the grids. Best-effort on
     * the file (system-owned MediaStore rows may need user consent); the row
     * and its cached thumbnails are always dropped.
     */
    suspend fun deletePermanently(context: Context, id: Long) {
        val item = mediaDao.getMediaById(id)
        if (item != null) {
            deleteBackingFile(context, item.uri)
            ThumbnailCache.remove(item.uri)
        }
        mediaDao.deletePermanently(id)
    }

    suspend fun emptyTrash(context: Context) {
        val trashed = mediaDao.getTrashMediaOnce()
        trashed.forEach { item ->
            deleteBackingFile(context, item.uri)
            ThumbnailCache.remove(item.uri)
        }
        mediaDao.emptyTrash()
    }

    private fun deleteBackingFile(context: Context, uriString: String) {
        try {
            val parsed = Uri.parse(uriString)
            when (parsed.scheme) {
                "content" -> context.contentResolver.delete(parsed, null, null)
                "file" -> parsed.path?.let { File(it).delete() }
            }
        } catch (_: Exception) {
            // Best-effort: row + thumbnail cache are still dropped below.
        }
    }

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) {
        mediaDao.toggleFavorite(id, isFavorite)
    }

    suspend fun moveMediaToAlbum(id: Long, newAlbum: String) {
        mediaDao.moveMediaToAlbum(id, newAlbum)
    }

    suspend fun createAlbum(name: String, coverUri: String = ""): Long {
        return mediaDao.insertAlbum(
            Album(name = name, coverUri = coverUri, isSystem = false)
        )
    }

    suspend fun deleteAlbum(id: Long) {
        mediaDao.deleteAlbum(id)
    }

    suspend fun installFont(font: FontItem) {
        mediaDao.insertFont(font)
    }

    suspend fun removeFont(id: String) {
        mediaDao.deleteFont(id)
    }
}
