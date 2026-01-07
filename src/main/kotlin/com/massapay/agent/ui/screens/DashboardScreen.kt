package com.massapay.agent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.massapay.agent.bridge.BridgeServer
import com.massapay.agent.bridge.ConnectedDevice
import com.massapay.agent.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

enum class NavSection { DASHBOARD, NODE, CONNECTIONS, SETTINGS, ABOUT }

@Composable
fun DashboardScreen(
    bridgeServer: BridgeServer,
    onDisconnect: () -> Unit
) {
    val serverState by bridgeServer.serverState.collectAsState()
    var currentSection by remember { mutableStateOf(NavSection.DASHBOARD) }

    // Calculate bridge uptime
    val startTime = remember { System.currentTimeMillis() }
    var bridgeUptime by remember { mutableStateOf(0L) }
    
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            bridgeUptime = (System.currentTimeMillis() - startTime) / 1000
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
    ) {
        // SIDEBAR
        Column(
            modifier = Modifier
                .width(260.dp)
                .fillMaxHeight()
                .background(LightSurface)
                .padding(20.dp)
        ) {
            // Logo header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(LightButtonBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Hub, null, Modifier.size(24.dp), tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Massa Agent", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = LightTextPrimary)
                    Text("Bridge v1.0.0", fontSize = 11.sp, color = LightTextTertiary)
                }
            }

            Spacer(Modifier.height(40.dp))

            // Navigation items
            SidebarItem(Icons.Outlined.Dashboard, "Dashboard", currentSection == NavSection.DASHBOARD) {
                currentSection = NavSection.DASHBOARD
            }
            SidebarItem(Icons.Outlined.Storage, "Node Status", currentSection == NavSection.NODE) {
                currentSection = NavSection.NODE
            }
            SidebarItem(Icons.Outlined.Devices, "Connections", currentSection == NavSection.CONNECTIONS) {
                currentSection = NavSection.CONNECTIONS
            }
            SidebarItem(Icons.Outlined.Settings, "Settings", currentSection == NavSection.SETTINGS) {
                currentSection = NavSection.SETTINGS
            }
            SidebarItem(Icons.Outlined.Info, "About", currentSection == NavSection.ABOUT) {
                currentSection = NavSection.ABOUT
            }

            Spacer(Modifier.weight(1f))

            // Connected devices summary
            if (serverState.connectedDevices.isNotEmpty()) {
                val device = serverState.connectedDevices.first()
                ConnectedDeviceCard(device = device, onDisconnect = onDisconnect)
            } else {
                // No devices connected
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = LightCardBackground)
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.PhonelinkOff,
                            null,
                            Modifier.size(32.dp),
                            tint = LightTextTertiary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "No devices connected",
                            fontSize = 12.sp,
                            color = LightTextTertiary
                        )
                    }
                }
            }
        }

        // MAIN CONTENT
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            when (currentSection) {
                NavSection.DASHBOARD -> DashboardContent(
                    serverState = serverState,
                    bridgeUptime = bridgeUptime
                )
                NavSection.NODE -> NodeContent(serverState = serverState)
                NavSection.CONNECTIONS -> ConnectionsContent(
                    serverState = serverState,
                    bridgeServer = bridgeServer
                )
                NavSection.SETTINGS -> SettingsContent(
                    serverState = serverState,
                    bridgeServer = bridgeServer
                )
                NavSection.ABOUT -> AboutContent()
            }
        }
    }
}

// ==================== SIDEBAR COMPONENTS ====================
@Composable
private fun SidebarItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) LightButtonBackground else Color.Transparent
    val textColor = if (isSelected) Color.White else LightTextSecondary
    val iconColor = if (isSelected) Color.White else LightTextSecondary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = iconColor)
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = textColor)
    }
}

