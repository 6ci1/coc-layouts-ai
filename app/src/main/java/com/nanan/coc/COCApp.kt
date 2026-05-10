package com.nanan.coc

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File

@HiltAndroidApp
class COCApp : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        // HTTP 层磁盘缓存（100MB），Coil 内部 disk cache 用于解码后图片
        val httpCacheDir = File(cacheDir, "http_image_cache")
        val httpCache = Cache(httpCacheDir, 100L * 1024 * 1024)

        val okHttpClient = OkHttpClient.Builder()
            .cache(httpCache)
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100L * 1024 * 1024)
                    .build()
            }
            .respectCacheHeaders(false)
            .build()
    }
}
