package com.balaji.callhistory.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateHelper {
    fun getDateSection(timestamp: Long): String {
        val callDate = Calendar.getInstance().apply { timeInMillis = timestamp }
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        
        return when {
            isSameDay(callDate, today) -> "Today"
            isSameDay(callDate, yesterday) -> "Yesterday"
            else -> SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()).format(Date(timestamp))
        }
    }
    
    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
