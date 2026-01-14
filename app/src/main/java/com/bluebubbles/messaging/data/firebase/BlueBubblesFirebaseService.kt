package com.bluebubbles.messaging.data.firebase

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import androidx.core.app.NotificationCompat
import com.bluebubbles.messaging.BlueBubblesApp
import com.bluebubbles.messaging.MainActivity
import com.bluebubbles.messaging.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BlueBubblesFirebaseService : FirebaseMessagingService() {

  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  companion object {
    private const val TAG = "BlueBubblesFirebase"
  }

  override fun onNewToken(token: String) {
    super.onNewToken(token)
    Log.d(TAG, "New FCM token: $token")

    // Register token with BlueBubbles server
    serviceScope.launch {
      try {
        // TODO: Inject repository and register token
        // serverRepository.registerFcmToken(deviceId, deviceName, token)
        Log.d(TAG, "FCM token registered with server")
      } catch (e: Exception) {
        Log.e(TAG, "Failed to register FCM token", e)
      }
    }
  }

  override fun onMessageReceived(message: RemoteMessage) {
    super.onMessageReceived(message)
    Log.d(TAG, "Message received: ${message.data}")

    val data = message.data

    when (data["type"]) {
      "new-message" -> handleNewMessage(data)
      "updated-message" -> handleUpdatedMessage(data)
      "group-name-change" -> handleGroupNameChange(data)
      "participant-left" -> handleParticipantChange(data)
      "participant-added" -> handleParticipantChange(data)
      else -> Log.d(TAG, "Unknown message type: ${data["type"]}")
    }
  }

  private fun handleNewMessage(data: Map<String, String>) {
    val chatGuid = data["chatGuid"] ?: return
    val senderName = data["senderName"] ?: "Unknown"
    val messageText = data["text"] ?: "New message"
    val messageGuid = data["guid"] ?: ""

    showNotification(
      title = senderName,
      body = messageText,
      chatGuid = chatGuid,
      messageGuid = messageGuid
    )
  }

  private fun handleUpdatedMessage(data: Map<String, String>) {
    // Handle message updates (reactions, edits, etc.)
    Log.d(TAG, "Message updated: ${data["guid"]}")
  }

  private fun handleGroupNameChange(data: Map<String, String>) {
    val chatGuid = data["chatGuid"] ?: return
    val newName = data["newName"] ?: "Unknown"

    showNotification(
      title = "Group renamed",
      body = "Group is now \"$newName\"",
      chatGuid = chatGuid,
      messageGuid = ""
    )
  }

  private fun handleParticipantChange(data: Map<String, String>) {
    val chatGuid = data["chatGuid"] ?: return
    val participantName = data["participantName"] ?: "Someone"
    val action = if (data["type"] == "participant-added") "joined" else "left"

    showNotification(
      title = "Group updated",
      body = "$participantName $action the group",
      chatGuid = chatGuid,
      messageGuid = ""
    )
  }

  private fun showNotification(
    title: String,
    body: String,
    chatGuid: String,
    messageGuid: String
  ) {
    val intent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
      putExtra("chatGuid", chatGuid)
      putExtra("messageGuid", messageGuid)
    }

    val pendingIntent = PendingIntent.getActivity(
      this,
      chatGuid.hashCode(),
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(this, BlueBubblesApp.CHANNEL_MESSAGES)
      .setSmallIcon(R.drawable.ic_notification)
      .setContentTitle(title)
      .setContentText(body)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .setColor(Color.parseColor("#00E5FF"))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setCategory(NotificationCompat.CATEGORY_MESSAGE)
      .setStyle(NotificationCompat.BigTextStyle().bigText(body))
      .build()

    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.notify(chatGuid.hashCode(), notification)
  }
}
