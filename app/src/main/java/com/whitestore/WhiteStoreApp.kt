package com.whitestore

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.whitestore.data.api.RuStoreApiService
import com.whitestore.data.installer.AppInstaller
import com.whitestore.data.network.NetworkClientProvider
import com.whitestore.data.updater.SelfUpdateManager
import okhttp3.OkHttpClient

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

        okHttpClient = NetworkClientProvider.createRuStoreClient(this)
        apiService = RuStoreApiService(okHttpClient)
        appInstaller = AppInstaller(this, okHttpClient)
        selfUpdateManager = SelfUpdateManager(this, okHttpClient, appInstaller)
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
            .build()
    }

    companion object {
        lateinit var instance: WhiteStoreApp
            private set
    }
}
