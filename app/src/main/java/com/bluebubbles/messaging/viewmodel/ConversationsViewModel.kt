package com.bluebubbles.messaging.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluebubbles.messaging.data.models.ConnectionState
import com.bluebubbles.messaging.data.models.Conversation
import com.bluebubbles.messaging.data.models.Participant
import com.bluebubbles.messaging.data.repository.ChatRepository
import com.bluebubbles.messaging.data.repository.ContactRepository
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
  val searchQuery: String = "",
  val contactPhotos: Map<String, Uri> = emptyMap() // address -> photo URI
)

@HiltViewModel
class ConversationsViewModel @Inject constructor(
  private val chatRepository: ChatRepository,
  private val serverRepository: ServerRepository,
  private val contactRepository: ContactRepository
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
          // Enrich with contact information
          val enrichedConversations = enrichConversationsWithContacts(conversations)
          _uiState.value = _uiState.value.copy(
            conversations = enrichedConversations,
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

  private suspend fun enrichConversationsWithContacts(conversations: List<Conversation>): List<Conversation> {
    val contactPhotos = mutableMapOf<String, Uri>()

    return conversations.map { conversation ->
      val enrichedParticipants = conversation.participants.map { participant ->
        // Try to find contact info
        val contactInfo = contactRepository.findContactByAddress(participant.address)

        if (contactInfo != null) {
          // Cache photo URI
          contactInfo.photoUri?.let { contactPhotos[participant.address] = it }

          // Update participant with contact name if available
          participant.copy(
            displayName = contactInfo.displayName.ifEmpty { participant.displayName },
            firstName = contactInfo.displayName.split(" ").firstOrNull(),
            lastName = contactInfo.displayName.split(" ").drop(1).joinToString(" ").takeIf { it.isNotEmpty() },
            avatarUrl = contactInfo.photoUri?.toString()
          )
        } else {
          participant
        }
      }

      conversation.copy(participants = enrichedParticipants)
    }.also {
      // Update contact photos map in state
      _uiState.value = _uiState.value.copy(contactPhotos = contactPhotos)
    }
  }

  fun refresh() {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isRefreshing = true)

      // Clear contact cache for fresh data
      contactRepository.clearCache()

      chatRepository.getConversations(forceRefresh = true)
        .onSuccess { conversations ->
          val enrichedConversations = enrichConversationsWithContacts(conversations)
          _uiState.value = _uiState.value.copy(
            conversations = enrichedConversations,
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
