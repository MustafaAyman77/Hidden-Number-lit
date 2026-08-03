package com.example.network

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.example.data.model.NetworkMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket

class LocalWifiNetworkManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var printWriter: PrintWriter? = null
    private var listenJob: Job? = null

    private val _isServer = MutableStateFlow(false)
    val isServer: StateFlow<Boolean> = _isServer.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _localIp = MutableStateFlow(getLocalIpAddress())
    val localIp: StateFlow<String> = _localIp.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<NetworkMessage>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<NetworkMessage> = _incomingMessages.asSharedFlow()

    fun startHosting(port: Int = 8888) {
        disconnect()
        _isServer.value = true
        _localIp.value = getLocalIpAddress()

        listenJob = scope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(port)
                Log.d("LocalWifi", "Server listening on port $port")

                val socket = serverSocket?.accept()
                if (socket != null) {
                    clientSocket = socket
                    printWriter = PrintWriter(socket.getOutputStream(), true)
                    _isConnected.value = true
                    listenToSocket(socket)
                }
            } catch (e: Exception) {
                Log.e("LocalWifi", "Server error: ${e.message}")
            }
        }
    }

    fun connectToHost(hostIp: String, port: Int = 8888) {
        disconnect()
        _isServer.value = false

        listenJob = scope.launch(Dispatchers.IO) {
            try {
                val socket = Socket(hostIp, port)
                clientSocket = socket
                printWriter = PrintWriter(socket.getOutputStream(), true)
                _isConnected.value = true
                listenToSocket(socket)
            } catch (e: Exception) {
                Log.e("LocalWifi", "Client connect error: ${e.message}")
                _isConnected.value = false
            }
        }
    }

    fun sendMessage(type: String, senderId: String, senderName: String, payload: String) {
        val json = JSONObject().apply {
            put("type", type)
            put("senderId", senderId)
            put("senderName", senderName)
            put("payload", payload)
            put("timestamp", System.currentTimeMillis())
        }.toString()

        scope.launch(Dispatchers.IO) {
            try {
                printWriter?.println(json)
            } catch (e: Exception) {
                Log.e("LocalWifi", "Send error: ${e.message}")
            }
        }
    }

    private fun listenToSocket(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let { raw ->
                    try {
                        val json = JSONObject(raw)
                        val message = NetworkMessage(
                            type = json.optString("type", ""),
                            senderId = json.optString("senderId", ""),
                            senderName = json.optString("senderName", ""),
                            payload = json.optString("payload", ""),
                            timestamp = json.optLong("timestamp", System.currentTimeMillis())
                        )
                        scope.launch { _incomingMessages.emit(message) }
                    } catch (e: Exception) {
                        Log.e("LocalWifi", "Json parse error: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("LocalWifi", "Socket stream read error: ${e.message}")
        } finally {
            _isConnected.value = false
        }
    }

    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address.address.size == 4) {
                        return address.hostAddress ?: "192.168.1.10"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("LocalWifi", "IP lookup error: ${e.message}")
        }
        return "192.168.43.1" // Common Hotspot default IP
    }

    fun disconnect() {
        listenJob?.cancel()
        listenJob = null
        try {
            printWriter?.close()
            clientSocket?.close()
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e("LocalWifi", "Disconnect error: ${e.message}")
        }
        _isConnected.value = false
        printWriter = null
        clientSocket = null
        serverSocket = null
    }
}
