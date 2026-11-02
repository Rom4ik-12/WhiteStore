package com.whitestore.data.model

import androidx.compose.ui.graphics.ImageBitmap

data class AppUpdateInfo(
    val appId: Long,
    val packageName: String,
    val appName: String,
    val currentVersionCode: Long,
    val currentVersionName: String,
    val targetVersionCode: Long,
    val targetVersionName: String,
    val fileSize: Long,
    val iconUrl: String?,
    val localIconBitmap: ImageBitmap? = null
)
