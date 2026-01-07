package com.massapay.agent.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.massapay.agent.bridge.BridgeServer
import com.massapay.agent.bridge.BridgeServerEvent
import com.massapay.agent.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.awt.image.BufferedImage

enum class ConnectionStatus { WAITING, CONNECTING, CONNECTED }

@Composable
fun PairingScreen(
    bridgeServer: BridgeServer,
    onPaired: () -> Unit
) {
    val serverState by bridgeServer.serverState.collectAsState()
    var pairingData by remember { mutableStateOf<com.massapay.agent.bridge.PairingData?>(null) }
    var status by remember { mutableStateOf(ConnectionStatus.WAITING) }

    // Generate pairing data only when server is ready with correct IP
    LaunchedEffect(serverState.isRunning, serverState.host) {
        if (serverState.isRunning && serverState.host != "0.0.0.0") {
            pairingData = bridgeServer.generatePairingData()
        }
    }
    val coroutineScope = rememberCoroutineScope()
    
    val json = remember { Json { encodeDefaults = true } }
    val qrContent = remember(pairingData) { pairingData?.let { json.encodeToString(it) } }

    // Listen for device connections
    LaunchedEffect(serverState.connectedDevices) {
        if (serverState.connectedDevices.isNotEmpty()) {
            status = ConnectionStatus.CONNECTED
            delay(1500)
            onPaired()
        }
    }

    // Listen to bridge events
    LaunchedEffect(Unit) {
        bridgeServer.events.collect { event ->
            when (event) {
                is BridgeServerEvent.DeviceConnected -> {
                    status = ConnectionStatus.CONNECTED
                    delay(1500)
                    onPaired()
                }
                else -> {}
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .padding(48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // LEFT SIDE - Welcome Text
        Column(
            modifier = Modifier.weight(1f).padding(end = 48.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome to",
                fontSize = 24.sp,
                color = LightTextSecondary
            )
            Text(
                text = "Massa Assisted Staking Agent",
                fontSize = 56.sp,
                fontWeight = FontWeight.Black,
                color = LightTextPrimary
            )
            Text(
                text = "Staking",
                fontSize = 56.sp,
                fontWeight = FontWeight.Black,
                color = LightTextPrimary
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Bridge Diagram
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = LightCardBackground)
            ) {
                Row(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Massa Node
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(LightButtonBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Massa Node", fontSize = 12.sp, color = LightTextSecondary)
                    }

                    // Connection Arrow
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(2.dp)
                                .background(LightBorder)
                        )
                        Icon(
                            imageVector = Icons.Default.SyncAlt,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = if (serverState.nodeConnected) AccentGreen else AccentOrange
                        )
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(2.dp)
                                .background(LightBorder)
                        )
                    }

                    // MassaConnect Wallet
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(LightButtonBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("MassaConnect", fontSize = 12.sp, color = LightTextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Server status
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (serverState.isRunning) AccentGreen else AccentRed)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (serverState.isRunning) 
                        "Bridge running on ${serverState.host}:${serverState.port}"
                    else "Starting bridge...",
                    fontSize = 13.sp,
                    color = LightTextSecondary
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Node status
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (serverState.nodeConnected) AccentGreen else AccentOrange)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (serverState.nodeConnected) 
                        "Node connected" 
                    else "Waiting for node connection",
                    fontSize = 13.sp,
                    color = LightTextSecondary
                )
            }
        }

        // RIGHT SIDE - QR Code Container
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (status) {
                ConnectionStatus.CONNECTED -> {
                    Card(
                        modifier = Modifier.size(400.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = LightCardBackground),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape)
                                        .background(AccentGreen),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(50.dp),
                                        tint = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = "Connected!",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LightTextPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Redirecting to dashboard...",
                                    fontSize = 14.sp,
                                    color = LightTextSecondary
                                )
                            }
                        }
                    }
                }
                else -> {
                    // QR Container Card
                    Card(
                        modifier = Modifier.width(400.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = LightCardBackground),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Header with title and refresh button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Scan QR Code",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = LightTextPrimary
                                )
                                
                                // Refresh button
                                IconButton(
                                    onClick = { 
                                        pairingData = bridgeServer.generatePairingData()
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(LightSurface)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Generate new QR",
                                        modifier = Modifier.size(20.dp),
                                        tint = LightTextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // QR Code Box
                            Box(
                                modifier = Modifier
                                    .size(300.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                if (status == ConnectionStatus.CONNECTING) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(60.dp),
                                        color = LightButtonBackground,
                                        strokeWidth = 4.dp
                                    )
                                } else if (qrContent != null) {
                                    QRCodeView(qrContent, 260)
                                } else {
                                    // Waiting for server to be ready
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(40.dp),
                                            color = LightButtonBackground,
                                            strokeWidth = 3.dp
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Starting server...",
                                            fontSize = 14.sp,
                                            color = LightTextSecondary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Status indicator
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (status == ConnectionStatus.CONNECTING) AccentOrange
                                            else AccentGreen
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (status == ConnectionStatus.CONNECTING)
                                        "Connecting..."
                                    else "Waiting for connection",
                                    fontSize = 14.sp,
                                    color = LightTextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Instruction text
                            Text(
                                text = "Open MassaConnect on your phone and scan this QR code to pair",
                                fontSize = 12.sp,
                                color = LightTextTertiary,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Demo button (for testing)
                            TextButton(onClick = {
                                status = ConnectionStatus.CONNECTING
                                // Simulate connection for demo
                                coroutineScope.launch {
                                    delay(2000)
                                    status = ConnectionStatus.CONNECTED
                                    delay(1500)
                                    onPaired()
                                }
                            }) {
                                Text(
                                    "[Demo] Simulate connection",
                                    fontSize = 12.sp,
                                    color = LightTextTertiary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QRCodeView(content: String, size: Int) {
    val qrBitmap = remember(content) {
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setRGB(x, y, if (bitMatrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
                }
            }
            bitmap
        } catch (e: Exception) { null }
    }

    qrBitmap?.let { bitmap ->
        Canvas(modifier = Modifier.size(size.dp)) {
            val pixelSize = this.size.width / bitmap.width
            for (x in 0 until bitmap.width) {
                for (y in 0 until bitmap.height) {
                    val color = if (bitmap.getRGB(x, y) == 0xFF000000.toInt()) Color.Black else Color.White
                    drawRect(
                        color = color,
                        topLeft = androidx.compose.ui.geometry.Offset(x * pixelSize, y * pixelSize),
                        size = androidx.compose.ui.geometry.Size(pixelSize, pixelSize)
                    )
                }
            }
        }
    }
}
