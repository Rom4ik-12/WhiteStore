package com.whitestore.util

import android.content.Context
import android.os.Environment
import com.whitestore.data.prefs.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class CachedApkItem(
    val file: File,
    val name: String,
    val sizeBytes: Long,
    val lastModified: Long
)

data class StorageUsage(
    val apksSizeBytes: Long,
    val imagesCacheSizeBytes: Long,
    val totalSizeBytes: Long,
    val apkFiles: List<CachedApkItem>
)

object StorageManager {

    fun getApkDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "apks")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    suspend fun getStorageUsage(context: Context): StorageUsage = withContext(Dispatchers.IO) {
        val apkDir = getApkDir(context)
        val apkFilesList = mutableListOf<CachedApkItem>()
        var apksSize = 0L

        if (apkDir.exists()) {
            apkDir.listFiles()?.forEach { file ->
                if (file.isFile && (file.name.endsWith(".apk", ignoreCase = true) || file.name.endsWith(".zip", ignoreCase = true))) {
                    val len = file.length()
                    apksSize += len
                    apkFilesList.add(
                        CachedApkItem(
                            file = file,
                            name = file.name,
                            sizeBytes = len,
                            lastModified = file.lastModified()
                        )
                    )
                }
            }
        }

        apkFilesList.sortByDescending { it.lastModified }

        val imageCacheDir = context.cacheDir.resolve("image_cache")
        val imageCacheSize = getFolderSize(imageCacheDir)

        StorageUsage(
            apksSizeBytes = apksSize,
            imagesCacheSizeBytes = imageCacheSize,
            totalSizeBytes = apksSize + imageCacheSize,
            apkFiles = apkFilesList
        )
    }

    private fun getFolderSize(file: File?): Long {
        if (file == null || !file.exists()) return 0L
        if (file.isFile) return file.length()
        var size = 0L
        file.listFiles()?.forEach { child ->
            size += getFolderSize(child)
        }
        return size
    }

    suspend fun deleteApk(file: File): Boolean = withContext(Dispatchers.IO) {
        try {
            if (file.exists()) file.delete() else false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun clearAllApks(context: Context): Long = withContext(Dispatchers.IO) {
        val apkDir = getApkDir(context)
        var freed = 0L
        if (apkDir.exists()) {
            apkDir.listFiles()?.forEach {
                val len = it.length()
                if (it.delete()) {
                    freed += len
                }
            }
        }
        freed
    }

    suspend fun clearImageCache(context: Context): Long = withContext(Dispatchers.IO) {
        val cacheDir = context.cacheDir.resolve("image_cache")
        val size = getFolderSize(cacheDir)
        try {
            cacheDir.deleteRecursively()
            cacheDir.mkdirs()
        } catch (e: Exception) {
            // ignore
        }
        size
    }

    /**
     * Checks if cached APKs exceed the user-defined max cache size limit.
     * If exceeded, deletes oldest APKs until within limit.
     */
    suspend fun pruneApksIfNeeded(context: Context) = withContext(Dispatchers.IO) {
        val maxLimitMb = AppPreferences.maxCacheSizeMb.value
        if (maxLimitMb <= 0) return@withContext // 0 means Unlimited

        val maxBytes = maxLimitMb.toLong() * 1024L * 1024L
        val apkDir = getApkDir(context)
        if (!apkDir.exists()) return@withContext

        val files = apkDir.listFiles()?.filter {
            it.isFile && (it.name.endsWith(".apk", ignoreCase = true) || it.name.endsWith(".zip", ignoreCase = true))
        }?.sortedBy { it.lastModified() } ?: return@withContext

        var currentTotal = files.sumOf { it.length() }
        for (file in files) {
            if (currentTotal <= maxBytes) break
            val len = file.length()
            if (file.delete()) {
                currentTotal -= len
            }
        }
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 Б"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(java.util.Locale.getDefault(), "%.2f ГБ", gb)
            mb >= 1.0 -> String.format(java.util.Locale.getDefault(), "%.1f МБ", mb)
            kb >= 1.0 -> String.format(java.util.Locale.getDefault(), "%.1f КБ", kb)
            else -> "$bytes Б"
        }
    }

    private fun transliterate(text: String): String {
        val map = mapOf(
            'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d",
            'е' to "e", 'ё' to "yo", 'ж' to "zh", 'з' to "z", 'и' to "i",
            'й' to "y", 'к' to "k", 'л' to "l", 'м' to "m", 'н' to "n",
            'о' to "o", 'п' to "p", 'р' to "r", 'с' to "s", 'т' to "t",
            'у' to "u", 'ф' to "f", 'х' to "kh", 'ц' to "ts", 'ч' to "ch",
            'ш' to "sh", 'щ' to "sch", 'ъ' to "", 'ы' to "y", 'ь' to "",
            'э' to "e", 'ю' to "yu", 'я' to "ya",
            'А' to "A", 'Б' to "B", 'В' to "V", 'Г' to "G", 'Д' to "D",
            'Е' to "E", 'Ё' to "Yo", 'Ж' to "Zh", 'З' to "Z", 'И' to "I",
            'Й' to "Y", 'К' to "K", 'Л' to "L", 'М' to "M", 'Н' to "N",
            'О' to "O", 'П' to "P", 'Р' to "R", 'С' to "S", 'Т' to "T",
            'У' to "U", 'Ф' to "F", 'Х' to "Kh", 'Ц' to "Ts", 'Ч' to "Ch",
            'Ш' to "Sh", 'Щ' to "Sch", 'Ъ' to "", 'Ы' to "Y", 'Ь' to "",
            'Э' to "E", 'Ю' to "Yu", 'Я' to "Ya"
        )
        val sb = StringBuilder()
        for (ch in text) {
            val trans = map[ch]
            if (trans != null) {
                sb.append(trans)
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Saves/copies a cached APK file directly into the public Downloads directory.
     * Returns the destination File or null on failure.
     */
    suspend fun saveApkToDownloads(context: Context, sourceFile: File, targetFileName: String): File? = withContext(Dispatchers.IO) {
        try {
            if (!sourceFile.exists()) return@withContext null
            val publicDownloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!publicDownloadsDir.exists()) publicDownloadsDir.mkdirs()

            val transliterated = transliterate(targetFileName)
            val sanitizedName = transliterated
                .replace(Regex("[\\s/\\\\:;*?\"<>|]+"), "_")
                .replace(Regex("[^a-zA-Z0-9._-]"), "_")
                .replace(Regex("_+"), "_")
                .trim('_')

            val baseName = if (sanitizedName.endsWith(".apk", ignoreCase = true)) sanitizedName else "$sanitizedName.apk"
            val destFile = File(publicDownloadsDir, baseName)

            sourceFile.inputStream().use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            destFile
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Shares an APK file via Android system share sheet.
     */
    fun shareApk(context: Context, apkFile: File) {
        try {
            val contentUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(android.content.Intent.EXTRA_STREAM, contentUri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(android.content.Intent.createChooser(shareIntent, "Сохранить / поделиться APK"))
        } catch (e: Exception) {
            // ignore or log
        }
    }
}