@Composable
private fun ConnectedDeviceCard(device: ConnectedDevice, onDisconnect: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = LightCardBackground)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AccentGreen.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (device.platform.contains("Android", ignoreCase = true)) 
                            Icons.Default.PhoneAndroid 
                        else 
                            Icons.Default.PhoneIphone,
                        null,
                        Modifier.size(18.dp),
                        tint = AccentGreen
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Connected", fontSize = 11.sp, color = AccentGreen)
                    Text(
                        device.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = LightTextPrimary
                    )
                }
            }
            
            // Show wallet address if available
            device.walletAddress?.let { address ->
                Spacer(Modifier.height(8.dp))
                Text(
                    "${address.take(8)}...${address.takeLast(6)}",
                    fontSize = 10.sp,
                    color = LightTextTertiary
                )
            }
            
            Spacer(Modifier.height(12.dp))
            // Disconnect button with dark container
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onDisconnect)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Status icon (green dot = connected)
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AccentGreen)
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Default.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Disconnect",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ==================== DASHBOARD SECTION ====================
@Composable
private fun DashboardContent(
    serverState: com.massapay.agent.bridge.BridgeServerState,
    bridgeUptime: Long
) {
    // Header
    Text(
        "Dashboard",
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        color = LightTextPrimary
    )
    Text(
        "Monitor your Massa node and connected devices",
        fontSize = 14.sp,
        color = LightTextSecondary
    )

    Spacer(Modifier.height(32.dp))

    // Status Cards Row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Node Status Card
        StatusCard(
            modifier = Modifier.weight(1f),
            title = "Node Status",
            value = if (serverState.nodeConnected) "Connected" else "Disconnected",
            icon = Icons.Default.Storage,
            color = if (serverState.nodeConnected) AccentGreen else AccentRed
        )

        // Connected Devices Card
        StatusCard(
            modifier = Modifier.weight(1f),
            title = "Devices",
            value = "${serverState.connectedDevices.size} connected",
            icon = Icons.Default.Devices,
            color = if (serverState.connectedDevices.isNotEmpty()) AccentGreen else LightTextTertiary
        )

        // Bridge Uptime Card
        StatusCard(
            modifier = Modifier.weight(1f),
            title = "Bridge Uptime",
            value = formatDuration(bridgeUptime),
            icon = Icons.Default.AccessTime,
            color = AccentBlue
        )
    }

    Spacer(Modifier.height(24.dp))

    // Node Details Section
    if (serverState.nodeConnected && serverState.nodeStatus != null) {
        val nodeStatus = serverState.nodeStatus!!
        
        Text(
            "Node Information",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = LightTextPrimary
        )
        Spacer(Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = LightCardBackground)
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    InfoItem("Node IP", "${serverState.nodeIp}:${serverState.nodePort}")
                    InfoItem("Version", nodeStatus.version ?: "Unknown")
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    InfoItem("Current Cycle", nodeStatus.currentCycle?.toString() ?: "-")
                    InfoItem("Current Period", nodeStatus.currentPeriod?.toString() ?: "-")
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    InfoItem("Connected Peers", nodeStatus.connectedPeers?.toString() ?: "-")
                    InfoItem("Network", nodeStatus.networkVersion ?: "Mainnet")
                }
            }
        }
    } else if (!serverState.nodeConnected) {
        // Node not connected warning
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AccentRed.copy(alpha = 0.1f))
        ) {
            Row(
                Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Warning,
                    null,
                    Modifier.size(32.dp),
                    tint = AccentRed
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        "Node Not Connected",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentRed
                    )
                    Text(
                        "Unable to connect to Massa node at ${serverState.nodeIp}:${serverState.nodePort}",
                        fontSize = 13.sp,
                        color = LightTextSecondary
                    )
                    Text(
                        "Make sure your node is running and accessible.",
                        fontSize = 12.sp,
                        color = LightTextTertiary
                    )
                }
            }
        }
    }


    Spacer(Modifier.height(24.dp))

    // Staking Metrics Section - Show if any device has staking info
    val connectedStakingInfo = serverState.connectedDevices.firstOrNull()?.stakingInfo
    val walletAddress = serverState.connectedDevices.firstOrNull()?.walletAddress
    
    if (connectedStakingInfo != null) {
        Text(
            "Staking Overview",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = LightTextPrimary
        )
        Spacer(Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1a1a2e),
                                Color(0xFF16213e)
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(20.dp)
            ) {
                Column {
                    // Wallet Address at top
                    if (walletAddress != null) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Wallet Address",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                            Text(
                                "${walletAddress.take(8)}...${walletAddress.takeLast(6)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Divider(color = Color.White.copy(alpha = 0.1f))
                        Spacer(Modifier.height(16.dp))
                    }

                    // Balance Section
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Balance
                        Column {
                            Text(
                                "Balance",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                            Text(
                                "${String.format("%.4f", connectedStakingInfo.balance.toDoubleOrNull() ?: 0.0)} MAS",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        // Candidate Balance
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "Candidate Balance",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                            Text(
                                "${String.format("%.4f", (connectedStakingInfo.candidateBalance?.toDoubleOrNull() ?: connectedStakingInfo.balance.toDoubleOrNull() ?: 0.0))} MAS",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    Divider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(Modifier.height(20.dp))

                    // Rolls Section - Like Explorer
                    Text(
                        "Rolls",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(12.dp))
                    
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Final Rolls
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Text(
                                "Final",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                            Text(
                                "${connectedStakingInfo.finalRolls}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4ADE80)
                            )
                        }
                        
                        Spacer(Modifier.width(12.dp))
                        
                        // Candidate Rolls
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Text(
                                "Candidate",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                            Text(
                                "${connectedStakingInfo.candidateRolls}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF667eea)
                            )
                        }
                        
                        Spacer(Modifier.width(12.dp))
                        
                        // Active Rolls
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Text(
                                "Active",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                            Text(
                                "${connectedStakingInfo.activeRolls}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (connectedStakingInfo.activeRolls > 0) Color(0xFF4ADE80) else Color(0xFFFF9800)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Staked Value
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF667eea).copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Staked Value",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                "${connectedStakingInfo.finalRolls * 100} MAS",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF667eea)
                            )
                        }
                    }

                    // Warning if rolls not active yet
                    val pendingRolls = connectedStakingInfo.finalRolls - connectedStakingInfo.activeRolls
                    if (pendingRolls > 0) {
                        Spacer(Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFF9800).copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFFFF9800)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "$pendingRolls roll${if (pendingRolls > 1) "s" else ""} pending activation (~3 cycles)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFFF9800)
                                )
                            }
                        }
                    }

                    // Deferred credits (if any)
                    val deferredAmt = connectedStakingInfo.deferredCredits.toDoubleOrNull() ?: 0.0
                    if (deferredAmt > 0) {
                        Spacer(Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF667eea).copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFF667eea)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Deferred: ${String.format("%.2f", deferredAmt)} MAS (from sold rolls)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF667eea)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    // Connected Devices Section
    if (serverState.connectedDevices.isNotEmpty()) {
        Text(
            "Connected Devices",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = LightTextPrimary
        )
        Spacer(Modifier.height(16.dp))

        serverState.connectedDevices.forEach { device ->
            DeviceCard(device = device)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun StakingMetricItem(
    icon: ImageVector,
    label: String,
    value: String,
    iconTint: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = iconTint
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun StatusCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LightCardBackground)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, Modifier.size(22.dp), tint = color)
                }
                Spacer(Modifier.width(12.dp))
                Text(title, fontSize = 13.sp, color = LightTextSecondary)
            }
            Spacer(Modifier.height(12.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = LightTextPrimary)
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    Column {
        Text(label, fontSize = 12.sp, color = LightTextTertiary)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = LightTextPrimary)
    }
}

