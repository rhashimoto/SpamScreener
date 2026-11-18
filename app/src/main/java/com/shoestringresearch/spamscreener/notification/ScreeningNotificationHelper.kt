package com.shoestringresearch.spamscreener.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.shoestringresearch.spamscreener.R
import com.shoestringresearch.spamscreener.util.PhoneNumberHelper

object ScreeningNotificationHelper {
  private const val CHANNEL_ID = "screened_calls_channel"
  private const val CHANNEL_NAME = "Screened Calls"
  private const val NOTIFICATION_ID_BASE = 10000

  fun ensureNotificationChannel(context: Context) {
    val channel = NotificationChannel(
      CHANNEL_ID,
      CHANNEL_NAME,
      NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
      description = "Notifications for calls screened by Spam Screener"
    }
    val notificationManager = context.getSystemService(NotificationManager::class.java)
    notificationManager?.createNotificationChannel(channel)
  }

  @SuppressLint("NotificationPermission")
  fun postScreenedCallNotification(context: Context, rawNumber: String) {
    ensureNotificationChannel(context)

    if (ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
      ) != PackageManager.PERMISSION_GRANTED
    ) {
      return
    }

    val formattedNumber = PhoneNumberHelper.formatForDisplay(rawNumber)
    val title = "Screened $formattedNumber"

    // Intent to open phone dialer with that number
    val dialIntent = Intent(Intent.ACTION_DIAL).apply {
      data = "tel:$rawNumber".toUri()
      flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    val pendingIntent = PendingIntent.getActivity(
      context,
      rawNumber.hashCode(),
      dialIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setContentTitle(title)
      .setContentText("Tap to open phone dialer with this number")
      .setContentIntent(pendingIntent)
      .setAutoCancel(true)
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .build()

    val notificationManager = context.getSystemService(NotificationManager::class.java)
    val notificationId = NOTIFICATION_ID_BASE + ((rawNumber.hashCode() and 0x7FFFFFFF) % 10000)
    try {
      notificationManager?.notify(notificationId, notification)
    } catch (_: SecurityException) {
      // Notification permission not granted
    }
  }
}
