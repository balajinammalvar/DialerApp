package com.balaji.callhistory.broadcastreceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.balaji.callhistory.MainActivity
import com.balaji.callhistory.repo.AppRepositoryProvider

class CallBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val state = intent?.getStringExtra(TelephonyManager.EXTRA_STATE)

        when(state){
            TelephonyManager.EXTRA_STATE_IDLE -> {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    AppRepositoryProvider.repository?.triggerRefresh()
                    Log.d("CallHistory", "CallBroadcastReceiver - EXTRA_STATE_IDLE")
                }, 1000)
            }
        }
    }
}
