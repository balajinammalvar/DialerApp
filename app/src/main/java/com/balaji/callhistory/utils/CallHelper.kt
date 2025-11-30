package com.balaji.callhistory.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

object CallHelper {
    fun makeCall(context: Context, phoneNumber: String) {
        val callIntent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        context.startActivity(callIntent)
    }
}
