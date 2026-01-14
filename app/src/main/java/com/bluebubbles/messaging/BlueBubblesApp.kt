package com.bluebubbles.messaging

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BlueBubblesApp : Application() {

  override fun onCreate() {
    super.onCreate()
    createNotificationChannels()
  }

  private fun createNotificationChannels() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val notificationManager = getSystemService(NotificationManager::class.java)

      // Messages channel
      val messagesChannel = NotificationChannel(
        CHANNEL_MESSAGES,
        "Messages",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "New iMessage notifications"
        enableVibration(true)
        enableLights(true)
        lightColor = 0xFF00E5FF.toInt()
      }

      // Service channel
      val serviceChannel = NotificationChannel(
        CHANNEL_SERVICE,
        "Background Service",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "BlueBubbles connection service"
      }

      notificationManager.createNotificationChannel(messagesChannel)
      notificationManager.createNotificationChannel(serviceChannel)
    }
  }

  companion object {
    const val CHANNEL_MESSAGES = "bluebubbles_messages"
    const val CHANNEL_SERVICE = "bluebubbles_service"
  }
}
