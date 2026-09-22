package com.example

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.data.ThumbnailCache

class GalleryApplication : Application(), ImageLoaderFactory, ComponentCallbacks2 {

    override fun onCreate() {
        super.onCreate()
        ThumbnailCache.init(this)
        // No registerComponentCallbacks needed: Application already receives
        // onTrimMemory/onLowMemory directly from the system.
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    override fun onLowMemory() {
        ThumbnailCache.trimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        ThumbnailCache.trimMemory(level)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil_cache"))
                    .maxSizeBytes(250L * 1024 * 1024)
                    .build()
            }
            .respectCacheHeaders(false)
            .crossfade(false)
            .build()
    }
}
