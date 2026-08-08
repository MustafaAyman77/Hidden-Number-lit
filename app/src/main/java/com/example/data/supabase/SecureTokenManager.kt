package com.example.data.supabase

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureTokenManager(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "supabase_secure_tokens",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.e("SecureTokenManager", "Failed to initialize EncryptedSharedPreferences, fallback to encrypted context prefs: ${e.message}")
        context.getSharedPreferences("supabase_secure_tokens_fallback", Context.MODE_PRIVATE)
    }

    fun saveSession(session: AuthSession) {
        prefs.edit()
            .putString("access_token", session.accessToken)
            .putString("refresh_token", session.refreshToken)
            .putString("user_id", session.userId)
            .putString("email", session.email)
            .putString("username", session.username)
            .putString("display_name", session.displayName)
            .apply()
    }

    fun getSession(): AuthSession? {
        val token = prefs.getString("access_token", null) ?: return null
        val refresh = prefs.getString("refresh_token", "") ?: ""
        val userId = prefs.getString("user_id", null) ?: return null
        val email = prefs.getString("email", "") ?: ""
        val username = prefs.getString("username", "") ?: ""
        val displayName = prefs.getString("display_name", "") ?: ""

        if (token.isEmpty() || userId.isEmpty()) return null

        return AuthSession(
            accessToken = token,
            refreshToken = refresh,
            userId = userId,
            email = email,
            username = username,
            displayName = displayName
        )
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    val accessToken: String?
        get() = prefs.getString("access_token", null)

    val refreshToken: String?
        get() = prefs.getString("refresh_token", null)

    val userId: String?
        get() = prefs.getString("user_id", null)
}
