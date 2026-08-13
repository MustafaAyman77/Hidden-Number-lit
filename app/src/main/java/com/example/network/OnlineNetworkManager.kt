package com.example.network

import android.util.Log
import com.example.data.model.NetworkMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.collections.LinkedHashSet

class OnlineNetworkManager(private val scope: CoroutineScope) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .pingInterval(10, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var currentRoomCode: String? = null
    private var currentMyPlayerId: String? = null
    private var reconnectJob: Job? = null
    private var lastVoiceSendTime = 0L
    private var isReconnecting = false

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _latencyMs = MutableStateFlow(35)
    val latencyMs: StateFlow<Int> = _latencyMs.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<NetworkMessage>(extraBufferCapacity = 128)
    val incomingMessages: SharedFlow<NetworkMessage> = _incomingMessages.asSharedFlow()

    private val _errorEvents = MutableSharedFlow<com.example.data.model.AppError>(extraBufferCapacity = 16)
    val errorEvents: SharedFlow<com.example.data.model.AppError> = _errorEvents.asSharedFlow()

    private val sentMessageCache = LinkedHashSet<String>()
    private val processedMsgKeys = LinkedHashSet<String>()

    /**
     * ✅ الاتصال بالغرفة عبر WebSocket فقط (Real-time)
     */
    fun connectToRoom(roomCode: String, myPlayerId: String) {
        disconnect()

        val cleanCode = roomCode.uppercase().trim()
        currentRoomCode = cleanCode
        currentMyPlayerId = myPlayerId
        isReconnecting = false

        val wsUrl = "wss://ntfy.sh/hn_game_$cleanCode/ws"
        Log.d("NetworkManager", "🔌 Connecting WebSocket: $wsUrl")

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("NetworkManager", "✅ WebSocket Connected!")
                _isConnected.value = true
                isReconnecting = false

                // إرسال رسالة انضمام فورية عند فتح الاتصال
                sendMessage("JOIN", myPlayerId, "", "")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val jsonObj = JSONObject(text)
                    val messageBody = jsonObj.optString("message", "")

                    if (messageBody.isNotEmpty()) {
                        parseAndEmitMessage(messageBody)
                    } else if (jsonObj.has("type")) {
                        parseAndEmitMessage(text)
                    }
                } catch (e: Exception) {
                    parseAndEmitMessage(text)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("NetworkManager", "❌ WebSocket failed: ${t.message}")
                _isConnected.value = false

                if (!isReconnecting) {
                    scheduleAutoReconnect(cleanCode, myPlayerId)
                }

                scope.launch {
                    _errorEvents.emit(com.example.data.model.AppError.NetworkConnectionFailed())
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("NetworkManager", "🔌 WebSocket closed: $reason")
                _isConnected.value = false

                if (code != 1000 && !isReconnecting) {
                    scheduleAutoReconnect(cleanCode, myPlayerId)
                }
            }
        })
    }

    /**
     * ✅ إعادة الاتصال التلقائي السريع
     */
    private fun scheduleAutoReconnect(roomCode: String, myPlayerId: String) {
        reconnectJob?.cancel()
        isReconnecting = true

        reconnectJob = scope.launch(Dispatchers.IO) {
            delay(1500)

            if (currentRoomCode == roomCode && !_isConnected.value) {
                Log.d("NetworkManager", "🔄 Attempting auto-reconnect...")
                connectToRoom(roomCode, myPlayerId)
                isReconnecting = false
            }
        }
    }

    /**
     * ✅ إرسال رسالة بسرعة
     */
    fun sendMessage(type: String, senderId: String, senderName: String, payload: String) {
        val room = currentRoomCode ?: return

        // التحكم في معدل إرسال الصوت
        if (type == "VOICE") {
            val now = System.currentTimeMillis()
            if (now - lastVoiceSendTime < 150) return
            lastVoiceSendTime = now
        }

        // منع تكرار رسائل JOIN المفرطة
        if (type == "JOIN") {
            val key = "JOIN_$senderId"
            if (sentMessageCache.contains(key)) return
            sentMessageCache.add(key)
            if (sentMessageCache.size > 50) {
                val iterator = sentMessageCache.iterator()
                if (iterator.hasNext()) {
                    iterator.next()
                    iterator.remove()
                }
            }
        }

        val json = JSONObject().apply {
            put("type", type)
            put("senderId", senderId)
            put("senderName", senderName)
            put("payload", payload)
            put("timestamp", System.currentTimeMillis())
        }.toString()

        scope.launch(Dispatchers.IO) {
            try {
                val url = "https://ntfy.sh/hn_game_$room"
                val body = json.toRequestBody("text/plain".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .build()

                val startTime = System.currentTimeMillis()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val latency = (System.currentTimeMillis() - startTime).toInt().coerceIn(15, 500)
                        _latencyMs.value = latency
                        Log.d("NetworkManager", "📤 Sent: $type ($latency ms)")
                    } else {
                        Log.e("NetworkManager", "❌ Send failed: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Log.e("NetworkManager", "❌ Send error: ${e.message}")
            }
        }
    }

    /**
     * ✅ معالجة الرسائل الواردة فوراً
     */
    private fun parseAndEmitMessage(rawText: String) {
        try {
            val json = JSONObject(rawText)
            val type = json.optString("type", "")
            val senderId = json.optString("senderId", "")
            val timestamp = json.optLong("timestamp", 0L)
            val payload = json.optString("payload", "")

            if (type.isEmpty() || senderId.isEmpty()) return
            if (type == "VOICE" && payload.isEmpty()) return

            val msgKey = "$type-$senderId-$timestamp-${payload.hashCode()}"
            if (processedMsgKeys.contains(msgKey)) return
            processedMsgKeys.add(msgKey)

            if (processedMsgKeys.size > 300) {
                val iterator = processedMsgKeys.iterator()
                repeat(50) {
                    if (iterator.hasNext()) {
                        iterator.next()
                        iterator.remove()
                    }
                }
            }

            val message = NetworkMessage(
                type = type,
                senderId = senderId,
                senderName = json.optString("senderName", ""),
                payload = payload,
                timestamp = if (timestamp > 0) timestamp else System.currentTimeMillis()
            )

            Log.d("NetworkManager", "📩 Received: $type from $senderId")

            scope.launch(Dispatchers.Main) {
                _incomingMessages.emit(message)
            }

        } catch (e: Exception) {
            Log.d("NetworkManager", "⚠️ Invalid message: ${e.message}")
        }
    }

    /**
     * ✅ قطع الاتصال
     */
    fun disconnect() {
        reconnectJob?.cancel()
        reconnectJob = null
        isReconnecting = false

        try {
            webSocket?.close(1000, "Leaving room")
            webSocket = null
        } catch (e: Exception) {
            Log.e("NetworkManager", "❌ Error closing WS: ${e.message}")
        }

        _isConnected.value = false
        currentRoomCode = null
        sentMessageCache.clear()
        processedMsgKeys.clear()

        Log.d("NetworkManager", "🔌 Disconnected")
    }

    /**
     * ✅ التحقق من الاتصال
     */
    fun isConnectedToRoom(): Boolean {
        return _isConnected.value && webSocket != null
    }
}
