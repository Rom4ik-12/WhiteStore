package com.whitestore.data.updater

import android.content.Context
import android.os.Environment
import android.util.Log
import com.whitestore.data.installer.AppInstaller
import com.whitestore.data.model.GithubRelease
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

sealed class SelfUpdateState {
    object Idle : SelfUpdateState()
    object Checking : SelfUpdateState()
    object UpToDate : SelfUpdateState()
    data class UpdateAvailable(
        val latestVersion: String,
        val releaseNotes: String,
        val downloadUrl: String,
        val fileSize: Long
    ) : SelfUpdateState()
    data class Downloading(val progress: Float) : SelfUpdateState()
    data class Error(val message: String) : SelfUpdateState()
}

class SelfUpdateManager(
    private val context: Context,
    private val client: OkHttpClient,
    private val appInstaller: AppInstaller
) {
    private val TAG = "SelfUpdateManager"
    private val GITHUB_REPO = "Rom4ik-12/WhiteStore"
    private val CURRENT_VERSION: String
        get() = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.2"
        } catch (e: Exception) {
            "1.0.2"
        }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val _updateState = MutableStateFlow<SelfUpdateState>(SelfUpdateState.Idle)
    val updateState: StateFlow<SelfUpdateState> = _updateState.asStateFlow()

    suspend fun checkForUpdates(): SelfUpdateState = withContext(Dispatchers.IO) {
        _updateState.value = SelfUpdateState.Checking
        val startTime = System.currentTimeMillis()
        try {
            val url = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "WhiteStoreApp")
                .build()

            val response = client.newCall(request).execute()

            // Ensure spinner is visible at least 600ms so user clearly perceives the check
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed < 600) {
                kotlinx.coroutines.delay(600 - elapsed)
            }

            if (!response.isSuccessful) {
                if (response.code == 404) {
                    _updateState.value = SelfUpdateState.UpToDate
                    return@withContext SelfUpdateState.UpToDate
                }
                val err = "Ошибка проверки обновлений: HTTP ${response.code}"
                _updateState.value = SelfUpdateState.Error(err)
                return@withContext SelfUpdateState.Error(err)
            }

            val stringBody = response.body?.string() ?: ""
            val release = json.decodeFromString<GithubRelease>(stringBody)
            val latestTag = release.tag_name.removePrefix("v").trim()

            if (isNewerVersion(latestTag, CURRENT_VERSION)) {
                val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
                if (apkAsset != null) {
                    val state = SelfUpdateState.UpdateAvailable(
                        latestVersion = latestTag,
                        releaseNotes = release.body ?: release.name ?: "Обновление WhiteStore",
                        downloadUrl = apkAsset.browser_download_url,
                        fileSize = apkAsset.size
                    )
                    _updateState.value = state
                    return@withContext state
                }
            }

            _updateState.value = SelfUpdateState.UpToDate
            SelfUpdateState.UpToDate
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed < 600) {
                kotlinx.coroutines.delay(600 - elapsed)
            }
            Log.e(TAG, "Error checking self-update", e)
            val state = SelfUpdateState.Error(e.message ?: "Сетевая ошибка")
            _updateState.value = state
            state
        }
    }

    suspend fun downloadAndInstallUpdate(downloadUrl: String) = withContext(Dispatchers.IO) {
        _updateState.value = SelfUpdateState.Downloading(0f)
        try {
            val request = Request.Builder().url(downloadUrl).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful || response.body == null) {
                _updateState.value = SelfUpdateState.Error("HTTP ${response.code}")
                return@withContext
            }

            val body = response.body!!
            val contentLength = body.contentLength()
            val apkDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "apks")
            if (!apkDir.exists()) apkDir.mkdirs()

            val apkFile = File(apkDir, "WhiteStore_update.apk")
            if (apkFile.exists()) apkFile.delete()

            val inputStream: InputStream = body.byteStream()
            val outputStream = FileOutputStream(apkFile)
            val buffer = ByteArray(8192)
            var totalRead: Long = 0
            var bytesRead: Int
            var lastUpdate = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalRead += bytesRead
                val now = System.currentTimeMillis()
                if (now - lastUpdate > 100 || totalRead == contentLength) {
                    val progress = if (contentLength > 0) (totalRead.toFloat() / contentLength) else 0f
                    _updateState.value = SelfUpdateState.Downloading(progress)
                    lastUpdate = now
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            _updateState.value = SelfUpdateState.Idle
            appInstaller.promptInstall(apkFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download update", e)
            _updateState.value = SelfUpdateState.Error(e.message ?: "Ошибка скачивания")
        }
    }

    private fun isNewerVersion(remote: String, current: String): Boolean {
        try {
            val rParts = remote.split(".").map { it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
            val cParts = current.split(".").map { it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
            val maxLen = maxOf(rParts.size, cParts.size)
            for (i in 0 until maxLen) {
                val r = rParts.getOrElse(i) { 0 }
                val c = cParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
        } catch (e: Exception) {
            return false
        }
        return false
    }
}
