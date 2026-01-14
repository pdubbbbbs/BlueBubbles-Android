package com.bluebubbles.messaging.ui.screens.conversations

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bluebubbles.messaging.data.models.ConnectionState
import com.bluebubbles.messaging.data.models.Conversation
import com.bluebubbles.messaging.ui.theme.*
import com.bluebubbles.messaging.viewmodel.ConversationsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
  onConversationClick: (String) -> Unit,
  onSettingsClick: () -> Unit,
  viewModel: ConversationsViewModel = hiltViewModel()
) {
  val uiState by viewModel.uiState.collectAsState()

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              "BlueBubbles",
              fontWeight = FontWeight.Bold
            )
            ConnectionIndicator(uiState.connectionState)
          }
        },
        actions = {
          IconButton(onClick = { viewModel.refresh() }) {
            Icon(Icons.Default.Refresh, "Refresh")
          }
          IconButton(onClick = onSettingsClick) {
            Icon(Icons.Default.Settings, "Settings")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = BackgroundDark,
          titleContentColor = TextPrimary
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
        uiState.error != null -> {
          ErrorState(
            message = uiState.error!!,
            onRetry = { viewModel.loadConversations() }
          )
        }
        uiState.conversations.isEmpty() -> {
          EmptyState()
        }
        else -> {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(
              items = viewModel.filteredConversations,
              key = { it.guid }
            ) { conversation ->
              ConversationItem(
                conversation = conversation,
                onClick = { onConversationClick(conversation.guid) }
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun ConversationItem(
  conversation: Conversation,
  onClick: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = CardBackground.copy(alpha = 0.6f)
    )
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Avatar
      Box(
        modifier = Modifier
          .size(56.dp)
          .clip(CircleShape)
          .background(
            Brush.linearGradient(listOf(CyanPrimary, PurplePrimary))
          ),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = conversation.participants.firstOrNull()?.initials ?: "?",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = BackgroundDark
        )
      }

      // Content
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = conversation.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Medium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
          )

          conversation.lastMessageDate?.let { date ->
            Text(
              text = formatDate(date),
              style = MaterialTheme.typography.bodySmall,
              color = if (conversation.unreadCount > 0) CyanPrimary else TextMuted
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = conversation.lastMessage?.text ?: "No messages",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
          )

          if (conversation.unreadCount > 0) {
            Badge(
              containerColor = CyanPrimary,
              contentColor = BackgroundDark
            ) {
              Text(
                text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun ConnectionIndicator(state: ConnectionState) {
  val color by animateColorAsState(
    targetValue = when (state) {
      is ConnectionState.Connected -> OnlineGreen
      is ConnectionState.Connecting -> ConnectingYellow
      is ConnectionState.Disconnected -> OfflineRed
      is ConnectionState.Error -> OfflineRed
    },
    label = "connection_color"
  )

  Box(
    modifier = Modifier
      .size(10.dp)
      .clip(CircleShape)
      .background(color)
  )
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Icon(
      imageVector = Icons.Default.Warning,
      contentDescription = null,
      modifier = Modifier.size(64.dp),
      tint = RedAccent
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = message,
      style = MaterialTheme.typography.bodyLarge,
      color = TextSecondary
    )
    Spacer(modifier = Modifier.height(24.dp))
    Button(
      onClick = onRetry,
      colors = ButtonDefaults.buttonColors(
        containerColor = CyanPrimary,
        contentColor = BackgroundDark
      )
    ) {
      Text("Retry")
    }
  }
}

@Composable
fun EmptyState() {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Icon(
      imageVector = Icons.Default.Message,
      contentDescription = null,
      modifier = Modifier.size(64.dp),
      tint = TextMuted
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = "No conversations yet",
      style = MaterialTheme.typography.titleMedium,
      color = TextSecondary
    )
    Text(
      text = "Your iMessage conversations will appear here",
      style = MaterialTheme.typography.bodyMedium,
      color = TextMuted
    )
  }
}

private fun formatDate(date: Date): String {
  val now = Calendar.getInstance()
  val messageDate = Calendar.getInstance().apply { time = date }

  return when {
    now.get(Calendar.DATE) == messageDate.get(Calendar.DATE) -> {
      SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
    }
    now.get(Calendar.WEEK_OF_YEAR) == messageDate.get(Calendar.WEEK_OF_YEAR) -> {
      SimpleDateFormat("EEE", Locale.getDefault()).format(date)
    }
    else -> {
      SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(date)
    }
  }
}
