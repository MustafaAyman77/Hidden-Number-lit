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
            // التحقق من أن userId ليس فارغاً
            if (userId.isEmpty()) {
                return@withContext Result.failure(Exception("User ID is required"))
            }
            
            // 1. ضغط الصورة
            val compressedBytes = compressImage(context, uri, maxSize = 200) 
                ?: return@withContext Result.failure(Exception("فشل ضغط الصورة"))
            
            // 2. رفع الصورة إلى Supabase مع استبدال القديمة
            val filePath = "$userId/avatar.jpg"
            
            // محاولة الرفع مع معالجة أفضل للخطأ
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
                // معالجة أخطاء محددة
                val errorMessage = when {
                    e.message?.contains("Permission denied", ignoreCase = true) == true -> 
                        "ليس لديك صلاحية للرفع في هذا المجلد"
                    e.message?.contains("Bucket not found", ignoreCase = true) == true ->
                        "المجلد غير موجود"
                    e.message?.contains("Network", ignoreCase = true) == true ->
                        "خطأ في الاتصال بالشبكة"
                    else -> e.message ?: "فشل الرفع"
                }
                return@withContext Result.failure(Exception(errorMessage, e))
            }
            
            // 3. الحصول على الرابط العام
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
            
            // محاولة حذف الصورة
            try {
                supabaseClient.storage.from(bucketName).delete(listOf(filePath))
            } catch (e: Exception) {
                // إذا كانت الصورة غير موجودة، نعتبر العملية ناجحة
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
    
    suspend fun getAvatarUrl(userId: String): String? = withContext(Dispatchers.IO) {
        try {
            if (userId.isEmpty()) return@withContext null
            val filePath = "$userId/avatar.jpg"
            // التحقق من وجود الصورة
            val exists = try {
                supabaseClient.storage.from(bucketName).download(filePath)
                true
            } catch (e: Exception) {
                false
            }
            if (exists) {
                supabaseClient.storage.from(bucketName).publicUrl(filePath)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun compressImage(context: Context, uri: Uri, maxSize: Int = 200): ByteArray? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            if (originalBitmap == null) return null
            
            // حساب النسبة المناسبة للحفاظ على الأبعاد
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
            
            // تنظيف الذاكرة
            originalBitmap.recycle()
            scaledBitmap.recycle()
            
            baos.toByteArray()
        } catch (e: Exception) {
            null
        }
    }
}