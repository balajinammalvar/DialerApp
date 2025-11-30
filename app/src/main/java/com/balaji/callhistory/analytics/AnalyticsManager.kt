package com.balaji.callhistory.analytics

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class AnalyticsManager private constructor() {
    private val mAgents = CopyOnWriteArrayList<AnalyticsAgent>()
    private var mExecutorService: ExecutorService? = null

    enum class TrackingEvent {
        // session
        START_SESSION, END_SESSION,

        // WHASTS APP DIRECT
        ENTERED_SPLASH_SCREEN, ENTERED_CALL_HISTORY, ENTERED_DIALER_SCREEN, ENTERED_CONTACT_SCREEN, ENTERED_CONTACT_DETAILS, ENTERED_CALL_HISTORY_DETAILS, CALL_HISTORY_SEARCH_USED, SESSION_OVER, ENTER_CONTACT_SEARCH, BACK,


        // exception
        EXCEPTIONS_TRACKING,
    }

    //private functions
    init {
        this.mExecutorService = Executors.newSingleThreadExecutor()
    }

    companion object {
        //private members
        private val CLASSTAG: String = AnalyticsManager::class.java.simpleName
        @SuppressLint("StaticFieldLeak")
        private var sInstance: AnalyticsManager? = null

        //public functions
        @SuppressLint("LongLogTag")
        @Synchronized
        fun initialize(): Boolean {
            try {
                if (sInstance == null) {
                    synchronized(AnalyticsManager::class.java) {
                        if (sInstance == null) {
                            Log.d(CLASSTAG, " initialize() creating new instance.")
                            sInstance = AnalyticsManager()
                            addAnalyticsAgent(FirebaseAnalyticsAgent())
                        }
                    }
                }
                return true
            } catch (e: Exception) {
                Log.i(CLASSTAG + " initialize() exception caught: " + e.message, "")
            }
            return false
        }

        @SuppressLint("LongLogTag")
        @Synchronized
        fun addAnalyticsAgent(agent: AnalyticsAgent): Boolean {
            try {
                Log.v(
                    CLASSTAG + " addAnalyticsAgent() adding agent type:" + agent.javaClass.getSimpleName(),
                    ""
                )
                return sInstance!!.mAgents.add(agent)
            } catch (e: Exception) {
                Log.i(CLASSTAG + " addAnalyticsAgent() exception caught: " + e.message, "")
            }
            return false
        }

        @SuppressLint("LongLogTag")
        @Synchronized
        fun removeAnalyticsAgent(agent: AnalyticsAgent): Boolean {
            try {
                Log.v(
                    CLASSTAG + " addAnalyticsAgent() adding agent type:" + agent.javaClass.getSimpleName(),
                    ""
                )
                return sInstance!!.mAgents.remove(agent)
            } catch (e: Exception) {
                Log.i(CLASSTAG + " addAnalyticsAgent() exception caught: " + e.message, "")
            }
            return false
        }


        @SuppressLint("LongLogTag")
        @Synchronized
        fun logAnalyticEvent(context: Context?, eventType: TrackingEvent, vararg params: Any?) {
            sInstance?.mExecutorService?.execute {
                try {
                    when (eventType) {
                        TrackingEvent.START_SESSION -> {
                            for (agent in sInstance!!.mAgents) {
                                agent.startSession(context)
                            }
                        }

                        TrackingEvent.END_SESSION -> {
                            for (agent in sInstance!!.mAgents) {
                                agent.endSession(context)
                            }
                        }

                        TrackingEvent.ENTERED_SPLASH_SCREEN, TrackingEvent.ENTERED_CALL_HISTORY,
                        TrackingEvent.ENTERED_DIALER_SCREEN, TrackingEvent.ENTERED_CONTACT_SCREEN, TrackingEvent.ENTERED_CALL_HISTORY_DETAILS, TrackingEvent.CALL_HISTORY_SEARCH_USED, TrackingEvent.ENTERED_CONTACT_DETAILS, TrackingEvent.SESSION_OVER, TrackingEvent.ENTER_CONTACT_SEARCH, TrackingEvent.EXCEPTIONS_TRACKING -> {
                            for (agent in sInstance?.mAgents!!) {
                                agent.logUserEvent(context, eventType, *params)
                            }
                        }

                        TrackingEvent.BACK -> {
                            for (agent in sInstance?.mAgents!!) {
                                agent.logUserEvent(context, eventType, *params)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.i(CLASSTAG + " logAnalyticEvent() exception caught: " + e.message, "")
                }
            }
        }
    }
}
