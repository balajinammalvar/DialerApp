package com.balaji.callhistory.data

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Contact(
    val id: Long,
    val name: String,
    val phoneNumber: String,
    val photoUri: String?,
    val contactId: String = UUID.randomUUID().toString(),
)
