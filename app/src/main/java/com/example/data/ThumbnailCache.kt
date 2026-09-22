package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Dedicated thumbnail cache manager.
 *
 *  - Decoded bitmaps are kept in memory (LruCache).
 *  - Fetched thumbnails are persisted to disk (WebP files in the cache dir).
 *  - Grids request low-res MediaStore thumbnails; app-created file:// items
 *    fall back to a single small Coil decode, then get cached like everything else.
 *
 * Entries are keyed by the image URI (not a numeric row id) so a folder cover
 * and a media tile can never collide on the same key.
 */
object ThumbnailCache {

    private const val MEMORY_CACHE_BYTES = 64 * 1024 * 1024
    private const val DISK_CACHE_MAX_BYTES = 200L * 1024 * 1024

    @Volatile
    private var appContext: Context? = null

    private val memoryCache = object : LruCache<String, Bitmap>(MEMORY_CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    // Keep MediaStore/Coil decodes from flooding the device while scrolling.
    private val decodeSemaphore = Semaphore(6)

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /** Synchronous memory-only lookup, so already-decoded tiles render without a placeholder flash. */
    fun peek(uri: String): Bitmap? = memoryCache.get(uri)

    private fun diskFile(key: String): File? {
        val context = appContext ?: return null
        val dir = File(context.cacheDir, "thumbnails")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val name = MessageDigest.getInstance("MD5")
            .digest(key.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return File(dir, "$name.webp")
    }

    private fun pruneDiskCacheIfNeeded() {
        val context = appContext ?: return
        try {
            val dir = File(context.cacheDir, "thumbnails")
            if (!dir.exists()) return
            val files = dir.listFiles() ?: return
            var total = files.sumOf { it.length() }
            if (total <= DISK_CACHE_MAX_BYTES) return
            // Oldest first — keep hot thumbnails.
            files.sortBy { it.lastModified() }
            for (f in files) {
                total -= f.length()
                f.delete()
                if (total <= DISK_CACHE_MAX_BYTES) break
            }
        } catch (_: Exception) {
        }
    }

    suspend fun get(uri: String, isVideo: Boolean, size: Int): Bitmap? {
        return withContext(Dispatchers.IO) {
            memoryCache.get(uri)?.let { return@withContext it }

            val file = diskFile(uri)
            if (file != null && file.exists()) {
                try {
                    val opts = BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.RGB_565
                    }
                    val cached = BitmapFactory.decodeFile(file.absolutePath, opts)
                    if (cached != null && cached.width > 0) {
                        memoryCache.put(uri, cached)
                        return@withContext cached
                    }
                } catch (_: Exception) {
                    file.delete()
                }
            }

            val decoded = decodeSemaphore.withPermit {
                var fresh = memoryCache.get(uri)
                if (fresh == null) {
                    fresh = toMemoryEfficient(decodeMediaThumbnail(uri, isVideo, size))
                    if (fresh != null) {
                        memoryCache.put(uri, fresh)
                    }
                }
                fresh
            } ?: return@withContext null
            if (file != null) {
                try {
                    FileOutputStream(file).use { out ->
                        decoded.compress(Bitmap.CompressFormat.WEBP, 80, out)
                    }
                    pruneDiskCacheIfNeeded()
                } catch (_: Exception) {
                    // Disk cache write is best-effort.
                }
            }
            decoded
        }
    }

    suspend fun remove(uri: String) {
        withContext(Dispatchers.IO) {
            memoryCache.remove(uri)
            diskFile(uri)?.delete()
        }
    }

    /**
     * Memory-pressure hook (wired to ComponentCallbacks2.onTrimMemory in GalleryApplication).
     * Drops the whole decoded-bitmap pool on critical pressure, halves it on moderate
     * pressure. Disk cache is untouched, so tiles reload from WebP without re-decoding.
     */
    fun trimMemory(level: Int) {
        try {
            if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
                memoryCache.evictAll()
            } else if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
                memoryCache.trimToSize(MEMORY_CACHE_BYTES / 2)
            }
        } catch (_: Exception) {
        }
    }

    /** Halve the memory footprint (and GC pressure) of cached tiles: photos need no alpha. */
    private fun toMemoryEfficient(bitmap: Bitmap?): Bitmap? {
        if (bitmap == null) return null
        if (bitmap.config == Bitmap.Config.RGB_565) return bitmap
        return try {
            bitmap.copy(Bitmap.Config.RGB_565, false) ?: bitmap
        } catch (_: Exception) {
            bitmap
        }
    }

    private suspend fun decodeMediaThumbnail(uri: String, isVideo: Boolean, size: Int): Bitmap? {
        val context = appContext ?: return null
        val parsed = Uri.parse(uri)
        return try {
            if (parsed.scheme == "content") {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(parsed, android.util.Size(size, size), null)
                } else {
                    val contentId = parsed.lastPathSegment?.toLongOrNull()
                    if (contentId == null) {
                        decodeWithCoil(uri, size)
                    } else {
                        @Suppress("DEPRECATION")
                        val thumb = if (isVideo) {
                            MediaStore.Video.Thumbnails.getThumbnail(
                                context.contentResolver,
                                contentId,
                                MediaStore.Video.Thumbnails.MINI_KIND,
                                null
                            )
                        } else {
                            MediaStore.Images.Thumbnails.getThumbnail(
                                context.contentResolver,
                                contentId,
                                MediaStore.Images.Thumbnails.MINI_KIND,
                                null
                            )
                        }
                        thumb ?: decodeWithCoil(uri, size)
                    }
                }
            } else {
                decodeWithCoil(uri, size)
            }
        } catch (_: Exception) {
            decodeWithCoil(uri, size)
        }
    }

    private suspend fun decodeWithCoil(uri: String, size: Int): Bitmap? {
        val context = appContext ?: return null
        return try {
            val loader = context.imageLoader
            val request = ImageRequest.Builder(context)
                .data(uri)
                .size(size)
                .crossfade(false)
                .build()
            (loader.execute(request).drawable as? BitmapDrawable)?.bitmap
        } catch (_: Exception) {
            null
        }
    }
}