package com.ftt.signal

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {

    const val CH_SIGNAL = "ftt_signal"
    const val CH_SCAN   = "ftt_scan"

    fun createChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // High-priority channel for BUY/SELL alerts
            nm.createNotificationChannel(NotificationChannel(
                CH_SIGNAL,
                "Signal Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "BUY/SELL signal notifications"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 200)
            })

            // Low-priority channel for background scan status
            nm.createNotificationChannel(NotificationChannel(
                CH_SCAN,
                "Background Scanner",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "FTT Signal background scanning"
                setShowBadge(false)
            })
        }
    }

    fun sendSignalNotif(
        ctx: Context,
        pair: String,
        dir: String,
        conf: Int,
        grade: String = "",
        id: Int = pair.hashCode()
    ) {
        val arrow = if (dir == "BUY") "▲" else "▼"
        val gradeStr = if (grade.isNotEmpty()) " [$grade]" else ""

        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            ctx, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(ctx, CH_SIGNAL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("FTT Signal — $pair$gradeStr")
            .setContentText("$arrow $dir · ${conf}% confidence")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$arrow $dir · ${conf}% confidence\nTap to open signal details"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()

        try {
            NotificationManagerCompat.from(ctx).notify(id, notif)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS permission not granted yet
        }
    }

    fun buildScanNotif(ctx: Context, pairCount: Int = 0): Notification {
        val intent = Intent(ctx, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        val text = if (pairCount > 0) "Watching $pairCount pairs..." else "Scanning for signals..."

        return NotificationCompat.Builder(ctx, CH_SCAN)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle("FTT Signal Scanner")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(pi)
            .build()
    }
}
