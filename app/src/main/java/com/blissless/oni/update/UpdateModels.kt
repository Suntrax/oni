package com.blissless.oni.update

import com.google.gson.annotations.SerializedName

data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String = "",
    val name: String = "",
    val body: String = "",
    val assets: List<ReleaseAsset> = emptyList()
)

data class ReleaseAsset(
    val name: String = "",
    @SerializedName("browser_download_url") val downloadUrl: String = "",
    val size: Long = 0
)
