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

class OnlineNetworkManager(private val scope: CoroutineScope) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS) // Infinite read timeout for WS
        .writeTimeout(10, TimeUnit.SECONDS)
        .pingInterval(10, TimeUnit.SECONDS) // Ping frames keep WS alive on slow networks
        .build()

    private var webSocket: WebSocket? = null
    private var currentRoomCode: String? = null
    private var currentMyPlayerId: String? = null
    private var pollingJob: Job? = null
    private var reconnectJob: Job? = null
    private var lastVoiceSendTime = 0L

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _latencyMs = MutableStateFlow(35)
    val latencyMs: StateFlow<Int> = _latencyMs.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<NetworkMessage>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<NetworkMessage> = _incomingMessages.asSharedFlow()

    private val _errorEvents = MutableSharedFlow<com.example.data.model.AppError>(extraBufferCapacity = 16)
    val errorEvents: SharedFlow<com.example.data.model.AppError> = _errorEvents.asSharedFlow()

    fun connectToRoom(roomCode: String, myPlayerId: String) {
        disconnect()
        val cleanCode = roomCode.uppercase().trim()
        currentRoomCode = cleanCode
        currentMyPlayerId = myPlayerId

        val wsUrl = "wss://ntfy.sh/hn_game_$cleanCode/ws"
        Log.d("OnlineNetworkManager", "Connecting WS to $wsUrl")

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("OnlineNetworkManager", "WebSocket Connected")
                _isConnected.value = true
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
                Log.e("OnlineNetworkManager", "WebSocket failed: ${t.message}, switching fallback")
                _isConnected.value = false
                scope.launch {
                    _errorEvents.emit(com.example.data.model.AppError.NetworkConnectionFailed())
                }
                scheduleAutoReconnect(cleanCode, myPlayerId)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _isConnected.value = false
            }
        })

        // Always run lightweight background HTTP poll alongside WS as a safety fallback
        startHttpFallbackPolling(cleanCode)
    }

    private fun scheduleAutoReconnect(roomCode: String, myPlayerId: String) {
        reconnectJob?.cancel()
        reconnectJob = scope.launch(Dispatchers.IO) {
            delay(4000)
            if (currentRoomCode == roomCode && !_isConnected.value) {
                Log.d("OnlineNetworkManager", "Attempting automatic WebSocket reconnect...")
                connectToRoom(roomCode, myPlayerId)
            }
        }
    }

    fun sendMessage(type: String, senderId: String, senderName: String, payload: String) {
        val room = currentRoomCode ?: return

        // Throttle high-frequency voice frames to prevent clogging weak connections
        if (type == "VOICE") {
            val now = System.currentTimeMillis()
            if (now - lastVoiceSendTime < 220) return
            lastVoiceSendTime = now
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
                // Post to ntfy topic endpoint
                val url = "https://ntfy.sh/hn_game_$room"
                val body = json.toRequestBody("text/plain".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .build()

                val startTime = System.currentTimeMillis()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        _latencyMs.value = (System.currentTimeMillis() - startTime).toInt().coerceAtLeast(15)
                    }
                }
            } catch (e: Exception) {
                Log.e("OnlineNetworkManager", "Failed to send msg: ${e.message}")
            }
        }
    }

    private val processedMsgKeys = java.util.Collections.synchronizedSet(LinkedHashSet<String>())

    private fun startHttpFallbackPolling(roomCode: String) {
        pollingJob?.cancel()
        pollingJob = scope.launch(Dispatchers.IO) {
            var lastSince = "all"
            _isConnected.value = true
            while (_isConnected.value && currentRoomCode == roomCode) {
                try {
                    val url = "https://ntfy.sh/hn_game_$roomCode/json?poll=1&since=$lastSince"
                    val request = Request.Builder().url(url).build()
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val bodyStr = response.body?.string() ?: ""
                            val lines = bodyStr.split("\n")
                            for (line in lines) {
                                if (line.trim().isNotEmpty()) {
                                    val jsonObj = JSONObject(line)
                                    val id = jsonObj.optString("id", "")
                                    if (id.isNotEmpty()) lastSince = id
                                    val msg = jsonObj.optString("message", "")
                                    if (msg.isNotEmpty()) {
                                        parseAndEmitMessage(msg)
                                    } else if (jsonObj.has("type")) {
                                        parseAndEmitMessage(line)
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("OnlineNetworkManager", "Polling error: ${e.message}")
                }
                delay(1200)
            }
        }
    }

    private fun parseAndEmitMessage(rawText: String) {
        try {
            val json = JSONObject(rawText)
            val type = json.optString("type", "")
            val senderId = json.optString("senderId", "")
            val timestamp = json.optLong("timestamp", 0L)
            val payload = json.optString("payload", "")

            // Deduplicate incoming messages across WS and HTTP polling
            val msgKey = "$type-$senderId-$timestamp-${payload.hashCode()}"
            if (processedMsgKeys.contains(msgKey)) return
            processedMsgKeys.add(msgKey)
            if (processedMsgKeys.size > 200) {
                val iterator = processedMsgKeys.iterator()
                if (iterator.hasNext()) {
                    iterator.next()
                    iterator.remove()
                }
            }

            if (type.isNotEmpty()) {
                val message = NetworkMessage(
                    type = type,
                    senderId = senderId,
                    senderName = json.optString("senderName", ""),
                    payload = payload,
                    timestamp = if (timestamp > 0) timestamp else System.currentTimeMillis()
                )
                scope.launch {
                    _incomingMessages.emit(message)
                }
            }
        } catch (e: Exception) {
            // Not a valid JSON game message
        }
    }

    fun disconnect() {
        pollingJob?.cancel()
        pollingJob = null
        reconnectJob?.cancel()
        reconnectJob = null
        try {
            webSocket?.close(1000, "Leaving room")
            webSocket = null
        } catch (e: Exception) {
            Log.e("OnlineNetworkManager", "Error closing WS: ${e.message}")
        }
        _isConnected.value = false
        currentRoomCode = null
    }
}
