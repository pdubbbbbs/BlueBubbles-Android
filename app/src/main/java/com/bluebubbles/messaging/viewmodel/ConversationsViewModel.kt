package com.bluebubbles.messaging.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluebubbles.messaging.data.models.ConnectionState
import com.bluebubbles.messaging.data.models.Conversation
import com.bluebubbles.messaging.data.repository.ChatRepository
import com.bluebubbles.messaging.data.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConversationsUiState(
  val conversations: List<Conversation> = emptyList(),
  val isLoading: Boolean = false,
  val isRefreshing: Boolean = false,
  val error: String? = null,
  val connectionState: ConnectionState = ConnectionState.Disconnected,
  val searchQuery: String = ""
)

@HiltViewModel
class ConversationsViewModel @Inject constructor(
  private val chatRepository: ChatRepository,
  private val serverRepository: ServerRepository
) : ViewModel() {

  private val _uiState = MutableStateFlow(ConversationsUiState())
  val uiState: StateFlow<ConversationsUiState> = _uiState.asStateFlow()

  init {
    checkConnection()
    loadConversations()
  }

  fun loadConversations() {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true, error = null)

      chatRepository.getConversations()
        .onSuccess { conversations ->
          _uiState.value = _uiState.value.copy(
            conversations = conversations,
            isLoading = false
          )
        }
        .onFailure { error ->
          _uiState.value = _uiState.value.copy(
            isLoading = false,
            error = error.message ?: "Failed to load conversations"
          )
        }
    }
  }

  fun refresh() {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isRefreshing = true)

      chatRepository.getConversations()
        .onSuccess { conversations ->
          _uiState.value = _uiState.value.copy(
            conversations = conversations,
            isRefreshing = false
          )
        }
        .onFailure {
          _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }
  }

  fun updateSearchQuery(query: String) {
    _uiState.value = _uiState.value.copy(searchQuery = query)
  }

  fun clearError() {
    _uiState.value = _uiState.value.copy(error = null)
  }

  private fun checkConnection() {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(connectionState = ConnectionState.Connecting)

      serverRepository.ping()
        .onSuccess {
          _uiState.value = _uiState.value.copy(connectionState = ConnectionState.Connected)
        }
        .onFailure { error ->
          _uiState.value = _uiState.value.copy(
            connectionState = ConnectionState.Error(error.message ?: "Connection failed")
          )
        }
    }
  }

  val filteredConversations: List<Conversation>
    get() {
      val query = _uiState.value.searchQuery.lowercase()
      return if (query.isEmpty()) {
        _uiState.value.conversations
      } else {
        _uiState.value.conversations.filter { conv ->
          conv.title.lowercase().contains(query) ||
            conv.participants.any { it.displayName.lowercase().contains(query) } ||
            conv.lastMessage?.text?.lowercase()?.contains(query) == true
        }
      }
    }
}
