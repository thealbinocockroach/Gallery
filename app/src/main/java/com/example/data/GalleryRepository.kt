package com.example.data

import kotlinx.coroutines.flow.Flow

class GalleryRepository(private val mediaDao: MediaDao) {

    val allMedia: Flow<List<MediaItem>> = mediaDao.getAllActiveMedia()
    val favorites: Flow<List<MediaItem>> = mediaDao.getFavorites()
    val videos: Flow<List<MediaItem>> = mediaDao.getVideos()
    val trashMedia: Flow<List<MediaItem>> = mediaDao.getTrashMedia()
    val albums: Flow<List<Album>> = mediaDao.getAllAlbums()
    val fonts: Flow<List<FontItem>> = mediaDao.getAllFonts()

    fun getMediaByAlbum(albumName: String): Flow<List<MediaItem>> {
        return mediaDao.getMediaByAlbum(albumName)
    }

    suspend fun getMediaById(id: Long): MediaItem? {
        return mediaDao.getMediaById(id)
    }

    suspend fun insertMedia(item: MediaItem): Long {
        return mediaDao.insertMedia(item)
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

    suspend fun deletePermanently(id: Long) {
        mediaDao.deletePermanently(id)
    }

    suspend fun emptyTrash() {
        mediaDao.emptyTrash()
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
