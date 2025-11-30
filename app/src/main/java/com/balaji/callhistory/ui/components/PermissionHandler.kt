package com.balaji.callhistory.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.balaji.callhistory.utils.PermissionManager

@Composable
fun PermissionHandler(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var permissionDeniedCount by remember { mutableIntStateOf(0) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var hasPermission by remember {
        mutableStateOf(PermissionManager.hasAllPermissions(context))
    }
    var isDefaultDialer by remember {
        mutableStateOf(PermissionManager.isDefaultDialer(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            hasPermission = true
        } else {
            permissionDeniedCount++
            if (permissionDeniedCount >= 2) {
                showSettingsDialog = true
            }
        }
    }

    val defaultDialerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        isDefaultDialer = PermissionManager.isDefaultDialer(context)
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(PermissionManager.REQUIRED_PERMISSIONS)
        }
    }

    if (showSettingsDialog) {
        PermissionSettingsDialog(
            onDismiss = { showSettingsDialog = false },
            onGoToSettings = {
                showSettingsDialog = false
                PermissionManager.openAppSettings(context)
            }
        )
    }

    when {
        !hasPermission -> {
            PermissionRequiredScreen(
                onGrantPermissions = {
                    permissionLauncher.launch(PermissionManager.REQUIRED_PERMISSIONS)
                }
            )
        }
        /*todofor Dialer Default for Pixel Devices*/
        isDefaultDialer -> {
            DefaultDialerRequiredScreen(
                onSetDefaultDialer = {
                    val intent = PermissionManager.getDefaultDialerIntent(context)
                    defaultDialerLauncher.launch(intent)
                }
            )
        }
        else -> {
            content()
        }
    }
}

@Composable
private fun PermissionRequiredScreen(
    onGrantPermissions: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Permissions Required",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Call log, and phone permissions are required to use this app",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onGrantPermissions) {
            Text("Grant Permissions")
        }
    }
}

@Composable
private fun DefaultDialerRequiredScreen(
    onSetDefaultDialer: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Default Dialer Required",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Make this app your default dialer to view call history",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onSetDefaultDialer) {
            Text("Set as Default Dialer")
        }
    }
}

@Composable
private fun PermissionSettingsDialog(
    onDismiss: () -> Unit,
    onGoToSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Permission Required") },
        text = {
            Text(
                "Call log and contacts permissions are required to view call history. " +
                        "Please enable them in app settings."
            )
        },
        confirmButton = {
            TextButton(onClick = onGoToSettings) {
                Text("Go to Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
