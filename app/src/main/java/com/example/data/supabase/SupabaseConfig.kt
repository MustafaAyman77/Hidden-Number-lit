package com.example.data.supabase

import com.example.BuildConfig

object SupabaseConfig {
    private const val DEFAULT_URL = "https://piavsqzyzqrmpurlflvc.supabase.co"
    private const val DEFAULT_PUBLISHABLE_KEY = "sb_publishable_28ZtHjX41UoNxZ7UaxGbPw_kMcXakkc"

    val url: String
        get() = BuildConfig.SUPABASE_URL.takeIf { it.isNotBlank() && !it.contains("your-") } ?: DEFAULT_URL

    val publishableKey: String
        get() = BuildConfig.SUPABASE_PUBLISHABLE_KEY.takeIf { it.isNotBlank() && !it.contains("your-") } ?: DEFAULT_PUBLISHABLE_KEY

    fun isConfigured(): Boolean {
        val u = url.trim()
        val k = publishableKey.trim()
        return u.isNotEmpty() && k.isNotEmpty() && !u.contains("your-") && !u.contains("YOUR_")
    }
}

