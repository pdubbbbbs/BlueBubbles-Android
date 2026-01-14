package com.bluebubbles.messaging.ui.screens.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bluebubbles.messaging.ui.theme.*
import com.bluebubbles.messaging.viewmodel.SearchViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
  onBackClick: () -> Unit,
  onMessageClick: (chatGuid: String, messageGuid: String) -> Unit,
  viewModel: SearchViewModel = hiltViewModel()
) {
  val uiState by viewModel.uiState.collectAsState()
  val focusRequester = remember { FocusRequester() }
  val focusManager = LocalFocusManager.current

  LaunchedEffect(Unit) {
    focusRequester.requestFocus()
  }

  Scaffold(
    topBar = {
      TopAppBar(
        navigationIcon = {
          IconButton(onClick = onBackClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
          }
        },
        title = {
          OutlinedTextField(
            value = uiState.query,
            onValueChange = viewModel::updateQuery,
            modifier = Modifier
              .fillMaxWidth()
              .focusRequester(focusRequester),
            placeholder = {
              Text("Search messages...", color = TextMuted)
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = CyanPrimary,
              unfocusedBorderColor = PurpleDark,
              cursorColor = CyanPrimary,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary,
              focusedContainerColor = CardBackground,
              unfocusedContainerColor = CardBackground
            ),
            shape = RoundedCornerShape(24.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
              onSearch = {
                viewModel.search()
                focusManager.clearFocus()
              }
            ),
            trailingIcon = {
              if (uiState.query.isNotEmpty()) {
                IconButton(onClick = { viewModel.updateQuery("") }) {
                  Icon(Icons.Default.Close, "Clear", tint = TextMuted)
                }
              }
            }
          )
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = BackgroundDark
        )
      )
    },
    containerColor = BackgroundDark
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .background(
          Brush.verticalGradient(
            colors = listOf(BackgroundDark, BackgroundPurple, BackgroundDark)
          )
        )
    ) {
      when {
        uiState.isLoading -> {
          CircularProgressIndicator(
            modifier = Modifier.align(Alignment.Center),
            color = CyanPrimary
          )
        }
        uiState.query.isEmpty() -> {
          EmptySearchState()
        }
        uiState.results.isEmpty() && uiState.hasSearched -> {
          NoResultsState(query = uiState.query)
        }
        else -> {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            item {
              Text(
                text = "${uiState.results.size} result${if (uiState.results.size != 1) "s" else ""}",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
                modifier = Modifier.padding(bottom = 8.dp)
              )
            }

            items(
              items = uiState.results,
              key = { it.messageGuid }
            ) { result ->
              SearchResultItem(
                result = result,
                query = uiState.query,
                onClick = { onMessageClick(result.chatGuid, result.messageGuid) }
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun EmptySearchState() {
  Column(
    modifier = Modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Icon(
      Icons.Default.Search,
      contentDescription = null,
      modifier = Modifier.size(64.dp),
      tint = TextMuted
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = "Search your messages",
      style = MaterialTheme.typography.titleMedium,
      color = TextSecondary
    )
    Text(
      text = "Find messages by typing keywords",
      style = MaterialTheme.typography.bodyMedium,
      color = TextMuted
    )
  }
}

@Composable
private fun NoResultsState(query: String) {
  Column(
    modifier = Modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Icon(
      Icons.Default.SearchOff,
      contentDescription = null,
      modifier = Modifier.size(64.dp),
      tint = TextMuted
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = "No results found",
      style = MaterialTheme.typography.titleMedium,
      color = TextSecondary
    )
    Text(
      text = "No messages matching \"$query\"",
      style = MaterialTheme.typography.bodyMedium,
      color = TextMuted
    )
  }
}

@Composable
private fun SearchResultItem(
  result: SearchResult,
  query: String,
  onClick: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick),
    colors = CardDefaults.cardColors(
      containerColor = CardBackground
    ),
    shape = RoundedCornerShape(16.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.Top
    ) {
      // Avatar
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(
            Brush.linearGradient(listOf(CyanPrimary, PurplePrimary))
          ),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = result.senderInitials,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = BackgroundDark
        )
      }

      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = result.senderName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
          )
          Text(
            text = formatDate(result.date),
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
          )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Highlighted text with query
        HighlightedText(
          text = result.messageText,
          query = query
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = result.conversationName,
          style = MaterialTheme.typography.labelSmall,
          color = CyanPrimary
        )
      }
    }
  }
}

@Composable
private fun HighlightedText(text: String, query: String) {
  val annotatedString = buildAnnotatedString {
    val lowercaseText = text.lowercase()
    val lowercaseQuery = query.lowercase()

    var startIndex = 0
    var index = lowercaseText.indexOf(lowercaseQuery)

    while (index >= 0) {
      // Append text before match
      withStyle(SpanStyle(color = TextSecondary)) {
        append(text.substring(startIndex, index))
      }
      // Append highlighted match
      withStyle(SpanStyle(color = CyanPrimary, fontWeight = FontWeight.Bold)) {
        append(text.substring(index, index + query.length))
      }
      startIndex = index + query.length
      index = lowercaseText.indexOf(lowercaseQuery, startIndex)
    }

    // Append remaining text
    if (startIndex < text.length) {
      withStyle(SpanStyle(color = TextSecondary)) {
        append(text.substring(startIndex))
      }
    }
  }

  Text(
    text = annotatedString,
    style = MaterialTheme.typography.bodyMedium,
    maxLines = 2,
    overflow = TextOverflow.Ellipsis
  )
}

private fun formatDate(date: Date): String {
  val now = Calendar.getInstance()
  val messageDate = Calendar.getInstance().apply { time = date }

  return when {
    now.get(Calendar.DATE) == messageDate.get(Calendar.DATE) &&
      now.get(Calendar.YEAR) == messageDate.get(Calendar.YEAR) -> {
      SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
    }
    now.get(Calendar.WEEK_OF_YEAR) == messageDate.get(Calendar.WEEK_OF_YEAR) &&
      now.get(Calendar.YEAR) == messageDate.get(Calendar.YEAR) -> {
      SimpleDateFormat("EEE", Locale.getDefault()).format(date)
    }
    now.get(Calendar.YEAR) == messageDate.get(Calendar.YEAR) -> {
      SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
    }
    else -> {
      SimpleDateFormat("M/d/yy", Locale.getDefault()).format(date)
    }
  }
}

data class SearchResult(
  val messageGuid: String,
  val chatGuid: String,
  val messageText: String,
  val senderName: String,
  val senderInitials: String,
  val conversationName: String,
  val date: Date,
  val isFromMe: Boolean
)
