package com.hbatia.sleeptimer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock

object TimerScheduler {

    const val REQUEST_FIRE = 1001
    const val REQUEST_CANCEL = 1002
    const val REQUEST_OPEN = 1003

    private fun alarmManager(context: Context) =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun firePendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context.applicationContext,
            REQUEST_FIRE,
            Intent(context.applicationContext, SleepTimerReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    /**
     * מתזמן את הכיבוי.
     *
     * משתמשים ב-ELAPSED_REALTIME_WAKEUP ולא ב-RTC_WAKEUP: הטלפון לא מחובר לרשת
     * ולכן אין סנכרון שעון, ושעון המערכת עלול להיות שגוי או לקפוץ. elapsedRealtime
     * נמדד מרגע האתחול, רץ גם בשינה עמוקה, ולא מושפע משינוי שעה או אזור זמן.
     */
    fun schedule(context: Context, minutes: Int) {
        val durationMs = minutes * 60_000L
        val endElapsed = SystemClock.elapsedRealtime() + durationMs
        val endWall = System.currentTimeMillis() + durationMs
        val bootId = System.currentTimeMillis() - SystemClock.elapsedRealtime()

        alarmManager(context).setExactAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            endElapsed,
            firePendingIntent(context)
        )

        Prefs.saveTimer(context, endElapsed, endWall, bootId)
        Prefs.setLastMinutes(context, minutes)
        Notifications.showCountdown(context, endWall)
    }

    fun cancel(context: Context) {
        alarmManager(context).cancel(firePendingIntent(context))
        Prefs.clearTimer(context)
        Notifications.cancelAll(context)
    }

    /** כמה אלפיות שנייה נותרו, או 0 אם אין טיימר פעיל. */
    fun remainingMs(context: Context): Long {
        val end = Prefs.endElapsed(context)
        if (end <= 0L) return 0L
        val remaining = end - SystemClock.elapsedRealtime()
        return if (remaining > 0L) remaining else 0L
    }

    fun isActive(context: Context): Boolean = remainingMs(context) > 0L
}
