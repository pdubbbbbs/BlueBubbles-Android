package com.bluebubbles.messaging.ui.screens.chat

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.bluebubbles.messaging.data.models.Attachment
import com.bluebubbles.messaging.data.models.Message
import com.bluebubbles.messaging.data.models.ReactionType
import com.bluebubbles.messaging.ui.components.*
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
  var showAttachmentPicker by remember { mutableStateOf(false) }
  var selectedAttachments by remember { mutableStateOf<List<SelectedAttachment>>(emptyList()) }
  var fullscreenAttachment by remember { mutableStateOf<Attachment?>(null) }
  var reactionPickerMessage by remember { mutableStateOf<Message?>(null) }

  // Attachment picker bottom sheet
  if (showAttachmentPicker) {
    ModalBottomSheet(
      onDismissRequest = { showAttachmentPicker = false },
      containerColor = SurfaceDark
    ) {
      AttachmentPickerSheet(
        onDismiss = { showAttachmentPicker = false },
        onAttachmentsSelected = { attachments ->
          selectedAttachments = attachments
          showAttachmentPicker = false
        }
      )
    }
  }

  // Fullscreen image viewer
  fullscreenAttachment?.let { attachment ->
    if (attachment.isImage) {
      FullscreenImageViewer(
        attachment = attachment,
        serverUrl = uiState.serverUrl ?: "",
        password = uiState.serverPassword ?: "",
        onDismiss = { fullscreenAttachment = null }
      )
    } else if (attachment.isVideo) {
      FullscreenVideoPlayer(
        attachment = attachment,
        serverUrl = uiState.serverUrl ?: "",
        password = uiState.serverPassword ?: "",
        onDismiss = { fullscreenAttachment = null }
      )
    }
  }

  // Reaction picker overlay
  reactionPickerMessage?.let { message ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(BackgroundDark.copy(alpha = 0.5f))
        .clickable { reactionPickerMessage = null }
    ) {
      Popup(
        alignment = Alignment.Center,
        onDismissRequest = { reactionPickerMessage = null },
        properties = PopupProperties(focusable = true)
      ) {
        ReactionPicker(
          onReactionSelected = { reactionType ->
            viewModel.sendReaction(message.guid, reactionType)
            reactionPickerMessage = null
          },
          onDismiss = { reactionPickerMessage = null }
        )
      }
    }
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
      Column {
        // Typing indicator
        AnimatedVisibility(
          visible = uiState.isOtherTyping,
          enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
          exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
          TypingIndicator(
            participantName = uiState.typingParticipant
          )
        }

        MessageInput(
          text = uiState.messageText,
          onTextChange = viewModel::updateMessageText,
          onSendClick = {
            viewModel.sendMessage(selectedAttachments)
            selectedAttachments = emptyList()
          },
          isSending = uiState.isSending,
          replyMessage = uiState.replyToMessage,
          onClearReply = viewModel::clearReply,
          onAttachmentClick = { showAttachmentPicker = true },
          selectedAttachments = selectedAttachments,
          onRemoveAttachment = { attachment ->
            selectedAttachments = selectedAttachments.filter { it != attachment }
          }
        )
      }
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
              serverUrl = uiState.serverUrl ?: "",
              serverPassword = uiState.serverPassword ?: "",
              onLongPress = { reactionPickerMessage = message },
              onDoubleTap = { viewModel.setReplyToMessage(message) },
              onAttachmentClick = { fullscreenAttachment = it }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
  message: Message,
  serverUrl: String,
  serverPassword: String,
  onLongPress: () -> Unit,
  onDoubleTap: () -> Unit,
  onAttachmentClick: (Attachment) -> Unit
) {
  val isFromMe = message.isFromMe
  val hasReactions = message.associatedMessages.isNotEmpty()

  Box(
    modifier = Modifier.fillMaxWidth(),
    contentAlignment = if (isFromMe) Alignment.CenterEnd else Alignment.CenterStart
  ) {
    Column(
      horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start
    ) {
      // Attachments
      if (message.hasAttachments) {
        Column(
          modifier = Modifier.padding(bottom = if (message.text != null) 4.dp else 0.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          message.attachments.forEach { attachment ->
            MessageAttachment(
              attachment = attachment,
              serverUrl = serverUrl,
              password = serverPassword,
              isFromMe = isFromMe,
              onFullscreenClick = onAttachmentClick
            )
          }
        }
      }

      // Text bubble (only if there's text)
      if (message.text != null || !message.hasAttachments) {
        Box {
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
              .combinedClickable(
                onClick = { },
                onLongClick = onLongPress,
                onDoubleClick = onDoubleTap
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

          // Reaction bubble (positioned at bottom corner of message)
          if (hasReactions) {
            ReactionBubble(
              reactions = message.associatedMessages,
              isFromMe = isFromMe,
              modifier = Modifier.align(
                if (isFromMe) Alignment.BottomStart else Alignment.BottomEnd
              )
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
  onClearReply: () -> Unit,
  onAttachmentClick: () -> Unit,
  selectedAttachments: List<SelectedAttachment>,
  onRemoveAttachment: (SelectedAttachment) -> Unit
) {
  val context = LocalContext.current
  val canSend = text.isNotBlank() || selectedAttachments.isNotEmpty()

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

    // Selected attachments preview
    if (selectedAttachments.isNotEmpty()) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        selectedAttachments.forEach { attachment ->
          AttachmentPreview(
            attachment = attachment,
            onRemove = { onRemoveAttachment(attachment) }
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
      IconButton(onClick = onAttachmentClick) {
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
        keyboardActions = KeyboardActions(onSend = { if (canSend) onSendClick() }),
        maxLines = 4
      )

      IconButton(
        onClick = onSendClick,
        enabled = canSend && !isSending,
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .background(
            if (canSend) CyanPrimary else PurpleDark
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
            tint = if (canSend) BackgroundDark else TextMuted
          )
        }
      }
    }
  }
}

@Composable
private fun AttachmentPreview(
  attachment: SelectedAttachment,
  onRemove: () -> Unit
) {
  Box(
    modifier = Modifier.size(72.dp)
  ) {
    val isImage = attachment.mimeType?.startsWith("image/") == true
    val isVideo = attachment.mimeType?.startsWith("video/") == true

    if (isImage || isVideo) {
      AsyncImage(
        model = attachment.uri,
        contentDescription = "Attachment preview",
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .fillMaxSize()
          .clip(RoundedCornerShape(8.dp))
      )
      if (isVideo) {
        Icon(
          Icons.Default.PlayCircle,
          contentDescription = null,
          tint = TextPrimary,
          modifier = Modifier
            .size(24.dp)
            .align(Alignment.Center)
        )
      }
    } else {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .clip(RoundedCornerShape(8.dp))
          .background(CardBackground),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            Icons.Default.InsertDriveFile,
            contentDescription = null,
            tint = PurplePrimary,
            modifier = Modifier.size(24.dp)
          )
          Text(
            text = attachment.fileName?.take(8) ?: "File",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            maxLines = 1
          )
        }
      }
    }

    // Remove button
    IconButton(
      onClick = onRemove,
      modifier = Modifier
        .size(20.dp)
        .align(Alignment.TopEnd)
        .offset(x = 4.dp, y = (-4).dp)
        .clip(CircleShape)
        .background(RedAccent)
    ) {
      Icon(
        Icons.Default.Close,
        contentDescription = "Remove",
        tint = TextPrimary,
        modifier = Modifier.size(12.dp)
      )
    }
  }
}

private fun formatTime(date: Date): String {
  return SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
}

@Composable
private fun TypingIndicator(
  participantName: String?
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(SurfaceDark)
      .padding(horizontal = 16.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Animated dots
    TypingDots()

    Text(
      text = if (participantName != null) {
        "$participantName is typing..."
      } else {
        "typing..."
      },
      style = MaterialTheme.typography.bodySmall,
      color = TextMuted
    )
  }
}

@Composable
private fun TypingDots() {
  val infiniteTransition = rememberInfiniteTransition(label = "typing")

  Row(
    modifier = Modifier
      .clip(RoundedCornerShape(12.dp))
      .background(CardBackground)
      .padding(horizontal = 12.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    repeat(3) { index ->
      val delay = index * 150

      val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
          animation = tween(600, delayMillis = delay),
          repeatMode = RepeatMode.Reverse
        ),
        label = "dot$index"
      )

      val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -4f,
        animationSpec = infiniteRepeatable(
          animation = tween(600, delayMillis = delay),
          repeatMode = RepeatMode.Reverse
        ),
        label = "offset$index"
      )

      Box(
        modifier = Modifier
          .offset(y = offsetY.dp)
          .size(8.dp)
          .clip(CircleShape)
          .background(CyanPrimary.copy(alpha = alpha))
      )
    }
  }
}
