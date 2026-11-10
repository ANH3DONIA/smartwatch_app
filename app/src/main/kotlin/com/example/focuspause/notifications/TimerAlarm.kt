/*
 * Copyright 2026 FocusPause
 */
package com.example.focuspause.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.focuspause.MainActivity
import com.example.focuspause.data.PomodoroMode

/** Programa un aviso para cuando termine la fase actual, aunque la app esté en segundo plano. */
interface TimerAlarmScheduler {
    fun schedule(endsAtMillis: Long, mode: PomodoroMode)
    fun cancel()
}

class AndroidTimerAlarmScheduler(context: Context) : TimerAlarmScheduler {

    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun schedule(endsAtMillis: Long, mode: PomodoroMode) {
        val intent = pendingIntent(mode)
        val canBeExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (canBeExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endsAtMillis, intent)
        } else {
            // Sin permiso de alarma exacta: el aviso puede retrasarse un poco, pero llega.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endsAtMillis, intent)
        }
    }

    override fun cancel() {
        alarmManager.cancel(pendingIntent(PomodoroMode.FOCUS))
    }

    private fun pendingIntent(mode: PomodoroMode): PendingIntent {
        val intent = Intent(appContext, TimerAlarmReceiver::class.java)
            .putExtra(TimerAlarmReceiver.EXTRA_MODE, mode.name)
        return PendingIntent.getBroadcast(
            appContext,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private companion object {
        const val REQUEST_CODE = 1001
    }
}

class TimerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val mode = runCatching {
            PomodoroMode.valueOf(intent.getStringExtra(EXTRA_MODE).orEmpty())
        }.getOrDefault(PomodoroMode.FOCUS)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(manager)

        val (title, text) = when (mode) {
            PomodoroMode.FOCUS -> "¡Sesión de enfoque terminada!" to "Es hora de tu pausa. Levántate y muévete un poco."
            PomodoroMode.SHORT_BREAK, PomodoroMode.LONG_BREAK -> "¡Pausa terminada!" to "Vuelve a enfocarte cuando estés listo."
        }

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = android.app.Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(android.app.Notification.CATEGORY_ALARM)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()

        // Si el usuario no concedió el permiso de notificaciones, el sistema simplemente la ignora.
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel(manager: NotificationManager) {
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Fin de sesión",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Avisa cuando termina una sesión de enfoque o una pausa"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 300, 150, 300)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val EXTRA_MODE = "mode"
        const val CHANNEL_ID = "timer_done"
        const val NOTIFICATION_ID = 42
    }
}
