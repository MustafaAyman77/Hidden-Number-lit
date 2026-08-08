package com.example.data.supabase

import com.example.BuildConfig

object SupabaseConfig {
    val url: String
        get() = BuildConfig.SUPABASE_URL

    val publishableKey: String
        get() = BuildConfig.SUPABASE_PUBLISHABLE_KEY

    fun isConfigured(): Boolean {
        val u = url.trim()
        val k = publishableKey.trim()
        return u.isNotEmpty() && k.isNotEmpty() && !u.contains("your-") && !u.contains("YOUR_")
    }
}
