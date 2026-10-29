package com.whitestore.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GithubRelease(
    val tag_name: String = "",
    val name: String? = null,
    val body: String? = null,
    val html_url: String = "",
    val assets: List<GithubAsset> = emptyList()
)

@Serializable
data class GithubAsset(
    val name: String = "",
    val size: Long = 0L,
    val browser_download_url: String = ""
)
