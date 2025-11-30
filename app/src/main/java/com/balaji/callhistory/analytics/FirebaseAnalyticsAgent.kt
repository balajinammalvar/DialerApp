package com.balaji.callhistory.analytics


import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources.NotFoundException
import android.os.Bundle
import android.util.Log
import com.balaji.callhistory.R
import com.google.firebase.analytics.FirebaseAnalytics
import java.util.concurrent.CopyOnWriteArrayList

class FirebaseAnalyticsAgent : AnalyticsAgent {
    private val mSessionActivities = CopyOnWriteArrayList<Context?>()
    private var mContext: Context? = null
    private var mFirebaseAnalytics: FirebaseAnalytics? = null


    override fun startSession(context: Context?) {
        try {
            context?.let {context ->
                this.initialize(context)
            }
        } catch (_: Exception) {
        }
    }


    //private function
    @SuppressLint("MissingPermission")
    private fun initialize(context: Context): Boolean {
        try {
            if (!mSessionActivities.contains(context)) {
                // Obtain the Firebase Analytics instance.
                mContext = context
                mFirebaseAnalytics = FirebaseAnalytics.getInstance(context)
                mFirebaseAnalytics!!.setAnalyticsCollectionEnabled(true)
                this.mSessionActivities.add(context)
                Log.d(TAG, " initialize() initializing : GoogleAnalytics agent")
            }
            return true
        } catch (e: Exception) {
//            Log.w(TAG + " initialize() exception caught: " + e.getMessage(), "");
        }
        return false
    }



    override fun endSession(context: Context?) {
        // empty method
    }


    override fun logUserEvent(
        context: Context?,
        eventType: AnalyticsManager.TrackingEvent?,
        vararg params: Any?
    ) {
        try {
            when (eventType) {
                AnalyticsManager.TrackingEvent.ENTERED_SPLASH_SCREEN -> {
                    val source = (if (params.size > 0) params[0] else "") as String?
                    trackEvent(getString(R.string.analytics_splash), source)
                }

                AnalyticsManager.TrackingEvent.ENTERED_CALL_HISTORY -> {
                    trackEvent(getString(R.string.analytics_call_history))
                }

                AnalyticsManager.TrackingEvent.ENTERED_DIALER_SCREEN -> {
                    trackEvent(getString(R.string.analytics_dialer))
                }

                AnalyticsManager.TrackingEvent.ENTERED_CONTACT_SCREEN -> {
                    trackEvent(getString(R.string.analytics_contact))
                }

                AnalyticsManager.TrackingEvent.ENTERED_CALL_HISTORY_DETAILS -> {
                    trackEvent(getString(R.string.analytics_call_history_details))
                }

                AnalyticsManager.TrackingEvent.CALL_HISTORY_SEARCH_USED -> {
                    trackEvent(getString(R.string.analytics_call_history_search))
                }

                AnalyticsManager.TrackingEvent.ENTER_CONTACT_SEARCH -> {
                    trackEvent(getString(R.string.analytics_contact_search))
                }

                AnalyticsManager.TrackingEvent.BACK -> {
                    trackEvent(getString(R.string.analytics_backp))
                }

                AnalyticsManager.TrackingEvent.SESSION_OVER -> {
                    trackEvent(getString(R.string.analytics_session_over))
                }

                AnalyticsManager.TrackingEvent.START_SESSION -> {
                    trackEvent(getString(R.string.analytics_session_start))
                }
                AnalyticsManager.TrackingEvent.END_SESSION -> {
                    trackEvent(getString(R.string.analytics_session_over))
                }
                AnalyticsManager.TrackingEvent.ENTERED_CONTACT_DETAILS -> {
                    trackEvent(getString(R.string.analytics_contact_details))
                }
                AnalyticsManager.TrackingEvent.EXCEPTIONS_TRACKING -> {
                    trackEvent(getString(R.string.analytics_session_exception))
                }
                null -> {
                    trackEvent(getString(R.string.analytics_session_exception))
                }
            }
        } catch (e: Exception) {
        }
    }

    fun trackEvent(eventName: String) {
        // Build and send an Event.
        mFirebaseAnalytics!!.logEvent(eventName, Bundle())
    }

    /***
     * Tracking event
     *
     * @param eventName event name
     * @param eventSource event source from
     */
    fun trackEvent(eventName: String, eventSource: String?) {
        // Build and send an Event.
        val bundle = Bundle()
        bundle.putString(getString(R.string.analytics_key_source), eventSource)
        mFirebaseAnalytics!!.logEvent(eventName, bundle)
    }

    //private
    private fun getString(rId: Int): String {
        try {
            return mContext!!.getString(rId)
        } catch (_: NotFoundException) {
            return rId.toString()
        }
    }

    companion object {
        //private members
        private val TAG: String = FirebaseAnalyticsAgent::class.java.getSimpleName()
    }
}