package com.balaji.callhistory.repo

import android.content.Context
import android.provider.CallLog
import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.balaji.callhistory.data.CallEntity
import com.balaji.callhistory.paging.CallHistoryPagingSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class CallHistoryRepository(private val context: Context) {
    private val _refreshTrigger = MutableStateFlow(0L)
    val refreshTrigger = _refreshTrigger.asStateFlow()
    
    fun triggerRefresh() {
        Log.d("CallHistory", "triggerRefresh")
        _refreshTrigger.value = System.currentTimeMillis()
    }
    
    fun getCallHistoryPager(callTypeFilter: String): Flow<PagingData<CallEntity>> {
        Log.d("CallHistory", "getCallHistoryPager")
        return Pager(
            config = PagingConfig(
                pageSize = 50,
                enablePlaceholders = false,
                initialLoadSize = 50
            ),
            pagingSourceFactory = { CallHistoryPagingSource(context, callTypeFilter) }
        ).flow
    }


    fun getCallsForSuggestions(): Flow<List<CallEntity>> = flow {
        val calls = mutableListOf<CallEntity>()
        val twoDaysAgo = System.currentTimeMillis() - java.util.concurrent.TimeUnit.DAYS.toMillis(2)

        withContext(Dispatchers.IO) {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(
                    CallLog.Calls._ID,
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.DATE,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DURATION
                ),
                "${CallLog.Calls.DATE} >= ?",
                arrayOf(twoDaysAgo.toString()),
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
                    val date = cursor.getLong(dateCol)
                    val type = cursor.getInt(typeCol)
                    val duration = cursor.getLong(durationCol)

                    val callType = when (type) {
                        CallLog.Calls.OUTGOING_TYPE -> "dialed"
                        CallLog.Calls.INCOMING_TYPE -> "received"
                        CallLog.Calls.MISSED_TYPE -> "missed"
                        else -> "unknown"
                    }

                    calls.add(CallEntity(id, number, date, callType, duration))
                }
            }
        }

        emit(calls)
    }
}
