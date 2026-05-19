package com.balaji.callhistory.viewmodel

import android.content.Context
import android.provider.CallLog
import androidx.lifecycle.ViewModel
import com.balaji.callhistory.data.CallEntity
import com.balaji.callhistory.utils.CallTypeMapper
import com.balaji.callhistory.utils.DateFormatterHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class CallHistoryDetailsViewModel(private val context: Context) : ViewModel() {

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
            val idCol       = cursor.getColumnIndexOrThrow(CallLog.Calls._ID)
            val numberCol   = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
            val dateCol     = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
            val typeCol     = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
            val durationCol = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)

            while (cursor.moveToNext()) {
                val ts = cursor.getLong(dateCol)
                calls.add(
                    CallEntity(
                        id            = cursor.getLong(idCol),
                        number        = cursor.getString(numberCol) ?: "Unknown",
                        timestamp     = ts,
                        callType      = CallTypeMapper.fromInt(cursor.getInt(typeCol)),
                        duration      = cursor.getLong(durationCol),
                        formattedDate = DateFormatterHelper.formatDateHeader(ts),
                        formattedTime = DateFormatterHelper.formatTimeWithDate(ts),
                        dayName       = DateFormatterHelper.formatDayName(ts)
                    )
                )
            }
        }

        emit(calls)
    }
}