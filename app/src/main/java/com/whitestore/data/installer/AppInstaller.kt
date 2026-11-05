package com.whitestore.data.installer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.whitestore.data.prefs.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

sealed class InstallState {
    object Idle : InstallState()
    object Resolving : InstallState() // Fetching direct download URL
    data class Downloading(val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : InstallState()
    object Extracting : InstallState() // Unpacking APK from ZIP
    object ReadyToInstall : InstallState()
    data class Error(val message: String) : InstallState()
}

class AppInstaller(
    private val context: Context,
    private val client: OkHttpClient
) {
    private val TAG = "AppInstaller"

    private val apkFileMap = java.util.concurrent.ConcurrentHashMap<String, File>()

    private val _installState = MutableStateFlow<Map<String, InstallState>>(emptyMap())
    val installState: StateFlow<Map<String, InstallState>> = _installState.asStateFlow()

    // State flow notifying UI whenever any package changes (install/uninstall)
    private val _packageUpdateTrigger = MutableStateFlow<Long>(0)
    val packageUpdateTrigger: StateFlow<Long> = _packageUpdateTrigger.asStateFlow()

    @Volatile
    private var installedPackagesCache: Set<String> = emptySet()

    init {
        refreshInstalledPackages()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                refreshInstalledPackages()
                val installedPkg = intent?.data?.schemeSpecificPart
                if (installedPkg != null) {
                    // Reset install state back to Idle when app was added or replaced (updated)
                    updateState(installedPkg, InstallState.Idle)

                    // Optional automatic cleanup of the installed APK file
                    if (AppPreferences.deleteApkAfterInstall.value) {
                        apkFileMap.remove(installedPkg)?.let { file ->
                            if (file.exists()) file.delete()
                        }
                    }

                    if (intent.action == Intent.ACTION_PACKAGE_ADDED && AppPreferences.openAfterInstall.value) {
                        openApp(installedPkg)
                    }
                }
                _packageUpdateTrigger.value = System.currentTimeMillis()
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
    }

    private fun refreshInstalledPackages() {
        try {
            val packages = context.packageManager.getInstalledApplications(0)
            installedPackagesCache = packages.map { it.packageName }.toSet()
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing installed packages cache", e)
        }
    }

    fun isAppInstalled(packageName: String): Boolean {
        if (installedPackagesCache.contains(packageName)) return true
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0)
            }
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    fun canRequestPackageInstalls(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    fun setResolving(packageName: String) {
        updateState(packageName, InstallState.Resolving)
    }

    suspend fun downloadAndInstall(
        packageName: String,
        downloadUrl: String,
        appName: String,
        saveOnly: Boolean = false,
        versionName: String? = null
    ) = withContext(Dispatchers.IO) {
        updateState(packageName, InstallState.Downloading(0f, 0, 0))

        try {
            Log.d(TAG, "Starting download for $appName ($packageName): $downloadUrl (saveOnly=$saveOnly)")
            val request = Request.Builder().url(downloadUrl).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful || response.body == null) {
                val errorMsg = "Ошибка скачивания: HTTP ${response.code}"
                Log.e(TAG, errorMsg)
                updateState(packageName, InstallState.Error(errorMsg))
                showToast("Не удалось скачать: HTTP ${response.code}")
                return@withContext
            }

            val body = response.body!!
            val contentLength = body.contentLength()

            val apkDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "apks")
            if (!apkDir.exists()) apkDir.mkdirs()

            val isZip = downloadUrl.contains(".zip", ignoreCase = true)
            val downloadedFile = File(apkDir, if (isZip) "$packageName.zip" else "$packageName.apk")
            if (downloadedFile.exists()) downloadedFile.delete()

            val inputStream: InputStream = body.byteStream()
            val outputStream = FileOutputStream(downloadedFile)

            val buffer = ByteArray(8192)
            var totalRead: Long = 0
            var bytesRead: Int
            var lastUpdateMs = 0L

            var speedLimitBytesPerSec = AppPreferences.downloadSpeedLimitKb.value * 1024L
            var windowStartMs = System.currentTimeMillis()
            var windowBytesRead = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalRead += bytesRead
                windowBytesRead += bytesRead

                // Rate limiting throttler
                if (speedLimitBytesPerSec > 0) {
                    val nowMs = System.currentTimeMillis()
                    val elapsedMs = nowMs - windowStartMs
                    val expectedMs = (windowBytesRead * 1000L) / speedLimitBytesPerSec
                    if (expectedMs > elapsedMs) {
                        val sleepMs = expectedMs - elapsedMs
                        if (sleepMs > 0) {
                            try {
                                Thread.sleep(sleepMs.coerceAtMost(500L))
                            } catch (e: InterruptedException) {
                                // proceed
                            }
                        }
                    }
                    if (elapsedMs >= 1000L) {
                        windowStartMs = System.currentTimeMillis()
                        windowBytesRead = 0L
                        // Dynamic recheck in case preference changed mid-download
                        speedLimitBytesPerSec = AppPreferences.downloadSpeedLimitKb.value * 1024L
                    }
                }

                val now = System.currentTimeMillis()
                if (now - lastUpdateMs > 100 || totalRead == contentLength) {
                    val progress = if (contentLength > 0) (totalRead.toFloat() / contentLength) else 0f
                    updateState(packageName, InstallState.Downloading(progress, totalRead, contentLength))
                    lastUpdateMs = now
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            val targetApkFile = if (isZip) {
                updateState(packageName, InstallState.Extracting)
                val extracted = extractApkFromZip(downloadedFile, apkDir, packageName)
                // Remove the zip archive immediately to free up device cache space
                try {
                    if (downloadedFile.exists()) {
                        downloadedFile.delete()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to delete zip after extraction", e)
                }
                extracted ?: downloadedFile
            } else {
                downloadedFile
            }

            apkFileMap[packageName] = targetApkFile

            if (saveOnly) {
                // Save directly to user device's Downloads directory
                val verSuffix = if (!versionName.isNullOrBlank()) "_$versionName" else ""
                val savedFile = com.whitestore.util.StorageManager.saveApkToDownloads(
                    context,
                    targetApkFile,
                    "${appName}${verSuffix}.apk"
                )
                updateState(packageName, InstallState.Idle)
                if (savedFile != null) {
                    showToast("APK сохранён в Загрузки:\n${savedFile.name}")
                } else {
                    showToast("APK сохранён в хранилище приложения")
                }
            } else {
                updateState(packageName, InstallState.ReadyToInstall)
                promptInstall(targetApkFile)
            }

            // Check if storage cache limit is exceeded and prune oldest if necessary, protecting targetApkFile
            com.whitestore.util.StorageManager.pruneApksIfNeeded(context, excludeFile = targetApkFile)
        } catch (e: Exception) {
            Log.e(TAG, "Download error", e)
            val msg = e.localizedMessage ?: "Ошибка скачивания"
            updateState(packageName, InstallState.Error(msg))
            showToast("Ошибка: $msg")
        }
    }

    private fun extractApkFromZip(zipFile: File, outputDir: File, packageName: String): File? {
        try {
            val extractedApk = File(outputDir, "$packageName.apk")
            if (extractedApk.exists()) extractedApk.delete()

            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.name.endsWith(".apk", ignoreCase = true)) {
                        FileOutputStream(extractedApk).use { fos ->
                            val buf = ByteArray(8192)
                            var len: Int
                            while (zis.read(buf).also { len = it } > 0) {
                                fos.write(buf, 0, len)
                            }
                            fos.flush()
                        }
                        return extractedApk
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract APK from zip", e)
        }
        return null
    }

    fun getApkFile(packageName: String): File? {
        val file = apkFileMap[packageName]
        if (file != null && file.exists()) return file
        val apkDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "apks")
        val fallback = File(apkDir, "$packageName.apk")
        return if (fallback.exists()) fallback else null
    }

