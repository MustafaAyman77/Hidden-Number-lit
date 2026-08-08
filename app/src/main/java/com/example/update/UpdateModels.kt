package com.example.update

import androidx.annotation.Keep

@Keep
data class UpdateManifest(
    val versionCode: Int = 1,
    val versionName: String = "1.0.0",
    val apkUrl: String = "",
    val size: String = "",
    val releaseNotes: List<String> = emptyList(),
    val mandatory: Boolean = false,
    val sha256: String? = null
)

sealed class UpdateUIState {
    object Idle : UpdateUIState()
    object Checking : UpdateUIState()
    
    data class Available(
        val manifest: UpdateManifest,
        val isWifi: Boolean,
        val currentVersionName: String,
        val currentVersionCode: Long
    ) : UpdateUIState()
    
    data class Downloading(
        val manifest: UpdateManifest,
        val progressPercent: Int,
        val downloadedFormatted: String,
        val totalFormatted: String
    ) : UpdateUIState()
    
    data class ReadyToInstall(
        val manifest: UpdateManifest,
        val apkFilePath: String
    ) : UpdateUIState()
    
    data class Error(
        val manifest: UpdateManifest?,
        val messageAr: String,
        val messageEn: String
    ) : UpdateUIState()
}
