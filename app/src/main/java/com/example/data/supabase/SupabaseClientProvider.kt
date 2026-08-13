package com.example.data.supabase

import com.example.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json

object SupabaseClientProvider {
    
    private val SUPABASE_URL: String = BuildConfig.SUPABASE_URL
    private val SUPABASE_ANON_KEY: String = BuildConfig.SUPABASE_ANON_KEY
    
    init {
        if (SUPABASE_URL.isEmpty() || SUPABASE_ANON_KEY.isEmpty()) {
            throw IllegalStateException(
                "❌ Supabase credentials not found!\n" +
                "Please add to gradle.properties:\n" +
                "SUPABASE_URL=https://your-project.supabase.co\n" +
                "SUPABASE_ANON_KEY=your-anon-key"
            )
        }
        println("✅ Supabase initialized with URL: $SUPABASE_URL")
    }
    
    val client: SupabaseClient by lazy {
        try {
            createSupabaseClient(SUPABASE_URL, SUPABASE_ANON_KEY) {
                install(Auth) {
                    saveSession = true
                }
                install(Storage)
                install(Logging) {
                    logger = object : Logger {
                        override fun log(message: String) {
                            println("📱 Supabase: $message")
                        }
                    }
                    level = LogLevel.BODY
                }
                install(ContentNegotiation) {
                    json()
                }
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to initialize Supabase client: ${e.message}", e)
        }
    }
    
    val authService: SupabaseAuthService by lazy {
        SupabaseAuthService(client)
    }
    
    val storageService: SupabaseStorageService by lazy {
        SupabaseStorageService(client)
    }
    
    suspend fun testConnection(): Boolean {
        return try {
            val user = authService.getCurrentUser()
            println("✅ Supabase connected! User: ${user?.email}")
            true
        } catch (e: Exception) {
            println("❌ Supabase connection failed: ${e.message}")
            false
        }
    }
}
