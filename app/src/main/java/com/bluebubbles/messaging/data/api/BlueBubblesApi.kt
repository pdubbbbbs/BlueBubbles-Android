package com.bluebubbles.messaging.data.api

import com.bluebubbles.messaging.data.api.dto.*
import retrofit2.Response
import retrofit2.http.*

interface BlueBubblesApi {

  // Server endpoints
  @GET("api/v1/server/info")
  suspend fun getServerInfo(): Response<ServerInfoResponse>

  @GET("api/v1/ping")
  suspend fun ping(): Response<PingResponse>

  // Chat endpoints
  @GET("api/v1/chat")
  suspend fun getChats(
    @Query("offset") offset: Int = 0,
    @Query("limit") limit: Int = 25,
    @Query("with") with: String = "lastMessage,participants"
  ): Response<ChatsResponse>

  @GET("api/v1/chat/{guid}")
  suspend fun getChat(
    @Path("guid") guid: String
  ): Response<ChatResponse>

  @POST("api/v1/chat/{guid}/read")
  suspend fun markChatRead(
    @Path("guid") guid: String
  ): Response<BasicResponse>

  // Message endpoints
  @GET("api/v1/chat/{guid}/message")
  suspend fun getChatMessages(
    @Path("guid") chatGuid: String,
    @Query("offset") offset: Int = 0,
    @Query("limit") limit: Int = 50,
    @Query("sort") sort: String = "DESC",
    @Query("with") with: String = "attachment,handle"
  ): Response<MessagesResponse>

  @POST("api/v1/message/text")
  suspend fun sendTextMessage(
    @Body request: SendMessageRequest
  ): Response<SendMessageResponse>

  @POST("api/v1/message/react")
  suspend fun sendReaction(
    @Body request: ReactionRequest
  ): Response<BasicResponse>

  // Attachment endpoints
  @GET("api/v1/attachment/{guid}/download")
  suspend fun downloadAttachment(
    @Path("guid") guid: String
  ): Response<ByteArray>

  @Multipart
  @POST("api/v1/message/attachment")
  suspend fun sendAttachment(
    @Part file: okhttp3.MultipartBody.Part,
    @Part("chatGuid") chatGuid: String
  ): Response<SendMessageResponse>

  // Handle endpoints
  @GET("api/v1/handle")
  suspend fun getHandles(
    @Query("offset") offset: Int = 0,
    @Query("limit") limit: Int = 100
  ): Response<HandlesResponse>

  // FCM Registration
  @POST("api/v1/fcm/register")
  suspend fun registerFcmDevice(
    @Body request: FcmRegisterRequest
  ): Response<BasicResponse>
}

// Request/Response DTOs
data class SendMessageRequest(
  val chatGuid: String,
  val message: String,
  val method: String = "private-api",
  val tempGuid: String? = null,
  val subject: String? = null,
  val effectId: String? = null,
  val replyToGuid: String? = null
)

data class ReactionRequest(
  val chatGuid: String,
  val messageGuid: String,
  val reaction: Int,  // Tapback type
  val partIndex: Int = 0
)

data class FcmRegisterRequest(
  val deviceId: String,
  val deviceName: String,
  val token: String
)
