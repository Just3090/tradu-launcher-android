package com.just6889.td.tradu_launcher.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.just6889.td.tradu_launcher.R

object NotificationUtils {
    // Notificación simple para el receiver
    fun showSimpleNotification(context: Context, title: String, text: String) {
        createNotificationChannel(context)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID + 9999, builder.build())
    }
    private const val CHANNEL_ID = "apk_download_channel"
    private const val NOTIFICATION_ID = 1001

    fun showApkDownloadedNotification(context: Context, project: Project, onInstallIntent: Intent) {
        createNotificationChannel(context)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            onInstallIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val largeIcon = BitmapFactory.decodeResource(context.resources, R.drawable.ic_launcher_foreground)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.apk_download_complete_title, project.titulo))
            .setContentText(context.getString(R.string.apk_download_complete_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setLargeIcon(largeIcon)
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID + project.id_proyecto.hashCode(), builder.build())
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.apk_download_channel_name)
            val descriptionText = context.getString(R.string.apk_download_channel_desc)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
