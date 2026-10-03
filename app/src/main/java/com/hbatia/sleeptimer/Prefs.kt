package com.hbatia.sleeptimer

import android.content.Context

/**
 * מצב הטיימר נשמר ב-SharedPreferences כדי שהמסך יוכל להציג זמן שנותר
 * גם אחרי שהאפליקציה נסגרה והאזעקה עדיין ממתינה במערכת.
 */
object Prefs {

    private const val FILE = "sleep_timer_prefs"
    private const val KEY_END_ELAPSED = "end_elapsed"     // SystemClock.elapsedRealtime של סוף הטיימר
    private const val KEY_END_WALL = "end_wall"           // זמן שעון רגיל, לתצוגה בהתראה בלבד
    private const val KEY_BOOT_ID = "boot_id"             // מזהה אתחול, לזיהוי ריסטארט
    private const val KEY_KILL_BG = "kill_background"
    private const val KEY_LAST_MINUTES = "last_minutes"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun saveTimer(context: Context, endElapsed: Long, endWall: Long, bootId: Long) {
        prefs(context).edit()
            .putLong(KEY_END_ELAPSED, endElapsed)
            .putLong(KEY_END_WALL, endWall)
            .putLong(KEY_BOOT_ID, bootId)
            .apply()
    }

    fun clearTimer(context: Context) {
        prefs(context).edit()
            .remove(KEY_END_ELAPSED)
            .remove(KEY_END_WALL)
            .remove(KEY_BOOT_ID)
            .apply()
    }

    fun endElapsed(context: Context): Long = prefs(context).getLong(KEY_END_ELAPSED, 0L)

    fun endWall(context: Context): Long = prefs(context).getLong(KEY_END_WALL, 0L)

    fun bootId(context: Context): Long = prefs(context).getLong(KEY_BOOT_ID, 0L)

    fun killBackground(context: Context): Boolean =
        prefs(context).getBoolean(KEY_KILL_BG, false)

    fun setKillBackground(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_KILL_BG, value).apply()
    }

    fun lastMinutes(context: Context): Int = prefs(context).getInt(KEY_LAST_MINUTES, 30)

    fun setLastMinutes(context: Context, minutes: Int) {
        prefs(context).edit().putInt(KEY_LAST_MINUTES, minutes).apply()
    }
}
