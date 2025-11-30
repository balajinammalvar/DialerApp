package com.balaji.callhistory.analytics

import android.content.Context

interface AnalyticsAgent {
    fun startSession(context: Context?)
    fun endSession(context: Context?)
    fun logUserEvent(
        context: Context?,
        eventType: AnalyticsManager.TrackingEvent?,
        vararg params: Any?
    )
}