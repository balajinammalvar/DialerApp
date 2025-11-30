package com.balaji.callhistory.viewmodel

import android.content.Context
import android.provider.CallLog
import androidx.lifecycle.ViewModel
import com.balaji.callhistory.data.CallEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.*

class CallHistoryDetailsViewModel(private val context: Context) : ViewModel() {

    private val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("hh:mm a dd-MMM-yy", Locale.getDefault())
    private val dayFormatter = SimpleDateFormat("EEEE", Locale.getDefault())

    fun getCallHistoryForNumber(phoneNumber: String): Flow<List<CallEntity>> = flow {
        val calls = mutableListOf<CallEntity>()
        
        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(
                CallLog.Calls._ID,
                CallLog.Calls.NUMBER,
                CallLog.Calls.DATE,
                CallLog.Calls.TYPE,
                CallLog.Calls.DURATION
            ),
            "${CallLog.Calls.NUMBER} = ?",
            arrayOf(phoneNumber),
            "${CallLog.Calls.DATE} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(CallLog.Calls._ID)
            val numberCol = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
            val dateCol = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
            val typeCol = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
            val durationCol = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)
            
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val number = cursor.getString(numberCol) ?: "Unknown"
                val ts = cursor.getLong(dateCol)
                val typeInt = cursor.getInt(typeCol)
                val duration = cursor.getLong(durationCol)
                
                val callType = when (typeInt) {
                    CallLog.Calls.OUTGOING_TYPE -> "dialed"
                    CallLog.Calls.INCOMING_TYPE -> "received"
                    CallLog.Calls.MISSED_TYPE -> "missed"
                    else -> "unknown"
                }
                
                val d = Date(ts)
                calls.add(
                    CallEntity(
                        id = id,
                        number = number,
                        timestamp = ts,
                        callType = callType,
                        duration = duration,
                        formattedDate = dateFormatter.format(d),
                        formattedTime = timeFormatter.format(d),
                        dayName = dayFormatter.format(d)
                    )
                )
            }
        }
        
        emit(calls)
    }
}