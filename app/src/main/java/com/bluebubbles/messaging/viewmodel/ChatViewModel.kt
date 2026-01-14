package com.bluebubbles.messaging.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluebubbles.messaging.data.models.Conversation
import com.bluebubbles.messaging.data.models.Message
import com.bluebubbles.messaging.data.models.ReactionType
import com.bluebubbles.messaging.data.repository.ChatRepository
import com.bluebubbles.messaging.data.repository.ServerRepository
import com.bluebubbles.messaging.data.socket.SocketEventHandler
import com.bluebubbles.messaging.data.socket.SocketManager
import com.bluebubbles.messaging.ui.components.SelectedAttachment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
  val conversation: Conversation? = null,
  val messages: List<Message> = emptyList(),
  val isLoading: Boolean = false,
  val isLoadingMore: Boolean = false,
  val isSending: Boolean = false,
  val error: String? = null,
  val messageText: String = "",
  val replyToMessage: Message? = null,
  val hasMoreMessages: Boolean = true,
  val serverUrl: String? = null,
  val serverPassword: String? = null,
  val isOtherTyping: Boolean = false,
  val typingParticipant: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
  private val chatRepository: ChatRepository,
  private val serverRepository: ServerRepository,
  private val socketManager: SocketManager,
  private val socketEventHandler: SocketEventHandler,
  savedStateHandle: SavedStateHandle
) : ViewModel() {

  private val chatGuid: String = savedStateHandle.get<String>("chatGuid") ?: ""

  private val _uiState = MutableStateFlow(ChatUiState())
  val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

  private var currentOffset = 0
  private val pageSize = 50
  private var typingJob: Job? = null
  private var isCurrentlyTyping = false

  init {
    loadServerConfig()
    observeTypingIndicators()
    if (chatGuid.isNotEmpty()) {
      loadMessages()
    }
  }

  private fun loadServerConfig() {
    viewModelScope.launch {
      val config = serverRepository.getServerConfig().first()
      config?.let {
        _uiState.value = _uiState.value.copy(
          serverUrl = it.serverUrl,
          serverPassword = it.password
        )
      }
    }
  }

  private fun observeTypingIndicators() {
    socketEventHandler.typingIndicators
      .filter { it.chatGuid == chatGuid }
      .onEach { indicator ->
        _uiState.value = _uiState.value.copy(
          isOtherTyping = indicator.isTyping,
          typingParticipant = indicator.senderAddress
        )

        // Auto-clear typing indicator after 5 seconds
        if (indicator.isTyping) {
          viewModelScope.launch {
            delay(5000)
            _uiState.value = _uiState.value.copy(
              isOtherTyping = false,
              typingParticipant = null
            )
          }
        }
      }
      .launchIn(viewModelScope)
  }

  fun loadMessages() {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true, error = null)
      currentOffset = 0

      chatRepository.getMessages(chatGuid, 0, pageSize)
        .onSuccess { messages ->
          _uiState.value = _uiState.value.copy(
            messages = messages,
            isLoading = false,
            hasMoreMessages = messages.size == pageSize
          )
          currentOffset = messages.size

          // Mark as read via API and WebSocket
          markChatAsRead(messages.firstOrNull()?.guid)
        }
        .onFailure { error ->
          _uiState.value = _uiState.value.copy(
            isLoading = false,
            error = error.message ?: "Failed to load messages"
          )
        }
    }
  }

  private fun markChatAsRead(lastMessageGuid: String?) {
    viewModelScope.launch {
      chatRepository.markChatRead(chatGuid)
      // Also send via WebSocket for real-time sync
      lastMessageGuid?.let { guid ->
        socketManager.sendReadReceipt(chatGuid, guid)
      }
    }
  }

  fun onMessageVisible(messageGuid: String) {
    // Send read receipt for visible messages from others
    val message = _uiState.value.messages.find { it.guid == messageGuid }
    if (message != null && !message.isFromMe && message.dateRead == null) {
      socketManager.sendReadReceipt(chatGuid, messageGuid)
    }
  }

  fun loadMoreMessages() {
    if (_uiState.value.isLoadingMore || !_uiState.value.hasMoreMessages) return

    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoadingMore = true)

      chatRepository.getMessages(chatGuid, currentOffset, pageSize)
        .onSuccess { newMessages ->
          _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + newMessages,
            isLoadingMore = false,
            hasMoreMessages = newMessages.size == pageSize
          )
          currentOffset += newMessages.size
        }
        .onFailure {
          _uiState.value = _uiState.value.copy(isLoadingMore = false)
        }
    }
  }

  fun updateMessageText(text: String) {
    _uiState.value = _uiState.value.copy(messageText = text)

    // Send typing indicator
    if (text.isNotEmpty() && !isCurrentlyTyping) {
      isCurrentlyTyping = true
      socketManager.sendTypingIndicator(chatGuid, true)
    }

    // Reset typing timeout
    typingJob?.cancel()
    typingJob = viewModelScope.launch {
      delay(3000) // Stop typing after 3 seconds of no input
      if (isCurrentlyTyping) {
        isCurrentlyTyping = false
        socketManager.sendTypingIndicator(chatGuid, false)
      }
    }

    // Clear typing when text is empty
    if (text.isEmpty() && isCurrentlyTyping) {
      isCurrentlyTyping = false
      socketManager.sendTypingIndicator(chatGuid, false)
    }
  }

  fun sendMessage(attachments: List<SelectedAttachment> = emptyList()) {
    val text = _uiState.value.messageText.trim()
    if (text.isEmpty() && attachments.isEmpty()) return
    if (_uiState.value.isSending) return

    // Stop typing indicator
    typingJob?.cancel()
    if (isCurrentlyTyping) {
      isCurrentlyTyping = false
      socketManager.sendTypingIndicator(chatGuid, false)
    }

    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isSending = true, messageText = "")

      // Upload attachments first if any
      val attachmentGuids = mutableListOf<String>()
      for (attachment in attachments) {
        chatRepository.uploadAttachment(attachment)
          .onSuccess { guid ->
            attachmentGuids.add(guid)
          }
          .onFailure { error ->
            _uiState.value = _uiState.value.copy(
              isSending = false,
              messageText = text,
              error = "Failed to upload attachment: ${error.message}"
            )
            return@launch
          }
      }

      // Send message with attachments
      chatRepository.sendMessage(
        chatGuid = chatGuid,
        text = text.ifEmpty { null },
        replyToGuid = _uiState.value.replyToMessage?.guid,
        attachmentGuids = attachmentGuids.ifEmpty { null }
      )
        .onSuccess { message ->
          _uiState.value = _uiState.value.copy(
            messages = listOf(message) + _uiState.value.messages,
            isSending = false,
            replyToMessage = null
          )
        }
        .onFailure { error ->
          _uiState.value = _uiState.value.copy(
            isSending = false,
            messageText = text,
            error = error.message ?: "Failed to send message"
          )
        }
    }
  }

  fun setReplyToMessage(message: Message?) {
    _uiState.value = _uiState.value.copy(replyToMessage = message)
  }

  fun clearReply() {
    _uiState.value = _uiState.value.copy(replyToMessage = null)
  }

  fun clearError() {
    _uiState.value = _uiState.value.copy(error = null)
  }

  fun sendReaction(messageGuid: String, reactionType: ReactionType) {
    viewModelScope.launch {
      chatRepository.sendReaction(chatGuid, messageGuid, reactionType)
        .onSuccess {
          // Refresh messages to show the new reaction
          loadMessages()
        }
        .onFailure { error ->
          _uiState.value = _uiState.value.copy(
            error = "Failed to send reaction: ${error.message}"
          )
        }
    }
  }
}
