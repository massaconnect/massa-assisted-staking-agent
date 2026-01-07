package com.massapay.agent.bridge

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Messages exchanged between MassaConnect and Massa Agent
 */

// Request from MassaConnect to Massa Agent
@Serializable
data class BridgeRequest(
    val id: String,
    val method: String,
    val params: Map<String, JsonElement> = emptyMap()
)

// Response from Massa Agent to MassaConnect
@Serializable
data class BridgeResponse(
    val id: String,
    val success: Boolean,
    val result: JsonElement? = null,
    val error: String? = null
)

// Event pushed from Massa Agent to MassaConnect
@Serializable
data class BridgeEvent(
    val type: String,
    val data: JsonElement
)

// Node status info
@Serializable
data class NodeStatus(
    val connected: Boolean,
    val nodeIp: String,
    val nodePort: Int,
    val version: String? = null,
    val networkVersion: String? = null,
    val currentCycle: Long? = null,
    val currentPeriod: Long? = null,
    val connectedPeers: Int? = null
)

// Staking info
@Serializable
data class StakingInfo(
    val address: String,
    val balance: String,
    val candidateBalance: String? = null,
    val finalRolls: Int,
    val candidateRolls: Int,
    val activeRolls: Int,
    val stakingAddress: String? = null,
    val deferredCredits: String = "0"
)

// Staking operation request
@Serializable
data class StakingOperation(
    val type: String, // "buy_rolls" or "sell_rolls"
    val rollCount: Int,
    val address: String,
    val fee: String = "0.01"
)

// Staking operation result
@Serializable
data class StakingOperationResult(
    val success: Boolean,
    val operationId: String? = null,
    val error: String? = null
)

// Connected device info
@Serializable
data class ConnectedDevice(
    val id: String,
    val name: String,
    val platform: String,
    val connectedAt: Long,
    val walletAddress: String? = null,
    val stakingInfo: StakingInfo? = null
)

// Bridge configuration
@Serializable
data class BridgeConfig(
    val nodeIp: String = "127.0.0.1",
    val nodeRpcPort: Int = 33034,
    val nodeGrpcPort: Int = 33035,
    val bridgePort: Int = 8765,
    val sessionTimeout: Long = 3600000 // 1 hour in ms
)

// QR Code data for pairing
@Serializable
data class PairingData(
    val type: String = "massa-agent",
    val version: String = "1.0.0",
    val host: String,
    val port: Int,
    val sessionId: String,
    val publicKey: String
)

/**
 * Bridge method names
 */
object BridgeMethods {
    // Connection
    const val CONNECT = "connect"
    const val DISCONNECT = "disconnect"
    const val PING = "ping"
    
    // Node status
    const val GET_NODE_STATUS = "get_node_status"
    const val GET_NETWORK_INFO = "get_network_info"
    
    // Staking
    const val GET_STAKING_INFO = "get_staking_info"
    const val BUY_ROLLS = "buy_rolls"
    const val SELL_ROLLS = "sell_rolls"
    const val GET_STAKING_ADDRESSES = "get_staking_addresses"
    const val ADD_STAKING_KEY = "add_staking_key"
    const val REMOVE_STAKING_KEY = "remove_staking_key"
    const val GET_REWARDS = "get_rewards"
    
    // Wallet operations (proxied to node)
    const val GET_ADDRESSES = "get_addresses"
    const val GET_OPERATIONS = "get_operations"
    const val SEND_OPERATIONS = "send_operations"
}

/**
 * Bridge event types
 */
object BridgeEvents {
    const val CONNECTED = "connected"
    const val DISCONNECTED = "disconnected"
    const val NODE_STATUS_CHANGED = "node_status_changed"
    const val STAKING_UPDATE = "staking_update"
    const val OPERATION_CONFIRMED = "operation_confirmed"
    const val ERROR = "error"
}

