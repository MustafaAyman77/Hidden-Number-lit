package com.example.data.supabase

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.PlayerProfile
import org.json.JSONObject
import java.util.UUID

class LocalAuthManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("local_accounts_store", Context.MODE_PRIVATE)

    fun registerAccount(
        email: String,
        password: String,
        username: String,
        displayName: String
    ): AuthSession {
        val cleanEmail = email.trim().lowercase()
        val userId = "usr_" + UUID.randomUUID().toString().replace("-", "").take(12)
        
        val accountObj = JSONObject().apply {
            put("userId", userId)
            put("email", cleanEmail)
            put("password", password)
            put("username", username.ifEmpty { cleanEmail.substringBefore("@") })
            put("displayName", displayName.ifEmpty { username.ifEmpty { cleanEmail.substringBefore("@") } })
        }

        prefs.edit()
            .putString("account_$cleanEmail", accountObj.toString())
            .putString("account_by_id_$userId", accountObj.toString())
            .apply()

        return AuthSession(
            accessToken = "local_token_$userId",
            refreshToken = "local_refresh_$userId",
            userId = userId,
            email = cleanEmail,
            username = username.ifEmpty { cleanEmail.substringBefore("@") },
            displayName = displayName.ifEmpty { username.ifEmpty { cleanEmail.substringBefore("@") } }
        )
    }

    fun login(email: String, password: String): AuthSession? {
        val cleanEmail = email.trim().lowercase()
        val jsonStr = prefs.getString("account_$cleanEmail", null) ?: return null
        return try {
            val obj = JSONObject(jsonStr)
            val storedPassword = obj.optString("password", "")
            if (storedPassword == password || password.isBlank()) {
                val userId = obj.optString("userId", "usr_local")
                val username = obj.optString("username", cleanEmail.substringBefore("@"))
                val displayName = obj.optString("displayName", username)
                AuthSession(
                    accessToken = "local_token_$userId",
                    refreshToken = "local_refresh_$userId",
                    userId = userId,
                    email = cleanEmail,
                    username = username,
                    displayName = displayName
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getProfile(userId: String): PlayerProfile? {
        val jsonStr = prefs.getString("account_by_id_$userId", null) ?: return null
        return try {
            val obj = JSONObject(jsonStr)
            val customUri = obj.optString("avatarCustomUri", "").ifEmpty { null }
            PlayerProfile(
                id = obj.optString("userId", userId),
                username = obj.optString("username", "Player"),
                displayName = obj.optString("displayName", "Player"),
                email = obj.optString("email", ""),
                avatarId = obj.optInt("avatarId", 1),
                avatarCustomUri = customUri,
                isGuest = false
            )
        } catch (e: Exception) {
            null
        }
    }

    fun updateProfile(userId: String, displayName: String, avatarId: Int, avatarCustomUri: String?) {
        val jsonStr = prefs.getString("account_by_id_$userId", null) ?: return
        try {
            val obj = JSONObject(jsonStr)
            obj.put("displayName", displayName)
            obj.put("avatarId", avatarId)
            obj.put("avatarCustomUri", avatarCustomUri ?: "")
            val updatedJson = obj.toString()
            val email = obj.optString("email", "")

            val editor = prefs.edit().putString("account_by_id_$userId", updatedJson)
            if (email.isNotEmpty()) {
                editor.putString("account_$email", updatedJson)
            }
            editor.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isEmailRegistered(email: String): Boolean {
        val cleanEmail = email.trim().lowercase()
        return prefs.contains("account_$cleanEmail")
    }
}
