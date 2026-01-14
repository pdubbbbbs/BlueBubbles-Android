package com.bluebubbles.messaging.data.socket

import android.util.Log
import com.bluebubbles.messaging.data.local.dao.ConversationDao
import com.bluebubbles.messaging.data.local.dao.MessageDao
import com.bluebubbles.messaging.data.local.entity.MessageEntity
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class TypingIndicatorState(
  val chatGuid: String,
  val isTyping: Boolean,
  val senderAddress: String? = null
)

@Singleton
class SocketEventHandler @Inject constructor(
  private val socketManager: SocketManager,
  private val conversationDao: ConversationDao,
  private val messageDao: MessageDao
) {

  companion object {
    private const val TAG = "SocketEventHandler"
  }

  private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
  private val gson = Gson()

  private val _typingIndicators = MutableSharedFlow<TypingIndicatorState>(replay = 0)
  val typingIndicators: SharedFlow<TypingIndicatorState> = _typingIndicators.asSharedFlow()

  fun startListening() {
    socketManager.events
      .onEach { event -> handleEvent(event) }
      .launchIn(scope)
  }

  private fun handleEvent(event: SocketEvent) {
    scope.launch {
      when (event) {
        is SocketEvent.Connected -> {
          Log.d(TAG, "Socket connected - refreshing data")
        }

        is SocketEvent.Disconnected -> {
          Log.d(TAG, "Socket disconnected: ${event.reason}")
        }

        is SocketEvent.Error -> {
          Log.e(TAG, "Socket error: ${event.message}")
        }

        is SocketEvent.NewMessage -> {
          handleNewMessage(event.data)
        }

        is SocketEvent.UpdatedMessage -> {
          handleUpdatedMessage(event.data)
        }

        is SocketEvent.TypingIndicator -> {
          handleTypingIndicator(event.data)
        }

        is SocketEvent.GroupNameChange -> {
          handleGroupNameChange(event.data)
        }

        is SocketEvent.ParticipantAdded -> {
          handleParticipantChange(event.data, added = true)
        }

        is SocketEvent.ParticipantRemoved -> {
          handleParticipantChange(event.data, added = false)
        }

        is SocketEvent.ChatReadStatusChanged -> {
          handleChatReadStatusChanged(event.data)
        }
      }
    }
  }

  private suspend fun handleNewMessage(data: JsonObject) {
    try {
      val message = data.getAsJsonObject("message") ?: return
      val chatGuid = data.get("chatGuid")?.asString ?: return

      val messageEntity = MessageEntity(
        guid = message.get("guid")?.asString ?: return,
        chatGuid = chatGuid,
        text = message.get("text")?.asString,
        subject = message.get("subject")?.asString,
        dateCreated = message.get("dateCreated")?.asLong ?: System.currentTimeMillis(),
        dateDelivered = message.get("dateDelivered")?.asLong,
        dateRead = message.get("dateRead")?.asLong,
        isFromMe = message.get("isFromMe")?.asBoolean ?: false,
        handleAddress = message.getAsJsonObject("handle")?.get("address")?.asString,
        handleDisplayName = message.getAsJsonObject("handle")?.get("displayName")?.asString,
        attachmentsJson = message.getAsJsonArray("attachments")?.toString(),
        threadOriginatorGuid = message.get("threadOriginatorGuid")?.asString,
        hasReactions = false,
        error = message.get("error")?.asInt ?: 0
      )

      messageDao.insertMessage(messageEntity)

      // Update conversation's last message
      val conversation = conversationDao.getConversation(chatGuid)
      conversation?.let { conv ->
        val updated = conv.copy(
          lastMessageGuid = messageEntity.guid,
          lastMessageText = messageEntity.text,
          lastMessageDate = messageEntity.dateCreated,
          lastMessageIsFromMe = messageEntity.isFromMe,
          unreadCount = if (!messageEntity.isFromMe) conv.unreadCount + 1 else conv.unreadCount,
          lastUpdated = System.currentTimeMillis()
        )
        conversationDao.updateConversation(updated)
      }

      Log.d(TAG, "New message cached: ${messageEntity.guid}")
    } catch (e: Exception) {
      Log.e(TAG, "Error handling new message", e)
    }
  }

  private suspend fun handleUpdatedMessage(data: JsonObject) {
    try {
      val message = data.getAsJsonObject("message") ?: return
      val guid = message.get("guid")?.asString ?: return

      val existing = messageDao.getMessage(guid) ?: return

      val updated = existing.copy(
        dateDelivered = message.get("dateDelivered")?.asLong ?: existing.dateDelivered,
        dateRead = message.get("dateRead")?.asLong ?: existing.dateRead,
        error = message.get("error")?.asInt ?: existing.error
      )

      messageDao.updateMessage(updated)
      Log.d(TAG, "Message updated: $guid")
    } catch (e: Exception) {
      Log.e(TAG, "Error handling updated message", e)
    }
  }

  private fun handleTypingIndicator(data: JsonObject) {
    try {
      val chatGuid = data.get("chatGuid")?.asString ?: return
      val isTyping = data.get("display")?.asBoolean ?: false
      val senderAddress = data.get("senderGuid")?.asString
        ?: data.getAsJsonObject("handle")?.get("address")?.asString

      scope.launch {
        _typingIndicators.emit(
          TypingIndicatorState(
            chatGuid = chatGuid,
            isTyping = isTyping,
            senderAddress = senderAddress
          )
        )
      }

      Log.d(TAG, "Typing indicator: $chatGuid isTyping=$isTyping sender=$senderAddress")
    } catch (e: Exception) {
      Log.e(TAG, "Error handling typing indicator", e)
    }
  }

  private suspend fun handleGroupNameChange(data: JsonObject) {
    try {
      val chatGuid = data.get("chatGuid")?.asString ?: return
      val newName = data.get("newName")?.asString ?: return

      val conversation = conversationDao.getConversation(chatGuid) ?: return
      val updated = conversation.copy(
        displayName = newName,
        lastUpdated = System.currentTimeMillis()
      )
      conversationDao.updateConversation(updated)

      Log.d(TAG, "Group name changed: $chatGuid -> $newName")
    } catch (e: Exception) {
      Log.e(TAG, "Error handling group name change", e)
    }
  }

  private suspend fun handleParticipantChange(data: JsonObject, added: Boolean) {
    try {
      val chatGuid = data.get("chatGuid")?.asString ?: return
      val participantAddress = data.get("participantAddress")?.asString

      // TODO: Update participants list in conversation
      Log.d(TAG, "Participant ${if (added) "added" else "removed"}: $chatGuid - $participantAddress")
    } catch (e: Exception) {
      Log.e(TAG, "Error handling participant change", e)
    }
  }

  private suspend fun handleChatReadStatusChanged(data: JsonObject) {
    try {
      val chatGuid = data.get("chatGuid")?.asString ?: return
      val read = data.get("read")?.asBoolean ?: false

      if (read) {
        conversationDao.markAsRead(chatGuid)
        Log.d(TAG, "Chat marked as read: $chatGuid")
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error handling chat read status", e)
    }
  }
}
