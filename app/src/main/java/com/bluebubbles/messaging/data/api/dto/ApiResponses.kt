package com.bluebubbles.messaging.data.api.dto

import com.google.gson.annotations.SerializedName

// Base response wrapper
data class ApiResponse<T>(
  val status: Int,
  val message: String,
  val data: T?
)

data class BasicResponse(
  val status: Int,
  val message: String
)

data class PingResponse(
  val status: Int,
  val message: String,
  val data: PingData?
)

data class PingData(
  val pong: Boolean
)

// Server Info
data class ServerInfoResponse(
  val status: Int,
  val message: String,
  val data: ServerInfoData?
)

data class ServerInfoData(
  @SerializedName("os_version") val osVersion: String,
  @SerializedName("server_version") val serverVersion: String,
  @SerializedName("private_api") val privateApiEnabled: Boolean,
  @SerializedName("proxy_service") val proxyService: String?,
  @SerializedName("helper_connected") val helperConnected: Boolean,
  @SerializedName("detected_icloud") val detectedIcloud: String?
)

// Chats
data class ChatsResponse(
  val status: Int,
  val message: String,
  val data: List<ChatDto>?,
  val metadata: MetadataDto?
)

data class ChatResponse(
  val status: Int,
  val message: String,
  val data: ChatDto?
)

data class ChatDto(
  val guid: String,
  @SerializedName("chat_identifier") val chatIdentifier: String,
  @SerializedName("display_name") val displayName: String?,
  @SerializedName("is_archived") val isArchived: Boolean,
  @SerializedName("is_filtered") val isFiltered: Boolean,
  val participants: List<HandleDto>?,
  @SerializedName("last_message") val lastMessage: MessageDto?
)

// Messages
data class MessagesResponse(
  val status: Int,
  val message: String,
  val data: List<MessageDto>?,
  val metadata: MetadataDto?
)

data class SendMessageResponse(
  val status: Int,
  val message: String,
  val data: MessageDto?
)

data class MessageDto(
  val guid: String,
  val text: String?,
  val subject: String?,
  @SerializedName("date_created") val dateCreated: Long,
  @SerializedName("date_delivered") val dateDelivered: Long?,
  @SerializedName("date_read") val dateRead: Long?,
  @SerializedName("is_from_me") val isFromMe: Boolean,
  val handle: HandleDto?,
  val attachments: List<AttachmentDto>?,
  @SerializedName("associated_message_guid") val associatedMessageGuid: String?,
  @SerializedName("associated_message_type") val associatedMessageType: Int?,
  val error: Int?
)

data class AttachmentDto(
  val guid: String,
  @SerializedName("mime_type") val mimeType: String?,
  @SerializedName("transfer_name") val transferName: String?,
  @SerializedName("total_bytes") val totalBytes: Long?,
  val width: Int?,
  val height: Int?,
  @SerializedName("is_sticker") val isSticker: Boolean?
)

// Handles
data class HandlesResponse(
  val status: Int,
  val message: String,
  val data: List<HandleDto>?,
  val metadata: MetadataDto?
)

data class HandleDto(
  val address: String,
  val service: String,
  @SerializedName("uncanonical_id") val uncanonicalId: String?,
  @SerializedName("first_name") val firstName: String?,
  @SerializedName("last_name") val lastName: String?,
  @SerializedName("display_name") val displayName: String?
)

// Metadata for pagination
data class MetadataDto(
  val offset: Int,
  val limit: Int,
  val total: Int
)
