package com.balaji.callhistory.data

import androidx.compose.runtime.Immutable
import java.util.UUID

@Immutable
data class CallEntity(
    val id: Long,
    val number: String,
    val timestamp: Long,
    val callType: String,
    val duration: Long,
    val formattedDate: String = "",
    val formattedTime: String = "",
    val dayName: String = "",
    val callHistoryId: String = UUID.randomUUID().toString(),
)
