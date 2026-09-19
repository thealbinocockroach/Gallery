package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_items")
data class MediaItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val uri: String,
    val isVideo: Boolean = false,
    val durationMs: Long = 0,
    val sizeBytes: Long = 0,
    val dateTaken: Long = System.currentTimeMillis(),
    val width: Int = 1920,
    val height: Int = 1080,
    val albumName: String = "Camera",
    val isFavorite: Boolean = false,
    val isInTrash: Boolean = false,
    val trashTimestamp: Long = 0,
    val filterName: String = "Normal",
    val rotationDegrees: Float = 0f,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val editJson: String? = null
)

@Entity(tableName = "albums")
data class Album(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val coverUri: String = "",
    val isSystem: Boolean = false,
    val dateCreated: Long = System.currentTimeMillis()
)

@Entity(tableName = "fonts")
data class FontItem(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val isCustomInstalled: Boolean = false,
    val fontPath: String? = null,
    val previewText: String = "NEOBRUTALIST GALLERY PRO 2026"
)
