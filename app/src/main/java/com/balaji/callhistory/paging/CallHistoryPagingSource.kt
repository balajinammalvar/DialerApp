package com.balaji.callhistory.paging

import android.content.Context
import android.provider.CallLog
import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.balaji.callhistory.data.CallEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CallHistoryPagingSource(
    private val context: Context,
    private val callTypeFilter: String = "all"
) : PagingSource<Int, CallEntity>() {

    private val dateHeaderFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("hh:mm a dd-MMM-yy", Locale.getDefault())
    private val dayFormatter = SimpleDateFormat("EEEE", Locale.getDefault())

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, CallEntity> {
        return try {
            val offset = params.key ?: 0
            val limit = params.loadSize

            val selection = when (callTypeFilter) {
                "missed" -> "${CallLog.Calls.TYPE} = ${CallLog.Calls.MISSED_TYPE}"
                "received" -> "${CallLog.Calls.TYPE} = ${CallLog.Calls.INCOMING_TYPE}"
                "dialed" -> "${CallLog.Calls.TYPE} = ${CallLog.Calls.OUTGOING_TYPE}"
                else -> null
            }

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
                selection,
                null,
                "${CallLog.Calls.DATE} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(CallLog.Calls._ID)
                val numberCol = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                val dateCol = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
                val typeCol = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                val durationCol = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)

                var currentIndex = 0
                while (cursor.moveToNext()) {
                    if (currentIndex < offset) {
                        currentIndex++
                        continue
                    }

                    if (currentIndex >= offset + limit) {
                        break
                    }
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
                    val formattedDate = dateHeaderFormatter.format(d)
                    val formattedTime = timeFormatter.format(d)
                    val dayName = dayFormatter.format(d)

                    calls.add(
                        CallEntity(
                            id = id,
                            number = number,
                            timestamp = ts,
                            callType = callType,
                            duration = duration,
                            formattedDate = formattedDate,
                            formattedTime = formattedTime,
                            dayName = dayName
                        )
                    )

                    currentIndex++
                }
            }
            Log.d("CallHistory", "Call List Size : ${calls.size}, offset : ${params.key}, Limit : ${params.loadSize}")
            LoadResult.Page(
                data = calls,
                prevKey = if (offset == 0) null else offset - limit,
                nextKey = if (calls.size < limit) null else offset + limit
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, CallEntity>): Int? {
        return state.anchorPosition
    }
}
