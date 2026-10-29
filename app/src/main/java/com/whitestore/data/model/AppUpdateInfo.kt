package com.whitestore.data.model

import android.graphics.drawable.Drawable

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
    val localIcon: Drawable? = null
)
