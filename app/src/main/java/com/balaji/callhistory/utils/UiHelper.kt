package com.balaji.callhistory.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.balaji.callhistory.R
import com.balaji.callhistory.data.CallEntity
import androidx.compose.material.icons.Icons

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
                contentDescription = stringResource(R.string.cd_contact_photo),
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

    /** Uses [DateFormatterHelper] — no new SimpleDateFormat per call. */
    fun formatDateAndTime(timestamp: Long): String =
        DateFormatterHelper.formatDateAndTime(timestamp)

    /** Uses [DateFormatterHelper] — no new SimpleDateFormat per call. */
    fun formatDateTimeWithYear(timestamp: Long): String =
        DateFormatterHelper.formatDateTimeWithYear(timestamp)

    /**
     * Returns the call type display string.
     * Not marked @Composable — uses string constants, not resources.
     * For localised strings use R.string.call_type_* directly in the UI.
     */
    fun getCallTypeText(callType: String): String = when (callType) {
        CallTypeMapper.TYPE_MISSED   -> "Missed call"
        CallTypeMapper.TYPE_RECEIVED -> "Incoming call"
        CallTypeMapper.TYPE_DIALED   -> "Outgoing call"
        else -> "Declined Call"
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

    /**
     * Returns the icon for a call type.
     * NOT marked @Composable — [ImageVector] does not require composition.
     * Uses pre-built [CallTypeMapper] instances to avoid per-call allocation.
     */
    fun CallIcon(call: CallEntity): ImageVector = CallTypeMapper.toIcon(call.callType)

    /** Uses [DateFormatterHelper] — no new SimpleDateFormat per call. */
    fun formatDateHeader(timestamp: Long): String =
        DateFormatterHelper.formatFullDate(timestamp)
}
