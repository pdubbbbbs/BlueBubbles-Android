package com.bluebubbles.messaging.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.bluebubbles.messaging.data.models.Attachment
import com.bluebubbles.messaging.data.models.Message
import com.bluebubbles.messaging.data.models.Participant
import java.util.Date

@Entity(
  tableName = "messages",
  foreignKeys = [
    ForeignKey(
      entity = ConversationEntity::class,
      parentColumns = ["guid"],
      childColumns = ["chatGuid"],
      onDelete = ForeignKey.CASCADE
    )
  ],
  indices = [
    Index(value = ["chatGuid"]),
    Index(value = ["dateCreated"])
  ]
)
data class MessageEntity(
  @PrimaryKey
  val guid: String,
  val chatGuid: String,
  val text: String?,
  val subject: String?,
  val dateCreated: Long,
  val dateDelivered: Long?,
  val dateRead: Long?,
  val isFromMe: Boolean,
  val handleAddress: String?,
  val handleDisplayName: String?,
  val attachmentsJson: String?, // JSON serialized list
  val threadOriginatorGuid: String?,
  val hasReactions: Boolean = false,
  val error: Int = 0,
  val isSending: Boolean = false,
  val tempGuid: String? = null
)

fun MessageEntity.toMessage(): Message {
  return Message(
    guid = guid,
    text = text,
    subject = subject,
    dateCreated = Date(dateCreated),
    dateDelivered = dateDelivered?.let { Date(it) },
    dateRead = dateRead?.let { Date(it) },
    isFromMe = isFromMe,
    handle = if (handleAddress != null) {
      Participant(
        address = handleAddress,
        displayName = handleDisplayName ?: handleAddress
      )
    } else null,
    attachments = emptyList(), // Parsed separately from JSON
    threadOriginatorGuid = threadOriginatorGuid,
    hasReactions = hasReactions,
    error = error
  )
}

fun Message.toEntity(chatGuid: String, attachmentsJson: String? = null): MessageEntity {
  return MessageEntity(
    guid = guid,
    chatGuid = chatGuid,
    text = text,
    subject = subject,
    dateCreated = dateCreated.time,
    dateDelivered = dateDelivered?.time,
    dateRead = dateRead?.time,
    isFromMe = isFromMe,
    handleAddress = handle?.address,
    handleDisplayName = handle?.displayName,
    attachmentsJson = attachmentsJson,
    threadOriginatorGuid = threadOriginatorGuid,
    hasReactions = hasReactions,
    error = error
  )
}
