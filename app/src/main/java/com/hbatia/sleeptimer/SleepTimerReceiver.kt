package com.hbatia.sleeptimer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager

/**
 * נורה על ידי ה-AlarmManager בתום הזמן.
 *
 * goAsync מאפשר לעבוד בחוט רקע אחרי שה-onReceive חוזר, במקום לחסום
 * את החוט הראשי. WakeLock קצר מוודא שהמכשיר לא חוזר לשינה עמוקה באמצע.
 */
class SleepTimerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val pendingResult = goAsync()

        val powerManager = app.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "sleeptimer:stop"
        )
        wakeLock.acquire(15_000L)

        Thread {
            try {
                MediaStopper.stopBlocking(app)
            } finally {
                Prefs.clearTimer(app)
                Notifications.showDone(app)
                if (wakeLock.isHeld) runCatching { wakeLock.release() }
                pendingResult.finish()
            }
        }.start()
    }
}
