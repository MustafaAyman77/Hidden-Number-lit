package com.example.data.supabase

import android.content.Context
import android.net.Uri
import android.graphics.BitmapFactory
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class SupabaseStorageService(
    private val supabaseClient: SupabaseClient
) {
    private val bucketName = "avatars"
    
    suspend fun uploadAvatar(
        context: Context,
        uri: Uri,
        userId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (userId.isEmpty()) {
                return@withContext Result.failure(Exception("User ID is required"))
            }
            
            val compressedBytes = compressImage(context, uri, maxSize = 200) 
                ?: return@withContext Result.failure(Exception("Failed to compress image"))
            
            val filePath = "$userId/avatar.jpg"
            
            try {
                supabaseClient.storage.from(bucketName).upload(
                    path = filePath,
                    data = compressedBytes,
                    upsert = true
                ) {
                    headers {
                        "Content-Type" to "image/jpeg"
                        "Cache-Control" to "max-age=3600"
                    }
                }
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("Permission denied", ignoreCase = true) == true -> 
                        "Permission denied to upload"
                    e.message?.contains("Bucket not found", ignoreCase = true) == true ->
                        "Bucket not found"
                    e.message?.contains("Network", ignoreCase = true) == true ->
                        "Network error"
                    else -> e.message ?: "Upload failed"
                }
                return@withContext Result.failure(Exception(errorMessage, e))
            }
            
            val publicUrl = supabaseClient.storage.from(bucketName).publicUrl(filePath)
            Result.success(publicUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteAvatar(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (userId.isEmpty()) {
                return@withContext Result.failure(Exception("User ID is required"))
            }
            
            val filePath = "$userId/avatar.jpg"
            
            try {
                supabaseClient.storage.from(bucketName).delete(listOf(filePath))
            } catch (e: Exception) {
                if (e.message?.contains("not found", ignoreCase = true) == true) {
                    return@withContext Result.success(Unit)
                }
                throw e
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun compressImage(context: Context, uri: Uri, maxSize: Int = 200): ByteArray? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            if (originalBitmap == null) return null
            
            val scale = minOf(
                maxSize.toFloat() / originalBitmap.width,
                maxSize.toFloat() / originalBitmap.height
            )
            val targetW = maxOf(1, (originalBitmap.width * scale).toInt())
            val targetH = maxOf(1, (originalBitmap.height * scale).toInt())
            
            val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(
                originalBitmap, 
                targetW, 
                targetH, 
                true
            )
            
            val baos = ByteArrayOutputStream()
            scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, baos)
            
            originalBitmap.recycle()
            scaledBitmap.recycle()
            
            baos.toByteArray()
        } catch (e: Exception) {
            null
        }
    }
}
