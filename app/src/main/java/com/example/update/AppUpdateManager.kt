package com.example.update

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

/**
 * مدير التحديثات للتطبيق.
 * يقوم بالتحقق من وجود إصدارات جديدة على GitHub، وتنزيلها وتثبيتها.
 */
class AppUpdateManager(
    private val context: Context,
    private val scope: CoroutineScope
) {

    companion object {
        private const val TAG = "AppUpdateManager"
        private const val PREFS_NAME = "app_update_preferences"
        private const val KEY_SKIPPED_VERSION = "skipped_version_code"
    }

    private val _updateState = MutableStateFlow<UpdateUIState>(UpdateUIState.Idle)
    val updateState: StateFlow<UpdateUIState> = _updateState.asStateFlow()

    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var downloadJob: Job? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var lastWifiState = isWifiConnected()

    init {
        registerNetworkCallback()
    }

    // ============================================================
    // التحقق من وجود تحديث
    // ============================================================

    fun checkForUpdates(manualTrigger: Boolean = false) {
        if (!isNetworkAvailable()) {
            if (manualTrigger) {
                _updateState.value = UpdateUIState.Error(
                    manifest = null,
                    messageAr = "لا يوجد اتصال بالإنترنت للتحقق من التحديثات.",
                    messageEn = "No internet connection."
                )
            }
            return
        }

        scope.launch(Dispatchers.IO) {
            try {
                _updateState.value = UpdateUIState.Checking

                val currentVersionCode = getCurrentVersionCode()
                val currentVersionName = getCurrentVersionName()
                val manifest = fetchGitHubReleaseManifest()

                if (manifest == null) {
                    Log.d(TAG, "No valid GitHub release found.")
                    if (manualTrigger) {
                        _updateState.value = UpdateUIState.Error(
                            manifest = null,
                            messageAr = "لم يتم العثور على إصدار تحديث صالح.",
                            messageEn = "No valid update was found."
                        )
                    } else {
                        _updateState.value = UpdateUIState.Idle
                    }
                    return@launch
                }

                val updateAvailable = isUpdateAvailable(manifest, currentVersionCode, currentVersionName)

                if (!updateAvailable) {
                    Log.d(TAG, "Application is up to date.")
                    if (manualTrigger) {
                        _updateState.value = UpdateUIState.Error(
                            manifest = null,
                            messageAr = "أنت تستخدم أحدث إصدار بالفعل ($currentVersionName).",
                            messageEn = "You are already using the latest version ($currentVersionName)."
                        )
                    } else {
                        _updateState.value = UpdateUIState.Idle
                    }
                    return@launch
                }

                val skippedVersion = preferences.getLong(KEY_SKIPPED_VERSION, -1L)
                if (!manifest.mandatory && manifest.versionCode.toLong() == skippedVersion && !manualTrigger) {
                    Log.d(TAG, "Version was skipped.")
                    _updateState.value = UpdateUIState.Idle
                    return@launch
                }

                _updateState.value = UpdateUIState.Available(
                    manifest = manifest,
                    isWifi = isWifiConnected(),
                    currentVersionName = currentVersionName,
                    currentVersionCode = currentVersionCode
                )

            } catch (e: Exception) {
                Log.e(TAG, "Update check failed.", e)
                if (manualTrigger) {
                    _updateState.value = UpdateUIState.Error(
                        manifest = null,
                        messageAr = "حدث خطأ أثناء التحقق من التحديث.",
                        messageEn = "Failed to check for updates."
                    )
                } else {
                    _updateState.value = UpdateUIState.Idle
                }
            }
        }
    }

    // ============================================================
    // تحميل ملف APK
    // ============================================================

    fun downloadAndInstallApk(manifest: UpdateManifest) {
        if (!isWifiConnected()) {
            _updateState.value = UpdateUIState.Error(
                manifest = manifest,
                messageAr = "يرجى الاتصال بشبكة Wi-Fi لتنزيل التحديث.",
                messageEn = "Please connect to Wi-Fi."
            )
            return
        }

        downloadJob?.cancel()
        downloadJob = scope.launch(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            var inputStream: InputStream? = null
            var outputStream: FileOutputStream? = null
            var apkFile: File? = null

            try {
                val updatesDirectory = File(context.cacheDir, "updates")
                if (!updatesDirectory.exists()) {
                    updatesDirectory.mkdirs()
                }

                apkFile = File(updatesDirectory, "game_v${manifest.versionCode}.apk")
                if (apkFile.exists()) {
                    apkFile.delete()
                }

                val downloadUrl = resolveDownloadUrl(manifest.apkUrl)
                connection = openConnection(downloadUrl, followRedirects = true)

                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw Exception("HTTP $responseCode")
                }

                val totalBytes = connection.contentLengthLong
                inputStream = connection.inputStream
                outputStream = FileOutputStream(apkFile)

                val buffer = ByteArray(16 * 1024)
                var downloadedBytes = 0L
                var bytesRead: Int
                var lastPercent = -1

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead

                    val percent = if (totalBytes > 0) {
                        (downloadedBytes * 100L / totalBytes).toInt().coerceIn(0, 100)
                    } else {
                        -1
                    }

                    if (percent != lastPercent) {
                        lastPercent = percent
                        val downloadedMB = String.format(Locale.US, "%.1f MB", downloadedBytes / (1024f * 1024f))
                        val totalMB = if (totalBytes > 0) {
                            String.format(Locale.US, "%.1f MB", totalBytes / (1024f * 1024f))
                        } else {
                            "غير معروف"
                        }

                        _updateState.value = UpdateUIState.Downloading(
                            manifest = manifest,
                            progressPercent = percent,
                            downloadedFormatted = downloadedMB,
                            totalFormatted = totalMB
                        )
                    }
                }

                outputStream.flush()

                // التحقق من SHA-256
                if (!manifest.sha256.isNullOrBlank()) {
                    val actualHash = calculateSha256(apkFile)
                    val expectedHash = manifest.sha256.trim()
                    if (!actualHash.equals(expectedHash, ignoreCase = true)) {
                        apkFile.delete()
                        throw SecurityException("SHA-256 mismatch")
                    }
                }

                _updateState.value = UpdateUIState.ReadyToInstall(
                    manifest = manifest,
                    apkFilePath = apkFile.absolutePath
                )

                withContext(Dispatchers.Main) {
                    installApk(apkFile.absolutePath)
                }

            } catch (e: SecurityException) {
                Log.e(TAG, "APK verification failed.", e)
                apkFile?.delete()
                _updateState.value = UpdateUIState.Error(
                    manifest = manifest,
                    messageAr = "فشل التحقق من سلامة ملف التحديث.",
                    messageEn = "APK integrity verification failed."
                )

            } catch (e: Exception) {
                Log.e(TAG, "APK download failed.", e)
                apkFile?.delete()
                _updateState.value = UpdateUIState.Error(
                    manifest = manifest,
                    messageAr = "تعذر تنزيل التحديث. تحقق من اتصال Wi-Fi وحاول مرة أخرى.",
                    messageEn = "Could not download the update."
                )

            } finally {
                try { inputStream?.close() } catch (_: Exception) {}
                try { outputStream?.close() } catch (_: Exception) {}
                connection?.disconnect()
            }
        }
    }

    // ============================================================
    // تثبيت APK
    // ============================================================

    fun installApk(filePath: String) {
        try {
            val apkFile = File(filePath)
            if (!apkFile.exists()) {
                _updateState.value = UpdateUIState.Error(
                    manifest = null,
                    messageAr = "ملف التحديث غير موجود.",
                    messageEn = "Update file does not exist."
                )
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val canInstall = context.packageManager.canRequestPackageInstalls()
                if (!canInstall) {
                    val settingsIntent = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}")
                    )
                    settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(settingsIntent)
                    // لا نغير الحالة هنا لأننا ننتظر عودة المستخدم من الإعدادات
                    return
                }
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)

            // بعد بدء التثبيت، نعيد الحالة إلى Idle لأن التطبيق سيغلق أو ينتقل للخلفية
            _updateState.value = UpdateUIState.Idle

        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch installer.", e)
            _updateState.value = UpdateUIState.Error(
                manifest = null,
                messageAr = "تعذر فتح مثبت التطبيقات.",
                messageEn = "Could not open Android installer."
            )
        }
    }

    // ============================================================
    // تخطي التحديث
    // ============================================================

    fun skipVersion(versionCode: Long) {
        preferences.edit().putLong(KEY_SKIPPED_VERSION, versionCode).apply()
        _updateState.value = UpdateUIState.Idle
    }

    fun dismissUpdateUi() {
        _updateState.value = UpdateUIState.Idle
    }

    // ============================================================
    // الشبكة
    // ============================================================

    private fun isNetworkAvailable(): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun isWifiConnected(): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    // ============================================================
    // كشف الـ Wi-Fi تلقائياً
    // ============================================================

    private fun registerNetworkCallback() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return

        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // نستخدم isActive للتحقق من عدم إلغاء السياق
                if (!scope.coroutineContext.isActive) return
                scope.launch {
                    delay(1500)
                    if (!isActive) return@launch
                    val wifi = isWifiConnected()
                    if (wifi && !lastWifiState) {
                        lastWifiState = true
                        Log.d(TAG, "Wi-Fi connected.")
                        checkForUpdates()
                    }
                }
            }

            override fun onLost(network: Network) {
                lastWifiState = isWifiConnected()
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                val wifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                if (wifi && !lastWifiState) {
                    lastWifiState = true
                    if (!scope.coroutineContext.isActive) return
                    scope.launch {
                        delay(1500)
                        if (!isActive) return@launch
                        checkForUpdates()
                    }
                } else if (!wifi) {
                    lastWifiState = false
                }
            }
        }

        networkCallback = callback
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            manager.registerNetworkCallback(request, callback)
        } catch (e: Exception) {
            Log.e(TAG, "Could not register network callback.", e)
        }
    }

    fun destroy() {
        downloadJob?.cancel()
        val callback = networkCallback ?: return
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                manager.unregisterNetworkCallback(callback)
            } catch (_: Exception) {
            }
        }
        networkCallback = null
    }

    // ============================================================
    // جلب بيانات الإصدار من GitHub
    // ============================================================

    private fun fetchGitHubReleaseManifest(): UpdateManifest? {
        val apiUrl = "https://api.github.com/repos/${UpdateConfig.GITHUB_REPOSITORY}/releases/latest"
        val json = fetchText(apiUrl) ?: return null

        return try {
            val root = JSONObject(json)
            val tag = root.optString("tag_name", "").trim()
            val body = root.optString("body", "")
            val assets = root.optJSONArray("assets") ?: return null

            var apkUrl = ""
            var apkSize = 0L

            for (index in 0 until assets.length()) {
                val asset = assets.getJSONObject(index)
                val name = asset.optString("name", "")
                if (name.endsWith(UpdateConfig.APK_EXTENSION, ignoreCase = true)) {
                    apkUrl = asset.optString("browser_download_url", "")
                    apkSize = asset.optLong("size", 0L)
                    break
                }
            }

            if (apkUrl.isBlank()) return null

            val cleanVersion = tag.removePrefix("v").removePrefix("V").trim()
            val versionCode = versionNameToVersionCode(cleanVersion)
            if (versionCode <= 0) return null

            val notes = if (body.isNotBlank()) {
                body.lines().map { it.trim() }.filter { it.isNotBlank() }
            } else {
                listOf("يتوفر تحديث جديد للعبة.")
            }

            UpdateManifest(
                versionCode = versionCode,
                versionName = cleanVersion,
                apkUrl = apkUrl,
                size = if (apkSize > 0) {
                    String.format(Locale.US, "%.1f MB", apkSize / (1024f * 1024f))
                } else "",
                releaseNotes = notes,
                mandatory = false,
                sha256 = null
            )

        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse GitHub release.", e)
            null
        }
    }

    private fun fetchText(urlString: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = UpdateConfig.CONNECT_TIMEOUT
            connection.readTimeout = UpdateConfig.READ_TIMEOUT
            connection.instanceFollowRedirects = true
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "HiddenNumber-Android-Updater")
            connection.setRequestProperty("Accept", "application/vnd.github+json")

            val response = connection.responseCode
            if (response == HttpURLConnection.HTTP_OK) {
                return connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                Log.w(TAG, "GitHub HTTP response: $response")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "GitHub request failed.", e)
            null
        } finally {
            connection?.disconnect()
        }
    }

    // ============================================================
    // معالجة روابط التنزيل
    // ============================================================

    private fun resolveDownloadUrl(originalUrl: String): String {
        var currentUrl = originalUrl
        repeat(UpdateConfig.MAX_REDIRECTS) {
            val connection = openConnection(currentUrl, followRedirects = false)
            try {
                val response = connection.responseCode
                if (response == HttpURLConnection.HTTP_MOVED_TEMP ||
                    response == HttpURLConnection.HTTP_MOVED_PERM ||
                    response == HttpURLConnection.HTTP_SEE_OTHER ||
                    response == 307 || response == 308
                ) {
                    val location = connection.getHeaderField("Location") ?: return currentUrl
                    if (!location.startsWith("https://", ignoreCase = true)) {
                        throw SecurityException("Insecure redirect blocked.")
                    }
                    currentUrl = location
                } else {
                    return currentUrl
                }
            } finally {
                connection.disconnect()
            }
        }
        return currentUrl
    }

    private fun openConnection(urlString: String, followRedirects: Boolean): HttpURLConnection {
        val url = URL(urlString)
        if (!url.protocol.equals("https", ignoreCase = true)) {
            throw SecurityException("Only HTTPS connections are allowed.")
        }

        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = UpdateConfig.CONNECT_TIMEOUT
        connection.readTimeout = UpdateConfig.READ_TIMEOUT
        connection.instanceFollowRedirects = followRedirects
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "HiddenNumber-Android-Updater")
        return connection
    }

    // ============================================================
    // إدارة الإصدارات
    // ============================================================

    private fun getCurrentVersionCode(): Long {
        return try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            }
        } catch (e: Exception) {
            1L
        }
    }

    private fun getCurrentVersionName(): String {
        return try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            info.versionName?.removePrefix("v")?.removePrefix("V") ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    private fun isUpdateAvailable(manifest: UpdateManifest, currentVersionCode: Long, currentVersionName: String): Boolean {
        // نفضل مقارنة الأجزاء أولاً، ثم نستخدم versionCode كاحتياط
        if (manifest.versionCode > currentVersionCode) return true
        return isSemverNewer(manifest.versionName, currentVersionName)
    }

    private fun isSemverNewer(newVersion: String, currentVersion: String): Boolean {
        val newParts = parseVersion(newVersion)
        val currentParts = parseVersion(currentVersion)
        val max = maxOf(newParts.size, currentParts.size)

        for (i in 0 until max) {
            val newPart = newParts.getOrElse(i) { 0 }
            val currentPart = currentParts.getOrElse(i) { 0 }
            if (newPart > currentPart) return true
            if (newPart < currentPart) return false
        }
        return false
    }

    private fun parseVersion(version: String): List<Int> {
        return version.trim()
            .removePrefix("v")
            .removePrefix("V")
            .takeWhile { it.isDigit() || it == '.' }
            .split(".")
            .mapNotNull { it.toIntOrNull() }
    }

    /**
     * تحويل اسم الإصدار (مثل 1.2.3) إلى رقم صحيح للمقارنة السريعة.
     * نضرب الأجزاء بقيم كبيرة لتجنب التصادم (بافتراض أن كل جزء <= 999).
     */
    private fun versionNameToVersionCode(version: String): Int {
        val parts = parseVersion(version)
        if (parts.isEmpty()) return 0

        val major = parts.getOrElse(0) { 0 }
        val minor = parts.getOrElse(1) { 0 }
        val patch = parts.getOrElse(2) { 0 }

        // نسمح حتى 999 لكل جزء
        return major * 1_000_000 + minor * 1_000 + patch
    }

    // ============================================================
    // حساب SHA-256
    // ============================================================

    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(16 * 1024)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}