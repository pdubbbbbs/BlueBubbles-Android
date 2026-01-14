package com.bluebubbles.messaging.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bluebubbles.messaging.data.models.Message
import com.bluebubbles.messaging.ui.theme.*
import com.bluebubbles.messaging.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
  onBackClick: () -> Unit,
  viewModel: ChatViewModel = hiltViewModel()
) {
  val uiState by viewModel.uiState.collectAsState()
  val listState = rememberLazyListState()

  Scaffold(
    topBar = {
      TopAppBar(
        navigationIcon = {
          IconButton(onClick = onBackClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
          }
        },
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                  Brush.linearGradient(listOf(CyanPrimary, PurplePrimary))
                ),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = uiState.conversation?.participants?.firstOrNull()?.initials ?: "?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = BackgroundDark
              )
            }
            Column {
              Text(
                text = uiState.conversation?.title ?: "Chat",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
              )
            }
          }
        },
        actions = {
          IconButton(onClick = { /* TODO: Info */ }) {
            Icon(Icons.Default.Info, "Info")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = BackgroundDark,
          titleContentColor = TextPrimary
        )
      )
    },
    bottomBar = {
      MessageInput(
        text = uiState.messageText,
        onTextChange = viewModel::updateMessageText,
        onSendClick = viewModel::sendMessage,
        isSending = uiState.isSending,
        replyMessage = uiState.replyToMessage,
        onClearReply = viewModel::clearReply
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
      if (uiState.isLoading) {
        CircularProgressIndicator(
          modifier = Modifier.align(Alignment.Center),
          color = CyanPrimary
        )
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          state = listState,
          reverseLayout = true,
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(
            items = uiState.messages,
            key = { it.guid }
          ) { message ->
            MessageBubble(
              message = message,
              onLongPress = { viewModel.setReplyToMessage(message) }
            )
          }

          if (uiState.isLoadingMore) {
            item {
              Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
              ) {
                CircularProgressIndicator(
                  modifier = Modifier.size(24.dp),
                  color = CyanPrimary,
                  strokeWidth = 2.dp
                )
              }
            }
          }
        }
      }
    }
  }

  // Load more when reaching the end
  LaunchedEffect(listState) {
    snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
      .collect { lastVisibleIndex ->
        if (lastVisibleIndex != null && lastVisibleIndex >= uiState.messages.size - 5) {
          viewModel.loadMoreMessages()
        }
      }
  }
}

@Composable
fun MessageBubble(
  message: Message,
  onLongPress: () -> Unit
) {
  val isFromMe = message.isFromMe

  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start
  ) {
    Box(
      modifier = Modifier
        .widthIn(max = 280.dp)
        .clip(
          RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = if (isFromMe) 16.dp else 4.dp,
            bottomEnd = if (isFromMe) 4.dp else 16.dp
          )
        )
        .background(
          if (isFromMe) {
            Brush.linearGradient(listOf(CyanPrimary, CyanDark))
          } else {
            Brush.linearGradient(listOf(CardBackground, SurfaceDark))
          }
        )
        .padding(12.dp)
    ) {
      Column {
        message.text?.let { text ->
          Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isFromMe) BackgroundDark else TextPrimary
          )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.align(Alignment.End)
        ) {
          Text(
            text = formatTime(message.dateCreated),
            style = MaterialTheme.typography.labelSmall,
            color = if (isFromMe) BackgroundDark.copy(alpha = 0.7f) else TextMuted
          )

          if (isFromMe) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
              imageVector = when {
                message.isRead -> Icons.Default.DoneAll
                message.isDelivered -> Icons.Default.Done
                else -> Icons.Default.Schedule
              },
              contentDescription = null,
              modifier = Modifier.size(14.dp),
              tint = if (message.isRead) GreenAccent else BackgroundDark.copy(alpha = 0.7f)
            )
          }
        }
      }
    }
  }
}

@Composable
fun MessageInput(
  text: String,
  onTextChange: (String) -> Unit,
  onSendClick: () -> Unit,
  isSending: Boolean,
  replyMessage: Message?,
  onClearReply: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(SurfaceDark)
  ) {
    // Reply preview
    replyMessage?.let { reply ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(CardBackground)
          .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Replying to",
            style = MaterialTheme.typography.labelSmall,
            color = CyanPrimary
          )
          Text(
            text = reply.text ?: "Message",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            maxLines = 1
          )
        }
        IconButton(onClick = onClearReply) {
          Icon(
            Icons.Default.Close,
            "Cancel reply",
            tint = TextMuted
          )
        }
      }
    }

    // Input row
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(onClick = { /* TODO: Attachments */ }) {
        Icon(
          Icons.Default.Add,
          "Add attachment",
          tint = CyanPrimary
        )
      }

      OutlinedTextField(
        value = text,
        onValueChange = onTextChange,
        modifier = Modifier.weight(1f),
        placeholder = {
          Text("iMessage", color = TextMuted)
        },
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = CyanPrimary,
          unfocusedBorderColor = PurpleDark,
          cursorColor = CyanPrimary,
          focusedTextColor = TextPrimary,
          unfocusedTextColor = TextPrimary
        ),
        shape = RoundedCornerShape(24.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = { onSendClick() }),
        maxLines = 4
      )

      IconButton(
        onClick = onSendClick,
        enabled = text.isNotBlank() && !isSending,
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .background(
            if (text.isNotBlank()) CyanPrimary else PurpleDark
          )
      ) {
        if (isSending) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = BackgroundDark,
            strokeWidth = 2.dp
          )
        } else {
          Icon(
            Icons.AutoMirrored.Filled.Send,
            "Send",
            tint = if (text.isNotBlank()) BackgroundDark else TextMuted
          )
        }
      }
    }
  }
}

private fun formatTime(date: Date): String {
  return SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
}
