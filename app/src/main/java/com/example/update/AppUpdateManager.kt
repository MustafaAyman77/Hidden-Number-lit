package com.example.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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

        /*
         * IMPORTANT:
         * Change this to your REAL GitHub repository.
         */
        private const val GITHUB_REPOSITORY =
            "MustafaAyman77/Hidden-Number-lit"

        private const val MAX_REDIRECTS = 10
        private const val CONNECT_TIMEOUT = 15_000
        private const val READ_TIMEOUT = 30_000

        private const val PREFS_NAME = "app_update_preferences"
        private const val KEY_SKIPPED_VERSION = "skipped_version_code"
    }

    private val _updateState =
        MutableStateFlow<UpdateUIState>(UpdateUIState.Idle)

    val updateState: StateFlow<UpdateUIState> =
        _updateState.asStateFlow()

    private val prefs =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var downloadJob: Job? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private var lastWifiState = false

    init {
        lastWifiState = isWifiConnected()
        registerNetworkCallback()
    }

    // -------------------------------------------------------------------------
    // PUBLIC API
    // -------------------------------------------------------------------------

    /**
     * Checks GitHub Releases for a newer APK.
     *
     * The check itself can happen on any internet connection.
     * Downloading can be restricted to Wi-Fi.
     */
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

                _updateState.value =
                    UpdateUIState.Checking

                val currentVersionCode =
                    getCurrentVersionCode()

                val currentVersionName =
                    getCurrentVersionName()

                val manifest =
                    fetchGitHubReleaseManifest(GITHUB_REPOSITORY)

                if (manifest == null) {

                    Log.d(
                        TAG,
                        "No valid GitHub release found."
                    )

                    if (manualTrigger) {

                        _updateState.value =
                            UpdateUIState.Error(
                                manifest = null,
                                messageAr =
                                    "لا يوجد إصدار تحديث صالح حاليًا.",
                                messageEn =
                                    "No valid update release was found."
                            )

                    } else {

                        _updateState.value =
                            UpdateUIState.Idle
                    }

                    return@launch
                }

                val isNewer =
                    isUpdateAvailable(
                        manifest,
                        currentVersionCode,
                        currentVersionName
                    )

                if (!isNewer) {

                    Log.d(
                        TAG,
                        "Application is up to date."
                    )

                    if (manualTrigger) {

                        _updateState.value =
                            UpdateUIState.Error(
                                manifest = null,
                                messageAr =
                                    "أنت تستخدم أحدث إصدار بالفعل ($currentVersionName).",
                                messageEn =
                                    "You are already using the latest version ($currentVersionName)."
                            )

                    } else {

                        _updateState.value =
                            UpdateUIState.Idle
                    }

                    return@launch
                }

                val skippedVersion =
                    prefs.getLong(
                        KEY_SKIPPED_VERSION,
                        -1L
                    )

                if (
                    !manifest.mandatory &&
                    manifest.versionCode.toLong() == skippedVersion &&
                    !manualTrigger
                ) {

                    Log.d(
                        TAG,
                        "Version ${manifest.versionCode} was skipped."
                    )

                    _updateState.value =
                        UpdateUIState.Idle

                    return@launch
                }

                _updateState.value =
                    UpdateUIState.Available(
                        manifest = manifest,
                        isWifi = isWifiConnected(),
                        currentVersionName = currentVersionName,
                        currentVersionCode = currentVersionCode
                    )

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Update check failed.",
                    e
                )

                if (manualTrigger) {

                    _updateState.value =
                        UpdateUIState.Error(
                            manifest = null,
                            messageAr =
                                "حدث خطأ أثناء التحقق من التحديث.",
                            messageEn =
                                "Failed to check for updates."
                        )

                } else {

                    _updateState.value =
                        UpdateUIState.Idle
                }
            }
        }
    }

    /**
     * Download APK.
     *
     * By default this method requires Wi-Fi.
     */
    fun downloadAndInstallApk(
        manifest: UpdateManifest
    ) {

        if (!isWifiConnected()) {

            _updateState.value =
                UpdateUIState.Error(
                    manifest = manifest,
                    messageAr =
                        "يرجى الاتصال بشبكة Wi-Fi لتنزيل التحديث.",
                    messageEn =
                        "Please connect to Wi-Fi to download the update."
                )

            return
        }

        downloadJob?.cancel()

        downloadJob =
            scope.launch(Dispatchers.IO) {

                var connection: HttpURLConnection? = null
                var inputStream: InputStream? = null
                var outputStream: FileOutputStream? = null

                var apkFile: File? = null

                try {

                    val destinationDir =
                        File(
                            context.cacheDir,
                            "updates"
                        )

                    if (!destinationDir.exists()) {
                        destinationDir.mkdirs()
                    }

                    apkFile =
                        File(
                            destinationDir,
                            "game_v${manifest.versionCode}.apk"
                        )

                    if (apkFile.exists()) {
                        apkFile.delete()
                    }

                    val finalUrl =
                        resolveDownloadUrl(
                            manifest.apkUrl
                        )

                    connection =
                        openConnection(finalUrl)

                    val responseCode =
                        connection.responseCode

                    if (responseCode != HttpURLConnection.HTTP_OK) {

                        throw Exception(
                            "HTTP $responseCode"
                        )
                    }

                    val totalBytes =
                        connection.contentLengthLong

                    inputStream =
                        connection.inputStream

                    outputStream =
                        FileOutputStream(apkFile)

                    val buffer =
                        ByteArray(16 * 1024)

                    var downloadedBytes = 0L
                    var bytesRead: Int

                    var lastPercent = -1

                    while (
                        inputStream
                            .read(buffer)
                            .also { bytesRead = it } != -1
                    ) {

                        outputStream.write(
                            buffer,
                            0,
                            bytesRead
                        )

                        downloadedBytes += bytesRead

                        val percent =
                            if (totalBytes > 0) {
                                (
                                    downloadedBytes * 100L /
                                        totalBytes
                                    )
                                    .toInt()
                                    .coerceIn(0, 100)
                            } else {
                                -1
                            }

                        if (
                            percent != lastPercent
                        ) {

                            lastPercent = percent

                            val downloadedMB =
                                String.format(
                                    Locale.US,
                                    "%.1f MB",
                                    downloadedBytes /
                                        (1024f * 1024f)
                                )

                            val totalMB =
                                if (totalBytes > 0) {
                                    String.format(
                                        Locale.US,
                                        "%.1f MB",
                                        totalBytes /
                                            (1024f * 1024f)
                                    )
                                } else {
                                    "غير معروف"
                                }

                            _updateState.value =
                                UpdateUIState.Downloading(
                                    manifest = manifest,
                                    progressPercent =
                                        percent,
                                    downloadedFormatted =
                                        downloadedMB,
                                    totalFormatted =
                                        totalMB
                                )
                        }
                    }

                    outputStream.flush()

                    // -----------------------------------------------------------------
                    // SHA-256 verification
                    // -----------------------------------------------------------------

                    if (
                        !manifest.sha256.isNullOrBlank()
                    ) {

                        val actualHash =
                            calculateSha256(
                                apkFile
                            )

                        if (
                            !actualHash.equals(
                                manifest.sha256.trim(),
                                ignoreCase = true
                            )
                        ) {

                            apkFile.delete()

                            throw SecurityException(
                                "SHA-256 mismatch"
                            )
                        }
                    }

                    _updateState.value =
                        UpdateUIState.ReadyToInstall(
                            manifest = manifest,
                            apkFilePath =
                                apkFile.absolutePath
                        )

                    withContext(Dispatchers.Main) {

                        installApk(
                            apkFile.absolutePath
                        )
                    }

                } catch (e: SecurityException) {

                    Log.e(
                        TAG,
                        "APK integrity verification failed.",
                        e
                    )

                    apkFile?.delete()

                    _updateState.value =
                        UpdateUIState.Error(
                            manifest = manifest,
                            messageAr =
                                "فشل التحقق من سلامة ملف التحديث.",
                            messageEn =
                                "APK integrity verification failed."
                        )

                } catch (e: Exception) {

                    Log.e(
                        TAG,
                        "APK download failed.",
                        e
                    )

                    apkFile?.delete()

                    _updateState.value =
                        UpdateUIState.Error(
                            manifest = manifest,
                            messageAr =
                                "تعذر تنزيل التحديث. تحقق من اتصال Wi-Fi وحاول مرة أخرى.",
                            messageEn =
                                "Could not download the update."
                        )

                } finally {

                    try {
                        inputStream?.close()
                    } catch (_: Exception) {
                    }

                    try {
                        outputStream?.close()
                    } catch (_: Exception) {
                    }

                    connection?.disconnect()
                }
            }
    }

    /**
     * Launches Android's package installer.
     */
    fun installApk(
        filePath: String
    ) {

        try {

            val file =
                File(filePath)

            if (!file.exists()) {

                _updateState.value =
                    UpdateUIState.Error(
                        manifest = null,
                        messageAr =
                            "ملف التحديث غير موجود.",
                        messageEn =
                            "Update file does not exist."
                    )

                return
            }

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O
            ) {

                if (
                    !context.packageManager
                        .canRequestPackageInstalls()
                ) {

                    val intent =
                        Intent(
                            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse(
                                "package:${context.packageName}"
                            )
                        )

                    intent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                    )

                    context.startActivity(intent)

                    return
                }
            }

            val apkUri =
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

            val intent =
                Intent(Intent.ACTION_VIEW).apply {

                    setDataAndType(
                        apkUri,
                        "application/vnd.android.package-archive"
                    )

                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )

                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                    )
                }

            context.startActivity(intent)

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Could not launch installer.",
                e
            )

            _updateState.value =
                UpdateUIState.Error(
                    manifest = null,
                    messageAr =
                        "تعذر فتح مثبت التطبيقات.",
                    messageEn =
                        "Could not open Android package installer."
                )
        }
    }

    fun skipVersion(
        versionCode: Long
    ) {

        prefs.edit()
            .putLong(
                KEY_SKIPPED_VERSION,
                versionCode
            )
            .apply()

        _updateState.value =
            UpdateUIState.Idle
    }

    fun dismissUpdateUi() {

        _updateState.value =
            UpdateUIState.Idle
    }

    /**
     * Call when the manager is no longer needed.
     */
    fun destroy() {

        downloadJob?.cancel()

        unregisterNetworkCallback()
    }

    // -------------------------------------------------------------------------
    // NETWORK MONITORING
    // -------------------------------------------------------------------------

    private fun registerNetworkCallback() {

        val connectivityManager =
            context.getSystemService(
                Context.CONNECTIVITY_SERVICE
            ) as? ConnectivityManager
                ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            val callback =
                object :
                    ConnectivityManager.NetworkCallback() {

                    override fun onAvailable(
                        network: Network
                    ) {

                        scope.launch {

                            delay(1200)

                            val wifi =
                                isWifiConnected()

                            if (
                                wifi &&
                                !lastWifiState
                            ) {

                                lastWifiState = true

                                Log.d(
                                    TAG,
                                    "Wi-Fi connected. Checking for updates."
                                )

                                checkForUpdates()
                            }
                        }
                    }

                    override fun onLost(
                        network: Network
                    ) {

                        lastWifiState =
                            isWifiConnected()
                    }

                    override fun onCapabilitiesChanged(
                        network: Network,
                        networkCapabilities:
                            NetworkCapabilities
                    ) {

                        val wifi =
                            networkCapabilities.hasTransport(
                                NetworkCapabilities.TRANSPORT_WIFI
                            ) ||
                            networkCapabilities.hasTransport(
                                NetworkCapabilities.TRANSPORT_ETHERNET
                            )

                        if (
                            wifi &&
                            !lastWifiState
                        ) {

                            lastWifiState = true

                            scope.launch {

                                delay(1200)

                                checkForUpdates()
                            }
                        } else if (!wifi) {

                            lastWifiState = false
                        }
                    }
                }

            networkCallback =
                callback

            val request =
                NetworkRequest.Builder()
                    .addCapability(
                        NetworkCapabilities.NET_CAPABILITY_INTERNET
                    )
                    .build()

            try {

                connectivityManager.registerNetworkCallback(
                    request,
                    callback
                )

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Failed to register network callback.",
                    e
                )
            }
        }
    }

    private fun unregisterNetworkCallback() {

        val callback =
            networkCallback
                ?: return

        val cm =
            context.getSystemService(
                Context.CONNECTIVITY_SERVICE
            ) as? ConnectivityManager
                ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            try {
                cm.unregisterNetworkCallback(
                    callback
                )
            } catch (_: Exception) {
            }
        }

        networkCallback = null
    }

    private fun isNetworkAvailable(): Boolean {

        val cm =
            context.getSystemService(
                Context.CONNECTIVITY_SERVICE
            ) as? ConnectivityManager
                ?: return false

        val network =
            cm.activeNetwork
                ?: return false

        val capabilities =
            cm.getNetworkCapabilities(network)
                ?: return false

        return capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_INTERNET
        )
    }

    private fun isWifiConnected(): Boolean {

        val cm =
            context.getSystemService(
                Context.CONNECTIVITY_SERVICE
            ) as? ConnectivityManager
                ?: return false

        val network =
            cm.activeNetwork
                ?: return false

        val capabilities =
            cm.getNetworkCapabilities(network)
                ?: return false

        return capabilities.hasTransport(
            NetworkCapabilities.TRANSPORT_WIFI
        ) ||
        capabilities.hasTransport(
            NetworkCapabilities.TRANSPORT_ETHERNET
        )
    }

    // -------------------------------------------------------------------------
    // GITHUB
    // -------------------------------------------------------------------------

    private fun fetchGitHubReleaseManifest(
        repository: String
    ): UpdateManifest? {

        val apiUrl =
            "https://api.github.com/repos/$repository/releases/latest"

        val json =
            fetchText(
                apiUrl
            )
                ?: return null

        return try {

            val root =
                JSONObject(json)

            val tagName =
                root.optString(
                    "tag_name",
                    ""
                )
                    .trim()

            val body =
                root.optString(
                    "body",
                    ""
                )

            val assets =
                root.optJSONArray(
                    "assets"
                )
                    ?: return null

            var apkUrl =
                ""

            var apkSize =
                0L

            var sha256: String? =
                null

            for (i in 0 until assets.length()) {

                val asset =
                    assets.getJSONObject(i)

                val name =
                    asset.optString(
                        "name",
                        ""
                    )

                if (
                    name.endsWith(
                        ".apk",
                        ignoreCase = true
                    )
                ) {

                    apkUrl =
                        asset.optString(
                            "browser_download_url",
                            ""
                        )

                    apkSize =
                        asset.optLong(
                            "size",
                            0L
                        )

                    /*
                     * If you upload a companion checksum file:
                     *
                     * game.apk.sha256
                     *
                     * we will try to read it later.
                     */
                }
            }

            if (apkUrl.isBlank()) {
                return null
            }

            val cleanTag =
                tagName
                    .removePrefix("v")
                    .removePrefix("V")
                    .trim()

            val versionCode =
                versionNameToVersionCode(
                    cleanTag
                )

            if (versionCode <= 0) {
                return null
            }

            val notes =
                if (body.isNotBlank()) {

                    body.lines()
                        .map { it.trim() }
                        .filter { it.isNotBlank() }

                } else {

                    listOf(
                        "يتوفر تحديث جديد للعبة."
                    )
                }

            UpdateManifest(
                versionCode = versionCode,
                versionName =
                    if (cleanTag.isNotBlank()) {
                        cleanTag
                    } else {
                        "1.0.1"
                    },
                apkUrl = apkUrl,
                size =
                    if (apkSize > 0) {
                        String.format(
                            Locale.US,
                            "%.1f MB",
                            apkSize /
                                (1024f * 1024f)
                        )
                    } else {
                        ""
                    },
                releaseNotes = notes,
                mandatory = false,
                sha256 = sha256
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Failed to parse GitHub release.",
                e
            )

            null
        }
    }

    private fun fetchText(
        urlString: String
    ): String? {

        var connection:
            HttpURLConnection? = null

        return try {

            val url =
                URL(urlString)

            connection =
                url.openConnection()
                    as HttpURLConnection

            connection.connectTimeout =
                CONNECT_TIMEOUT

            connection.readTimeout =
                READ_TIMEOUT

            connection.instanceFollowRedirects =
                true

            connection.requestMethod =
                "GET"

            connection.setRequestProperty(
                "User-Agent",
                "HiddenNumber-Android-Updater"
            )

            connection.setRequestProperty(
                "Accept",
                "application/vnd.github+json"
            )

            val responseCode =
                connection.responseCode

            if (
                responseCode ==
                HttpURLConnection.HTTP_OK
            ) {

                connection.inputStream
                    .bufferedReader()
                    .use {
                        it.readText()
                    }

            } else {

                Log.w(
                    TAG,
                    "HTTP $responseCode for $urlString"
                )

                null
            }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "HTTP request failed.",
                e
            )

            null

        } finally {

            connection?.disconnect()
        }
    }

    private fun resolveDownloadUrl(
        urlString: String
    ): String {

        /*
         * GitHub browser_download_url normally redirects
         * to GitHub's CDN.
         *
         * We allow redirects only through HTTPS.
         */

        var currentUrl =
            urlString

        repeat(MAX_REDIRECTS) {

            val connection =
                openConnection(
                    currentUrl,
                    followRedirects = false
                )

            try {

                val response =
                    connection.responseCode

                if (
                    response ==
                    HttpURLConnection.HTTP_MOVED_TEMP ||
                    response ==
                    HttpURLConnection.HTTP_MOVED_PERM ||
                    response ==
                    HttpURLConnection.HTTP_SEE_OTHER ||
                    response == 307 ||
                    response == 308
                ) {

                    val location =
                        connection.getHeaderField(
                            "Location"
                        )
                            ?: return currentUrl

                    if (
                        !location.startsWith(
                            "https://",
                            ignoreCase = true
                        )
                    ) {

                        throw SecurityException(
                            "Blocked insecure redirect."
                        )
                    }

                    currentUrl =
                        location

                } else {

                    return currentUrl
                }

            } finally {

                connection.disconnect()
            }
        }

        return currentUrl
    }

    private fun openConnection(
        urlString: String,
        followRedirects: Boolean = true
    ): HttpURLConnection {

        val url =
            URL(urlString)

        if (
            !url.protocol.equals(
                "https",
                ignoreCase = true
            )
        ) {

            throw SecurityException(
                "Only HTTPS URLs are allowed."
            )
        }

        val connection =
            url.openConnection()
                as HttpURLConnection

        connection.connectTimeout =
            CONNECT_TIMEOUT

        connection.readTimeout =
            READ_TIMEOUT

        connection.instanceFollowRedirects =
            followRedirects

        connection.requestMethod =
            "GET"

        connection.setRequestProperty(
            "User-Agent",
            "HiddenNumber-Android-Updater"
        )

        return connection
    }

    // -------------------------------------------------------------------------
    // VERSION
    // -------------------------------------------------------------------------

    private fun getCurrentVersionCode(): Long {

        return try {

            val info =
                context.packageManager
                    .getPackageInfo(
                        context.packageName,
                        0
                    )

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.P
            ) {

                info.longVersionCode

            } else {

                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Failed to read versionCode.",
                e
            )

            1L
        }
    }

    private fun getCurrentVersionName(): String {

        return try {

            val info =
                context.packageManager
                    .getPackageInfo(
                        context.packageName,
                        0
                    )

            info.versionName
                ?.removePrefix("v")
                ?.removePrefix("V")
                ?: "1.0.0"

        } catch (e: Exception) {

            "1.0.0"
        }
    }

    private fun isUpdateAvailable(
        manifest: UpdateManifest,
        currentVersionCode: Long,
        currentVersionName: String
    ): Boolean {

        if (
            manifest.versionCode >
            currentVersionCode
        ) {
            return true
        }

        return isSemverNewer(
            manifest.versionName,
            currentVersionName
        )
    }

    private fun isSemverNewer(
        newVersion: String,
        currentVersion: String
    ): Boolean {

        val newParts =
            parseVersion(
                newVersion
            )

        val currentParts =
            parseVersion(
                currentVersion
            )

        val max =
            maxOf(
                newParts.size,
                currentParts.size
            )

        for (i in 0 until max) {

            val newPart =
                newParts.getOrElse(i) {
                    0
                }

            val currentPart =
                currentParts.getOrElse(i) {
                    0
                }

            if (newPart > currentPart) {
                return true
            }

            if (newPart < currentPart) {
                return false
            }
        }

        return false
    }

    private fun parseVersion(
        version: String
    ): List<Int> {

        return version
            .trim()
            .removePrefix("v")
            .removePrefix("V")
            .takeWhile {
                it.isDigit() ||
                    it == '.'
            }
            .split(".")
            .mapNotNull {
                it.toIntOrNull()
            }
    }

    private fun versionNameToVersionCode(
        version: String
    ): Int {

        val parts =
            parseVersion(
                version
            )

        if (parts.isEmpty()) {
            return 0
        }

        val major =
            parts.getOrElse(0) {
                0
            }

        val minor =
            parts.getOrElse(1) {
                0
            }

        val patch =
            parts.getOrElse(2) {
                0
            }

        return (
            major * 10_000 +
                minor * 100 +
                patch
            )
    }

    // -------------------------------------------------------------------------
    // SHA-256
    // -------------------------------------------------------------------------

    private fun calculateSha256(
        file: File
    ): String {

        val digest =
            MessageDigest.getInstance(
                "SHA-256"
            )

        file.inputStream().use { input ->

            val buffer =
                ByteArray(16 * 1024)

            var read: Int

            while (
                input
                    .read(buffer)
                    .also { read = it } != -1
            ) {

                digest.update(
                    buffer,
                    0,
                    read
                )
            }
        }

        return digest
            .digest()
            .joinToString("") {
                "%02x".format(it)
            }
    }
}                            messageAr = "أنت تستخدم أحدث إصدار بالفعل ($currentVersionName).",
                            messageEn = "You are already using the latest version ($currentVersionName)."
                        )
                    } else {
                        _updateState.value = UpdateUIState.Idle
                    }
                }
            } catch (e: Exception) {
                Log.e("AppUpdateManager", "Error checking for updates: ${e.message}", e)
                if (manualTrigger) {
                    _updateState.value = UpdateUIState.Error(
                        manifest = null,
                        messageAr = "حدث خطأ أثناء الاتصال بسيرفر التحديثات.",
                        messageEn = "An error occurred while checking for updates."
                    )
                } else {
                    _updateState.value = UpdateUIState.Idle
                }
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
                        messageAr = "فشل تنزيل التحديث من السيرفر (رمز الخطأ: $code).",
                        messageEn = "Failed to download update (HTTP $code)."
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

    private fun isUpdateAvailable(
        manifest: UpdateManifest,
        currentVersionCode: Long,
        currentVersionName: String
    ): Boolean {
        val cleanNew = manifest.versionName.trim().removePrefix("v").removePrefix("V")
        val cleanCurrent = currentVersionName.trim().removePrefix("v").removePrefix("V")

        // If versions are identical, no update needed
        if (cleanNew.equals(cleanCurrent, ignoreCase = true)) {
            return false
        }

        // If installed app is on legacy template version "1.1.0" or "1.0.0" and GitHub has a release tag like "1.0.34"
        if ((cleanCurrent == "1.1.0" || cleanCurrent == "1.0.0") && cleanNew != cleanCurrent) {
            return true
        }

        // Compare versionCode if manifest versionCode is higher
        if (manifest.versionCode > currentVersionCode) {
            return true
        }

        // Semver comparison
        return isSemverNewer(cleanNew, cleanCurrent)
    }

    private fun isSemverNewer(newVersion: String, currentVersion: String): Boolean {
        val cleanNew = newVersion.trim().removePrefix("v").removePrefix("V").takeWhile { it.isDigit() || it == '.' }
        val cleanCurrent = currentVersion.trim().removePrefix("v").removePrefix("V").takeWhile { it.isDigit() || it == '.' }

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

    private fun fetchManifestJson(urlString: String): String? {
        var currentUrl = urlString
        var redirectCount = 0
        var urlConnection: HttpURLConnection? = null

        while (redirectCount < 10) {
            try {
                val url = URL(currentUrl)
                urlConnection = url.openConnection() as HttpURLConnection
                urlConnection.connectTimeout = 10000
                urlConnection.readTimeout = 10000
                urlConnection.instanceFollowRedirects = true
                urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; AppUpdateChecker)")
                urlConnection.setRequestProperty("Accept", "application/json, text/plain, */*")
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
                    Log.w("AppUpdateManager", "Manifest fetch HTTP status: $status")
                    return null
                }
            } catch (e: Exception) {
                Log.e("AppUpdateManager", "Manifest fetch exception: ${e.message}")
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
        val jsonString = fetchManifestJson(apiUrl) ?: return null
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
            val versionCode = major * 10000 + minor * 100 + patch

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
            Log.e("AppUpdateManager", "Failed to parse GitHub release JSON: ${e.message}")
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
