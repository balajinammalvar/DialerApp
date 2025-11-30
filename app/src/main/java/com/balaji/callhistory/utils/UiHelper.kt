package com.balaji.callhistory.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.PhoneMissed
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.balaji.callhistory.data.CallEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val ICON_SIZE_MULTIPLIER = 0.6f

object UiHelper {

    @Composable
    fun ContactAvatar(
        contactName: String?,
        photoUri: String? = null,
        size: Dp = 48.dp,
        modifier: Modifier = Modifier
    ) {
        if (photoUri != null) {
            AsyncImage(
                model = photoUri,
                contentDescription = "Contact photo",
                modifier = modifier
                    .size(size)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (contactName != null) {
                    Text(
                        text = contactName.firstOrNull()?.uppercase() ?: "?",
                        style = when {
                            size >= 80.dp -> MaterialTheme.typography.displayMedium
                            size >= 48.dp -> MaterialTheme.typography.titleLarge
                            else -> MaterialTheme.typography.bodyLarge
                        },
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(size * ICON_SIZE_MULTIPLIER),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }

    fun formatDateAndTime(timestamp: Long): String {
        val date = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
        val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))
        return "$time  $date"
    }

    fun formatDateTimeWithYear(timestamp: Long): String {
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("MMM dd, yy", Locale.getDefault())
        return "${timeFormat.format(Date(timestamp))} • ${dateFormat.format(Date(timestamp))}"
    }

    fun getCallTypeText(callType: String): String {
        return when (callType) {
            "missed" -> "Missed call"
            "received" -> "Incoming call"
            "dialed" -> "Outgoing call"
            else -> "Declined Call"
        }
    }

    @Composable
    fun ContactInfo(contactName: String?, phoneNumber: String) {
        Text(
            text = contactName ?: phoneNumber,
            style = MaterialTheme.typography.bodyLarge
        )
        if (contactName != null) {
            Text(
                text = phoneNumber,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    @Composable
    fun CallIcon(call: CallEntity): ImageVector {
        val callIcon = when (call.callType) {
            "missed" -> Icons.AutoMirrored.Filled.PhoneMissed
            "received" -> Icons.AutoMirrored.Filled.CallReceived
            "dialed" -> Icons.AutoMirrored.Filled.CallMade
            else -> Icons.AutoMirrored.Filled.CallReceived
        }
        return callIcon
    }

    fun formatDateHeader(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(now))
        val yesterday = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(now - 86400000))
        val callDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(timestamp))
        
        return when (callDate) {
            today -> "Today"
            yesterday -> "Yesterday"
            else -> SimpleDateFormat("MMMM dd, yy", Locale.getDefault()).format(Date(timestamp))
        }
    }
}
