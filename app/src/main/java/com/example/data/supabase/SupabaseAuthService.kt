package com.example.data.supabase

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val email: String,
    val username: String = "",
    val displayName: String = ""
)

sealed class AuthResult<out T> {
    data class Success<out T>(val data: T) : AuthResult<T>()
    data class Error(val messageAr: String, val messageEn: String = messageAr) : AuthResult<Nothing>()
}

class SupabaseAuthService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun signUp(
        email: String,
        password: String,
        username: String,
        displayName: String
    ): AuthResult<AuthSession> = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured()) {
            return@withContext AuthResult.Error(
                "لم يتم ضبط إعدادات Supabase (SUPABASE_URL و SUPABASE_PUBLISHABLE_KEY)."
            )
        }

        try {
            val url = "${SupabaseConfig.url}/auth/v1/signup"
            val metadataObj = JSONObject().apply {
                put("username", username)
                put("display_name", displayName)
            }
            val requestObj = JSONObject().apply {
                put("email", email)
                put("password", password)
                put("data", metadataObj)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.publishableKey)
                .addHeader("Content-Type", "application/json")
                .post(requestObj.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                Log.d("SupabaseAuthService", "SignUp Response Code: ${response.code}, Body: $bodyStr")

                if (!response.isSuccessful) {
                    val errorMsg = parseErrorMessage(bodyStr, response.code)
                    return@withContext AuthResult.Error(errorMsg)
                }

                val json = JSONObject(bodyStr)
                val userObj = json.optJSONObject("user") ?: json
                val userId = userObj.optString("id", "")
                val accessToken = json.optString("access_token", "")
                val refreshToken = json.optString("refresh_token", "")

                if (userId.isEmpty()) {
                    return@withContext AuthResult.Error("فشل إنشاء الحساب. تأكد من البيانات وإعادة المحاولة.")
                }

                val session = AuthSession(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    userId = userId,
                    email = email,
                    username = username,
                    displayName = displayName
                )

                AuthResult.Success(session)
            }
        } catch (e: Exception) {
            Log.e("SupabaseAuthService", "SignUp Exception: ${e.message}", e)
            AuthResult.Error("تعذر الاتصال بالخادم. يرجى التحقق من اتصال الإنترنت والإعادة.")
        }
    }

    suspend fun login(
        email: String,
        password: String
    ): AuthResult<AuthSession> = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured()) {
            return@withContext AuthResult.Error(
                "لم يتم ضبط إعدادات Supabase (SUPABASE_URL و SUPABASE_PUBLISHABLE_KEY)."
            )
        }

        try {
            val url = "${SupabaseConfig.url}/auth/v1/token?grant_type=password"
            val requestObj = JSONObject().apply {
                put("email", email)
                put("password", password)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.publishableKey)
                .addHeader("Content-Type", "application/json")
                .post(requestObj.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                Log.d("SupabaseAuthService", "Login Response Code: ${response.code}")

                if (!response.isSuccessful) {
                    val errorMsg = parseErrorMessage(bodyStr, response.code)
                    return@withContext AuthResult.Error(errorMsg)
                }

                val json = JSONObject(bodyStr)
                val accessToken = json.optString("access_token", "")
                val refreshToken = json.optString("refresh_token", "")
                val userObj = json.optJSONObject("user")
                val userId = userObj?.optString("id", "") ?: ""
                val userEmail = userObj?.optString("email", email) ?: email

                val metadataObj = userObj?.optJSONObject("user_metadata")
                val username = metadataObj?.optString("username", "") ?: ""
                val displayName = metadataObj?.optString("display_name", "") ?: ""

                if (accessToken.isEmpty() || userId.isEmpty()) {
                    return@withContext AuthResult.Error("فشل تسجيل الدخول. البيانات غير مكتملة.")
                }

                val session = AuthSession(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    userId = userId,
                    email = userEmail,
                    username = username,
                    displayName = displayName
                )

                AuthResult.Success(session)
            }
        } catch (e: Exception) {
            Log.e("SupabaseAuthService", "Login Exception: ${e.message}", e)
            AuthResult.Error("تعذر الاتصال بالخادم. يرجى التحقق من اتصال الإنترنت والإعادة.")
        }
    }

    suspend fun refreshToken(refreshToken: String): AuthResult<AuthSession> = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured() || refreshToken.isEmpty()) {
            return@withContext AuthResult.Error("جلسة غير صالحة.")
        }

        try {
            val url = "${SupabaseConfig.url}/auth/v1/token?grant_type=refresh_token"
            val requestObj = JSONObject().apply {
                put("refresh_token", refreshToken)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.publishableKey)
                .addHeader("Content-Type", "application/json")
                .post(requestObj.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext AuthResult.Error("انتهت صلاحية الجلسة.")
                }

                val json = JSONObject(bodyStr)
                val newAccess = json.optString("access_token", "")
                val newRefresh = json.optString("refresh_token", refreshToken)
                val userObj = json.optJSONObject("user")
                val userId = userObj?.optString("id", "") ?: ""
                val userEmail = userObj?.optString("email", "") ?: ""

                if (newAccess.isEmpty() || userId.isEmpty()) {
                    return@withContext AuthResult.Error("انتهت صلاحية الجلسة.")
                }

                AuthResult.Success(
                    AuthSession(
                        accessToken = newAccess,
                        refreshToken = newRefresh,
                        userId = userId,
                        email = userEmail
                    )
                )
            }
        } catch (e: Exception) {
            AuthResult.Error("خطأ في تحديث الجلسة.")
        }
    }

    suspend fun sendPasswordReset(email: String): AuthResult<Unit> = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured()) {
            return@withContext AuthResult.Error("لم يتم ضبط إعدادات Supabase.")
        }

        try {
            val url = "${SupabaseConfig.url}/auth/v1/recover"
            val requestObj = JSONObject().apply {
                put("email", email)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.publishableKey)
                .addHeader("Content-Type", "application/json")
                .post(requestObj.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    AuthResult.Success(Unit)
                } else {
                    val bodyStr = response.body?.string() ?: ""
                    val errorMsg = parseErrorMessage(bodyStr, response.code)
                    AuthResult.Error(errorMsg)
                }
            }
        } catch (e: Exception) {
            AuthResult.Error("فشل إرسال رابط استعادة كلمة المرور.")
        }
    }

    suspend fun logout(accessToken: String) = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured() || accessToken.isEmpty()) return@withContext
        try {
            val url = "${SupabaseConfig.url}/auth/v1/logout"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.publishableKey)
                .addHeader("Authorization", "Bearer $accessToken")
                .post("{}".toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().close()
        } catch (e: Exception) {
            Log.e("SupabaseAuthService", "Logout exception: ${e.message}")
        }
    }

    private fun parseErrorMessage(responseBody: String, statusCode: Int): String {
        return try {
            val json = JSONObject(responseBody)
            val msg = json.optString("msg", json.optString("error_description", json.optString("message", "")))
            when {
                msg.contains("User already registered", ignoreCase = true) || msg.contains("already exists", ignoreCase = true) ->
                    "البريد الإلكتروني مسجل بالفعل. يرجى تسجيل الدخول."
                msg.contains("Invalid login credentials", ignoreCase = true) || msg.contains("invalid_credentials", ignoreCase = true) ->
                    "البريد الإلكتروني أو كلمة المرور غير صحيحة."
                msg.contains("Password should be at least", ignoreCase = true) ->
                    "كلمة المرور قصيرة جداً (يجب أن تكون 8 أحرف على الأقل)."
                msg.contains("rate limit", ignoreCase = true) ->
                    "تم تجاوز عدد المحاولات المسموح بها. يرجى الانتظار قليلاً."
                msg.isNotEmpty() -> msg
                else -> "حدث خطأ غير متوقع ($statusCode)."
            }
        } catch (e: Exception) {
            "حدث خطأ في الاتصال بالخادم ($statusCode)."
        }
    }
}
