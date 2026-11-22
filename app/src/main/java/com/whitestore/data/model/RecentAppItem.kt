package com.whitestore.data.model

import kotlinx.serialization.Serializable

@Serializable
data class RecentAppItem(
    val name: String,
    val packageName: String,
    val versionName: String,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val isUpdated: Boolean,
    val timestamp: Long,
    val isInRuStore: Boolean,
    val appSizeBytes: Long = 0L
)
