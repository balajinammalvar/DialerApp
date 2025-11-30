package com.balaji.callhistory.utils

import com.balaji.callhistory.data.CallEntity
import com.balaji.callhistory.data.Contact

object SearchHelper {
    
    fun filterCalls(
        calls: List<CallEntity>,
        searchQuery: String,
        contactNameCache: Map<String, String?> = emptyMap()
    ): List<CallEntity> {
        if (searchQuery.isEmpty()) return calls
        
        return calls.filter { call ->
            val contactName = contactNameCache[call.number]
            call.number.contains(searchQuery, ignoreCase = true) ||
                    contactName?.contains(searchQuery, ignoreCase = true) == true
        }
    }
    
    fun filterContacts(
        contacts: List<Contact>,
        searchQuery: String
    ): List<Contact> {
        if (searchQuery.isEmpty()) return contacts
        
        return contacts.filter { contact ->
            contact.name.contains(searchQuery, ignoreCase = true) ||
                    contact.phoneNumber.contains(searchQuery, ignoreCase = true)
        }
    }
    
    fun filterCallsByType(
        calls: List<CallEntity>,
        callType: String
    ): List<CallEntity> {
        return when (callType) {
            "missed" -> calls.filter { it.callType == "missed" }
            "received" -> calls.filter { it.callType == "received" }
            "dialed" -> calls.filter { it.callType == "dialed" }
            else -> calls
        }
    }
    
    fun filterCallsByDay(
        calls: List<CallEntity>,
        selectedDay: String
    ): List<CallEntity> {
        if (selectedDay == "all") return calls
        
        val dayFormat = java.text.SimpleDateFormat("EEEE", java.util.Locale.getDefault())
        return calls.filter { call ->
            dayFormat.format(java.util.Date(call.timestamp)) == selectedDay
        }
    }
}
