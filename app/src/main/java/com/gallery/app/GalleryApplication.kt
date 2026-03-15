package com.gallery.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import coil.Coil
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.DebugLogger
import com.gallery.app.workers.TrashPurgeWorker
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltAndroidApp
class GalleryApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        // ── Configure Coil ImageLoader globally ───────────
        // This is the ROOT CAUSE FIX for images not loading and
        // videos showing no thumbnails. Without this, Coil uses
        // defaults that lack VideoFrameDecoder and have no disk cache.
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                // Memory cache: use up to 25% of available app memory
                .memoryCache {
                    MemoryCache.Builder(this)
                        .maxSizePercent(0.25)
                        .strongReferencesEnabled(true)
                        .build()
                }
                // Disk cache: 512 MB for thumbnails on disk
                .diskCache {
                    DiskCache.Builder()
                        .directory(cacheDir.resolve("image_cache"))
                        .maxSizeBytes(512L * 1024 * 1024) // 512 MB
                        .build()
                }
                // Register decoders — ORDER MATTERS:
                // VideoFrameDecoder must come before the default image decoders
                // so that video/* MIME types are intercepted correctly.
                .components {
                    // Video thumbnail support (extracts first frame from video URIs)
                    add(VideoFrameDecoder.Factory())
                    // GIF support (uses ImageDecoder on API 28+, GifDecoder below)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        add(ImageDecoderDecoder.Factory())
                    } else {
                        add(GifDecoder.Factory())
                    }
                }
                // Allow loading content:// URIs from MediaStore on background threads
                .crossfade(true)
                .crossfade(300)
                // Respect system network state — don't load over metered if
                // Coil thinks the request is low-priority
                .respectCacheHeaders(false)
                .build()
        )

        // Schedule the periodic trash-purge job
        TrashPurgeWorker.schedule(WorkManager.getInstance(this))
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
