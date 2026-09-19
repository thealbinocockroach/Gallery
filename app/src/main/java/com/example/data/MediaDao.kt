package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_items WHERE isInTrash = 0 ORDER BY dateTaken DESC")
    fun getAllActiveMedia(): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE isInTrash = 0 AND albumName = :albumName ORDER BY dateTaken DESC")
    fun getMediaByAlbum(albumName: String): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE isInTrash = 0 AND isFavorite = 1 ORDER BY dateTaken DESC")
    fun getFavorites(): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE isInTrash = 0 AND isVideo = 1 ORDER BY dateTaken DESC")
    fun getVideos(): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE isInTrash = 1 ORDER BY trashTimestamp DESC")
    fun getTrashMedia(): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE id = :id LIMIT 1")
    suspend fun getMediaById(id: Long): MediaItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(item: MediaItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMedia(items: List<MediaItem>)

    @Update
    suspend fun updateMedia(item: MediaItem)

    @Query("UPDATE media_items SET isInTrash = 1, trashTimestamp = :timestamp WHERE id = :id")
    suspend fun moveToTrash(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE media_items SET isInTrash = 0, trashTimestamp = 0 WHERE id = :id")
    suspend fun restoreFromTrash(id: Long)

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deletePermanently(id: Long)

    @Query("DELETE FROM media_items WHERE isInTrash = 1")
    suspend fun emptyTrash()

    @Query("UPDATE media_items SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE media_items SET albumName = :newAlbum WHERE id = :id")
    suspend fun moveMediaToAlbum(id: Long, newAlbum: String)

    // Albums
    @Query("SELECT * FROM albums ORDER BY isSystem DESC, name ASC")
    fun getAllAlbums(): Flow<List<Album>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: Album): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllAlbums(albums: List<Album>)

    @Query("DELETE FROM albums WHERE id = :id AND isSystem = 0")
    suspend fun deleteAlbum(id: Long)

    // Fonts
    @Query("SELECT * FROM fonts ORDER BY isCustomInstalled DESC, name ASC")
    fun getAllFonts(): Flow<List<FontItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFont(font: FontItem)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllFonts(fonts: List<FontItem>)

    @Query("DELETE FROM fonts WHERE id = :id")
    suspend fun deleteFont(id: String)
}
