package com.hbatia.sleeptimer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object Notifications {

    private const val CHANNEL_ID = "sleep_timer"
    private const val ID_COUNTDOWN = 1
    private const val ID_DONE = 2

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.channel_description)
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    /**
     * התראה עם ספירה לאחור חיה. setUsesChronometer + setChronometerCountDown
     * נותנים שעון שמתעדכן לבד במערכת, בלי שירות רקע ובלי צריכת סוללה.
     */
    fun showCountdown(context: Context, endWallClock: Long) {
        createChannel(context)

        val cancelIntent = PendingIntent.getBroadcast(
            context,
            TimerScheduler.REQUEST_CANCEL,
            Intent(context, CancelReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openIntent = PendingIntent.getActivity(
            context,
            TimerScheduler.REQUEST_OPEN,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_running_title))
            .setContentText(context.getString(R.string.notif_running_text))
            .setWhen(endWallClock)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openIntent)
            .addAction(0, context.getString(R.string.action_cancel), cancelIntent)
            .build()

        runCatching { NotificationManagerCompat.from(context).notify(ID_COUNTDOWN, notification) }
    }

    fun showDone(context: Context) {
        createChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_done_title))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setTimeoutAfter(60_000L)
            .build()

        val manager = NotificationManagerCompat.from(context)
        runCatching {
            manager.cancel(ID_COUNTDOWN)
            manager.notify(ID_DONE, notification)
        }
    }

    fun cancelAll(context: Context) {
        val manager = NotificationManagerCompat.from(context)
        runCatching {
            manager.cancel(ID_COUNTDOWN)
            manager.cancel(ID_DONE)
        }
    }
}
