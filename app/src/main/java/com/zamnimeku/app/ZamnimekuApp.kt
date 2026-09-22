package com.zamnimeku.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import com.zamnimeku.app.data.notify.UpdateNotifier

class ZamnimekuApp : Application(), ImageLoaderFactory {

    companion object {
        lateinit var instance: ZamnimekuApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        UpdateNotifier.ensureChannel(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient {
                OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .addInterceptor { chain ->
                        val original = chain.request()
                        val host = original.url.host.lowercase()
                        val referer = if (host.contains("mydriveku") || host.contains("mynimeku")) {
                            "https://www.mynimeku.com/"
                        } else {
                            "https://otakudesu.blog/"
                        }
                        val req = original.newBuilder()
                            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                            .header("Referer", referer)
                            .build()
                        chain.proceed(req)
                    }
                    .build()
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(150L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
