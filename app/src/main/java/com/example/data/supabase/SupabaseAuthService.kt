package com.example.data.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SupabaseAuthService(
    private val supabaseClient: SupabaseClient
) {
    
    suspend fun getCurrentUserId(): String? = withContext(Dispatchers.IO) {
        try {
            supabaseClient.auth.currentUserOrNull()?.id
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun getCurrentUser(): UserInfo? = withContext(Dispatchers.IO) {
        try {
            supabaseClient.auth.currentUserOrNull()
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun signInWithEmail(email: String, password: String): Result<UserInfo> = 
        withContext(Dispatchers.IO) {
            try {
                val result = supabaseClient.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                Result.success(result.user)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    suspend fun signUpWithEmail(email: String, password: String): Result<UserInfo> = 
        withContext(Dispatchers.IO) {
            try {
                val result = supabaseClient.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }
                Result.success(result.user)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supabaseClient.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun isAuthenticated(): Boolean = withContext(Dispatchers.IO) {
        try {
            supabaseClient.auth.currentUserOrNull() != null
        } catch (e: Exception) {
            false
        }
    }
}