@Composable
private fun DeviceCard(device: ConnectedDevice) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = LightCardBackground)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AccentGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (device.platform.contains("Android", ignoreCase = true))
                        Icons.Default.PhoneAndroid
                    else
                        Icons.Default.PhoneIphone,
                    null,
                    Modifier.size(24.dp),
                    tint = AccentGreen
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(Modifier.weight(1f)) {
                Text(
                    device.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LightTextPrimary
                )
                Text(
                    device.platform,
                    fontSize = 12.sp,
                    color = LightTextSecondary
                )
                device.walletAddress?.let { address ->
                    Text(
                        "${address.take(10)}...${address.takeLast(8)}",
                        fontSize = 11.sp,
                        color = LightTextTertiary
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AccentGreen)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    formatTimeSince(device.connectedAt),
                    fontSize = 11.sp,
                    color = LightTextTertiary
                )
            }
        }
    }
}

// ==================== NODE SECTION ====================
@Composable
private fun NodeContent(serverState: com.massapay.agent.bridge.BridgeServerState) {
    Text(
        "Node Status",
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        color = LightTextPrimary
    )
    Text(
        "Massa node connection details",
        fontSize = 14.sp,
        color = LightTextSecondary
    )

    Spacer(Modifier.height(32.dp))

    // Connection status
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A2E)
        )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (serverState.nodeConnected) AccentGreen else AccentRed),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (serverState.nodeConnected) Icons.Default.Check else Icons.Default.Close,
                    null,
                    Modifier.size(32.dp),
                    tint = Color.White
                )
            }
            
            Spacer(Modifier.width(20.dp))
            
            Column {
                Text(
                    if (serverState.nodeConnected) "Node Connected" else "Node Disconnected",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (serverState.nodeConnected) AccentGreen else AccentRed
                )
                Text(
                    "${serverState.nodeIp}:${serverState.nodePort}",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }

    if (serverState.nodeConnected && serverState.nodeStatus != null) {
        val status = serverState.nodeStatus!!
        
        Spacer(Modifier.height(24.dp))
        
        Text(
            "Node Details",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = LightTextPrimary
        )
        
        Spacer(Modifier.height(16.dp))
        
        // Details grid
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DetailCard(
                modifier = Modifier.weight(1f),
                label = "Version",
                value = status.version ?: "Unknown",
                icon = Icons.Default.Info
            )
            DetailCard(
                modifier = Modifier.weight(1f),
                label = "Network",
                value = status.networkVersion ?: "Mainnet",
                icon = Icons.Default.Language
            )
        }
        
        Spacer(Modifier.height(16.dp))
        
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DetailCard(
                modifier = Modifier.weight(1f),
                label = "Current Cycle",
                value = status.currentCycle?.toString() ?: "-",
                icon = Icons.Default.Loop
            )
            DetailCard(
                modifier = Modifier.weight(1f),
                label = "Current Period",
                value = status.currentPeriod?.toString() ?: "-",
                icon = Icons.Default.Schedule
            )
        }
        
        Spacer(Modifier.height(16.dp))
        
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DetailCard(
                modifier = Modifier.weight(1f),
                label = "Connected Peers",
                value = status.connectedPeers?.toString() ?: "-",
                icon = Icons.Default.People
            )
            DetailCard(
                modifier = Modifier.weight(1f),
                label = "Status",
                value = "Running",
                icon = Icons.Default.PlayArrow
            )
        }
    }
}

