package com.massapay.agent.bridge

import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import java.util.Base64
import java.net.InetAddress

/**
 * Client to communicate with Massa Node via JSON-RPC
 * Auto-detects port and configuration for any user setup
 */
class MassaNodeClient(
    private var nodeIp: String = "127.0.0.1"
) {
    // Common Massa ports to try (in order of preference)
    private val PUBLIC_API_PORTS = listOf(33035, 33034, 8080, 8545)
    private val PRIVATE_API_PORTS = listOf(33034, 33035)
    
    // Auto-detected port (null until detected)
    private var detectedPort: Int? = null
    private var isPrivateApi: Boolean = false
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val rpcUrl: String
        get() = "http://$nodeIp:${detectedPort ?: 33035}/api/v2"

    /**
     * Auto-detect the correct port by trying common Massa node ports
     */
    suspend fun autoDetectPort(): Int? {
        return withContext(Dispatchers.IO) {
            println("[MassaNodeClient] Auto-detecting node port...")
            
            // Try public API ports first (most common for users)
            for (port in PUBLIC_API_PORTS) {
                if (tryPort(port)) {
                    detectedPort = port
                    println("[MassaNodeClient] Found node on port $port")
                    return@withContext port
                }
            }
            
            println("[MassaNodeClient] No node found on standard ports")
            null
        }
    }
    
    /**
     * Try connecting to a specific port
     */
    private suspend fun tryPort(port: Int): Boolean {
        return try {
            val testUrl = "http://$nodeIp:$port/api/v2"
            val requestBody = buildJsonObject {
                put("jsonrpc", "2.0")
                put("id", 1)
                put("method", "get_status")
                put("params", JsonArray(emptyList()))
            }
            
            val request = Request.Builder()
                .url(testUrl)
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return false
            
            val jsonResponse = json.parseToJsonElement(body).jsonObject
            
            // Check if it's a valid response (has result, not wrong API error)
            if (jsonResponse.containsKey("result")) {
                return true
            }
            
            // Check for "wrong API" error - means port works but wrong API type
            val error = jsonResponse["error"]?.jsonObject
            val errorCode = error?.get("code")?.jsonPrimitive?.intOrNull
            if (errorCode == -32019) {
                // Wrong API type, but port is valid - try other type
                println("[MassaNodeClient] Port $port responds but wrong API type")
                return false
            }
            
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Initialize connection - auto-detect if needed
     */
    suspend fun initialize(): Boolean {
        if (detectedPort == null) {
            autoDetectPort()
        }
        return detectedPort != null && isNodeConnected()
    }

    /**
     * Execute JSON-RPC call to Massa node
     */
    suspend fun rpcCall(method: String, params: JsonElement = JsonArray(emptyList())): Result<JsonElement> {
        // Auto-detect port if not set
        if (detectedPort == null) {
            autoDetectPort()
            if (detectedPort == null) {
                return Result.failure(Exception("No Massa node found. Please ensure your node is running."))
            }
        }
        
        return withContext(Dispatchers.IO) {
            try {
                val requestBody = buildJsonObject {
                    put("jsonrpc", "2.0")
                    put("id", System.currentTimeMillis())
                    put("method", method)
                    put("params", params)
                }

                val request = Request.Builder()
                    .url(rpcUrl)
                    .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: throw Exception("Empty response")

                val jsonResponse = json.parseToJsonElement(body).jsonObject

                if (jsonResponse.containsKey("error")) {
                    val error = jsonResponse["error"]?.jsonObject
                    val message = error?.get("message")?.jsonPrimitive?.content ?: "Unknown error"
                    val code = error?.get("code")?.jsonPrimitive?.intOrNull
                    
                    // If wrong API, try to switch ports
                    if (code == -32019) {
                        println("[MassaNodeClient] Wrong API on port $detectedPort, re-detecting...")
                        detectedPort = null
                        autoDetectPort()
                        return@withContext Result.failure(Exception("Node API mismatch, please retry"))
                    }
                    
                    Result.failure(Exception(message))
                } else {
                    val result = jsonResponse["result"] ?: JsonNull
                    Result.success(result)
                }
            } catch (e: Exception) {
                // Connection failed, reset detected port to retry next time
                if (e.message?.contains("Connection refused") == true ||
                    e.message?.contains("timeout") == true) {
                    detectedPort = null
                }
                Result.failure(e)
            }
        }
    }

    /**
     * Check if node is reachable
     */
    suspend fun isNodeConnected(): Boolean {
        return try {
            val result = rpcCall("get_status")
            result.isSuccess
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get node status
     */
    suspend fun getNodeStatus(): Result<NodeStatus> {
        return try {
            val result = rpcCall("get_status").getOrThrow()
            val statusObj = result.jsonObject

            val version = statusObj["version"]?.jsonPrimitive?.content
            val config = statusObj["config"]?.jsonObject
            val networkVersion = config?.get("genesis_timestamp")?.jsonPrimitive?.content

            NodeStatus(
                connected = true,
                nodeIp = nodeIp,
                nodePort = detectedPort ?: 0,
                version = version,
                networkVersion = networkVersion,
                currentCycle = statusObj["current_cycle"]?.jsonPrimitive?.longOrNull,
                currentPeriod = statusObj["current_period"]?.jsonPrimitive?.longOrNull,
                connectedPeers = statusObj["connected_nodes"]?.jsonObject?.size ?: 0
            ).let { Result.success(it) }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get staking info for address
     */
    suspend fun getStakingInfo(address: String): Result<StakingInfo> {
        return try {
            val params = buildJsonArray {
                add(buildJsonArray { add(address) })
            }
            val result = rpcCall("get_addresses", params).getOrThrow()
            val addresses = result.jsonArray

            if (addresses.isEmpty()) {
                return Result.failure(Exception("Address not found"))
            }

            val addressInfo = addresses[0].jsonObject

            // Parse deferred credits
            val deferredCreditsArray = addressInfo["deferred_credits"]?.jsonArray
            val totalDeferredCredits = deferredCreditsArray?.sumOf { credit ->
                credit.jsonObject["amount"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
            } ?: 0.0

            StakingInfo(
                address = address,
                balance = addressInfo["candidate_balance"]?.jsonPrimitive?.content
                    ?: addressInfo["final_balance"]?.jsonPrimitive?.content
                    ?: "0",
                candidateBalance = addressInfo["candidate_balance"]?.jsonPrimitive?.content,
                finalRolls = addressInfo["final_roll_count"]?.jsonPrimitive?.intOrNull ?: 0,
                candidateRolls = addressInfo["candidate_roll_count"]?.jsonPrimitive?.intOrNull ?: 0,
                activeRolls = addressInfo["active_rolls"]?.jsonPrimitive?.intOrNull
                    ?: addressInfo["candidate_roll_count"]?.jsonPrimitive?.intOrNull ?: 0,
                stakingAddress = addressInfo["address"]?.jsonPrimitive?.content,
                deferredCredits = totalDeferredCredits.toString()
            ).let { Result.success(it) }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get staking addresses registered on node
     */
    suspend fun getStakingAddresses(): Result<List<String>> {
        return try {
            val result = rpcCall("get_staking_addresses").getOrThrow()
            val addresses = result.jsonArray.map { it.jsonPrimitive.content }
            Result.success(addresses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Buy rolls (requires private key - operation will be signed on MassaConnect)
     */
    suspend fun prepareBuyRolls(address: String, rollCount: Int, fee: String = "0.01"): Result<JsonElement> {
        return try {
            val operationData = buildJsonObject {
                put("type", "buy_rolls")
                put("address", address)
                put("roll_count", rollCount)
                put("fee", fee)
            }
            Result.success(operationData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sell rolls (requires private key - operation will be signed on MassaConnect)
     */
    suspend fun prepareSellRolls(address: String, rollCount: Int, fee: String = "0.01"): Result<JsonElement> {
        return try {
            val operationData = buildJsonObject {
                put("type", "sell_rolls")
                put("address", address)
                put("roll_count", rollCount)
                put("fee", fee)
            }
            Result.success(operationData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send signed operations to network
     */
    suspend fun sendOperations(operations: JsonArray): Result<List<String>> {
        return try {
            val result = rpcCall("send_operations", operations).getOrThrow()
            val opIds = result.jsonArray.map { it.jsonPrimitive.content }
            Result.success(opIds)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get operations status
     */
    suspend fun getOperations(operationIds: List<String>): Result<JsonElement> {
        return try {
            val params = buildJsonArray {
                add(buildJsonArray {
                    operationIds.forEach { add(it) }
                })
            }
            rpcCall("get_operations", params)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get network info
     */
    suspend fun getNetworkInfo(): Result<JsonElement> {
        return try {
            rpcCall("get_status")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get detected port (for display purposes)
     */
    fun getDetectedPort(): Int? = detectedPort
    
    /**
     * Force re-detection of port
     */
    fun resetPortDetection() {
        detectedPort = null
    }

    // Password for private API (from node config)
    private var nodePassword: String = "massa123"

    /**
     * Set the node password for private API calls
     */
    fun setNodePassword(password: String) {
        nodePassword = password
    }

    /**
     * Execute JSON-RPC call to Massa PRIVATE API (port 33034)
     * Requires authentication
     */
    suspend fun privateRpcCall(method: String, params: JsonElement = JsonArray(emptyList())): Result<JsonElement> {
        return withContext(Dispatchers.IO) {
            try {
                val privateUrl = "http://$nodeIp:33034/api/v2"
                
                val requestBody = buildJsonObject {
                    put("jsonrpc", "2.0")
                    put("id", System.currentTimeMillis())
                    put("method", method)
                    put("params", params)
                }

                // Basic auth with empty username and password
                val credentials = ":$nodePassword"
                val basicAuth = "Basic " + Base64.getEncoder().encodeToString(credentials.toByteArray())

                val request = Request.Builder()
                    .url(privateUrl)
                    .addHeader("Authorization", basicAuth)
                    .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: throw Exception("Empty response")

                val jsonResponse = json.parseToJsonElement(body).jsonObject

                if (jsonResponse.containsKey("error")) {
                    val error = jsonResponse["error"]?.jsonObject
                    val message = error?.get("message")?.jsonPrimitive?.content ?: "Unknown error"
                    Result.failure(Exception(message))
                } else {
                    val result = jsonResponse["result"] ?: JsonNull
                    Result.success(result)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Add staking secret key to node (starts staking with this key)
     * This is the key operation to enable staking
     */
    suspend fun addStakingSecretKey(secretKey: String): Result<Boolean> {
        return try {
            val params = buildJsonArray {
                add(buildJsonArray { add(secretKey) })
            }
            privateRpcCall("add_staking_secret_keys", params).getOrThrow()
            println("[MassaNodeClient] Successfully added staking key")
            Result.success(true)
        } catch (e: Exception) {
            println("[MassaNodeClient] Failed to add staking key: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Remove staking address from node (stops staking for this address)
     */
    suspend fun removeStakingAddress(address: String): Result<Boolean> {
        return try {
            val params = buildJsonArray {
                add(buildJsonArray { add(address) })
            }
            privateRpcCall("remove_staking_addresses", params).getOrThrow()
            println("[MassaNodeClient] Successfully removed staking address: $address")
            Result.success(true)
        } catch (e: Exception) {
            println("[MassaNodeClient] Failed to remove staking address: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get staking addresses from private API
     */
    suspend fun getStakingAddressesPrivate(): Result<List<String>> {
        return try {
            val result = privateRpcCall("get_staking_addresses").getOrThrow()
            val addresses = result.jsonArray.map { it.jsonPrimitive.content }
            Result.success(addresses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}