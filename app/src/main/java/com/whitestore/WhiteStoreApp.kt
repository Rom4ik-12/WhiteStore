package com.whitestore

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.whitestore.data.api.RuStoreApiService
import com.whitestore.data.installer.AppInstaller
import com.whitestore.data.network.NetworkClientProvider
import com.whitestore.data.prefs.AppPreferences
import com.whitestore.data.updater.SelfUpdateManager
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath

class WhiteStoreApp : Application(), SingletonImageLoader.Factory {

    lateinit var okHttpClient: OkHttpClient
        private set

    lateinit var apiService: RuStoreApiService
        private set

    lateinit var appInstaller: AppInstaller
        private set

    lateinit var selfUpdateManager: SelfUpdateManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        AppPreferences.init(this)

        okHttpClient = NetworkClientProvider.createRuStoreClient(this)
        val standardClient = NetworkClientProvider.createStandardClient()
        apiService = RuStoreApiService(okHttpClient)
        appInstaller = AppInstaller(this, okHttpClient)
        selfUpdateManager = SelfUpdateManager(this, standardClient, appInstaller)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(
                    OkHttpNetworkFetcherFactory(
                        callFactory = { okHttpClient }
                    )
                )
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizePercent(0.05)
                    .build()
            }
            .build()
    }

    companion object {
        lateinit var instance: WhiteStoreApp
            private set
    }
}
