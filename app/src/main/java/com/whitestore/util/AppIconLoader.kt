package com.whitestore.util

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * High-performance AppIcon loader & memory cache.
 * Caches downscaled thumbnails (~120x120px) to prevent OutOfMemory and GC stutters
 * when scrolling lists with hundreds of installed applications.
 */
object AppIconLoader {
    // Cache up to 200 decoded app icons (~200 * 120 * 120 * 4 bytes ≈ 11.5 MB max)
    private val memoryCache = object : LruCache<String, ImageBitmap>(200) {}

    fun getCached(packageName: String): ImageBitmap? {
        return memoryCache.get(packageName)
    }

    suspend fun loadIcon(context: Context, packageName: String, targetSizePx: Int = 128): ImageBitmap? {
        memoryCache.get(packageName)?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val pm = context.packageManager
                val appInfo = pm.getApplicationInfo(packageName, 0)
                val drawable = pm.getApplicationIcon(appInfo)
                val bitmap = drawableToBitmap(drawable, targetSizePx, targetSizePx)
                val imageBitmap = bitmap.asImageBitmap()
                memoryCache.put(packageName, imageBitmap)
                imageBitmap
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun drawableToBitmap(drawable: Drawable, width: Int, height: Int): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            val src = drawable.bitmap
            if (src.width <= width && src.height <= height) {
                return src
            }
            return Bitmap.createScaledBitmap(src, width, height, true)
        }

        val bitmap = Bitmap.createBitmap(
            if (width > 0) width else 1,
            if (height > 0) height else 1,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}

@Composable
fun AppIcon(
    packageName: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    shapeRadius: Dp = 12.dp
) {
    val context = LocalContext.current
    var iconBitmap by remember(packageName) {
        mutableStateOf(AppIconLoader.getCached(packageName))
    }

    LaunchedEffect(packageName) {
        if (iconBitmap == null) {
            iconBitmap = AppIconLoader.loadIcon(context, packageName)
        }
    }

    val currentBitmap = iconBitmap
    if (currentBitmap != null) {
        Image(
            bitmap = currentBitmap,
            contentDescription = null,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(shapeRadius))
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(shapeRadius))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    }
}
