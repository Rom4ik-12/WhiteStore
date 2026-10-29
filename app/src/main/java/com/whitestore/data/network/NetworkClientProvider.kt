package com.whitestore.data.network

import android.content.Context
import android.util.Log
import com.whitestore.R
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.InputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

object NetworkClientProvider {

    private const val TAG = "NetworkClientProvider"
    private const val USER_AGENT = "RuStore/3.14.0 (Android 14; Mobile; arm64-v8a)"
    private const val RUSTORE_VER_CODE = "12000"

    fun createRuStoreClient(context: Context): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d("RuStoreHttp", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .header("ruStoreVerCode", RUSTORE_VER_CODE)
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(loggingInterceptor)

        try {
            val cf = CertificateFactory.getInstance("X.509")
            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
                load(null, null)
            }

            var certCount = 0

            // 1. Load Russian Ministry CA certificates (for api-m.rustore.ru)
            fun loadCertificatesFromRaw(rawResId: Int, prefix: String) {
                try {
                    context.resources.openRawResource(rawResId).use { stream ->
                        val certs = cf.generateCertificates(stream)
                        for ((index, cert) in certs.withIndex()) {
                            if (cert is X509Certificate) {
                                keyStore.setCertificateEntry("${prefix}_$index", cert)
                                certCount++
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load cert resource $prefix", e)
                }
            }

            loadCertificatesFromRaw(R.raw.ministry_of_digital_root, "min_root")
            loadCertificatesFromRaw(R.raw.ministry_of_digital_sub_2024, "min_sub")
            loadCertificatesFromRaw(R.raw.harica_chain, "harica")

            Log.d(TAG, "Loaded $certCount custom certificates into KeyStore")

            // 2. Default system trust manager
            val systemTmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
                init(null as KeyStore?)
            }
            val systemTm = systemTmf.trustManagers.firstOrNull { it is X509TrustManager } as? X509TrustManager

            // 3. Custom trust manager
            val customTmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
                init(keyStore)
            }
            val customTm = customTmf.trustManagers.firstOrNull { it is X509TrustManager } as? X509TrustManager

            // 4. Combined TrustManager that checks both system and custom CAs
            val combinedTrustManager = object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                    try {
                        systemTm?.checkClientTrusted(chain, authType)
                    } catch (e: Exception) {
                        customTm?.checkClientTrusted(chain, authType) ?: throw e
                    }
                }

                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                    try {
                        systemTm?.checkServerTrusted(chain, authType)
                    } catch (systemEx: Exception) {
                        try {
                            customTm?.checkServerTrusted(chain, authType)
                        } catch (customEx: Exception) {
                            Log.e(TAG, "Server verification failed both system and custom trust managers", customEx)
                            throw systemEx
                        }
                    }
                }

                override fun getAcceptedIssuers(): Array<X509Certificate> {
                    val systemIssuers = systemTm?.acceptedIssuers ?: emptyArray()
                    val customIssuers = customTm?.acceptedIssuers ?: emptyArray()
                    return systemIssuers + customIssuers
                }
            }

            val sslContext = SSLContext.getInstance("TLS").apply {
                init(null, arrayOf(combinedTrustManager), null)
            }

            clientBuilder.sslSocketFactory(sslContext.socketFactory, combinedTrustManager)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up SSLContext", e)
        }

        return clientBuilder.build()
    }
}
