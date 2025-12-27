package com.balaji.callhistory.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsPhone
import androidx.compose.material.icons.filled.Tty
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private const val ICON_ALPHA = 0.6f
private const val ANIMATION_DURATION = 1500

@Composable
fun EmptyStateContent(
    isInitialLoad: Boolean,
    hasFilters: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "flip")
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(ANIMATION_DURATION),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation"
        )
        if (isInitialLoad) {
            Icon(
                imageVector = Icons.Default.SettingsPhone,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .graphicsLayer { rotationZ = rotation },
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = ICON_ALPHA)
            )
        } else {
            Icon(
                imageVector = Icons.Default.SettingsPhone,
                contentDescription = null,
                modifier = Modifier.size(80.dp).graphicsLayer(
                    rotationZ = rotation
                ),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = ICON_ALPHA)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = when {
                isInitialLoad -> "Getting things ready"
                hasFilters -> "No calls found"
                else -> "No calls yet"
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (isInitialLoad) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Loading your call history...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EmptyStateContentPreview() {
    EmptyStateContent(isInitialLoad = true, hasFilters = false)
}

@Preview(showBackground = true)
@Composable
fun EmptyStateFilteredPreview() {
    EmptyStateContent(isInitialLoad = false, hasFilters = true)
}

@Preview(showBackground = true)
@Composable
fun EmptyStateNoCallsPreview() {
    EmptyStateContent(isInitialLoad = false, hasFilters = false)
}
