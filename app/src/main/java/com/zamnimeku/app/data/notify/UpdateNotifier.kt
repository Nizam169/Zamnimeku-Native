package com.zamnimeku.app.data.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.zamnimeku.app.MainActivity
import com.zamnimeku.app.data.api.UpdateInfo

// Notifikasi status-bar saat ada update baru. Diketuk untuk buka app
// (dialog update langsung tampil).
object UpdateNotifier {
    private const val CHANNEL_ID = "zamnimeku_updates"
    private const val NOTIF_ID = 1001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pembaruan App",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Pemberitahuan versi baru Zamnimeku"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showUpdateAvailable(context: Context, info: UpdateInfo) {
        ensureChannel(context)
        val openApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context, 0, openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Versi baru Zamnimeku tersedia")
            .setContentText("v${info.versionName} sudah bisa didownload. Ketuk untuk update.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("v${info.versionName} sudah bisa didownload. Ketuk untuk buka app dan update.")
            )
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID, notif)
        } catch (_: SecurityException) {
            // Izin notifikasi belum diberikan — dialog dalam app tetap jalan
        }
    }
}
