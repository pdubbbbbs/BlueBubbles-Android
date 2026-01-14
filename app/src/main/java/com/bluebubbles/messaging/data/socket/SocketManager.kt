package com.bluebubbles.messaging.data.socket

import android.util.Log
import com.bluebubbles.messaging.data.models.ConnectionState
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketManager @Inject constructor() {

  companion object {
    private const val TAG = "SocketManager"
    private const val RECONNECT_DELAY_MS = 5000L
    private const val MAX_RECONNECT_ATTEMPTS = 10
  }

  private var webSocket: WebSocket? = null
  private var client: OkHttpClient? = null
  private var serverUrl: String = ""
  private var password: String = ""
  private var reconnectAttempts = 0
  private var shouldReconnect = true

  private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
  private val gson = Gson()

  private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
  val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

  private val _events = MutableSharedFlow<SocketEvent>(replay = 0, extraBufferCapacity = 64)
  val events: SharedFlow<SocketEvent> = _events.asSharedFlow()

  fun connect(baseUrl: String, serverPassword: String) {
    serverUrl = baseUrl.replace("http", "ws")
    password = serverPassword
    shouldReconnect = true
    reconnectAttempts = 0

    doConnect()
  }

  private fun doConnect() {
    if (_connectionState.value is ConnectionState.Connected) {
      Log.d(TAG, "Already connected")
      return
    }

    _connectionState.value = ConnectionState.Connecting

    client = OkHttpClient.Builder()
      .pingInterval(30, TimeUnit.SECONDS)
      .readTimeout(0, TimeUnit.MILLISECONDS)
      .build()

    val request = Request.Builder()
      .url("$serverUrl/socket.io/?EIO=4&transport=websocket")
      .addHeader("password", password)
      .build()

    webSocket = client?.newWebSocket(request, createWebSocketListener())
  }

  fun disconnect() {
    shouldReconnect = false
    webSocket?.close(1000, "Client disconnect")
    webSocket = null
    client?.dispatcher?.executorService?.shutdown()
    client = null
    _connectionState.value = ConnectionState.Disconnected
  }

  fun sendMessage(event: String, data: Any) {
    val payload = gson.toJson(listOf(event, data))
    webSocket?.send("42$payload")
  }

  fun sendTypingIndicator(chatGuid: String, isTyping: Boolean) {
    val data = mapOf(
      "chatGuid" to chatGuid,
      "display" to isTyping
    )
    sendMessage("typing-indicator", data)
  }

  fun sendReadReceipt(chatGuid: String, messageGuid: String) {
    val data = mapOf(
      "chatGuid" to chatGuid,
      "messageGuid" to messageGuid
    )
    sendMessage("mark-chat-read", data)
  }

  private fun createWebSocketListener() = object : WebSocketListener() {

    override fun onOpen(webSocket: WebSocket, response: Response) {
      Log.d(TAG, "WebSocket connected")
      reconnectAttempts = 0
      _connectionState.value = ConnectionState.Connected

      // Socket.IO handshake
      webSocket.send("40")

      scope.launch {
        _events.emit(SocketEvent.Connected)
      }
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
      Log.d(TAG, "Message received: $text")
      handleMessage(text)
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
      Log.d(TAG, "Binary message received")
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
      Log.d(TAG, "WebSocket closing: $code $reason")
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
      Log.d(TAG, "WebSocket closed: $code $reason")
      _connectionState.value = ConnectionState.Disconnected

      scope.launch {
        _events.emit(SocketEvent.Disconnected(reason))
      }

      attemptReconnect()
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
      Log.e(TAG, "WebSocket failure", t)
      _connectionState.value = ConnectionState.Error(t.message ?: "Connection failed")

      scope.launch {
        _events.emit(SocketEvent.Error(t.message ?: "Unknown error"))
      }

      attemptReconnect()
    }
  }

  private fun handleMessage(text: String) {
    scope.launch {
      try {
        when {
          // Socket.IO ping
          text == "2" -> webSocket?.send("3")

          // Socket.IO connected
          text.startsWith("40") -> {
            Log.d(TAG, "Socket.IO handshake complete")
          }

          // Socket.IO event message
          text.startsWith("42") -> {
            val payload = text.substring(2)
            parseAndEmitEvent(payload)
          }
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error handling message", e)
      }
    }
  }

  private suspend fun parseAndEmitEvent(payload: String) {
    try {
      val array = gson.fromJson(payload, com.google.gson.JsonArray::class.java)
      if (array.size() >= 2) {
        val eventName = array[0].asString
        val eventData = array[1]

        when (eventName) {
          "new-message" -> {
            val data = gson.fromJson(eventData, JsonObject::class.java)
            _events.emit(SocketEvent.NewMessage(data))
          }
          "updated-message" -> {
            val data = gson.fromJson(eventData, JsonObject::class.java)
            _events.emit(SocketEvent.UpdatedMessage(data))
          }
          "typing-indicator" -> {
            val data = gson.fromJson(eventData, JsonObject::class.java)
            _events.emit(SocketEvent.TypingIndicator(data))
          }
          "group-name-change" -> {
            val data = gson.fromJson(eventData, JsonObject::class.java)
            _events.emit(SocketEvent.GroupNameChange(data))
          }
          "participant-added" -> {
            val data = gson.fromJson(eventData, JsonObject::class.java)
            _events.emit(SocketEvent.ParticipantAdded(data))
          }
          "participant-removed" -> {
            val data = gson.fromJson(eventData, JsonObject::class.java)
            _events.emit(SocketEvent.ParticipantRemoved(data))
          }
          "chat-read-status-changed" -> {
            val data = gson.fromJson(eventData, JsonObject::class.java)
            _events.emit(SocketEvent.ChatReadStatusChanged(data))
          }
          else -> {
            Log.d(TAG, "Unknown event: $eventName")
          }
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error parsing event", e)
    }
  }

  private fun attemptReconnect() {
    if (!shouldReconnect) return
    if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
      Log.e(TAG, "Max reconnect attempts reached")
      _connectionState.value = ConnectionState.Error("Max reconnection attempts reached")
      return
    }

    reconnectAttempts++
    Log.d(TAG, "Attempting reconnect ($reconnectAttempts/$MAX_RECONNECT_ATTEMPTS)")

    scope.launch {
      delay(RECONNECT_DELAY_MS)
      if (shouldReconnect) {
        doConnect()
      }
    }
  }
}

sealed class SocketEvent {
  object Connected : SocketEvent()
  data class Disconnected(val reason: String) : SocketEvent()
  data class Error(val message: String) : SocketEvent()
  data class NewMessage(val data: JsonObject) : SocketEvent()
  data class UpdatedMessage(val data: JsonObject) : SocketEvent()
  data class TypingIndicator(val data: JsonObject) : SocketEvent()
  data class GroupNameChange(val data: JsonObject) : SocketEvent()
  data class ParticipantAdded(val data: JsonObject) : SocketEvent()
  data class ParticipantRemoved(val data: JsonObject) : SocketEvent()
  data class ChatReadStatusChanged(val data: JsonObject) : SocketEvent()
}
