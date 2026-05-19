package com.balaji.callhistory.utils

import android.provider.CallLog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.PhoneMissed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Single source of truth for call-type string constants, icon and colour
 * mappings. Eliminates the identical [when] blocks duplicated across
 * [CallHistoryPagingSource], [CallHistoryDetailsViewModel] and
 * [CallHistoryRepository].
 */
object CallTypeMapper {

    const val TYPE_MISSED   = "missed"
    const val TYPE_RECEIVED = "received"
    const val TYPE_DIALED   = "dialed"
    const val TYPE_UNKNOWN  = "unknown"

    // Pre-built icon instances – no allocation on every call
    val ICON_MISSED:   ImageVector = Icons.AutoMirrored.Filled.PhoneMissed
    val ICON_RECEIVED: ImageVector = Icons.AutoMirrored.Filled.CallReceived
    val ICON_DIALED:   ImageVector = Icons.AutoMirrored.Filled.CallMade

    /** Maps a raw [CallLog.Calls.TYPE] integer to a string constant. */
    fun fromInt(typeInt: Int): String = when (typeInt) {
        CallLog.Calls.OUTGOING_TYPE -> TYPE_DIALED
        CallLog.Calls.INCOMING_TYPE -> TYPE_RECEIVED
        CallLog.Calls.MISSED_TYPE   -> TYPE_MISSED
        else                        -> TYPE_UNKNOWN
    }

    /** Returns the vector icon for the given callType string. */
    fun toIcon(callType: String): ImageVector = when (callType) {
        TYPE_MISSED  -> ICON_MISSED
        TYPE_DIALED  -> ICON_DIALED
        else         -> ICON_RECEIVED
    }

    /** Returns true when the callType represents a missed call. */
    fun isMissed(callType: String): Boolean = callType == TYPE_MISSED

    /** Returns Color.Red for missed calls, the provided [defaultColor] otherwise. */
    fun tintColor(callType: String, defaultColor: Color): Color =
        if (isMissed(callType)) Color.Red else defaultColor
}

