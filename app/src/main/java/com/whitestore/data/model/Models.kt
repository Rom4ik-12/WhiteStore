package com.whitestore.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val code: String? = null,
    val message: String? = null,
    val body: T? = null
)

@Serializable
data class SearchSuggestBody(
    val suggests: List<SuggestItem> = emptyList(),
    val history: List<String> = emptyList(),
    val suggestQueryId: String? = null
)

@Serializable
data class SuggestItem(
    val text: String = "",
    val textMarked: String? = null,
    val packageName: String? = null,
    val iconUrl: String? = null,
    val versionId: Long? = null
)

@Serializable
data class SearchAppsBody(
    val content: List<AppSummaryItem> = emptyList(),
    val totalElements: Long? = null,
    val totalPages: Int? = null,
    val pageNumber: Int? = null,
    val pageSize: Int? = null
)

@Serializable
data class AppSummaryItem(
    val appId: Long = 0,
    val packageName: String = "",
    val appName: String = "",
    val shortDescription: String? = null,
    val iconUrl: String? = null,
    val appType: String? = null,
    val versionCode: Long? = null,
    val price: Long? = null,
    val averageUserRating: Double? = null,
    val totalRatings: Long? = null,
    val roundedDownloadsText: String? = null,
    val companyName: String? = null
)

@Serializable
data class AppDetail(
    val appId: Long = 0,
    val packageName: String = "",
    val appName: String = "",
    val companyName: String? = null,
    val categories: List<String> = emptyList(),
    val shortDescription: String? = null,
    val fullDescription: String? = null,
    val fileSize: Long? = null,
    val versionName: String? = null,
    val versionCode: Long? = null,
    val appType: String? = null,
    val downloads: Long? = null,
    val price: Long? = null,
    val appVerUpdatedAt: String? = null,
    val iconUrl: String? = null,
    val whatsNew: String? = null,
    val fileUrls: List<AppScreenshot> = emptyList(),
    val ageRestriction: AgeRestriction? = null
)

@Serializable
data class AppScreenshot(
    val fileUrl: String = "",
    val ordinal: Int = 0,
    val type: String? = null,
    val orientation: String? = null
)

@Serializable
data class AgeRestriction(
    val category: String? = null,
    val name: String? = null,
    val description: String? = null,
    val imageUrl: String? = null
)

@Serializable
data class DownloadLinkRequest(
    val appId: Long,
    val firstInstall: Boolean = true
)

@Serializable
data class DownloadLinkBody(
    val appId: Long? = null,
    val apkUrl: String? = null,
    val versionCode: Long? = null
)
