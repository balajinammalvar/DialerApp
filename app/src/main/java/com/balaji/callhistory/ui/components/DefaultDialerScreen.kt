package com.balaji.callhistory.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.balaji.callhistory.R
import com.balaji.callhistory.defaultdialer.DefaultDialerState

/**
 * Full-screen UI shown when the app is not yet the default dialer.
 *
 * Renders one of two sub-screens depending on [state]:
 *  • [DefaultDialerState.CanRequestViaDialog]   → single button launches system dialog
 *  • [DefaultDialerState.RequiresManualSetting] → OEM-specific step guide + Open Settings button
 */
@Composable
fun DefaultDialerSetupScreen(
    state: DefaultDialerState,
    onRequestViaDialog: () -> Unit,
    onOpenManualSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Dialpad,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.default_dialer_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.default_dialer_description),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        when (state) {
            is DefaultDialerState.CanRequestViaDialog -> {
                DialogRequestSection(onRequestViaDialog = onRequestViaDialog)
            }
            is DefaultDialerState.RequiresManualSetting -> {
                OemManualGuide(
                    oemName = state.oemName,
                    steps = state.steps,
                    onOpenSettings = onOpenManualSettings
                )
            }
            is DefaultDialerState.IsDefault -> {
                // Not shown — PermissionHandler gates this screen before IsDefault is reached
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Sub-sections
// ---------------------------------------------------------------------------

@Composable
private fun DialogRequestSection(onRequestViaDialog: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onRequestViaDialog,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.default_dialer_btn_set))
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.default_dialer_dialog_hint),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OemManualGuide(
    oemName: String,
    steps: String,
    onOpenSettings: () -> Unit
) {
    OemNoticeBadge(oemName = oemName)
    Spacer(Modifier.height(16.dp))
    OemStepsCard(steps = steps)
    Spacer(Modifier.height(24.dp))
    Button(
        onClick = onOpenSettings,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(R.string.default_dialer_btn_open_settings))
    }
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.default_dialer_manual_hint),
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun OemNoticeBadge(oemName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = stringResource(R.string.default_dialer_oem_notice, oemName),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}

@Composable
private fun OemStepsCard(steps: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = stringResource(R.string.default_dialer_follow_steps),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = steps,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.6f
            )
        }
    }
}
