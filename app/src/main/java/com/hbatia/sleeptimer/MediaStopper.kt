package com.hbatia.sleeptimer

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent

/**
 * עצירת הנגינה בפועל.
 *
 * הגישה היא לא להרוג את הנגן אלא להשהות אותו, בשתי דרכים משלימות:
 *   1. שליחת KEYCODE_MEDIA_PAUSE — בדיוק כמו לחיצה על כפתור ההשהיה באוזניות.
 *      עובד על כל נגן שרשום כ-MediaSession, כולל נגנים מקומיים ודפדפן.
 *   2. תפיסת audio focus מסוג GAIN והחזקתו כמה שניות — כך נגן שמכבד
 *      audio focus (רובם המוחלט) עוצר ולא מתחיל מחדש לבד.
 *
 * אף אחת מהשתיים לא דורשת הרשאה מסוכנת או root.
 */
object MediaStopper {

    private const val TAG = "MediaStopper"

    /** רץ בחוט רקע. חוסם כ-8 שניות. */
    fun stopBlocking(context: Context) {
        val app = context.applicationContext
        val audio = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setWillPauseWhenDucked(true)
            .setOnAudioFocusChangeListener({ /* לא עושים כלום */ }, Handler(Looper.getMainLooper()))
            .build()

        try {
            sendPause(audio)
            audio.requestAudioFocus(focusRequest)

            // שליחה שנייה: נגנים מסוימים מתחילים לנגן מחדש כשהם מקבלים
            // בחזרה focus, או מגיבים באיחור לאירוע הראשון.
            Thread.sleep(1500)
            sendPause(audio)

            if (Prefs.killBackground(context)) {
                Thread.sleep(2000)
                killBackgroundApps(app)
            }

            // מחזיקים את ה-focus עוד קצת לפני שמשחררים, כדי שהנגן לא
            // יתעורר מיד. סך הכול נשארים הרבה מתחת למגבלת ה-10 שניות
            // של BroadcastReceiver.
            Thread.sleep(3000)
            sendPause(audio)
        } catch (t: Throwable) {
            Log.w(TAG, "stopBlocking failed", t)
        } finally {
            runCatching { audio.abandonAudioFocusRequest(focusRequest) }
        }
    }

    /** עצירה מיידית מתוך המסך, בלחיצת כפתור. */
    fun stopAsync(context: Context) {
        val app = context.applicationContext
        Thread { stopBlocking(app) }.start()
    }

    private fun sendPause(audio: AudioManager) {
        runCatching {
            audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE))
            audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE))
        }
    }

    /**
     * גיבוי: סגירת תהליכי רקע של כל האפליקציות שאינן מערכת.
     *
     * מגבלה חשובה: אפליקציה שמחזיקה foreground service בזמן נגינה לא תמיד
     * תיסגר כך. לכן זה שלב משלים ולא תחליף להשהיה.
     */
    private fun killBackgroundApps(context: Context) {
        val activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val packageManager = context.packageManager
        val self = context.packageName

        val installed = runCatching {
            packageManager.getInstalledApplications(0)
        }.getOrElse { emptyList<ApplicationInfo>() }

        for (info in installed) {
            val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isUpdatedSystem = (info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            if (isSystem && !isUpdatedSystem) continue
            if (info.packageName == self) continue
            runCatching { activityManager.killBackgroundProcesses(info.packageName) }
        }
    }
}
