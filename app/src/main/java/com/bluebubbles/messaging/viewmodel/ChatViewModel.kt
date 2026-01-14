package com.bluebubbles.messaging.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluebubbles.messaging.data.models.Conversation
import com.bluebubbles.messaging.data.models.Message
import com.bluebubbles.messaging.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
  val hasMoreMessages: Boolean = true
)

@HiltViewModel
class ChatViewModel @Inject constructor(
  private val chatRepository: ChatRepository,
  savedStateHandle: SavedStateHandle
) : ViewModel() {

  private val chatGuid: String = savedStateHandle.get<String>("chatGuid") ?: ""

  private val _uiState = MutableStateFlow(ChatUiState())
  val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

  private var currentOffset = 0
  private val pageSize = 50

  init {
    if (chatGuid.isNotEmpty()) {
      loadMessages()
    }
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

          // Mark as read
          chatRepository.markChatRead(chatGuid)
        }
        .onFailure { error ->
          _uiState.value = _uiState.value.copy(
            isLoading = false,
            error = error.message ?: "Failed to load messages"
          )
        }
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
  }

  fun sendMessage() {
    val text = _uiState.value.messageText.trim()
    if (text.isEmpty() || _uiState.value.isSending) return

    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isSending = true, messageText = "")

      chatRepository.sendMessage(
        chatGuid = chatGuid,
        text = text,
        replyToGuid = _uiState.value.replyToMessage?.guid
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
            messageText = text, // Restore text on failure
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
}
