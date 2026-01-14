package com.bluebubbles.messaging.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluebubbles.messaging.data.repository.ChatRepository
import com.bluebubbles.messaging.ui.screens.search.SearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
  val query: String = "",
  val results: List<SearchResult> = emptyList(),
  val isLoading: Boolean = false,
  val hasSearched: Boolean = false
)

@HiltViewModel
class SearchViewModel @Inject constructor(
  private val chatRepository: ChatRepository
) : ViewModel() {

  private val _uiState = MutableStateFlow(SearchUiState())
  val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

  private var searchJob: Job? = null

  fun updateQuery(query: String) {
    _uiState.value = _uiState.value.copy(query = query)

    // Debounced search
    searchJob?.cancel()
    if (query.length >= 2) {
      searchJob = viewModelScope.launch {
        delay(300) // Debounce
        performSearch(query)
      }
    } else {
      _uiState.value = _uiState.value.copy(
        results = emptyList(),
        hasSearched = false
      )
    }
  }

  fun search() {
    val query = _uiState.value.query
    if (query.length >= 2) {
      viewModelScope.launch {
        performSearch(query)
      }
    }
  }

  private suspend fun performSearch(query: String) {
    _uiState.value = _uiState.value.copy(isLoading = true)

    try {
      // Get all conversations for context
      val conversationsResult = chatRepository.getConversations()
      val conversationsMap = conversationsResult.getOrNull()
        ?.associateBy { it.guid } ?: emptyMap()

      // Search messages
      val messages = chatRepository.searchMessages(query).first()

      val results = messages.mapNotNull { message ->
        val text = message.text ?: return@mapNotNull null

        // Find associated conversation
        // Since we don't have direct access to chatGuid in Message,
        // we need to get it from the cached entity
        // For now, we'll use a workaround by searching in conversations

        val conversation = conversationsMap.values.find { conv ->
          conv.lastMessage?.guid == message.guid ||
            conv.participants.any { p -> p.address == message.handle?.address }
        }

        if (conversation != null) {
          SearchResult(
            messageGuid = message.guid,
            chatGuid = conversation.guid,
            messageText = text,
            senderName = if (message.isFromMe) {
              "You"
            } else {
              message.handle?.displayName ?: message.handle?.address ?: "Unknown"
            },
            senderInitials = if (message.isFromMe) {
              "Me"
            } else {
              message.handle?.initials ?: "?"
            },
            conversationName = conversation.title,
            date = message.dateCreated,
            isFromMe = message.isFromMe
          )
        } else {
          // If we can't find the conversation, still show the result
          SearchResult(
            messageGuid = message.guid,
            chatGuid = "", // Will be resolved when clicked
            messageText = text,
            senderName = if (message.isFromMe) "You" else message.handle?.displayName ?: "Unknown",
            senderInitials = if (message.isFromMe) "Me" else message.handle?.initials ?: "?",
            conversationName = message.handle?.displayName ?: "Unknown",
            date = message.dateCreated,
            isFromMe = message.isFromMe
          )
        }
      }.sortedByDescending { it.date }

      _uiState.value = _uiState.value.copy(
        results = results,
        isLoading = false,
        hasSearched = true
      )
    } catch (e: Exception) {
      _uiState.value = _uiState.value.copy(
        isLoading = false,
        hasSearched = true,
        results = emptyList()
      )
    }
  }
}
