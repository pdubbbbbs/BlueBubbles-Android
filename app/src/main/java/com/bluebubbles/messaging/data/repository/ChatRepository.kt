package com.bluebubbles.messaging.data.repository

import com.bluebubbles.messaging.data.api.BlueBubblesApi
import com.bluebubbles.messaging.data.api.SendMessageRequest
import com.bluebubbles.messaging.data.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
  private val api: BlueBubblesApi
) {

  suspend fun getConversations(offset: Int = 0, limit: Int = 25): Result<List<Conversation>> {
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
        Result.success(conversations)
      } else {
        Result.failure(Exception("Failed to fetch conversations: ${response.message()}"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun getMessages(chatGuid: String, offset: Int = 0, limit: Int = 50): Result<List<Message>> {
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
        Result.success(messages)
      } else {
        Result.failure(Exception("Failed to fetch messages: ${response.message()}"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun sendMessage(chatGuid: String, text: String, replyToGuid: String? = null): Result<Message> {
    return try {
      val request = SendMessageRequest(
        chatGuid = chatGuid,
        message = text,
        tempGuid = java.util.UUID.randomUUID().toString(),
        replyToGuid = replyToGuid
      )
      val response = api.sendTextMessage(request)
      if (response.isSuccessful && response.body()?.data != null) {
        val msgDto = response.body()!!.data!!
        Result.success(
          Message(
            guid = msgDto.guid,
            text = msgDto.text,
            dateCreated = Date(msgDto.dateCreated),
            isFromMe = true,
            error = msgDto.error ?: 0
          )
        )
      } else {
        Result.failure(Exception("Failed to send message: ${response.message()}"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun markChatRead(chatGuid: String): Result<Unit> {
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

  fun observeMessages(chatGuid: String): Flow<List<Message>> = flow {
    // Initial fetch
    val result = getMessages(chatGuid)
    if (result.isSuccess) {
      emit(result.getOrDefault(emptyList()))
    }
    // In production, would use WebSocket or polling here
  }
}