@Composable
private fun DetailCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = LightCardBackground)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(LightButtonBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, Modifier.size(18.dp), tint = Color.White)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(label, fontSize = 11.sp, color = LightTextTertiary)
                Text(value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LightTextPrimary)
            }
        }
    }
}

// ==================== CONNECTIONS SECTION ====================
@Composable
private fun ConnectionsContent(
    serverState: com.massapay.agent.bridge.BridgeServerState,
    bridgeServer: BridgeServer
) {
    Text(
        "Connections",
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        color = LightTextPrimary
    )
    Text(
        "Manage connected MassaConnect devices",
        fontSize = 14.sp,
        color = LightTextSecondary
    )

    Spacer(Modifier.height(32.dp))

    // Bridge info
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LightCardBackground)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "Bridge Server",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = LightTextPrimary
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                InfoItem("Status", if (serverState.isRunning) "Running" else "Stopped")
                InfoItem("Address", "${serverState.host}:${serverState.port}")
            }
        }
    }

    Spacer(Modifier.height(24.dp))

    // Connected devices
    Text(
        "Connected Devices (${serverState.connectedDevices.size})",
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = LightTextPrimary
    )
    
    Spacer(Modifier.height(16.dp))

    if (serverState.connectedDevices.isEmpty()) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = LightCardBackground)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.DevicesOther,
                    null,
                    Modifier.size(48.dp),
                    tint = LightTextTertiary
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "No devices connected",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = LightTextSecondary
                )
                Text(
                    "Open MassaConnect on your phone and scan the QR code to connect",
                    fontSize = 13.sp,
                    color = LightTextTertiary
                )
            }
        }
    } else {
        serverState.connectedDevices.forEach { device ->
            DeviceDetailCard(
                device = device,
                onDisconnect = { bridgeServer.disconnectDevice(device.id) }
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun DeviceDetailCard(
    device: ConnectedDevice,
    onDisconnect: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LightCardBackground)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (device.platform.contains("Android", ignoreCase = true))
                            Icons.Default.PhoneAndroid
                        else
                            Icons.Default.PhoneIphone,
                        null,
                        Modifier.size(28.dp),
                        tint = AccentGreen
                    )
                }
                
                Spacer(Modifier.width(16.dp))
                
                Column(Modifier.weight(1f)) {
                    Text(
                        device.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LightTextPrimary
                    )
                    Text(
                        device.platform,
                        fontSize = 13.sp,
                        color = LightTextSecondary
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(AccentGreen)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Online", fontSize = 13.sp, color = AccentGreen, fontWeight = FontWeight.Medium)
                }
            }
            
            Spacer(Modifier.height(16.dp))
            Divider(color = LightTextTertiary.copy(alpha = 0.2f))
            Spacer(Modifier.height(16.dp))
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                InfoItem("Session ID", device.id.take(8) + "...")
                InfoItem("Connected", formatTimeSince(device.connectedAt))
            }
            
            device.walletAddress?.let { address ->
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth()) {
                    InfoItem("Wallet Address", "${address.take(12)}...${address.takeLast(10)}")
                }
            }

            Spacer(Modifier.height(16.dp))
            Divider(color = LightTextTertiary.copy(alpha = 0.2f))
            Spacer(Modifier.height(16.dp))

            // Disconnect button
            Button(
                onClick = onDisconnect,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentRed.copy(alpha = 0.15f),
                    contentColor = AccentRed
                )
            ) {
                Icon(
                    Icons.Default.LinkOff,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Disconnect Device", fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ==================== SETTINGS SECTION ====================
@Composable
private fun SettingsContent(
    serverState: com.massapay.agent.bridge.BridgeServerState,
    bridgeServer: BridgeServer
) {
    Text(
        "Settings",
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        color = LightTextPrimary
    )
    Text(
        "Configure Massa Agent settings",
        fontSize = 14.sp,
        color = LightTextSecondary
    )

    Spacer(Modifier.height(32.dp))

    // Node Configuration
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LightCardBackground)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(LightButtonBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Storage, null, Modifier.size(20.dp), tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "Node Configuration",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LightTextPrimary
                )
            }
            
            Spacer(Modifier.height(20.dp))
            
            SettingItem(
                label = "Node IP Address",
                value = serverState.nodeIp,
                icon = Icons.Default.Computer
            )
            
            Spacer(Modifier.height(12.dp))
            
            SettingItem(
                label = "RPC Port",
                value = serverState.nodePort.toString(),
                icon = Icons.Default.Settings
            )
        }
    }
    
    Spacer(Modifier.height(16.dp))

    // Bridge Configuration
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LightCardBackground)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(LightButtonBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Hub, null, Modifier.size(20.dp), tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "Bridge Configuration",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LightTextPrimary
                )
            }
            
            Spacer(Modifier.height(20.dp))
            
            SettingItem(
                label = "Bridge Address",
                value = "${serverState.host}:${serverState.port}",
                icon = Icons.Default.Wifi
            )
            
            Spacer(Modifier.height(12.dp))
            
            SettingItem(
                label = "Status",
                value = if (serverState.isRunning) "Running" else "Stopped",
                icon = Icons.Default.CheckCircle
            )
        }
    }
    
    Spacer(Modifier.height(16.dp))

    // About
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LightCardBackground)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(LightButtonBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Info, null, Modifier.size(20.dp), tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "About",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LightTextPrimary
                )
            }
            
            Spacer(Modifier.height(20.dp))
            
            SettingItem(
                label = "Version",
                value = "1.0.0",
                icon = Icons.Default.Tag
            )
            
            Spacer(Modifier.height(12.dp))
            
            SettingItem(
                label = "Build",
                value = "2024.01.04",
                icon = Icons.Default.Build
            )
        }
    }
}

