package com.whitestore.data.network

import android.content.Context
import android.os.Build
import android.util.Log
import com.whitestore.R
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.InputStream
import java.net.Socket
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.*

object NetworkClientProvider {

    private const val TAG = "NetworkClientProvider"
    private const val USER_AGENT = "RuStore/3.14.0 (Android 14; Mobile; arm64-v8a)"
    private const val RUSTORE_VER_CODE = "12000"

    /**
     * Standard client for general network operations (e.g. GitHub Releases API).
     */
    fun createStandardClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d("StandardHttp", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        return OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    /**
     * Client tailored for RuStore API with Ministry of Digital Development SSL support.
     */
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

            val customTmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
                init(keyStore)
            }
            val customTm = customTmf.trustManagers.firstOrNull { it is X509TrustManager } as? X509TrustManager

            val systemTmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
                init(null as KeyStore?)
            }
            val systemTm = systemTmf.trustManagers.firstOrNull { it is X509TrustManager } as? X509TrustManager

            // Combined X509ExtendedTrustManager for Android Conscrypt RootTrustManager compatibility
            val combinedTrustManager = object : X509ExtendedTrustManager() {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?, socket: Socket?) {
                    checkClientTrusted(chain, authType)
                }

                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?, engine: SSLEngine?) {
                    checkClientTrusted(chain, authType)
                }

                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                    try {
                        systemTm?.checkClientTrusted(chain, authType)
                    } catch (e: Exception) {
                        customTm?.checkClientTrusted(chain, authType) ?: throw e
                    }
                }

                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?, socket: Socket?) {
                    checkServerTrusted(chain, authType)
                }

                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?, engine: SSLEngine?) {
                    checkServerTrusted(chain, authType)
                }

                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                    try {
                        systemTm?.checkServerTrusted(chain, authType)
                    } catch (systemEx: Exception) {
                        try {
                            customTm?.checkServerTrusted(chain, authType)
                        } catch (customEx: Exception) {
                            throw systemEx
                        }
                    }
                }

                override fun getAcceptedIssuers(): Array<X509Certificate> {
                    val s = systemTm?.acceptedIssuers ?: emptyArray()
                    val c = customTm?.acceptedIssuers ?: emptyArray()
                    return s + c
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
