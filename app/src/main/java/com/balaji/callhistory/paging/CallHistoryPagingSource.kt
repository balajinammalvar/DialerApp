package com.balaji.callhistory.paging

import android.content.Context
import android.provider.CallLog
import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.balaji.callhistory.data.CallEntity
import com.balaji.callhistory.utils.CallTypeMapper
import com.balaji.callhistory.utils.DateFormatterHelper

class CallHistoryPagingSource(
    private val context: Context,
    private val callTypeFilter: String = "all"
) : PagingSource<Int, CallEntity>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, CallEntity> {
        return try {
            val offset = params.key ?: 0
            val limit = params.loadSize

            val selection = when (callTypeFilter) {
                CallTypeMapper.TYPE_MISSED   -> "${CallLog.Calls.TYPE} = ${CallLog.Calls.MISSED_TYPE}"
                CallTypeMapper.TYPE_RECEIVED -> "${CallLog.Calls.TYPE} = ${CallLog.Calls.INCOMING_TYPE}"
                CallTypeMapper.TYPE_DIALED   -> "${CallLog.Calls.TYPE} = ${CallLog.Calls.OUTGOING_TYPE}"
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
                val idCol       = cursor.getColumnIndexOrThrow(CallLog.Calls._ID)
                val numberCol   = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                val dateCol     = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
                val typeCol     = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                val durationCol = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)

                var currentIndex = 0
                while (cursor.moveToNext()) {
                    if (currentIndex < offset) { currentIndex++; continue }
                    if (currentIndex >= offset + limit) break

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
                    currentIndex++
                }
            }
            Log.d("CallHistory", "Call List Size : ${calls.size}, offset : ${params.key}, Limit : ${params.loadSize}")
            LoadResult.Page(
                data     = calls,
                prevKey  = if (offset == 0) null else offset - limit,
                nextKey  = if (calls.size < limit) null else offset + limit
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, CallEntity>): Int? =
        state.anchorPosition
}
