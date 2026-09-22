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
                    "gallery_db"
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
