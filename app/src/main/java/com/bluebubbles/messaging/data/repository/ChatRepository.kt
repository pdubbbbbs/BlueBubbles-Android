package com.bluebubbles.messaging.data.repository

import android.content.Context
import com.bluebubbles.messaging.data.api.BlueBubblesApi
import com.bluebubbles.messaging.data.api.SendMessageRequest
import com.bluebubbles.messaging.data.local.dao.ConversationDao
import com.bluebubbles.messaging.data.local.dao.MessageDao
import com.bluebubbles.messaging.data.local.entity.toConversation
import com.bluebubbles.messaging.data.local.entity.toEntity
import com.bluebubbles.messaging.data.local.entity.toMessage
import com.bluebubbles.messaging.data.models.*
import com.bluebubbles.messaging.ui.components.SelectedAttachment
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
  @ApplicationContext private val context: Context,
  private val api: BlueBubblesApi,
  private val conversationDao: ConversationDao,
  private val messageDao: MessageDao
) {

  private val gson = Gson()

  // ==================== CONVERSATIONS ====================

  fun observeConversations(): Flow<List<Conversation>> {
    return conversationDao.getActiveConversations().map { entities ->
      entities.map { entity ->
        val participants = try {
          gson.fromJson(entity.participantsJson, Array<Participant>::class.java).toList()
        } catch (e: Exception) {
          emptyList()
        }
        entity.toConversation(participants)
      }
    }
  }

  suspend fun getConversations(offset: Int = 0, limit: Int = 25, forceRefresh: Boolean = false): Result<List<Conversation>> {
    // Try cache first if not forcing refresh
    if (!forceRefresh && offset == 0) {
      val cached = conversationDao.getActiveConversations().first()
      if (cached.isNotEmpty()) {
        val conversations = cached.map { entity ->
          val participants = try {
            gson.fromJson(entity.participantsJson, Array<Participant>::class.java).toList()
          } catch (e: Exception) {
            emptyList()
          }
          entity.toConversation(participants)
        }
        // Fetch fresh in background but return cached immediately
        refreshConversationsFromServer(offset, limit)
        return Result.success(conversations)
      }
    }

    return refreshConversationsFromServer(offset, limit)
  }

  private suspend fun refreshConversationsFromServer(offset: Int, limit: Int): Result<List<Conversation>> {
    return try {
      val response = api.getChats(offset, limit)
      if (response.isSuccessful && response.body()?.data != null) {
        val conversations = response.body()!!.data!!.map { chatDto ->
          Conversation(
            guid = chatDto.guid,
            chatIdentifier = chatDto.chatIdentifier,
            displayName = chatDto.displayName ?: "",
            participants = chatDto.participants?.map { handle ->
              Participant(
                address = handle.address,
                displayName = handle.displayName ?: handle.address,
                firstName = handle.firstName,
                lastName = handle.lastName
              )
            } ?: emptyList(),
            lastMessage = chatDto.lastMessage?.let { msg ->
              Message(
                guid = msg.guid,
                text = msg.text,
                dateCreated = Date(msg.dateCreated),
                isFromMe = msg.isFromMe,
                error = msg.error ?: 0
              )
            },
            lastMessageDate = chatDto.lastMessage?.let { Date(it.dateCreated) },
            isGroup = (chatDto.participants?.size ?: 0) > 1
          )
        }

        // Cache to database
        val entities = conversations.map { conv ->
          conv.toEntity(gson.toJson(conv.participants))
        }
        conversationDao.insertConversations(entities)

        Result.success(conversations)
      } else {
        Result.failure(Exception("Failed to fetch conversations: ${response.message()}"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  // ==================== MESSAGES ====================

  fun observeMessages(chatGuid: String): Flow<List<Message>> {
    return messageDao.getMessagesForChat(chatGuid).map { entities ->
      entities.map { it.toMessage() }
    }
  }

  suspend fun getMessages(chatGuid: String, offset: Int = 0, limit: Int = 50, forceRefresh: Boolean = false): Result<List<Message>> {
    // Try cache first
    if (!forceRefresh && offset == 0) {
      val cached = messageDao.getMessagesForChatPaged(chatGuid, limit, 0)
      if (cached.isNotEmpty()) {
        // Fetch fresh in background but return cached immediately
        refreshMessagesFromServer(chatGuid, offset, limit)
        return Result.success(cached.map { it.toMessage() })
      }
    }

    return refreshMessagesFromServer(chatGuid, offset, limit)
  }

  private suspend fun refreshMessagesFromServer(chatGuid: String, offset: Int, limit: Int): Result<List<Message>> {
    return try {
      val response = api.getChatMessages(chatGuid, offset, limit)
      if (response.isSuccessful && response.body()?.data != null) {
        val messages = response.body()!!.data!!.map { msgDto ->
          Message(
            guid = msgDto.guid,
            text = msgDto.text,
            subject = msgDto.subject,
            dateCreated = Date(msgDto.dateCreated),
            dateDelivered = msgDto.dateDelivered?.let { Date(it) },
            dateRead = msgDto.dateRead?.let { Date(it) },
            isFromMe = msgDto.isFromMe,
            handle = msgDto.handle?.let { h ->
              Participant(
                address = h.address,
                displayName = h.displayName ?: h.address,
                firstName = h.firstName,
                lastName = h.lastName
              )
            },
            attachments = msgDto.attachments?.map { att ->
              Attachment(
                guid = att.guid,
                mimeType = att.mimeType,
                fileName = att.transferName,
                filePath = null,
                width = att.width,
                height = att.height,
                totalBytes = att.totalBytes,
                isSticker = att.isSticker ?: false
              )
            } ?: emptyList(),
            error = msgDto.error ?: 0
          )
        }

        // Cache to database
        val entities = messages.map { msg ->
          msg.toEntity(chatGuid, gson.toJson(msg.attachments))
        }
        messageDao.insertMessages(entities)

        Result.success(messages)
      } else {
        Result.failure(Exception("Failed to fetch messages: ${response.message()}"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  // ==================== ATTACHMENTS ====================

  suspend fun uploadAttachment(attachment: SelectedAttachment): Result<String> {
    return withContext(Dispatchers.IO) {
      try {
        // Copy file from content URI to cache
        val inputStream = context.contentResolver.openInputStream(attachment.uri)
          ?: return@withContext Result.failure(Exception("Cannot open file"))

        val tempFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}_${attachment.fileName ?: "file"}")
        tempFile.outputStream().use { output ->
          inputStream.copyTo(output)
        }
        inputStream.close()

        val mimeType = attachment.mimeType ?: "application/octet-stream"
        val requestBody = tempFile.asRequestBody(mimeType.toMediaType())
        val multipartBody = MultipartBody.Part.createFormData(
          "attachment",
          attachment.fileName ?: tempFile.name,
          requestBody
        )

        val response = api.uploadAttachment(multipartBody)

        // Clean up temp file
        tempFile.delete()

        if (response.isSuccessful && response.body()?.data != null) {
          Result.success(response.body()!!.data!!.guid)
        } else {
          Result.failure(Exception("Failed to upload attachment: ${response.message()}"))
        }
      } catch (e: Exception) {
        Result.failure(e)
      }
    }
  }

  // ==================== SEND MESSAGE ====================

  suspend fun sendMessage(
    chatGuid: String,
    text: String?,
    replyToGuid: String? = null,
    attachmentGuids: List<String>? = null
  ): Result<Message> {
    val tempGuid = java.util.UUID.randomUUID().toString()

    // Create optimistic message
    val optimisticMessage = Message(
      guid = tempGuid,
      text = text,
      dateCreated = Date(),
      isFromMe = true,
      error = 0
    )

    // Insert optimistic message to cache
    val entity = optimisticMessage.toEntity(chatGuid).copy(isSending = true, tempGuid = tempGuid)
    messageDao.insertMessage(entity)

    return try {
      val request = SendMessageRequest(
        chatGuid = chatGuid,
        message = text ?: "",
        tempGuid = tempGuid,
        replyToGuid = replyToGuid,
        selectedMessageGuid = attachmentGuids?.joinToString(",")
      )
      val response = api.sendTextMessage(request)
      if (response.isSuccessful && response.body()?.data != null) {
        val msgDto = response.body()!!.data!!

        // Update cache with real guid
        messageDao.confirmSent(tempGuid, msgDto.guid)

        Result.success(
          Message(
            guid = msgDto.guid,
            text = msgDto.text,
            dateCreated = Date(msgDto.dateCreated),
            isFromMe = true,
            attachments = msgDto.attachments?.map { att ->
              Attachment(
                guid = att.guid,
                mimeType = att.mimeType,
                fileName = att.transferName,
                filePath = null,
                width = att.width,
                height = att.height,
                totalBytes = att.totalBytes,
                isSticker = att.isSticker ?: false
              )
            } ?: emptyList(),
            error = msgDto.error ?: 0
          )
        )
      } else {
        messageDao.setError(tempGuid, 1)
        Result.failure(Exception("Failed to send message: ${response.message()}"))
      }
    } catch (e: Exception) {
      messageDao.setError(tempGuid, 1)
      Result.failure(e)
    }
  }

  // ==================== MARK READ ====================

  suspend fun markChatRead(chatGuid: String): Result<Unit> {
    // Update cache immediately
    conversationDao.markAsRead(chatGuid)

    return try {
      val response = api.markChatRead(chatGuid)
      if (response.isSuccessful) {
        Result.success(Unit)
      } else {
        Result.failure(Exception("Failed to mark chat as read"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  // ==================== SEARCH ====================

  fun searchConversations(query: String): Flow<List<Conversation>> {
    return conversationDao.searchConversations(query).map { entities ->
      entities.map { entity ->
        val participants = try {
          gson.fromJson(entity.participantsJson, Array<Participant>::class.java).toList()
        } catch (e: Exception) {
          emptyList()
        }
        entity.toConversation(participants)
      }
    }
  }

  fun searchMessages(query: String): Flow<List<Message>> {
    return messageDao.searchAllMessages(query).map { entities ->
      entities.map { it.toMessage() }
    }
  }

  // ==================== CACHE MANAGEMENT ====================

  suspend fun clearAllCache() {
    conversationDao.deleteAllConversations()
    messageDao.deleteAllMessages()
  }

  suspend fun deleteConversation(guid: String) {
    conversationDao.deleteConversationByGuid(guid)
    messageDao.deleteMessagesForChat(guid)
  }
}
