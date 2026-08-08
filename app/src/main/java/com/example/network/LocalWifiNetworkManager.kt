package com.example.network

import android.content.Context
import android.util.Log
import com.example.data.model.AppError
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList

class LocalWifiNetworkManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var clientPrintWriter: PrintWriter? = null
    private val hostClientWriters = CopyOnWriteArrayList<PrintWriter>()

    private var listenJob: Job? = null
    private var reconnectJob: Job? = null

    private val _isServer = MutableStateFlow(false)
    val isServer: StateFlow<Boolean> = _isServer.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _localIp = MutableStateFlow(getLocalIpAddress())
    val localIp: StateFlow<String> = _localIp.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<NetworkMessage>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<NetworkMessage> = _incomingMessages.asSharedFlow()

    private val _errorEvents = MutableSharedFlow<AppError>(extraBufferCapacity = 16)
    val errorEvents: SharedFlow<AppError> = _errorEvents.asSharedFlow()

    private var udpSocket: java.net.DatagramSocket? = null
    private var udpJob: Job? = null

    fun startHosting(port: Int = 8888) {
        disconnect()
        _isServer.value = true
        _localIp.value = getLocalIpAddress()

        // Start UDP Auto-Discovery responder on port 8889
        startUdpDiscoveryResponder()

        listenJob = scope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(port)
                _isConnected.value = true
                Log.d("LocalWifi", "Server listening on IP: ${_localIp.value}, port: $port")

                while (isActive && serverSocket?.isClosed == false) {
                    try {
                        val socket = serverSocket?.accept() ?: break
                        Log.d("LocalWifi", "Server accepted client connection from: ${socket.inetAddress}")
                        val writer = PrintWriter(socket.getOutputStream(), true)
                        hostClientWriters.add(writer)
                        _isConnected.value = true

                        // Launch reader for this connected client socket
                        launch(Dispatchers.IO) {
                            listenToSocket(socket, writer)
                        }
                    } catch (e: Exception) {
                        if (isActive && serverSocket?.isClosed == false) {
                            Log.e("LocalWifi", "Accept client error: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("LocalWifi", "Server socket bind error: ${e.message}")
            }
        }
    }

    private fun startUdpDiscoveryResponder() {
        udpJob?.cancel()
        udpJob = scope.launch(Dispatchers.IO) {
            try {
                val socket = java.net.DatagramSocket(8889)
                socket.broadcast = true
                udpSocket = socket
                val buf = ByteArray(256)

                while (isActive && !socket.isClosed) {
                    val packet = java.net.DatagramPacket(buf, buf.size)
                    socket.receive(packet)
                    val received = String(packet.data, 0, packet.length).trim()
                    if (received == "DISCOVER_CYBER_HOST") {
                        val currentIp = getLocalIpAddress()
                        val resp = "CYBER_HOST_HERE|$currentIp".toByteArray()
                        val respPacket = java.net.DatagramPacket(
                            resp,
                            resp.size,
                            packet.address,
                            packet.port
                        )
                        socket.send(respPacket)
                        Log.d("LocalWifi", "UDP Discovery responded to ${packet.address} with IP: $currentIp")
                    }
                }
            } catch (e: Exception) {
                Log.d("LocalWifi", "UDP Responder stopped/error: ${e.message}")
            }
        }
    }

    fun discoverAndConnect(onResult: (Boolean, String) -> Unit) {
        scope.launch(Dispatchers.IO) {
            var foundIp: String? = null
            try {
                val socket = java.net.DatagramSocket()
                socket.broadcast = true
                socket.soTimeout = 1200

                val req = "DISCOVER_CYBER_HOST".toByteArray()
                val targets = listOf(
                    java.net.InetAddress.getByName("255.255.255.255"),
                    java.net.InetAddress.getByName("192.168.43.255"),
                    java.net.InetAddress.getByName("192.168.1.255"),
                    java.net.InetAddress.getByName("192.168.0.255")
                )

                for (target in targets) {
                    try {
                        val packet = java.net.DatagramPacket(req, req.size, target, 8889)
                        socket.send(packet)
                    } catch (e: Exception) {}
                }

                val buf = ByteArray(256)
                val respPacket = java.net.DatagramPacket(buf, buf.size)
                socket.receive(respPacket)

                val resp = String(respPacket.data, 0, respPacket.length).trim()
                if (resp.startsWith("CYBER_HOST_HERE|")) {
                    foundIp = resp.substringAfter("CYBER_HOST_HERE|").ifEmpty { respPacket.address.hostAddress }
                } else {
                    foundIp = respPacket.address.hostAddress
                }
                socket.close()
            } catch (e: Exception) {
                Log.d("LocalWifi", "UDP Discovery search timeout: ${e.message}")
            }

            // Fallbacks if UDP broadcast failed
            val candidateIps = mutableListOf<String>()
            if (!foundIp.isNullOrEmpty()) candidateIps.add(foundIp)
            candidateIps.add(getGatewayOrHostIpAddress())
            candidateIps.add("192.168.43.1")
            candidateIps.add("192.168.1.1")
            candidateIps.add("192.168.0.1")

            var connected = false
            var finalIp = ""

            for (ip in candidateIps.distinct()) {
                if (connected) break
                try {
                    Log.d("LocalWifi", "Auto-Connect trying host candidate IP: $ip")
                    val testSocket = Socket()
                    testSocket.connect(java.net.InetSocketAddress(ip, 8888), 1200)
                    testSocket.close()

                    connected = true
                    finalIp = ip
                    connectToHost(ip)
                    break
                } catch (e: Exception) {
                    Log.d("LocalWifi", "Candidate IP $ip ping failed: ${e.message}")
                }
            }

            scope.launch(Dispatchers.Main) {
                onResult(connected, finalIp)
            }
        }
    }

    fun connectToHost(hostIp: String, port: Int = 8888) {
        disconnect()
        _isServer.value = false

        listenJob = scope.launch(Dispatchers.IO) {
            var attempt = 0
            val maxAttempts = 5
            while (isActive && !_isConnected.value && attempt < maxAttempts) {
                attempt++
                try {
                    Log.d("LocalWifi", "Client connecting to $hostIp:$port (Attempt $attempt/$maxAttempts)...")
                    val socket = Socket()
                    socket.connect(java.net.InetSocketAddress(hostIp, port), 2500)
                    clientSocket = socket
                    clientPrintWriter = PrintWriter(socket.getOutputStream(), true)
                    _isConnected.value = true
                    Log.d("LocalWifi", "Client connected successfully to $hostIp")

                    listenToSocket(socket, clientPrintWriter)
                } catch (e: Exception) {
                    Log.d("LocalWifi", "Client connect attempt $attempt/$maxAttempts failed: ${e.message}")
                    _isConnected.value = false
                    if (attempt == maxAttempts) {
                        Log.w("LocalWifi", "Host $hostIp unreachable after $maxAttempts attempts.")
                        scope.launch {
                            _errorEvents.emit(AppError.LocalWifiConnectionFailed(hostIp))
                        }
                    } else {
                        delay(1000)
                    }
                }
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
                if (_isServer.value) {
                    // Host broadcasts to all active client writers
                    val deadWriters = mutableListOf<PrintWriter>()
                    for (writer in hostClientWriters) {
                        try {
                            writer.println(json)
                            writer.flush()
                        } catch (e: Exception) {
                            deadWriters.add(writer)
                        }
                    }
                    hostClientWriters.removeAll(deadWriters)
                } else {
                    // Client sends to host
                    clientPrintWriter?.let { writer ->
                        writer.println(json)
                        writer.flush()
                    }
                }
            } catch (e: Exception) {
                Log.e("LocalWifi", "SendMessage error: ${e.message}")
            }
        }
    }

    private suspend fun listenToSocket(socket: Socket, writer: PrintWriter?) {
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
            if (_isServer.value) {
                writer?.let { hostClientWriters.remove(it) }
                if (hostClientWriters.isEmpty()) {
                    _isConnected.value = false
                }
            } else {
                _isConnected.value = false
            }
            try { socket.close() } catch (ignored: Exception) {}
        }
    }

    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            var hotspotIp: String? = null
            var wlanIp: String? = null
            var fallbackIp: String? = null

            while (interfaces != null && interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val name = networkInterface.name.lowercase()
                val addresses = networkInterface.inetAddresses

                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address.address.size == 4) {
                        val ip = address.hostAddress ?: continue
                        if (name.contains("ap") || name.contains("softap") || name.contains("swlan")) {
                            hotspotIp = ip
                            break
                        } else if (name.contains("wlan") || name.contains("p2p") || name.contains("rndis")) {
                            if (wlanIp == null) wlanIp = ip
                        } else if (fallbackIp == null) {
                            fallbackIp = ip
                        }
                    }
                }
                if (hotspotIp != null) break
            }

            if (hotspotIp != null) return hotspotIp
            if (wlanIp != null) return wlanIp
            if (fallbackIp != null) return fallbackIp
        } catch (e: Exception) {
            Log.e("LocalWifi", "IP lookup error: ${e.message}")
        }
        return "192.168.43.1" // Common Hotspot default gateway IP
    }

    fun getGatewayOrHostIpAddress(): String {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
            val dhcpInfo = wifiManager?.dhcpInfo
            if (dhcpInfo != null && dhcpInfo.gateway != 0) {
                val gatewayInt = dhcpInfo.gateway
                val gatewayIp = String.format(
                    java.util.Locale.US,
                    "%d.%d.%d.%d",
                    gatewayInt and 0xff,
                    gatewayInt shr 8 and 0xff,
                    gatewayInt shr 16 and 0xff,
                    gatewayInt shr 24 and 0xff
                )
                if (gatewayIp.isNotEmpty() && gatewayIp != "0.0.0.0") {
                    return gatewayIp
                }
            }
        } catch (e: Exception) {
            Log.e("LocalWifi", "DHCP lookup error: ${e.message}")
        }

        val myIp = getLocalIpAddress()
        if (myIp.contains(".")) {
            val parts = myIp.split(".")
            if (parts.size == 4) {
                return "${parts[0]}.${parts[1]}.${parts[2]}.1"
            }
        }
        return "192.168.43.1"
    }

    fun disconnect() {
        udpJob?.cancel()
        udpJob = null
        try { udpSocket?.close() } catch (e: Exception) {}
        udpSocket = null

        listenJob?.cancel()
        listenJob = null
        reconnectJob?.cancel()
        reconnectJob = null

        try {
            clientPrintWriter?.close()
            clientSocket?.close()
            for (w in hostClientWriters) {
                try { w.close() } catch (e: Exception) {}
            }
            hostClientWriters.clear()
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e("LocalWifi", "Disconnect error: ${e.message}")
        }
        _isConnected.value = false
        clientPrintWriter = null
        clientSocket = null
        serverSocket = null
    }
}
