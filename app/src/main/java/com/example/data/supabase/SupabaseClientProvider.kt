package com.example.data.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.storage.Storage

object SupabaseClientProvider {
    // ⚠️ استبدل هذه القيم بقيم مشروعك الفعلية من Supabase Dashboard
    private const val SUPABASE_URL = "https://YOUR_PROJECT_ID.supabase.co"
    private const val SUPABASE_ANON_KEY = "YOUR_ANON_KEY"
    
    val client: SupabaseClient by lazy {
        createSupabaseClient(SUPABASE_URL, SUPABASE_ANON_KEY) {
            install(Auth) {
                // حفظ الجلسة محلياً
                saveSession = true
            }
            install(Storage)
        }
    }
    
    val authService: SupabaseAuthService by lazy {
        SupabaseAuthService(client)
    }
    
    val storageService: SupabaseStorageService by lazy {
        SupabaseStorageService(client)
    }
}