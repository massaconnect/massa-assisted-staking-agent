package com.massapay.agent.bridge

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.net.InetAddress
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * WebSocket server that acts as a bridge between MassaConnect and Massa Node
 */
class BridgeServer(
    private val config: BridgeConfig = BridgeConfig()
) {
    private val json = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }
    
    private var server: ApplicationEngine? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Node client
    private val nodeClient = MassaNodeClient(config.nodeIp)
    
    // Connected sessions
    private val sessions = ConcurrentHashMap<String, WebSocketSession>()
    private val connectedDevices = ConcurrentHashMap<String, ConnectedDevice>()
    
    // State flows
    private val _serverState = MutableStateFlow(BridgeServerState())
    val serverState: StateFlow<BridgeServerState> = _serverState.asStateFlow()
    
    private val _events = MutableSharedFlow<BridgeServerEvent>()
    val events: SharedFlow<BridgeServerEvent> = _events.asSharedFlow()
    
    // Current session for pairing
    private var currentSessionId: String = UUID.randomUUID().toString()
    
    /**
     * Start the WebSocket server
     */
    fun start() {
        if (server != null) {
            println("[BridgeServer] Server already running")
            return
        }
        
        scope.launch {
            try {
                server = embeddedServer(Netty, port = config.bridgePort) {
                    install(WebSockets) {
                        pingPeriod = Duration.ofSeconds(0)
                        timeout = Duration.ofSeconds(0)
                        maxFrameSize = Long.MAX_VALUE
                        masking = false
                    }
                    
                    routing {
                        webSocket("/bridge") {
                            handleConnection(this)
                        }
                        
                        // Health check endpoint
                        webSocket("/health") {
                            send(Frame.Text("""{"status":"ok","version":"1.0.0"}"""))
                            close(CloseReason(CloseReason.Codes.NORMAL, "Health check complete"))
                        }
                    }
                }.start(wait = false)
                
                val localIp = getLocalIpAddress()
                _serverState.update { it.copy(
                    isRunning = true,
                    host = localIp,
                    port = config.bridgePort
                ) }
                
                println("[BridgeServer] Started on $localIp:${config.bridgePort}")
                _events.emit(BridgeServerEvent.Started(localIp, config.bridgePort))
                
                // Start node status polling
                startNodeStatusPolling()
                
            } catch (e: Exception) {
                println("[BridgeServer] Failed to start: ${e.message}")
                _events.emit(BridgeServerEvent.Error("Failed to start server: ${e.message}"))
            }
        }
    }
    
    /**
     * Stop the server
     */
    fun stop() {
        scope.launch {
            server?.stop(1000, 2000)
            server = null
            _serverState.update { it.copy(isRunning = false) }
            _events.emit(BridgeServerEvent.Stopped)
            println("[BridgeServer] Stopped")
        }
    }

    /**
     * Disconnect all connected devices
     */
    fun disconnectAllDevices() {
        scope.launch {
            println("[BridgeServer] Disconnecting all devices...")
            val devicesCopy = connectedDevices.toList()
            devicesCopy.forEach { (sessionId, device) ->
                try {
                    sessions[sessionId]?.close(io.ktor.websocket.CloseReason(
                        io.ktor.websocket.CloseReason.Codes.NORMAL,
                        "Disconnected by server"
                    ))
                    println("[BridgeServer] Closed session: $sessionId")
                } catch (e: Exception) {
                    println("[BridgeServer] Error closing session $sessionId: ${e.message}")
                }
                sessions.remove(sessionId)
                connectedDevices.remove(sessionId)
                _serverState.update { state ->
                    state.copy(connectedDevices = state.connectedDevices - device)
                }
                _events.emit(BridgeServerEvent.DeviceDisconnected(device))
            }
            println("[BridgeServer] Disconnected all devices")
        }
    }

    /**
     * Disconnect a specific device by its ID
     */
    fun disconnectDevice(deviceId: String) {
        scope.launch {
            println("[BridgeServer] Disconnecting device: $deviceId")
            val device = connectedDevices[deviceId]
            if (device != null) {
                try {
                    sessions[deviceId]?.close(io.ktor.websocket.CloseReason(
                        io.ktor.websocket.CloseReason.Codes.NORMAL,
                        "Disconnected by server"
                    ))
                    println("[BridgeServer] Closed session: $deviceId")
                } catch (e: Exception) {
                    println("[BridgeServer] Error closing session $deviceId: ${e.message}")
                }
                sessions.remove(deviceId)
                connectedDevices.remove(deviceId)
                _serverState.update { state ->
                    state.copy(connectedDevices = state.connectedDevices - device)
                }
                _events.emit(BridgeServerEvent.DeviceDisconnected(device))
                println("[BridgeServer] Device disconnected: ${device.name}")
            } else {
                println("[BridgeServer] Device not found: $deviceId")
            }
        }
    }
    
    /**
     * Handle WebSocket connection
     */
    private suspend fun handleConnection(session: WebSocketSession) {
        val sessionId = UUID.randomUUID().toString()
        sessions[sessionId] = session
        
        println("[BridgeServer] New connection: $sessionId")
        
        try {
            // Wait for device info
            for (frame in session.incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        handleMessage(sessionId, session, text)
                    }
                    is Frame.Close -> {
                        println("[BridgeServer] Connection closed: $sessionId")
                        break
                    }
                    else -> {}
                }
            }
        } catch (e: Exception) {
            println("[BridgeServer] Connection error: ${e.message}")
        } finally {
            sessions.remove(sessionId)
            val device = connectedDevices.remove(sessionId)
            if (device != null) {
                _serverState.update { state ->
                    state.copy(connectedDevices = state.connectedDevices - device)
                }
                _events.emit(BridgeServerEvent.DeviceDisconnected(device))
            }
        }
    }
    
    /**
     * Handle incoming message
     */
    private suspend fun handleMessage(sessionId: String, session: WebSocketSession, text: String) {
        try {
            val request = json.decodeFromString<BridgeRequest>(text)
            println("[BridgeServer] Request: ${request.method} from $sessionId")
            
            val response = when (request.method) {
                BridgeMethods.CONNECT -> handleConnect(sessionId, request)
                BridgeMethods.DISCONNECT -> handleDisconnect(sessionId)
                BridgeMethods.PING -> handlePing(request)
                BridgeMethods.GET_NODE_STATUS -> handleGetNodeStatus(request)
                BridgeMethods.GET_NETWORK_INFO -> handleGetNetworkInfo(request)
                BridgeMethods.GET_STAKING_INFO -> handleGetStakingInfo(sessionId, request)
                BridgeMethods.BUY_ROLLS -> handleBuyRolls(request)
                BridgeMethods.SELL_ROLLS -> handleSellRolls(request)
                BridgeMethods.GET_STAKING_ADDRESSES -> handleGetStakingAddresses(request)
                BridgeMethods.ADD_STAKING_KEY -> handleAddStakingKey(request)
                BridgeMethods.REMOVE_STAKING_KEY -> handleRemoveStakingKey(request)
                BridgeMethods.GET_REWARDS -> handleGetRewards(request)
                BridgeMethods.SEND_OPERATIONS -> handleSendOperations(request)
                BridgeMethods.GET_OPERATIONS -> handleGetOperations(request)
                else -> BridgeResponse(request.id, false, error = "Unknown method: ${request.method}")
            }
            
            session.send(Frame.Text(json.encodeToString(response)))
            
        } catch (e: Exception) {
            println("[BridgeServer] Error handling message: ${e.message}")
            val errorResponse = BridgeResponse("error", false, error = e.message)
            session.send(Frame.Text(json.encodeToString(errorResponse)))
        }
    }
    
    // === Request Handlers ===
    
    private suspend fun handleConnect(sessionId: String, request: BridgeRequest): BridgeResponse {
        val deviceName = request.params["deviceName"]?.jsonPrimitive?.content ?: "Unknown"
        val platform = request.params["platform"]?.jsonPrimitive?.content ?: "Unknown"
        val walletAddress = request.params["walletAddress"]?.jsonPrimitive?.content
        
        val device = ConnectedDevice(
            id = sessionId,
            name = deviceName,
            platform = platform,
            connectedAt = System.currentTimeMillis(),
            walletAddress = walletAddress
        )
        
        connectedDevices[sessionId] = device
        _serverState.update { state ->
            state.copy(connectedDevices = state.connectedDevices + device)
        }
        _events.emit(BridgeServerEvent.DeviceConnected(device))
        
        // Get initial node status
        val nodeStatus = nodeClient.getNodeStatus().getOrNull()
        
        return BridgeResponse(
            id = request.id,
            success = true,
            result = buildJsonObject {
                put("sessionId", sessionId)
                put("nodeConnected", nodeStatus?.connected ?: false)
                if (nodeStatus != null) {
                    put("nodeStatus", json.encodeToJsonElement(nodeStatus))
                }
            }
        )
    }
    
    private suspend fun handleDisconnect(sessionId: String): BridgeResponse {
        val device = connectedDevices.remove(sessionId)
        if (device != null) {
            _serverState.update { state ->
                state.copy(connectedDevices = state.connectedDevices - device)
            }
            _events.emit(BridgeServerEvent.DeviceDisconnected(device))
        }
        return BridgeResponse("disconnect", true)
    }
    
    private fun handlePing(request: BridgeRequest): BridgeResponse {
        return BridgeResponse(
            id = request.id,
            success = true,
            result = buildJsonObject {
                put("pong", System.currentTimeMillis())
            }
        )
    }
    
    private suspend fun handleGetNodeStatus(request: BridgeRequest): BridgeResponse {
        val result = nodeClient.getNodeStatus()
        return if (result.isSuccess) {
            BridgeResponse(
                id = request.id,
                success = true,
                result = json.encodeToJsonElement(result.getOrThrow())
            )
        } else {
            BridgeResponse(request.id, false, error = result.exceptionOrNull()?.message)
        }
    }
    
    private suspend fun handleGetNetworkInfo(request: BridgeRequest): BridgeResponse {
        val result = nodeClient.getNetworkInfo()
        return if (result.isSuccess) {
            BridgeResponse(request.id, true, result = result.getOrThrow())
        } else {
            BridgeResponse(request.id, false, error = result.exceptionOrNull()?.message)
        }
    }
    
    private suspend fun handleGetStakingInfo(sessionId: String, request: BridgeRequest): BridgeResponse {
        val address = request.params["address"]?.jsonPrimitive?.content
            ?: return BridgeResponse(request.id, false, error = "Address required")
        
        val result = nodeClient.getStakingInfo(address)
        return if (result.isSuccess) {
            val stakingInfo = result.getOrThrow()
            
            // Update device with staking info and wallet address
            connectedDevices[sessionId]?.let { device ->
                val updatedDevice = device.copy(stakingInfo = stakingInfo, walletAddress = address)
                connectedDevices[sessionId] = updatedDevice
                _serverState.update { state -> state.copy(connectedDevices = connectedDevices.values.toList()) }
            }
            
            BridgeResponse(
                id = request.id,
                success = true,
                result = json.encodeToJsonElement(stakingInfo)
            )
        } else {
            BridgeResponse(request.id, false, error = result.exceptionOrNull()?.message)
        }
    }
    
    private suspend fun handleBuyRolls(request: BridgeRequest): BridgeResponse {
        val address = request.params["address"]?.jsonPrimitive?.content
            ?: return BridgeResponse(request.id, false, error = "Address required")
        val rollCount = request.params["rollCount"]?.jsonPrimitive?.intOrNull
            ?: return BridgeResponse(request.id, false, error = "Roll count required")
        val fee = request.params["fee"]?.jsonPrimitive?.content ?: "0.01"
        
        val result = nodeClient.prepareBuyRolls(address, rollCount, fee)
        return if (result.isSuccess) {
            BridgeResponse(request.id, true, result = result.getOrThrow())
        } else {
            BridgeResponse(request.id, false, error = result.exceptionOrNull()?.message)
        }
    }
    
    private suspend fun handleSellRolls(request: BridgeRequest): BridgeResponse {
        val address = request.params["address"]?.jsonPrimitive?.content
            ?: return BridgeResponse(request.id, false, error = "Address required")
        val rollCount = request.params["rollCount"]?.jsonPrimitive?.intOrNull
            ?: return BridgeResponse(request.id, false, error = "Roll count required")
        val fee = request.params["fee"]?.jsonPrimitive?.content ?: "0.01"
        
        val result = nodeClient.prepareSellRolls(address, rollCount, fee)
        return if (result.isSuccess) {
            BridgeResponse(request.id, true, result = result.getOrThrow())
        } else {
            BridgeResponse(request.id, false, error = result.exceptionOrNull()?.message)
        }
    }
    
    private suspend fun handleGetStakingAddresses(request: BridgeRequest): BridgeResponse {
        // Use private API to get staking addresses
        val result = nodeClient.getStakingAddressesPrivate()
        return if (result.isSuccess) {
            BridgeResponse(
                id = request.id,
                success = true,
                result = buildJsonArray {
                    result.getOrThrow().forEach { add(it) }
                }
            )
        } else {
            BridgeResponse(request.id, false, error = result.exceptionOrNull()?.message)
        }
    }

    private suspend fun handleSendOperations(request: BridgeRequest): BridgeResponse {
        val operations = request.params["operations"]?.jsonArray
            ?: return BridgeResponse(request.id, false, error = "Operations required")
        
        val result = nodeClient.sendOperations(operations)
        return if (result.isSuccess) {
            BridgeResponse(
                id = request.id,
                success = true,
                result = buildJsonArray {
                    result.getOrThrow().forEach { add(it) }
                }
            )
        } else {
            BridgeResponse(request.id, false, error = result.exceptionOrNull()?.message)
        }
    }
    
    private suspend fun handleGetOperations(request: BridgeRequest): BridgeResponse {
        val operationIds = request.params["operationIds"]?.jsonArray?.map { 
            it.jsonPrimitive.content 
        } ?: return BridgeResponse(request.id, false, error = "Operation IDs required")
        
        val result = nodeClient.getOperations(operationIds)
        return if (result.isSuccess) {
            BridgeResponse(request.id, true, result = result.getOrThrow())
        } else {
            BridgeResponse(request.id, false, error = result.exceptionOrNull()?.message)
        }
    }
    

    private suspend fun handleAddStakingKey(request: BridgeRequest): BridgeResponse {
        val secretKey = request.params["secretKey"]?.jsonPrimitive?.content
            ?: return BridgeResponse(request.id, false, error = "Secret key required")

        println("[BridgeServer] Adding staking key to node...")
        val result = nodeClient.addStakingSecretKey(secretKey)
        
        return if (result.isSuccess) {
            val addresses = nodeClient.getStakingAddressesPrivate().getOrNull() ?: emptyList()
            println("[BridgeServer] Staking key added! Active addresses: $addresses")
            BridgeResponse(
                id = request.id,
                success = true,
                result = buildJsonObject {
                    put("success", true)
                    put("message", "Staking key registered on node")
                    put("stakingAddresses", buildJsonArray { addresses.forEach { add(it) } })
                }
            )
        } else {
            val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
            println("[BridgeServer] Failed to add staking key: $errorMsg")
            BridgeResponse(request.id, false, error = "Failed to register: $errorMsg")
        }
    }

    private suspend fun handleRemoveStakingKey(request: BridgeRequest): BridgeResponse {
        val address = request.params["address"]?.jsonPrimitive?.content
            ?: return BridgeResponse(request.id, false, error = "Address required")

        println("[BridgeServer] Removing staking address: $address")
        val result = nodeClient.removeStakingAddress(address)
        
        return if (result.isSuccess) {
            println("[BridgeServer] Staking address removed")
            BridgeResponse(
                id = request.id,
                success = true,
                result = buildJsonObject {
                    put("success", true)
                    put("message", "Staking stopped for $address")
                }
            )
        } else {
            val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
            BridgeResponse(request.id, false, error = "Failed: $errorMsg")
        }
    }


    private suspend fun handleGetRewards(request: BridgeRequest): BridgeResponse {
        val address = request.params["address"]?.jsonPrimitive?.content
            ?: return BridgeResponse(request.id, false, error = "Address required")
        
        return BridgeResponse(
            id = request.id,
            success = true,
            result = buildJsonObject {
                put("address", address)
                put("totalRewards", "0")
                put("cycleRewards", buildJsonArray {})
            }
        )
    }

    /**
     * Start polling node status
     */
    private fun startNodeStatusPolling() {
        scope.launch {
            while (isActive) {
                try {
                    val connected = nodeClient.isNodeConnected()
                    val currentState = _serverState.value
                    
                    if (connected != currentState.nodeConnected) {
                        _serverState.update { it.copy(nodeConnected = connected) }
                        
                        // Notify all connected devices
                        broadcastEvent(BridgeEvent(
                            type = BridgeEvents.NODE_STATUS_CHANGED,
                            data = buildJsonObject { put("connected", connected) }
                        ))
                    }
                    
                    // If connected, get more detailed status
                    if (connected) {
                        nodeClient.getNodeStatus().getOrNull()?.let { status ->
                            _serverState.update { it.copy(nodeStatus = status) }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore polling errors
                }
                
                delay(5000) // Poll every 5 seconds
            }
        }
    }
    
    /**
     * Broadcast event to all connected devices
     */
    private suspend fun broadcastEvent(event: BridgeEvent) {
        val message = json.encodeToString(event)
        sessions.values.forEach { session ->
            try {
                session.send(Frame.Text(message))
            } catch (e: Exception) {
                // Ignore send errors
            }
        }
    }
    
    /**
     * Generate pairing data for QR code
     */
    fun generatePairingData(): PairingData {
        currentSessionId = UUID.randomUUID().toString()
        return PairingData(
            host = _serverState.value.host,
            port = config.bridgePort,
            sessionId = currentSessionId,
            publicKey = "pk_${System.currentTimeMillis()}" // TODO: Real key pair
        )
    }
    
    /**
     * Get local IP address
     */
    private fun getLocalIpAddress(): String {
        return try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is java.net.Inet4Address && !addr.isLoopbackAddress) {
                        return addr.hostAddress
                    }
                }
            }
            InetAddress.getLocalHost().hostAddress
        } catch (e: Exception) {
            "127.0.0.1"
        }
    }
    
    /**
     * Update node configuration
     */
    fun updateNodeConfig(nodeIp: String, rpcPort: Int) {
        // Would need to recreate nodeClient - for now just track config
        _serverState.update { it.copy(
            nodeIp = nodeIp,
            nodePort = rpcPort
        ) }
    }
}

/**
 * Bridge server state
 */
data class BridgeServerState(
    val isRunning: Boolean = false,
    val host: String = "0.0.0.0",
    val port: Int = 8765,
    val nodeConnected: Boolean = false,
    val nodeIp: String = "127.0.0.1",
    val nodePort: Int = 33034,
    val nodeStatus: NodeStatus? = null,
    val connectedDevices: List<ConnectedDevice> = emptyList()
)

/**
 * Bridge server events
 */
sealed class BridgeServerEvent {
    data class Started(val host: String, val port: Int) : BridgeServerEvent()
    object Stopped : BridgeServerEvent()
    data class DeviceConnected(val device: ConnectedDevice) : BridgeServerEvent()
    data class DeviceDisconnected(val device: ConnectedDevice) : BridgeServerEvent()
    data class Error(val message: String) : BridgeServerEvent()
}

