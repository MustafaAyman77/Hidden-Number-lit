package com.example.data.supabase

import com.example.BuildConfig

object SupabaseConfig {
    private var overrideUrl: String? = null
    private var overrideKey: String? = null

    val url: String
        get() = overrideUrl ?: BuildConfig.SUPABASE_URL

    val publishableKey: String
        get() = overrideKey ?: BuildConfig.SUPABASE_PUBLISHABLE_KEY

    fun configure(supabaseUrl: String, supabaseKey: String) {
        overrideUrl = supabaseUrl
        overrideKey = supabaseKey
    }

    fun isConfigured(): Boolean {
        val u = url.trim()
        val k = publishableKey.trim()
        return u.isNotEmpty() && k.isNotEmpty() && !u.contains("your-") && !u.contains("YOUR_")
    }
}
