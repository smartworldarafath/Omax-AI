package me.proton.android.lumo.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import me.proton.android.lumo.BuildConfig
import me.proton.android.lumo.update.model.DownloadStatus
import me.proton.android.lumo.update.model.GitHubRelease
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val _downloadStatus = MutableStateFlow<DownloadStatus>(DownloadStatus.Idle)
    val downloadStatus: StateFlow<DownloadStatus> = _downloadStatus.asStateFlow()

    private val _recentReleases = MutableStateFlow<List<GitHubRelease>>(emptyList())
    val recentReleases: StateFlow<List<GitHubRelease>> = _recentReleases.asStateFlow()

    private val _latestRelease = MutableStateFlow<GitHubRelease?>(null)
    val latestRelease: StateFlow<GitHubRelease?> = _latestRelease.asStateFlow()

    private val _isUpdateAvailable = MutableStateFlow(false)
    val isUpdateAvailable: StateFlow<Boolean> = _isUpdateAvailable.asStateFlow()

    val currentVersion: String
        get() {
            return try {
                val pInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(context.packageName, 0)
                }
                pInfo.versionName ?: BuildConfig.VERSION_NAME
            } catch (e: Exception) {
                BuildConfig.VERSION_NAME
            }
        }

    val updatesDirectory: File
        get() {
            val dir = File(context.getExternalFilesDir(null), "updates")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    suspend fun checkForUpdates(): Result<List<GitHubRelease>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/smartworldarafath/Omax-AI/releases")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("User-Agent", "Omax-AI-Android-App")
                setRequestProperty("Accept", "application/vnd.github+json")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val releases = json.decodeFromString<List<GitHubRelease>>(responseText)

                _recentReleases.value = releases.take(5)

                val latest = releases.firstOrNull()
                _latestRelease.value = latest

                if (latest != null) {
                    val hasUpdate = isNewerVersion(latest.tagName, currentVersion)
                    _isUpdateAvailable.value = hasUpdate

                    // Check if already downloaded on disk
                    checkExistingDownload(latest)
                }

                Result.success(releases)
            } else {
                Timber.tag(TAG).e("GitHub API error: ${connection.responseCode}")
                Result.failure(Exception("HTTP ${connection.responseCode}: ${connection.responseMessage}"))
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error checking for updates")
            Result.failure(e)
        }
    }

    fun checkExistingDownload(release: GitHubRelease) {
        val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") } ?: return
        val apkFile = File(updatesDirectory, apkAsset.name)
        if (apkFile.exists() && apkFile.length() > 0) {
            if (apkAsset.size == 0L || apkFile.length() == apkAsset.size) {
                _downloadStatus.value = DownloadStatus.Downloaded(
                    filePath = apkFile.absolutePath,
                    versionName = release.tagName,
                    fileSizeBytes = apkFile.length()
                )
            }
        }
    }

    suspend fun startDownload(release: GitHubRelease) = withContext(Dispatchers.IO) {
        val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }
        if (apkAsset == null || apkAsset.browserDownloadUrl.isEmpty()) {
            _downloadStatus.value = DownloadStatus.Error("No APK file found in this release.")
            return@withContext
        }

        val targetFile = File(updatesDirectory, apkAsset.name)
        val tempFile = File(updatesDirectory, "${apkAsset.name}.tmp")

        // If complete file already exists, directly mark downloaded
        if (targetFile.exists() && (apkAsset.size == 0L || targetFile.length() == apkAsset.size)) {
            _downloadStatus.value = DownloadStatus.Downloaded(
                filePath = targetFile.absolutePath,
                versionName = release.tagName,
                fileSizeBytes = targetFile.length()
            )
            return@withContext
        }

        try {
            _downloadStatus.value = DownloadStatus.Downloading(
                progress = 0f,
                downloadedBytes = 0L,
                totalBytes = apkAsset.size
            )

            // Connect with redirects handling
            var currentUrl = apkAsset.browserDownloadUrl
            var connection: HttpURLConnection
            var redirectCount = 0

            while (true) {
                val urlObj = URL(currentUrl)
                connection = (urlObj.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true
                    connectTimeout = 30000
                    readTimeout = 30000
                    setRequestProperty("User-Agent", "Omax-AI-Android-App")
                }
                val code = connection.responseCode
                if (code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_MOVED_TEMP || code == 307 || code == 308) {
                    val location = connection.getHeaderField("Location")
                    if (location != null && redirectCount < 5) {
                        currentUrl = location
                        redirectCount++
                        continue
                    }
                }
                break
            }

            val totalBytes = if (apkAsset.size > 0) apkAsset.size else connection.contentLengthLong
            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(tempFile)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var downloadedBytes = 0L
            var lastUpdateTime = System.currentTimeMillis()
            var bytesSinceLastUpdate = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                downloadedBytes += bytesRead
                bytesSinceLastUpdate += bytesRead

                val now = System.currentTimeMillis()
                if (now - lastUpdateTime >= 100) {
                    val speed = if (now > lastUpdateTime) {
                        (bytesSinceLastUpdate * 1000) / (now - lastUpdateTime)
                    } else 0L
                    val progress = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
                    _downloadStatus.value = DownloadStatus.Downloading(
                        progress = progress,
                        downloadedBytes = downloadedBytes,
                        totalBytes = totalBytes,
                        speedBytesPerSec = speed
                    )
                    lastUpdateTime = now
                    bytesSinceLastUpdate = 0L
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            // Rename temp file to final APK
            if (targetFile.exists()) targetFile.delete()
            tempFile.renameTo(targetFile)

            _downloadStatus.value = DownloadStatus.Downloaded(
                filePath = targetFile.absolutePath,
                versionName = release.tagName,
                fileSizeBytes = targetFile.length()
            )
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Download failed")
            if (tempFile.exists()) tempFile.delete()
            _downloadStatus.value = DownloadStatus.Error(e.localizedMessage ?: "Download failed. Please check internet connection.")
        }
    }

    fun installApk(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) {
            _downloadStatus.value = DownloadStatus.Error("APK file not found on device.")
            return
        }

        try {
            // Check Android 8+ install permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val permissionIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(permissionIntent)
                    return
                }
            }

            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to launch package installer")
            _downloadStatus.value = DownloadStatus.Error("Unable to open installer: ${e.message}")
        }
    }

    private fun isNewerVersion(remoteTag: String, currentVer: String): Boolean {
        val cleanRemote = remoteTag.removePrefix("v").substringBefore("-").trim()
        val cleanCurrent = currentVer.removePrefix("v").substringBefore("-").trim()

        val remoteParts = cleanRemote.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    companion object {
        const val TAG = "UpdateManager"
    }
}