@Composable
private fun SettingItem(
    label: String,
    value: String,
    icon: ImageVector
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(LightBackground)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(18.dp), tint = LightTextSecondary)
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 13.sp, color = LightTextSecondary, modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = LightTextPrimary)
    }
}

// ==================== UTILITY FUNCTIONS ====================
private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${secs}s"
        else -> "${secs}s"
    }
}

private fun formatTimeSince(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    
    return when {
        hours > 0 -> "${hours}h ago"
        minutes > 0 -> "${minutes}m ago"
        else -> "Just now"
    }
}
// ==================== ABOUT SECTION ====================
@Composable
private fun AboutContent() {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            "About",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = LightTextPrimary
        )
        Text(
            "Information about Massa Agent",
            fontSize = 14.sp,
            color = LightTextSecondary
        )

        Spacer(Modifier.height(32.dp))

        // Main About Card with gradient header
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = LightCardBackground)
        ) {
            Column {
                // Header with gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF1a1a2e),
                                    Color(0xFF16213e)
                                )
                            )
                        )
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Logo
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(Color.White, RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "M",
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        
                        Text(
                            "Massa Agent",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        
                        Text(
                            "Staking Bridge",
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        
                        Spacer(Modifier.height(12.dp))
                        
                        // Version badge
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "v1.0.0",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }
                }

                // Content
                Column(Modifier.padding(24.dp)) {
                    Text(
                        "A secure bridge application that connects MassaConnect mobile wallet with your local Massa node for staking operations. Control your staking directly from your phone while keeping your node secure.",
                        fontSize = 14.sp,
                        color = LightTextSecondary,
                        lineHeight = 22.sp
                    )
                    
                    Spacer(Modifier.height(24.dp))
                    
                    // Features
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        AboutFeature(Icons.Default.Security, "Secure")
                        AboutFeature(Icons.Default.Wifi, "Local Network")
                        AboutFeature(Icons.Default.Code, "Open Source")
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    
                    Divider(color = LightBorder)
                    
                    Spacer(Modifier.height(24.dp))
                    
                    // Technology Stack
                    Text(
                        "Technology Stack",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LightTextPrimary
                    )
                    
                    Spacer(Modifier.height(12.dp))
                    
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Kotlin", "Compose Desktop", "Ktor", "WebSocket").forEach { tech ->
                            Box(
                                modifier = Modifier
                                    .background(LightButtonBackground.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(tech, fontSize = 12.sp, color = LightButtonBackground, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    
                    Divider(color = LightBorder)
                    
                    Spacer(Modifier.height(24.dp))
                    
                    // Developer info
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(LightButtonBackground.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(LightButtonBackground, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Code, null, Modifier.size(22.dp), tint = Color.White)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Developer", fontSize = 12.sp, color = LightTextSecondary)
                            Text("mderramus", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = LightTextPrimary)
                        }
                        Icon(Icons.Default.Verified, null, Modifier.size(24.dp), tint = LightButtonBackground)
                    }
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // How it works card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = LightCardBackground)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    "How It Works",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LightTextPrimary
                )
                
                Spacer(Modifier.height(16.dp))
                
                HowItWorksStep(1, "Scan QR Code", "Open MassaConnect and scan the QR code to pair")
                Spacer(Modifier.height(12.dp))
                HowItWorksStep(2, "Secure Connection", "WebSocket connection over local network (no internet)")
                Spacer(Modifier.height(12.dp))
                HowItWorksStep(3, "Staking Control", "Buy/sell rolls and manage staking from your phone")
                Spacer(Modifier.height(12.dp))
                HowItWorksStep(4, "Node Operations", "Agent executes commands on your local Massa node")
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Copyright
        Text(
            "© 2024-2026 MassaPay. All rights reserved.",
            fontSize = 12.sp,
            color = LightTextTertiary
        )
    }
}

@Composable
private fun AboutFeature(icon: ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(LightButtonBackground.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, Modifier.size(24.dp), tint = LightButtonBackground)
        }
        Spacer(Modifier.height(8.dp))
        Text(label, fontSize = 12.sp, color = LightTextSecondary)
    }
}

@Composable
private fun HowItWorksStep(number: Int, title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(LightButtonBackground, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(number.toString(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = LightTextPrimary)
            Text(description, fontSize = 12.sp, color = LightTextSecondary)
        }
    }
}



