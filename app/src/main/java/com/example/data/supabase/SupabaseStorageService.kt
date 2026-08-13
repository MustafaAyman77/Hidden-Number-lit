package com.example.data.supabase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

class SupabaseStorageService {

    companion object {
        private const val TAG = "SupabaseStorageService"
        private const val BUCKET_NAME = "avatars"
        private const val MAX_IMAGE_SIZE = 300 // px
        private const val COMPRESS_QUALITY = 85 // %
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jpegMediaType = "image/jpeg".toMediaType()

    /**
     * Upload avatar image to Supabase Storage and cache locally on device
     * @return Result<String> containing either the public URL or local file path
     */
    suspend fun uploadAvatar(
        context: Context,
        uri: Uri,
        userId: String,
        accessToken: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (userId.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("User ID is required"))
            }

            // 1. Compress image to high-efficiency JPEG byte array
            val compressedBytes = compressImage(context, uri, MAX_IMAGE_SIZE)
                ?: return@withContext Result.failure(IllegalStateException("Failed to read or compress image"))

            // 2. Save local file immediately for instant offline & cache availability
            val localFileUri = saveImageLocally(context, userId, compressedBytes)

            // 3. If Supabase is configured and accessToken is provided, upload to Supabase Storage
            if (SupabaseConfig.isConfigured() && accessToken.isNotEmpty()) {
                val filePath = "$userId/avatar.jpg"
                val uploadUrl = "${SupabaseConfig.url}/storage/v1/object/$BUCKET_NAME/$filePath"

                val request = Request.Builder()
                    .url(uploadUrl)
                    .addHeader("apikey", SupabaseConfig.publishableKey)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("x-upsert", "true")
                    .post(compressedBytes.toRequestBody(jpegMediaType))
                    .build()

                try {
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val publicUrl = "${SupabaseConfig.url}/storage/v1/object/public/$BUCKET_NAME/$filePath?t=${System.currentTimeMillis()}"
                            Log.d(TAG, "Uploaded avatar successfully to Supabase Storage: $publicUrl")
                            return@withContext Result.success(publicUrl)
                        } else {
                            Log.w(TAG, "Supabase Storage upload returned code ${response.code}, falling back to local storage")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Network error uploading to Supabase Storage: ${e.message}, using local storage")
                }
            }

            // Fallback to local stored image URI if offline or unconfigured
            Result.success(localFileUri ?: uri.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Upload avatar error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Save compressed image bytes to app internal storage dedicated avatar directory
     */
    fun saveImageLocally(context: Context, userId: String, imageBytes: ByteArray): String? {
        return try {
            val avatarsDir = File(context.filesDir, "user_avatars").apply { mkdirs() }
            val file = File(avatarsDir, "avatar_${userId}.jpg")
            FileOutputStream(file).use { out ->
                out.write(imageBytes)
                out.flush()
            }
            Uri.fromFile(file).toString()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save avatar locally: ${e.message}")
            null
        }
    }

    /**
     * Compress and downscale an image Uri
     */
    fun compressImage(context: Context, uri: Uri, maxSize: Int): ByteArray? {
        return try {
            val inputStream: InputStream = context.contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap == null) return null

            val scale = minOf(
                maxSize.toFloat() / originalBitmap.width,
                maxSize.toFloat() / originalBitmap.height,
                1.0f
            )

            val targetW = maxOf(1, (originalBitmap.width * scale).toInt())
            val targetH = maxOf(1, (originalBitmap.height * scale).toInt())

            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, targetW, targetH, true)
            val baos = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESS_QUALITY, baos)

            originalBitmap.recycle()
            scaledBitmap.recycle()

            baos.toByteArray()
        } catch (e: Exception) {
            Log.e(TAG, "Image compression failed: ${e.message}", e)
            null
        }
    }
}
