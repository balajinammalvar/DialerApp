package com.balaji.callhistory.navigation

import kotlinx.serialization.Serializable

@Serializable
data object HistoryRoute

@Serializable
data object DialerRoute

@Serializable
data object ContactsRoute

@Serializable
data object ThemeRoute

@Serializable
data class CallHistoryDetailsRoute(val phoneNumber: String)

@Serializable
data class ContactDetailsRoute(val contactId: Long)
