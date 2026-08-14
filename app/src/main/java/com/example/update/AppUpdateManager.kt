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
    companion object {
        private const val TAG = "AppUpdateManager"
    }

    private val _updateState = MutableStateFlow<UpdateUIState>(UpdateUIState.Idle)
    val updateState: StateFlow<UpdateUIState> = _updateState.asStateFlow()

    private var skippedVersionCode: Long = -1L
    private var downloadJob: Job? = null
    private var isChecking = false

    /**
     * ✅ التحقق من وجود تحديثات (يدوياً أو تلقائياً)
     */
    fun checkForUpdates(manualTrigger: Boolean = false) {
        // منع تكرار التحقق
        if (isChecking) {
            Log.d(TAG, "⏳ Check already in progress")
            return
        }

        isChecking = true
        scope.launch(Dispatchers.IO) {
            try {
                _updateState.value = UpdateUIState.Checking

                // ✅ التحقق من الاتصال بالإنترنت
                if (!isNetworkAvailable()) {
                    Log.d(TAG, "❌ No network connection")
                    if (manualTrigger) {
                        _updateState.value = UpdateUIState.Error(
                            manifest = null,
                            messageAr = "لا يوجد اتصال بالإنترنت للتحقق من التحديثات.",
                            messageEn = "No internet connection to check for updates."
                        )
                    } else {
                        _updateState.value = UpdateUIState.Idle
                    }
                    isChecking = false
                    return@launch
                }

                // ✅ قراءة الإصدار الحالي
                val currentVersionCode = getCurrentVersionCode()
                val currentVersionName = getCurrentVersionName()

                Log.d(TAG, "📱 Current version: $currentVersionName ($currentVersionCode)")

                // ✅ جلب التحديث من قائمة الروابط المتعددة أو GitHub Releases
                val manifest = fetchUpdateManifest() ?: fetchGitHubReleaseManifest(UpdateConfig.GITHUB_REPO)

                if (manifest == null) {
                    Log.d(TAG, "❌ No update manifest found from any source")
                    if (manualTrigger) {
                        _updateState.value = UpdateUIState.Error(
                            manifest = null,
                            messageAr = "تعذر التحقق من التحديثات من جميع المصادر. يرجى التحقق من اتصال الإنترنت أو توفر ملف version.json على المستودع.",
                            messageEn = "Could not check for updates from all sources. Please check internet connection or repository availability."
                        )
                    } else {
                        _updateState.value = UpdateUIState.Idle
                    }
                    isChecking = false
                    return@launch
                }

                Log.d(TAG, "📦 Latest version: ${manifest.versionName} (${manifest.versionCode})")

                // ✅ مقارنة الإصدارات
                val isNewer = isUpdateAvailable(manifest, currentVersionCode, currentVersionName)

                if (isNewer) {
                    // ✅ التحقق من أن المستخدم لم يتخطى هذا الإصدار
                    if (!manifest.mandatory && manifest.versionCode.toLong() == skippedVersionCode && !manualTrigger) {
                        Log.d(TAG, "⏭️ User skipped version ${manifest.versionCode}")
                        _updateState.value = UpdateUIState.Idle
                        isChecking = false
                        return@launch
                    }

                    // ✅ التحقق من نوع الاتصال
                    val isWifi = isWifiConnected()

                    Log.d(TAG, "🆕 Update available! New: ${manifest.versionName}, Current: $currentVersionName")

                    _updateState.value = UpdateUIState.Available(
                        manifest = manifest,
                        isWifi = isWifi,
                        currentVersionName = currentVersionName,
                        currentVersionCode = currentVersionCode
                    )
                } else {
                    Log.d(TAG, "✅ App is up to date")
                    if (manualTrigger) {
                        _updateState.value = UpdateUIState.Error(
                            manifest = null,
                            messageAr = "أنت تستخدم أحدث إصدار بالفعل ($currentVersionName).",
                            messageEn = "You are already using the latest version ($currentVersionName)."
                        )
                    } else {
                        _updateState.value = UpdateUIState.Idle
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error checking updates: ${e.message}", e)
                if (manualTrigger) {
                    _updateState.value = UpdateUIState.Error(
                        manifest = null,
                        messageAr = "حدث خطأ أثناء التحقق من التحديثات.",
                        messageEn = "An error occurred while checking for updates."
                    )
                } else {
                    _updateState.value = UpdateUIState.Idle
                }
            } finally {
                isChecking = false
            }
        }
    }

    /**
     * ✅ تنزيل وتثبيت التحديث
     */
    fun downloadAndInstallApk(manifest: UpdateManifest) {
        // ✅ التحقق من الاتصال
        if (!isNetworkAvailable()) {
            _updateState.value = UpdateUIState.Error(
                manifest = manifest,
                messageAr = "لا يوجد اتصال بالإنترنت لبدء التنزيل.",
                messageEn = "No internet connection to start download."
            )
            return
        }

        // ✅ إلغاء أي تنزيل سابق
        downloadJob?.cancel()
        
        downloadJob = scope.launch(Dispatchers.IO) {
            var urlConnection: HttpURLConnection? = null
            var inputStream: InputStream? = null
            var outputStream: FileOutputStream? = null
            var apkFile: File? = null

            try {
                // ✅ إنشاء مجلد التحديثات
                val destinationDir = File(context.cacheDir, "updates")
                if (!destinationDir.exists()) destinationDir.mkdirs()
                
                apkFile = File(destinationDir, "game_v${manifest.versionCode}.apk")
                if (apkFile.exists()) apkFile.delete()

                Log.d(TAG, "📥 Starting download: ${manifest.apkUrl}")

                // ✅ تنزيل الملف مع متابعة التحويلات (Redirects)
                var currentDownloadUrl = manifest.apkUrl
                var redirectCount = 0
                var connectionSuccess = false

                while (redirectCount < 10) {
                    val url = URL(currentDownloadUrl)
                    urlConnection = url.openConnection() as HttpURLConnection
                    urlConnection.connectTimeout = 15000
                    urlConnection.readTimeout = 30000
                    urlConnection.instanceFollowRedirects = true
                    urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; AppUpdateChecker)")
                    urlConnection.requestMethod = "GET"
                    urlConnection.connect()

                    val status = urlConnection.responseCode
                    if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                        status == HttpURLConnection.HTTP_MOVED_PERM ||
                        status == HttpURLConnection.HTTP_SEE_OTHER ||
                        status == 307 || status == 308) {
                        val location = urlConnection.getHeaderField("Location")
                        if (!location.isNullOrEmpty()) {
                            currentDownloadUrl = location
                            urlConnection.disconnect()
                            redirectCount++
                            continue
                        }
                    }

                    if (status == HttpURLConnection.HTTP_OK) {
                        connectionSuccess = true
                        break
                    } else {
                        break
                    }
                }

                if (!connectionSuccess || urlConnection == null || urlConnection.responseCode != HttpURLConnection.HTTP_OK) {
                    val code = urlConnection?.responseCode ?: -1
                    _updateState.value = UpdateUIState.Error(
                        manifest = manifest,
                        messageAr = "فشل تنزيل التحديث (رمز الخطأ: $code).",
                        messageEn = "Failed to download update (HTTP $code)."
                    )
                    return@launch
                }

                // ✅ قراءة الملف مع تتبع التقدم
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

                Log.d(TAG, "✅ Download complete: ${apkFile.absolutePath}")

                // ✅ التحقق من سلامة الملف (SHA-256)
                if (!manifest.sha256.isNullOrBlank()) {
                    val actualHash = calculateSha256(apkFile)
                    val expectedHash = manifest.sha256.trim().removePrefix("sha256:").removePrefix("SHA256:").trim()
                    if (!actualHash.equals(expectedHash, ignoreCase = true)) {
                        apkFile.delete()
                        _updateState.value = UpdateUIState.Error(
                            manifest = manifest,
                            messageAr = "الملف المُنزل تالف (SHA-256 غير صحيح).",
                            messageEn = "Downloaded file is corrupted (SHA-256 mismatch)."
                        )
                        return@launch
                    }
                }

                // ✅ جاهز للتثبيت
                _updateState.value = UpdateUIState.ReadyToInstall(
                    manifest = manifest,
                    apkFilePath = apkFile.absolutePath
                )

                // بدء التثبيت التلقائي
                withContext(Dispatchers.Main) {
                    installApk(apkFile.absolutePath)
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Download error: ${e.message}", e)
                apkFile?.delete()
                _updateState.value = UpdateUIState.Error(
                    manifest = manifest,
                    messageAr = "فشل التنزيل. تحقق من اتصال الإنترنت.",
                    messageEn = "Download failed. Check your internet connection."
                )
            } finally {
                try { inputStream?.close() } catch (ignored: Exception) {}
                try { outputStream?.close() } catch (ignored: Exception) {}
                try { urlConnection?.disconnect() } catch (ignored: Exception) {}
            }
        }
    }

    /**
     * ✅ تثبيت APK
     */
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

            // ✅ التحقق من أذونات التثبيت (Android 8+)
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

            // ✅ تثبيت APK
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

            Log.d(TAG, "✅ Installation started")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Install error: ${e.message}", e)
            _updateState.value = UpdateUIState.Error(
                manifest = null,
                messageAr = "فشل التثبيت. تحقق من الأذونات.",
                messageEn = "Installation failed. Check permissions."
            )
        }
    }

    /**
     * ✅ تخطي الإصدار الحالي
     */
    fun skipVersion(versionCode: Long) {
        skippedVersionCode = versionCode
        _updateState.value = UpdateUIState.Idle
        Log.d(TAG, "⏭️ Skipped version: $versionCode")
    }

    /**
     * ✅ إلغاء التحديث
     */
    fun dismissUpdateUi() {
        _updateState.value = UpdateUIState.Idle
        downloadJob?.cancel()
    }

    // ============== دوال مساعدة ==============

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
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || 
               caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
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

    private fun isUpdateAvailable(
        manifest: UpdateManifest,
        currentVersionCode: Long,
        currentVersionName: String
    ): Boolean {
        if (manifest.versionCode > currentVersionCode) return true
        if (manifest.versionCode < currentVersionCode) return false

        val cleanNew = manifest.versionName.trim().removePrefix("v").removePrefix("V")
        val cleanCurrent = currentVersionName.trim().removePrefix("v").removePrefix("V")
        return isSemverNewer(cleanNew, cleanCurrent)
    }

    private fun isSemverNewer(newVersion: String, currentVersion: String): Boolean {
        val cleanNew = newVersion.takeWhile { it.isDigit() || it == '.' }
        val cleanCurrent = currentVersion.takeWhile { it.isDigit() || it == '.' }
        if (cleanNew.isEmpty() || cleanCurrent.isEmpty()) return false

        val newParts = cleanNew.split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = cleanCurrent.split(".").map { it.toIntOrNull() ?: 0 }

        val maxLength = maxOf(newParts.size, currentParts.size)
        for (i in 0 until maxLength) {
            val newPart = newParts.getOrElse(i) { 0 }
            val currentPart = currentParts.getOrElse(i) { 0 }
            if (newPart > currentPart) return true
            if (newPart < currentPart) return false
        }
        return false
    }

    private suspend fun fetchUpdateManifest(): UpdateManifest? = withContext(Dispatchers.IO) {
        for (url in UpdateConfig.MANIFEST_URLS) {
            try {
                Log.d(TAG, "🔍 Trying to fetch update manifest from: $url")
                val jsonString = fetchUrlContent(url)
                if (!jsonString.isNullOrBlank()) {
                    val manifest = parseUpdateManifest(jsonString)
                    if (manifest != null) {
                        Log.d(TAG, "✅ Successfully loaded manifest from: $url (v${manifest.versionName})")
                        return@withContext manifest
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Failed to fetch from $url: ${e.message}")
            }
        }
        null
    }

    private fun fetchUrlContent(urlString: String): String? {
        var currentUrl = urlString
        var redirectCount = 0
        var urlConnection: HttpURLConnection? = null

        while (redirectCount < 10) {
            try {
                val url = URL(currentUrl)
                urlConnection = url.openConnection() as HttpURLConnection
                urlConnection.connectTimeout = 8000
                urlConnection.readTimeout = 8000
                urlConnection.instanceFollowRedirects = true
                urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko)")
                urlConnection.setRequestProperty("Accept", "application/json, text/plain, */*")
                urlConnection.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                urlConnection.setRequestProperty("Pragma", "no-cache")
                urlConnection.requestMethod = "GET"
                urlConnection.connect()

                val status = urlConnection.responseCode
                if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                    status == HttpURLConnection.HTTP_MOVED_PERM ||
                    status == HttpURLConnection.HTTP_SEE_OTHER ||
                    status == 307 || status == 308) {
                    val location = urlConnection.getHeaderField("Location")
                    if (!location.isNullOrEmpty()) {
                        currentUrl = location
                        urlConnection.disconnect()
                        redirectCount++
                        continue
                    }
                }

                if (status == HttpURLConnection.HTTP_OK) {
                    return urlConnection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    Log.d(TAG, "⚠️ HTTP $status from $currentUrl")
                    return null
                }
            } catch (e: Exception) {
                Log.d(TAG, "⚠️ Network error on $currentUrl: ${e.message}")
                return null
            } finally {
                urlConnection?.disconnect()
            }
        }
        return null
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

    private fun fetchGitHubReleaseManifest(repoOwnerAndName: String): UpdateManifest? {
        val apiUrl = "https://api.github.com/repos/$repoOwnerAndName/releases/latest"
        val jsonString = fetchUrlContent(apiUrl) ?: return null
        return try {
            val jsonObj = JSONObject(jsonString)
            val rawTag = jsonObj.optString("tag_name", "").trim()
            val cleanTag = rawTag.removePrefix("v").removePrefix("V").trim()
            val body = jsonObj.optString("body", "")

            val assetsArray = jsonObj.optJSONArray("assets") ?: return null
            var apkUrl = ""
            var apkSize = ""
            for (i in 0 until assetsArray.length()) {
                val asset = assetsArray.getJSONObject(i)
                val assetName = asset.optString("name", "")
                if (assetName.endsWith(".apk", ignoreCase = true)) {
                    apkUrl = asset.optString("browser_download_url", "")
                    val bytes = asset.optLong("size", 0L)
                    if (bytes > 0) {
                        apkSize = String.format(Locale.US, "%.1f MB", bytes / (1024f * 1024f))
                    }
                    break
                }
            }

            if (apkUrl.isEmpty()) return null

            val numericParts = cleanTag.takeWhile { it.isDigit() || it == '.' }.split(".")
            val major = numericParts.getOrNull(0)?.toIntOrNull() ?: 1
            val minor = numericParts.getOrNull(1)?.toIntOrNull() ?: 0
            val patch = numericParts.getOrNull(2)?.toIntOrNull() ?: 0
            val versionCode = if (major == 1 && minor == 0 && patch > 0) {
                100000 + patch
            } else {
                major * 10000 + minor * 100 + patch
            }

            val notes = if (body.isNotBlank()) {
                body.split("\n").map { it.trim() }.filter { it.isNotBlank() }
            } else {
                listOf("تحديث جديد متوفر على GitHub Releases ($cleanTag)")
            }

            UpdateManifest(
                versionCode = versionCode,
                versionName = if (cleanTag.isNotEmpty()) cleanTag else "1.0.1",
                apkUrl = apkUrl,
                size = apkSize,
                releaseNotes = notes,
                mandatory = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse GitHub release JSON: ${e.message}")
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
