package com.balaji.callhistory.data

import androidx.compose.runtime.Immutable

/**
 * Holds both contact display fields resolved from the device contacts in a
 * **single ContentResolver query**, replacing the two separate
 * [ContactHelper.getContactName] + [ContactHelper.getContactPhotoUri] calls
 * that were made per phone number.
 */
@Immutable
data class ContactDisplayInfo(
    val name: String?,
    val photoUri: String?
) {
    companion object {
        /** Sentinel used when contacts permission is missing or lookup failed. */
        val EMPTY = ContactDisplayInfo(name = null, photoUri = null)
    }
}

