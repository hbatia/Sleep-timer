package com.hbatia.sleeptimer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.hbatia.sleeptimer.databinding.ActivityMainBinding
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val handler = Handler(Looper.getMainLooper())

    private val ticker = object : Runnable {
        override fun run() {
            refreshStatus()
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Notifications.createChannel(this)
        requestNotificationPermissionIfNeeded()

        binding.btn15.setOnClickListener { startTimer(15) }
        binding.btn30.setOnClickListener { startTimer(30) }
        binding.btn45.setOnClickListener { startTimer(45) }
        binding.btn60.setOnClickListener { startTimer(60) }

        binding.inputMinutes.setText(Prefs.lastMinutes(this).toString())
        binding.btnCustom.setOnClickListener {
            val minutes = binding.inputMinutes.text.toString().trim().toIntOrNull()
            if (minutes == null || minutes <= 0 || minutes > 600) {
                Toast.makeText(this, R.string.invalid_minutes, Toast.LENGTH_SHORT).show()
            } else {
                hideKeyboard()
                startTimer(minutes)
            }
        }

        binding.btnCancel.setOnClickListener {
            TimerScheduler.cancel(this)
            Toast.makeText(this, R.string.toast_cancelled, Toast.LENGTH_SHORT).show()
            refreshStatus()
        }

        binding.btnStopNow.setOnClickListener {
            MediaStopper.stopAsync(this)
            Toast.makeText(this, R.string.toast_stopping, Toast.LENGTH_SHORT).show()
        }

        binding.switchKill.isChecked = Prefs.killBackground(this)
        binding.switchKill.setOnCheckedChangeListener { _, checked ->
            Prefs.setKillBackground(this, checked)
        }

        binding.btnBattery.setOnClickListener { openBatterySettings() }
    }

    override fun onResume() {
        super.onResume()
        handler.post(ticker)
        updateBatteryButton()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(ticker)
    }

    private fun startTimer(minutes: Int) {
        TimerScheduler.schedule(this, minutes)
        binding.inputMinutes.setText(minutes.toString())
        Toast.makeText(
            this,
            getString(R.string.toast_started, minutes),
            Toast.LENGTH_SHORT
        ).show()
        refreshStatus()
    }

    @SuppressLint("SetTextI18n")
    private fun refreshStatus() {
        val remaining = TimerScheduler.remainingMs(this)
        if (remaining <= 0L) {
            binding.statusText.text = getString(R.string.status_idle)
            binding.btnCancel.isEnabled = false
        } else {
            val totalSeconds = (remaining + 999L) / 1000L
            val hours = totalSeconds / 3600L
            val minutes = (totalSeconds % 3600L) / 60L
            val seconds = totalSeconds % 60L
            val formatted = if (hours > 0L) {
                String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(Locale.US, "%02d:%02d", minutes, seconds)
            }
            binding.statusText.text = formatted
            binding.btnCancel.isEnabled = true
        }
    }

    private fun updateBatteryButton() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val exempt = runCatching {
            powerManager.isIgnoringBatteryOptimizations(packageName)
        }.getOrDefault(false)
        binding.btnBattery.visibility = if (exempt) android.view.View.GONE else android.view.View.VISIBLE
    }

    private fun openBatterySettings() {
        val direct = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:$packageName")
        )
        val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        val launched = runCatching { startActivity(direct) }.isSuccess
        if (!launched) {
            runCatching { startActivity(fallback) }
                .onFailure { Toast.makeText(this, R.string.battery_unavailable, Toast.LENGTH_LONG).show() }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            runCatching {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 7001)
            }
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        runCatching { imm.hideSoftInputFromWindow(binding.inputMinutes.windowToken, 0) }
    }
}
