package com.example.update

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

class AppUpdateManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val _updateState = MutableStateFlow<UpdateUIState>(UpdateUIState.Idle)
    val updateState: StateFlow<UpdateUIState> = _updateState.asStateFlow()

    private var skippedVersionCode: Long = -1L
    private var downloadJob: Job? = null

    fun checkForUpdates(manualTrigger: Boolean = false) {
        scope.launch(Dispatchers.IO) {
            try {
                if (!isNetworkAvailable()) {
                    Log.d("AppUpdateManager", "No network connection. Skipping update check.")
                    if (manualTrigger) {
                        _updateState.value = UpdateUIState.Error(
                            manifest = null,
                            messageAr = "لا يوجد اتصال بالإنترنت للتحقق من التحديثات.",
                            messageEn = "No internet connection to check for updates."
                        )
                    }
                    return@launch
                }

                val currentVersionCode = getCurrentVersionCode()
                val currentVersionName = getCurrentVersionName()

                // Fetch update manifest from UPDATE_MANIFEST_URL
                val jsonString = fetchManifestJson(UpdateConfig.UPDATE_MANIFEST_URL)
                if (jsonString.isNullOrEmpty()) {
                    Log.d("AppUpdateManager", "Failed to fetch or empty update manifest.")
                    if (manualTrigger) {
                        _updateState.value = UpdateUIState.Error(
                            manifest = null,
                            messageAr = "تعذر الاتصال بسيرفر التحديثات.",
                            messageEn = "Could not connect to update server."
                        )
                    }
                    return@launch
                }

                val manifest = parseUpdateManifest(jsonString) ?: run {
                    Log.d("AppUpdateManager", "Invalid JSON update manifest.")
                    return@launch
                }

                val isWifi = isWifiConnected()

                // Check version comparison
                if (manifest.versionCode > currentVersionCode) {
                    // Check if user skipped this version (if optional)
                    if (!manifest.mandatory && manifest.versionCode.toLong() == skippedVersionCode && !manualTrigger) {
                        Log.d("AppUpdateManager", "User skipped version ${manifest.versionCode}")
                        return@launch
                    }

                    _updateState.value = UpdateUIState.Available(
                        manifest = manifest,
                        isWifi = isWifi,
                        currentVersionName = currentVersionName,
                        currentVersionCode = currentVersionCode
                    )
                } else {
                    Log.d("AppUpdateManager", "App is up to date ($currentVersionName - $currentVersionCode).")
                    if (manualTrigger) {
                        _updateState.value = UpdateUIState.Error(
                            manifest = null,
                            messageAr = "أنت تستخدم أحدث إصدار بالفعل ($currentVersionName).",
                            messageEn = "You are already using the latest version ($currentVersionName)."
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("AppUpdateManager", "Error checking for updates: ${e.message}", e)
            }
        }
    }

    fun downloadAndInstallApk(manifest: UpdateManifest) {
        if (!isWifiConnected() && !isNetworkAvailable()) {
            _updateState.value = UpdateUIState.Error(
                manifest = manifest,
                messageAr = "لا يوجد اتصال بالإنترنت لبدء تنزيل التحديث.",
                messageEn = "No internet connection to start download."
            )
            return
        }

        downloadJob?.cancel()
        downloadJob = scope.launch(Dispatchers.IO) {
            var urlConnection: HttpURLConnection? = null
            var inputStream: InputStream? = null
            var outputStream: FileOutputStream? = null
            var apkFile: File? = null

            try {
                val destinationDir = File(context.cacheDir, "updates")
                if (!destinationDir.exists()) destinationDir.mkdirs()
                
                apkFile = File(destinationDir, "game_v${manifest.versionCode}.apk")
                if (apkFile.exists()) apkFile.delete()

                val url = URL(manifest.apkUrl)
                urlConnection = url.openConnection() as HttpURLConnection
                urlConnection.connectTimeout = 15000
                urlConnection.readTimeout = 30000
                urlConnection.requestMethod = "GET"
                urlConnection.connect()

                if (urlConnection.responseCode != HttpURLConnection.HTTP_OK) {
                    _updateState.value = UpdateUIState.Error(
                        manifest = manifest,
                        messageAr = "فشل تنزيل التحديث من السيرفر (رمز الخطأ: ${urlConnection.responseCode}).",
                        messageEn = "Failed to download update (HTTP ${urlConnection.responseCode})."
                    )
                    return@launch
                }

                val totalBytes = urlConnection.contentLengthLong.let { if (it > 0) it else 1L }
                inputStream = urlConnection.inputStream
                outputStream = FileOutputStream(apkFile)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                var downloadedBytes = 0L
                var lastReportedPercent = -1

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead

                    val percent = ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                    if (percent != lastReportedPercent) {
                        lastReportedPercent = percent
                        val downloadedMB = String.format(Locale.US, "%.1f MB", downloadedBytes / (1024f * 1024f))
                        val totalMB = String.format(Locale.US, "%.1f MB", totalBytes / (1024f * 1024f))

                        _updateState.value = UpdateUIState.Downloading(
                            manifest = manifest,
                            progressPercent = percent,
                            downloadedFormatted = downloadedMB,
                            totalFormatted = totalMB
                        )
                    }
                }

                outputStream.flush()

                // Check SHA-256 Checksum if provided
                if (!manifest.sha256.isNullOrBlank()) {
                    val actualHash = calculateSha256(apkFile)
                    if (!actualHash.equals(manifest.sha256.trim(), ignoreCase = true)) {
                        apkFile.delete()
                        _updateState.value = UpdateUIState.Error(
                            manifest = manifest,
                            messageAr = "فشل التثبيت: الملف المُنزل غير صالح أو تالف (تطابق SHA-256 غير صحيح).",
                            messageEn = "Installation failed: Downloaded file is corrupted (SHA-256 mismatch)."
                        )
                        return@launch
                    }
                }

                _updateState.value = UpdateUIState.ReadyToInstall(
                    manifest = manifest,
                    apkFilePath = apkFile.absolutePath
                )

                // Trigger installation
                withContext(Dispatchers.Main) {
                    installApk(apkFile.absolutePath)
                }

            } catch (e: Exception) {
                Log.e("AppUpdateManager", "Error downloading APK: ${e.message}", e)
                apkFile?.delete()
                _updateState.value = UpdateUIState.Error(
                    manifest = manifest,
                    messageAr = "تعذر إكمال تنزيل التحديث. تحقق من اتصال الإنترنت وحاول مرة أخرى.",
                    messageEn = "Could not complete update download. Check your internet connection and try again."
                )
            } finally {
                try { inputStream?.close() } catch (ignored: Exception) {}
                try { outputStream?.close() } catch (ignored: Exception) {}
                try { urlConnection?.disconnect() } catch (ignored: Exception) {}
            }
        }
    }

    fun installApk(filePath: String) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                _updateState.value = UpdateUIState.Error(
                    manifest = null,
                    messageAr = "ملف التحديث غير موجود.",
                    messageEn = "Update file does not exist."
                )
                return
            }

            // Check unknown sources installation permission on Android O+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}")
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
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
            Log.e("AppUpdateManager", "Error launching APK installer: ${e.message}", e)
            _updateState.value = UpdateUIState.Error(
                manifest = null,
                messageAr = "تعذر فتح برنامج تثبيت التطبيقات. يرجى التحقق من الأذونات.",
                messageEn = "Failed to launch package installer. Please check permissions."
            )
        }
    }

    fun skipVersion(versionCode: Long) {
        skippedVersionCode = versionCode
        _updateState.value = UpdateUIState.Idle
    }

    fun dismissUpdateUi() {
        _updateState.value = UpdateUIState.Idle
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun isWifiConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    private fun getCurrentVersionCode(): Long {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            1L
        }
    }

    private fun getCurrentVersionName(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    private fun fetchManifestJson(urlString: String): String? {
        var urlConnection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.connectTimeout = 8000
            urlConnection.readTimeout = 8000
            urlConnection.requestMethod = "GET"
            urlConnection.connect()

            if (urlConnection.responseCode == HttpURLConnection.HTTP_OK) {
                urlConnection.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            urlConnection?.disconnect()
        }
    }

    private fun parseUpdateManifest(jsonString: String): UpdateManifest? {
        return try {
            val jsonObj = JSONObject(jsonString)
            val versionCode = jsonObj.optInt("versionCode", 0)
            val versionName = jsonObj.optString("versionName", "1.0.0")
            val apkUrl = jsonObj.optString("apkUrl", "")
            val size = jsonObj.optString("size", "")
            val mandatory = jsonObj.optBoolean("mandatory", false)
            val sha256 = if (jsonObj.has("sha256")) jsonObj.optString("sha256") else null

            val notesList = mutableListOf<String>()
            val notesArray: JSONArray? = jsonObj.optJSONArray("releaseNotes")
            if (notesArray != null) {
                for (i in 0 until notesArray.length()) {
                    notesList.add(notesArray.getString(i))
                }
            }

            if (versionCode <= 0 || apkUrl.isEmpty()) return null

            UpdateManifest(
                versionCode = versionCode,
                versionName = versionName,
                apkUrl = apkUrl,
                size = size,
                releaseNotes = notesList,
                mandatory = mandatory,
                sha256 = sha256
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { inputStream ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
