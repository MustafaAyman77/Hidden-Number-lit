package com.example.data.supabase

import android.util.Log
import com.example.data.model.PlayerProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class SupabaseProfileService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun getProfile(userId: String, accessToken: String): PlayerProfile? = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured() || userId.isEmpty()) return@withContext null

        try {
            val url = "${SupabaseConfig.url}/rest/v1/profiles?id=eq.$userId&select=*"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.publishableKey)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                Log.d("SupabaseProfileService", "GetProfile Code: ${response.code}, Body: $bodyStr")

                if (!response.isSuccessful || bodyStr.isEmpty()) {
                    return@withContext null
                }

                val jsonArray = JSONArray(bodyStr)
                if (jsonArray.length() == 0) return@withContext null

                val obj = jsonArray.getJSONObject(0)
                parseProfileJson(obj, userId)
            }
        } catch (e: Exception) {
            Log.e("SupabaseProfileService", "GetProfile exception: ${e.message}", e)
            null
        }
    }

    suspend fun isUsernameTaken(username: String): Boolean = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured() || username.trim().isEmpty()) return@withContext false

        try {
            val encodedUsername = URLEncoder.encode(username.trim(), "UTF-8")
            val url = "${SupabaseConfig.url}/rest/v1/profiles?username=eq.$encodedUsername&select=id"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.publishableKey)
                .addHeader("Authorization", "Bearer ${SupabaseConfig.publishableKey}")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful && bodyStr.isNotEmpty()) {
                    val jsonArray = JSONArray(bodyStr)
                    return@withContext jsonArray.length() > 0
                }
            }
            false
        } catch (e: Exception) {
            Log.e("SupabaseProfileService", "CheckUsername exception: ${e.message}")
            false
        }
    }

    suspend fun updateDisplayMetadata(
        userId: String,
        accessToken: String,
        displayName: String? = null,
        avatar: String? = null
    ): Boolean = updateProfile(userId, accessToken, displayName = displayName, avatar = avatar)

    /**
     * Record match result using Supabase Database RPC function for secure server-side verification
     * and atomic increments (protecting against client-side stats forging).
     */
    suspend fun recordMatchResultRpc(
        accessToken: String,
        isWin: Boolean,
        isDraw: Boolean,
        xpEarned: Int,
        coinsEarned: Int
    ): Boolean = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured() || accessToken.isEmpty()) return@withContext false

        try {
            val url = "${SupabaseConfig.url}/rest/v1/rpc/record_match_result"
            val rpcParams = JSONObject().apply {
                put("p_is_win", isWin)
                put("p_is_draw", isDraw)
                put("p_xp_earned", xpEarned)
                put("p_coins_earned", coinsEarned)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.publishableKey)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .post(rpcParams.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                Log.d("SupabaseProfileService", "RecordMatchResultRpc Response Code: ${response.code}")
                if (response.isSuccessful) {
                    return@withContext true
                }
            }
            false
        } catch (e: Exception) {
            Log.e("SupabaseProfileService", "recordMatchResultRpc exception: ${e.message}")
            false
        }
    }

    suspend fun updateProfile(
        userId: String,
        accessToken: String,
        displayName: String? = null,
        avatar: String? = null,
        level: Int? = null,
        xp: Int? = null,
        coins: Int? = null,
        wins: Int? = null,
        losses: Int? = null,
        draws: Int? = null,
        gamesPlayed: Int? = null
    ): Boolean = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured() || userId.isEmpty() || accessToken.isEmpty()) return@withContext false

        try {
            val url = "${SupabaseConfig.url}/rest/v1/profiles?id=eq.$userId"
            val patchObj = JSONObject().apply {
                displayName?.let { put("display_name", it) }
                avatar?.let { put("avatar", it) }
                level?.let { put("level", it) }
                xp?.let { put("xp", it) }
                coins?.let { put("coins", it) }
                wins?.let { put("wins", it) }
                losses?.let { put("losses", it) }
                draws?.let { put("draws", it) }
                gamesPlayed?.let { put("games_played", it) }
                put("updated_at", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date()))
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.publishableKey)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .patch(patchObj.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                Log.d("SupabaseProfileService", "UpdateProfile Code: ${response.code}")
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e("SupabaseProfileService", "UpdateProfile exception: ${e.message}")
            false
        }
    }

    private fun parseProfileJson(obj: JSONObject, defaultId: String): PlayerProfile {
        val id = obj.optString("id", defaultId)
        val username = obj.optString("username", "اللاعب")
        val displayName = obj.optString("display_name", username)
        val level = obj.optInt("level", 1)
        val xp = obj.optInt("xp", 0)
        val coins = obj.optInt("coins", 0)
        val wins = obj.optInt("wins", 0)
        val losses = obj.optInt("losses", 0)
        val draws = obj.optInt("draws", 0)
        val gamesPlayed = obj.optInt("games_played", wins + losses + draws)
        val avatarStr = obj.optString("avatar", "1")

        // Parse avatar string into avatarId / customUri
        val avatarId = avatarStr.toIntOrNull() ?: 1
        val customUri = if (avatarStr.length > 20 || avatarStr.startsWith("data:") || avatarStr.startsWith("http") || avatarStr.startsWith("file:")) avatarStr else null

        return PlayerProfile(
            id = id,
            username = username,
            displayName = if (displayName.isNotEmpty()) displayName else username,
            avatarId = avatarId,
            avatarCustomUri = customUri,
            level = level,
            xp = xp,
            coins = coins,
            wins = wins,
            losses = losses,
            draws = draws,
            totalGames = gamesPlayed,
            isGuest = false
        )
    }
}
