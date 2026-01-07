package com.massapay.agent

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.massapay.agent.bridge.BridgeServer
import com.massapay.agent.bridge.BridgeConfig
import com.massapay.agent.ui.screens.DashboardScreen
import com.massapay.agent.ui.screens.PairingScreen
import com.massapay.agent.ui.theme.MassaAgentTheme
import java.util.prefs.Preferences

enum class Screen { PAIRING, DASHBOARD }

val bridgeServer = BridgeServer(BridgeConfig())

@Composable
fun App() {
    val prefs = remember { Preferences.userRoot().node("massa-agent") }
    var currentScreen by remember { mutableStateOf(Screen.PAIRING) }
    LaunchedEffect(Unit) { bridgeServer.start() }
    MassaAgentTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            AnimatedContent(targetState = currentScreen, transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) }) { screen ->
                when (screen) {
                    Screen.PAIRING -> PairingScreen(bridgeServer = bridgeServer, onPaired = { prefs.putBoolean("is_paired", true); currentScreen = Screen.DASHBOARD })
                    Screen.DASHBOARD -> DashboardScreen(bridgeServer = bridgeServer, onDisconnect = { bridgeServer.disconnectAllDevices(); prefs.putBoolean("is_paired", false); currentScreen = Screen.PAIRING })
                }
            }
        }
    }
}

fun main() = application {
    val windowState = rememberWindowState(size = DpSize(1000.dp, 650.dp), position = WindowPosition(Alignment.Center))
    Window(onCloseRequest = { bridgeServer.stop(); exitApplication() }, title = "Massa Agent", state = windowState, resizable = true) { App() }
}