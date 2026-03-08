package com.goernhardt.ledhttpservice

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.goernhardt.ledhttpservice.ui.theme.LEDHTTPServiceTheme
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Main activity showing the LED HTTP Service status and manual test controls.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Ensure the service is running
        startService(Intent(this, LedService::class.java))
        
        setContent {
            LEDHTTPServiceTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DashboardScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
    val ipAddress = remember { getLocalIpAddress() ?: "Unknown" }
    val modes = remember { LedController.getAvailableModes() }

    Column(modifier = modifier.padding(16.dp).fillMaxSize()) {
        Text("Service Status: Running", style = MaterialTheme.typography.headlineMedium)
        Text("IP Address: $ipAddress", style = MaterialTheme.typography.bodyLarge)
        Text("Port: 8080", style = MaterialTheme.typography.bodyLarge)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Test Controls (Modes: ${modes.size})", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(modes) { mode ->
                Button(
                    onClick = { LedController.setLed(mode) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Set Mode: $mode")
                }
            }
        }
    }
}

/**
 * Returns the first non-loopback IPv4 address found on the device.
 */
fun getLocalIpAddress(): String? {
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()
            val addresses = networkInterface.inetAddresses
            while (addresses.hasMoreElements()) {
                val inetAddress = addresses.nextElement()
                if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                    return inetAddress.hostAddress
                }
            }
        }
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
    return null
}
