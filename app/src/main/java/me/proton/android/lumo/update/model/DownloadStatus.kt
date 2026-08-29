package me.proton.android.lumo.update.model

sealed interface DownloadStatus {
    data object Idle : DownloadStatus
    data class Downloading(
        val progress: Float,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val speedBytesPerSec: Long = 0L
    ) : DownloadStatus
    data class Downloaded(
        val filePath: String,
        val versionName: String,
        val fileSizeBytes: Long
    ) : DownloadStatus
    data class Error(val message: String) : DownloadStatus
}
