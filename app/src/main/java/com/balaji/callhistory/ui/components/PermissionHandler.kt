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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.balaji.callhistory.R
import com.balaji.callhistory.defaultdialer.DefaultDialerManager
import com.balaji.callhistory.defaultdialer.DefaultDialerState
import com.balaji.callhistory.utils.PermissionManager

/**
 * Gates [content] behind two sequential checks:
 *  1. All required runtime permissions must be granted.
 *  2. App must be the default dialer (OEM-aware via [DefaultDialerManager]).
 *
 * A [DisposableEffect] on ON_RESUME re-checks both states whenever the user
 * returns from a system permission dialog, the default-dialer role dialog,
 * or manual Settings navigation — so the UI unlocks automatically with no
 * restart required.
 */
@Composable
fun PermissionHandler(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var permissionDeniedCount by remember { mutableIntStateOf(0) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    var hasPermission by remember {
        mutableStateOf(PermissionManager.hasAllPermissions(context))
    }
    var dialerState by remember {
        mutableStateOf(DefaultDialerManager.getState(context))
    }

    // Re-check on every resume so the UI reacts when the user:
    //  • Grants a permission from the system Settings and returns
    //  • Sets the default dialer via the system dialog and returns
    //  • Manually navigates Settings on OEM devices and returns
    //  • An OEM ROM resets the default dialer (e.g. Xiaomi after reboot)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = PermissionManager.hasAllPermissions(context)
                dialerState = DefaultDialerManager.getState(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Launcher: runtime permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            hasPermission = true
        } else {
            permissionDeniedCount++
            if (permissionDeniedCount >= 2) showSettingsDialog = true
        }
    }

    // Launcher: system default-dialer role dialog
    // Only used for CanRequestViaDialog path — OEM manual path uses startActivity directly
    val defaultDialerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // User dismissed or accepted the system dialog — re-check
        dialerState = DefaultDialerManager.getState(context)
    }

    if (showSettingsDialog) {
        PermissionSettingsDialog(
            onDismiss = { showSettingsDialog = false },
            onGoToSettings = {
                PermissionManager.openAppSettings(context)
                showSettingsDialog = false
            }
        )
    }

    when {
        // Step 1 — Permissions not yet granted
        !hasPermission -> {
            PermissionRequiredScreen(
                onGrantPermissions = {
                    permissionLauncher.launch(PermissionManager.REQUIRED_PERMISSIONS)
                }
            )
        }

        // Step 2 — Not default dialer yet (CanRequestViaDialog or RequiresManualSetting)
        dialerState !is DefaultDialerState.IsDefault -> {
            DefaultDialerSetupScreen(
                state = dialerState,
                onRequestViaDialog = {
                    val intent = DefaultDialerManager.createRequestDefaultDialerIntent(context)
                    if (intent != null) {
                        defaultDialerLauncher.launch(intent)
                    } else {
                        // Intent creation failed on this device — fall back to manual settings
                        DefaultDialerManager.openDefaultAppsSettings(context)
                    }
                },
                onOpenManualSettings = {
                    DefaultDialerManager.openDefaultAppsSettings(context)
                }
            )
        }

        // Step 3 — All checks passed, show the actual screen
        else -> content()
    }
}

@Composable
private fun PermissionRequiredScreen(onGrantPermissions: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.permission_required_title),
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.permission_required_description),
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onGrantPermissions) {
            Text(stringResource(R.string.permission_grant_btn))
        }
    }
}

@Composable
private fun PermissionSettingsDialog(onDismiss: () -> Unit, onGoToSettings: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.permission_dialog_title)) },
        text = { Text(stringResource(R.string.permission_dialog_message)) },
        confirmButton = {
            TextButton(onClick = onGoToSettings) {
                Text(stringResource(R.string.permission_go_to_settings))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
