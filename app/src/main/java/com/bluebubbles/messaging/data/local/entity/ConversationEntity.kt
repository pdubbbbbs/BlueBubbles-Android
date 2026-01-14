package com.bluebubbles.messaging.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bluebubbles.messaging.data.models.Conversation
import com.bluebubbles.messaging.data.models.Message
import com.bluebubbles.messaging.data.models.Participant
import java.util.Date

@Entity(tableName = "conversations")
data class ConversationEntity(
  @PrimaryKey
  val guid: String,
  val chatIdentifier: String,
  val displayName: String,
  val participantsJson: String, // JSON serialized list
  val lastMessageGuid: String?,
  val lastMessageText: String?,
  val lastMessageDate: Long?,
  val lastMessageIsFromMe: Boolean?,
  val unreadCount: Int = 0,
  val isPinned: Boolean = false,
  val isMuted: Boolean = false,
  val isArchived: Boolean = false,
  val isGroup: Boolean = false,
  val lastUpdated: Long = System.currentTimeMillis()
)

fun ConversationEntity.toConversation(participants: List<Participant>): Conversation {
  return Conversation(
    guid = guid,
    chatIdentifier = chatIdentifier,
    displayName = displayName,
    participants = participants,
    lastMessage = if (lastMessageGuid != null) {
      Message(
        guid = lastMessageGuid,
        text = lastMessageText,
        dateCreated = Date(lastMessageDate ?: 0),
        isFromMe = lastMessageIsFromMe ?: false
      )
    } else null,
    lastMessageDate = lastMessageDate?.let { Date(it) },
    unreadCount = unreadCount,
    isPinned = isPinned,
    isMuted = isMuted,
    isArchived = isArchived,
    isGroup = isGroup
  )
}

fun Conversation.toEntity(participantsJson: String): ConversationEntity {
  return ConversationEntity(
    guid = guid,
    chatIdentifier = chatIdentifier,
    displayName = displayName,
    participantsJson = participantsJson,
    lastMessageGuid = lastMessage?.guid,
    lastMessageText = lastMessage?.text,
    lastMessageDate = lastMessageDate?.time,
    lastMessageIsFromMe = lastMessage?.isFromMe,
    unreadCount = unreadCount,
    isPinned = isPinned,
    isMuted = isMuted,
    isArchived = isArchived,
    isGroup = isGroup
  )
}
