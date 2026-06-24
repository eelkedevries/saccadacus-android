package com.example.saccadacusandroid

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.saccadacusandroid.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ControlScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun ControlScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var running by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val cameraGranted = result[Manifest.permission.CAMERA] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (cameraGranted) {
            startTrackingService(context)
            running = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Saccadacus — camera feasibility probe")
        Spacer(Modifier.height(24.dp))
        Button(
            enabled = !running,
            onClick = {
                val permissions = buildList {
                    add(Manifest.permission.CAMERA)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                permissionLauncher.launch(permissions.toTypedArray())
            },
        ) {
            Text("Start tracking")
        }
        Spacer(Modifier.height(12.dp))
        Button(
            enabled = running,
            onClick = {
                stopTrackingService(context)
                running = false
            },
        ) {
            Text("Stop")
        }
        Spacer(Modifier.height(24.dp))
        Text(
            if (running) {
                "Running. Switch to another app — frames keep logging. Stop here or from the notification."
            } else {
                "Idle. Press Start (grant camera) to begin a background camera session."
            },
        )
    }
}

private fun startTrackingService(context: Context) {
    val intent = Intent(context, CameraTrackingService::class.java)
        .setAction(CameraTrackingService.ACTION_START)
    ContextCompat.startForegroundService(context, intent)
}

private fun stopTrackingService(context: Context) {
    val intent = Intent(context, CameraTrackingService::class.java)
        .setAction(CameraTrackingService.ACTION_STOP)
    context.startService(intent)
}
