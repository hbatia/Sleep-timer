package com.hbatia.sleeptimer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * אזעקות נמחקות באתחול. מנקים מצב ישן כדי שלא תוצג התראה
 * של טיימר שכבר לא קיים.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext
        Prefs.clearTimer(app)
        Notifications.cancelAll(app)
    }
}