    fun installDownloadedApk(packageName: String) {
        val apkFile = apkFileMap[packageName]
        if (apkFile != null && apkFile.exists()) {
            promptInstall(apkFile)
        }
    }

    fun promptInstall(apkFile: File) {
        try {
            if (!canRequestPackageInstalls()) {
                showToast("Разрешите установку неизвестных приложений для WhiteStore")
                openInstallPermissionSettings()
                return
            }

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "promptInstall error", e)
            showToast("Ошибка запуска установщика: ${e.message}")
        }
    }

    fun openApp(packageName: String) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } else {
                showToast("Не удалось открыть приложение")
            }
        } catch (e: Exception) {
            Log.e(TAG, "openApp error", e)
        }
    }

    fun uninstallApp(packageName: String) {
        try {
            Log.d(TAG, "Initiating uninstallation for $packageName")
            // Try ACTION_UNINSTALL_PACKAGE first (standard uninstaller prompt)
            val uninstallIntent = Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
                data = Uri.fromParts("package", packageName, null)
                putExtra(Intent.EXTRA_RETURN_RESULT, false)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (uninstallIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(uninstallIntent)
                return
            }

            // Fallback to ACTION_DELETE
            val deleteIntent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.fromParts("package", packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(deleteIntent)
        } catch (e: Exception) {
            Log.e(TAG, "uninstallApp error, fallback to application settings", e)
            try {
                // Secondary fallback: open App Info in Settings where user can click Uninstall
                val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(settingsIntent)
            } catch (e2: Exception) {
                showToast("Не удалось запустить удаление: ${e.message}")
            }
        }
    }

    fun updateState(packageName: String, state: InstallState) {
        val current = _installState.value.toMutableMap()
        current[packageName] = state
        _installState.value = current
    }

    private fun showToast(text: String) {
        kotlinx.coroutines.CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }
}
