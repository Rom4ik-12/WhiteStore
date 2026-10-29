package com.whitestore.data.api

import android.util.Log
import com.whitestore.data.model.ApiResponse
import com.whitestore.data.model.AppDetail
import com.whitestore.data.model.DownloadLinkBody
import com.whitestore.data.model.DownloadLinkRequest
import com.whitestore.data.model.SearchAppsBody
import com.whitestore.data.model.SearchSuggestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class RuStoreApiService(
    private val client: OkHttpClient
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val backapiHost = "https://backapi.rustore.ru"

    suspend fun getSearchSuggestions(query: String): SearchSuggestBody = withContext(Dispatchers.IO) {
        try {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) return@withContext SearchSuggestBody()

            val httpUrl = "$backapiHost/search/suggest".toHttpUrl().newBuilder()
                .addQueryParameter("query", trimmed)
                .build()

            val request = Request.Builder().url(httpUrl).get().build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e("RuStoreApiService", "getSearchSuggestions failed: HTTP ${response.code}")
                return@withContext SearchSuggestBody()
            }
            val stringBody = response.body?.string() ?: return@withContext SearchSuggestBody()
            val parsed = json.decodeFromString<ApiResponse<SearchSuggestBody>>(stringBody)
            parsed.body ?: SearchSuggestBody()
        } catch (e: Exception) {
            Log.e("RuStoreApiService", "getSearchSuggestions error", e)
            SearchSuggestBody()
        }
    }

    suspend fun searchApps(query: String, pageNumber: Int = 0, pageSize: Int = 20): SearchAppsBody = withContext(Dispatchers.IO) {
        try {
            val trimmed = query.trim().ifEmpty { "приложения" }

            val httpUrl = "$backapiHost/applicationData/apps".toHttpUrl().newBuilder()
                .addQueryParameter("query", trimmed)
                .addQueryParameter("pageNumber", pageNumber.toString())
                .addQueryParameter("pageSize", pageSize.toString())
                .build()

            val request = Request.Builder().url(httpUrl).get().build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e("RuStoreApiService", "searchApps failed: HTTP ${response.code}")
                return@withContext SearchAppsBody()
            }
            val stringBody = response.body?.string() ?: return@withContext SearchAppsBody()
            val parsed = json.decodeFromString<ApiResponse<SearchAppsBody>>(stringBody)
            parsed.body ?: SearchAppsBody()
        } catch (e: Exception) {
            Log.e("RuStoreApiService", "searchApps error", e)
            SearchAppsBody()
        }
    }

    suspend fun getAppDetail(packageName: String): AppDetail? = withContext(Dispatchers.IO) {
        try {
            val trimmed = packageName.trim()
            if (trimmed.isEmpty()) return@withContext null

            val httpUrl = "$backapiHost/applicationData/overallInfo/$trimmed".toHttpUrl()
            val request = Request.Builder().url(httpUrl).get().build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e("RuStoreApiService", "getAppDetail failed: HTTP ${response.code}")
                return@withContext null
            }
            val stringBody = response.body?.string() ?: return@withContext null
            val parsed = json.decodeFromString<ApiResponse<AppDetail>>(stringBody)
            parsed.body
        } catch (e: Exception) {
            Log.e("RuStoreApiService", "getAppDetail error", e)
            null
        }
    }

    suspend fun getDownloadLink(appId: Long): String? = withContext(Dispatchers.IO) {
        try {
            val url = "$backapiHost/applicationData/download-link"
            val payload = DownloadLinkRequest(appId = appId, firstInstall = true)
            val jsonString = json.encodeToString(payload)
            Log.d("RuStoreApiService", "getDownloadLink request: $jsonString")
            val bodyContent = jsonString.toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .post(bodyContent)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errBody = response.body?.string()
                Log.e("RuStoreApiService", "getDownloadLink failed: HTTP ${response.code}, body: $errBody")
                return@withContext null
            }
            val stringBody = response.body?.string() ?: return@withContext null
            Log.d("RuStoreApiService", "getDownloadLink response: $stringBody")
            val parsed = json.decodeFromString<ApiResponse<DownloadLinkBody>>(stringBody)
            parsed.body?.apkUrl
        } catch (e: Exception) {
            Log.e("RuStoreApiService", "getDownloadLink error", e)
            null
        }
    }
}
