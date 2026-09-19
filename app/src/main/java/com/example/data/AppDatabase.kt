package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [MediaItem::class, Album::class, FontItem::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gallery_pro_db"
                )
                .addCallback(DatabaseCallback(scope, context.applicationContext))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope,
            private val context: Context
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.mediaDao(), context)
                    }
                }
            }
        }

        suspend fun populateInitialData(dao: MediaDao, context: Context) {
            // Pre-populate system albums
            val initialAlbums = listOf(
                Album(id = 1, name = "Camera", coverUri = "android.resource://${context.packageName}/drawable/sample_neon_city_1789826462211", isSystem = true),
                Album(id = 2, name = "Screenshots", coverUri = "android.resource://${context.packageName}/drawable/sample_retro_car_1789826484714", isSystem = true),
                Album(id = 3, name = "Downloads", coverUri = "android.resource://${context.packageName}/drawable/sample_nature_mountain_1789826472915", isSystem = true),
                Album(id = 4, name = "Edits", coverUri = "", isSystem = true)
            )
            dao.insertAllAlbums(initialAlbums)

            val now = System.currentTimeMillis()
            val dayMs = 86400000L

            // Pre-populate sample photos & videos
            val initialMedia = listOf(
                MediaItem(
                    title = "Cyber Neon Cityscape",
                    uri = "android.resource://${context.packageName}/drawable/sample_neon_city_1789826462211",
                    isVideo = false,
                    durationMs = 0,
                    sizeBytes = 3450000L,
                    dateTaken = now,
                    width = 2400,
                    height = 1800,
                    albumName = "Camera",
                    isFavorite = true
                ),
                MediaItem(
                    title = "Golden Hour Alpine Lake",
                    uri = "android.resource://${context.packageName}/drawable/sample_nature_mountain_1789826472915",
                    isVideo = false,
                    durationMs = 0,
                    sizeBytes = 4120000L,
                    dateTaken = now - (dayMs / 3),
                    width = 1800,
                    height = 2400,
                    albumName = "Camera",
                    isFavorite = true
                ),
                MediaItem(
                    title = "Vintage Roadster & Concrete",
                    uri = "android.resource://${context.packageName}/drawable/sample_retro_car_1789826484714",
                    isVideo = false,
                    durationMs = 0,
                    sizeBytes = 2890000L,
                    dateTaken = now - dayMs,
                    width = 2048,
                    height = 2048,
                    albumName = "Screenshots",
                    isFavorite = false
                ),
                MediaItem(
                    title = "Neobrutalist Motion Reel",
                    uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                    isVideo = true,
                    durationMs = 15000L,
                    sizeBytes = 8500000L,
                    dateTaken = now - (dayMs * 2),
                    width = 1920,
                    height = 1080,
                    albumName = "Camera",
                    isFavorite = false
                ),
                MediaItem(
                    title = "Urban Horizon Loop",
                    uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    isVideo = true,
                    durationMs = 32000L,
                    sizeBytes = 14200000L,
                    dateTaken = now - (dayMs * 3),
                    width = 1920,
                    height = 1080,
                    albumName = "Downloads",
                    isFavorite = true
                )
            )
            dao.insertAllMedia(initialMedia)

            // Pre-populate Free Fonts Repository
            val initialFonts = listOf(
                FontItem(
                    id = "font_neo_black",
                    name = "Neo Display Black",
                    category = "Display Neo",
                    previewText = "STRIKING NEOBRUTALIST TITLES"
                ),
                FontItem(
                    id = "font_editorial_serif",
                    name = "Editorial Serif Bold",
                    category = "Editorial Serif",
                    previewText = "Timeless Elegance & Literary Grace"
                ),
                FontItem(
                    id = "font_retro_mono",
                    name = "Space Terminal Mono",
                    category = "Retro Mono",
                    previewText = "SYS_VER: 2026.09.19 // CODE READY"
                ),
                FontItem(
                    id = "font_bebas_impact",
                    name = "Impact Block Condensed",
                    category = "Impact Header",
                    previewText = "BOLD HEADLINES CATCH THE EYE"
                ),
                FontItem(
                    id = "font_marker_brush",
                    name = "Wild Marker Brush",
                    category = "Playful Marker",
                    previewText = "Casual hand-drawn doodle vibe!"
                ),
                FontItem(
                    id = "font_grotesk_clean",
                    name = "Modern Grotesk Medium",
                    category = "Grotesk Sans",
                    previewText = "Crisp, minimalist, ultra-readable type"
                ),
                FontItem(
                    id = "font_script_flow",
                    name = "Velvet Script Cursive",
                    category = "Elegant Script",
                    previewText = "Handcrafted with passion and flair"
                )
            )
            dao.insertAllFonts(initialFonts)
        }
    }
}
