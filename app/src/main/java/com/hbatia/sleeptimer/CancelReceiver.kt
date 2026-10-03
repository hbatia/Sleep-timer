package com.hbatia.sleeptimer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** כפתור "ביטול" שבהתראה. */
class CancelReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        TimerScheduler.cancel(context.applicationContext)
    }
}
