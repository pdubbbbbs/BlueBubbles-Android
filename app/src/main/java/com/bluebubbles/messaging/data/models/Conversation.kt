package com.bluebubbles.messaging.data.models

import java.util.Date

data class Conversation(
  val guid: String,
  val chatIdentifier: String,
  val displayName: String,
  val participants: List<Participant>,
  val lastMessage: Message?,
  val lastMessageDate: Date?,
  val unreadCount: Int = 0,
  val isPinned: Boolean = false,
  val isMuted: Boolean = false,
  val isArchived: Boolean = false,
  val isGroup: Boolean = false
) {
  val title: String
    get() = displayName.ifEmpty {
      if (participants.size == 1) {
        participants.first().displayName
      } else {
        participants.joinToString(", ") { it.displayName }
      }
    }

  val avatarUrl: String?
    get() = if (!isGroup && participants.size == 1) {
      participants.first().avatarUrl
    } else null
}

data class Participant(
  val address: String,
  val displayName: String,
  val firstName: String? = null,
  val lastName: String? = null,
  val avatarUrl: String? = null,
  val isMe: Boolean = false
) {
  val initials: String
    get() = when {
      firstName != null && lastName != null -> "${firstName.first()}${lastName.first()}"
      displayName.isNotEmpty() -> displayName.take(2).uppercase()
      else -> address.take(2).uppercase()
    }
}

data class Message(
  val guid: String,
  val text: String?,
  val subject: String? = null,
  val dateCreated: Date,
  val dateDelivered: Date? = null,
  val dateRead: Date? = null,
  val isFromMe: Boolean,
  val handle: Participant? = null,
  val attachments: List<Attachment> = emptyList(),
  val associatedMessages: List<AssociatedMessage> = emptyList(),
  val threadOriginatorGuid: String? = null,
  val hasReactions: Boolean = false,
  val error: Int = 0
) {
  val isDelivered: Boolean get() = dateDelivered != null
  val isRead: Boolean get() = dateRead != null
  val hasError: Boolean get() = error != 0
  val hasAttachments: Boolean get() = attachments.isNotEmpty()
}

data class Attachment(
  val guid: String,
  val mimeType: String?,
  val fileName: String?,
  val filePath: String?,
  val width: Int? = null,
  val height: Int? = null,
  val totalBytes: Long? = null,
  val isSticker: Boolean = false
) {
  val isImage: Boolean get() = mimeType?.startsWith("image/") == true
  val isVideo: Boolean get() = mimeType?.startsWith("video/") == true
  val isAudio: Boolean get() = mimeType?.startsWith("audio/") == true
}

data class AssociatedMessage(
  val guid: String,
  val type: ReactionType,
  val handle: Participant?
)

enum class ReactionType(val value: Int) {
  LOVE(2000),
  LIKE(2001),
  DISLIKE(2002),
  LAUGH(2003),
  EMPHASIS(2004),
  QUESTION(2005);

  companion object {
    fun fromValue(value: Int): ReactionType? = entries.find { it.value == value }
  }
}
