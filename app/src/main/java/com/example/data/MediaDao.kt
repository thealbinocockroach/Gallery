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

    @Query("SELECT * FROM media_items WHERE isInTrash = 1")
    suspend fun getTrashMediaOnce(): List<MediaItem>

    @Query("SELECT * FROM media_items WHERE id = :id LIMIT 1")
    suspend fun getMediaById(id: Long): MediaItem?

    @Query("SELECT uri FROM media_items")
    suspend fun getAllUris(): List<String>

    @Query("SELECT * FROM media_items")
    suspend fun getAllMediaOnce(): List<MediaItem>

    @Query("SELECT name FROM albums WHERE isSystem = 1")
    suspend fun getSystemAlbumNames(): List<String>

    @Update
    suspend fun updateMediaList(items: List<MediaItem>)

    @Query("SELECT * FROM albums WHERE name = :name LIMIT 1")
    suspend fun getAlbumByName(name: String): Album?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(item: MediaItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMedia(items: List<MediaItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaReturningIds(items: List<MediaItem>): List<Long>

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

    @Query("SELECT * FROM media_items WHERE isInTrash = 1 AND trashTimestamp < :cutoff")
    suspend fun getExpiredTrash(cutoff: Long): List<MediaItem>

    @Query("DELETE FROM media_items WHERE isInTrash = 1 AND trashTimestamp < :cutoff")
    suspend fun purgeExpiredTrash(cutoff: Long)

    @Query("UPDATE media_items SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE media_items SET albumName = :newAlbum WHERE id = :id")
    suspend fun moveMediaToAlbum(id: Long, newAlbum: String)

    @Query("UPDATE media_items SET albumName = :newAlbum WHERE id IN (:ids)")
    suspend fun moveMediaToAlbumBulk(ids: List<Long>, newAlbum: String)

    @Query(
        "UPDATE media_items SET dateTaken = :dateTaken, sizeBytes = :sizeBytes, width = :width, height = :height, albumName = :albumName " +
            "WHERE uri = :uri AND albumName IN (SELECT name FROM albums WHERE isSystem = 1)"
    )
    suspend fun refreshScannedMetadata(
        uri: String,
        dateTaken: Long,
        sizeBytes: Long,
        width: Int,
        height: Int,
        albumName: String
    )

    @Query("SELECT * FROM media_items WHERE id IN (:ids)")
    suspend fun getMediaByIds(ids: List<Long>): List<MediaItem>

    @Query("DELETE FROM media_items WHERE id IN (:ids)")
    suspend fun deleteMediaByIds(ids: List<Long>)

    // Albums
    @Query("SELECT * FROM albums ORDER BY isSystem DESC, name ASC")
    fun getAllAlbums(): Flow<List<Album>>

    @Query("SELECT * FROM albums WHERE isSystem = 0 ORDER BY name ASC")
    fun getAllUserAlbums(): Flow<List<Album>>

    @Query(
        """
        SELECT a.* FROM albums a
        LEFT JOIN media_items m ON m.albumName = a.name AND m.isInTrash = 0
        GROUP BY a.id
        ORDER BY MAX(m.dateTaken) DESC, a.dateCreated DESC
        """
    )
    fun getAllAlbumsByLastModified(): Flow<List<Album>>

    @Query(
        """
        SELECT a.* FROM albums a
        LEFT JOIN media_items m ON m.albumName = a.name AND m.isInTrash = 0
        WHERE a.isSystem = 0
        GROUP BY a.id
        ORDER BY MAX(m.dateTaken) DESC, a.dateCreated DESC
        """
    )
    fun getUserAlbumsByLastModified(): Flow<List<Album>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: Album): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllAlbums(albums: List<Album>)

    @Query("DELETE FROM albums WHERE id = :id AND isSystem = 0")
    suspend fun deleteAlbum(id: Long)

    // Only active (non-trashed) items keep a system album alive — otherwise an
    // album of purely trashed photos would linger with a trashed cover.
    @Query(
        "DELETE FROM albums WHERE isSystem = 1 AND name NOT IN (SELECT DISTINCT albumName FROM media_items WHERE isInTrash = 0)"
    )
    suspend fun deleteEmptySystemAlbums()

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
