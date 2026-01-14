package com.bluebubbles.messaging.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import com.bluebubbles.messaging.data.models.Attachment
import com.bluebubbles.messaging.ui.theme.*

@Composable
fun MessageAttachment(
  attachment: Attachment,
  serverUrl: String,
  password: String,
  isFromMe: Boolean,
  onFullscreenClick: (Attachment) -> Unit
) {
  val attachmentUrl = buildAttachmentUrl(attachment.guid, serverUrl, password)

  when {
    attachment.isSticker -> {
      InlineSticker(
        attachment = attachment,
        serverUrl = serverUrl,
        password = password,
        isFromMe = isFromMe
      )
    }
    attachment.isImage -> {
      ImageAttachment(
        url = attachmentUrl,
        width = attachment.width,
        height = attachment.height,
        isFromMe = isFromMe,
        onClick = { onFullscreenClick(attachment) }
      )
    }
    attachment.isVideo -> {
      VideoThumbnail(
        url = attachmentUrl,
        isFromMe = isFromMe,
        onClick = { onFullscreenClick(attachment) }
      )
    }
    attachment.isAudio -> {
      AudioMessagePlayer(
        audioUrl = attachmentUrl,
        duration = null, // Will be determined by player
        isFromMe = isFromMe
      )
    }
    else -> {
      FileAttachment(
        fileName = attachment.fileName,
        mimeType = attachment.mimeType,
        size = attachment.totalBytes,
        isFromMe = isFromMe
      )
    }
  }
}

@Composable
private fun ImageAttachment(
  url: String,
  width: Int?,
  height: Int?,
  isFromMe: Boolean,
  onClick: () -> Unit
) {
  var isLoading by remember { mutableStateOf(true) }
  var hasError by remember { mutableStateOf(false) }

  val aspectRatio = if (width != null && height != null && height > 0) {
    width.toFloat() / height.toFloat()
  } else 1f

  Box(
    modifier = Modifier
      .widthIn(max = 240.dp)
      .aspectRatio(aspectRatio.coerceIn(0.5f, 2f))
      .clip(RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
  ) {
    AsyncImage(
      model = ImageRequest.Builder(LocalContext.current)
        .data(url)
        .crossfade(true)
        .build(),
      contentDescription = "Image attachment",
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize(),
      onState = { state ->
        isLoading = state is AsyncImagePainter.State.Loading
        hasError = state is AsyncImagePainter.State.Error
      }
    )

    if (isLoading) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(CardBackground),
        contentAlignment = Alignment.Center
      ) {
        CircularProgressIndicator(
          color = CyanPrimary,
          strokeWidth = 2.dp,
          modifier = Modifier.size(24.dp)
        )
      }
    }

    if (hasError) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(CardBackground),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          Icons.Default.BrokenImage,
          contentDescription = "Error",
          tint = TextMuted,
          modifier = Modifier.size(32.dp)
        )
      }
    }
  }
}

@Composable
private fun VideoThumbnail(
  url: String,
  isFromMe: Boolean,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .size(200.dp, 150.dp)
      .clip(RoundedCornerShape(12.dp))
      .background(CardBackground)
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center
  ) {
    // Video thumbnail would go here - using placeholder
    Icon(
      Icons.Default.Videocam,
      contentDescription = null,
      tint = TextMuted,
      modifier = Modifier.size(40.dp)
    )

    // Play button overlay
    Box(
      modifier = Modifier
        .size(48.dp)
        .clip(CircleShape)
        .background(CyanPrimary.copy(alpha = 0.9f)),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        Icons.Default.PlayArrow,
        contentDescription = "Play",
        tint = BackgroundDark,
        modifier = Modifier.size(28.dp)
      )
    }
  }
}

@Composable
private fun AudioAttachment(
  url: String,
  fileName: String?,
  isFromMe: Boolean
) {
  var isPlaying by remember { mutableStateOf(false) }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(if (isFromMe) CyanDark.copy(alpha = 0.3f) else CardBackground)
      .padding(12.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    IconButton(
      onClick = { isPlaying = !isPlaying },
      modifier = Modifier
        .size(40.dp)
        .clip(CircleShape)
        .background(CyanPrimary)
    ) {
      Icon(
        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
        contentDescription = if (isPlaying) "Pause" else "Play",
        tint = BackgroundDark
      )
    }

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = fileName ?: "Audio message",
        style = MaterialTheme.typography.bodyMedium,
        color = if (isFromMe) BackgroundDark else TextPrimary,
        maxLines = 1
      )
      // Audio waveform visualization could go here
      LinearProgressIndicator(
        progress = { 0f },
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 4.dp)
          .height(3.dp)
          .clip(RoundedCornerShape(2.dp)),
        color = CyanPrimary,
        trackColor = if (isFromMe) BackgroundDark.copy(alpha = 0.3f) else TextMuted.copy(alpha = 0.3f),
      )
    }
  }
}

@Composable
private fun FileAttachment(
  fileName: String?,
  mimeType: String?,
  size: Long?,
  isFromMe: Boolean
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(if (isFromMe) CyanDark.copy(alpha = 0.3f) else CardBackground)
      .padding(12.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(40.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(PurplePrimary.copy(alpha = 0.3f)),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = getFileIcon(mimeType),
        contentDescription = null,
        tint = PurplePrimary
      )
    }

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = fileName ?: "File",
        style = MaterialTheme.typography.bodyMedium,
        color = if (isFromMe) BackgroundDark else TextPrimary,
        fontWeight = FontWeight.Medium,
        maxLines = 1
      )
      if (size != null) {
        Text(
          text = formatFileSize(size),
          style = MaterialTheme.typography.bodySmall,
          color = if (isFromMe) BackgroundDark.copy(alpha = 0.7f) else TextMuted
        )
      }
    }

    Icon(
      Icons.Default.Download,
      contentDescription = "Download",
      tint = if (isFromMe) BackgroundDark else CyanPrimary
    )
  }
}

@Composable
fun FullscreenImageViewer(
  attachment: Attachment,
  serverUrl: String,
  password: String,
  onDismiss: () -> Unit
) {
  var scale by remember { mutableFloatStateOf(1f) }
  var offsetX by remember { mutableFloatStateOf(0f) }
  var offsetY by remember { mutableFloatStateOf(0f) }

  val attachmentUrl = buildAttachmentUrl(attachment.guid, serverUrl, password)

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      decorFitsSystemWindows = false
    )
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
        .pointerInput(Unit) {
          detectTapGestures(
            onDoubleTap = {
              scale = if (scale > 1f) 1f else 2f
              offsetX = 0f
              offsetY = 0f
            },
            onTap = { onDismiss() }
          )
        }
    ) {
      AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
          .data(attachmentUrl)
          .crossfade(true)
          .build(),
        contentDescription = "Full image",
        contentScale = ContentScale.Fit,
        modifier = Modifier
          .fillMaxSize()
          .graphicsLayer(
            scaleX = scale,
            scaleY = scale,
            translationX = offsetX,
            translationY = offsetY
          )
          .pointerInput(Unit) {
            detectTransformGestures { _, pan, zoom, _ ->
              scale = (scale * zoom).coerceIn(1f, 5f)
              if (scale > 1f) {
                offsetX += pan.x
                offsetY += pan.y
              } else {
                offsetX = 0f
                offsetY = 0f
              }
            }
          }
      )

      // Close button
      IconButton(
        onClick = onDismiss,
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(16.dp)
          .statusBarsPadding()
      ) {
        Icon(
          Icons.Default.Close,
          contentDescription = "Close",
          tint = Color.White
        )
      }
    }
  }
}

@Composable
fun FullscreenVideoPlayer(
  attachment: Attachment,
  serverUrl: String,
  password: String,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val attachmentUrl = buildAttachmentUrl(attachment.guid, serverUrl, password)

  val exoPlayer = remember {
    ExoPlayer.Builder(context).build().apply {
      setMediaItem(MediaItem.fromUri(attachmentUrl))
      prepare()
      playWhenReady = true
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      exoPlayer.release()
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      decorFitsSystemWindows = false
    )
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
    ) {
      AndroidView(
        factory = { ctx ->
          PlayerView(ctx).apply {
            player = exoPlayer
            useController = true
          }
        },
        modifier = Modifier.fillMaxSize()
      )

      IconButton(
        onClick = onDismiss,
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(16.dp)
          .statusBarsPadding()
      ) {
        Icon(
          Icons.Default.Close,
          contentDescription = "Close",
          tint = Color.White
        )
      }
    }
  }
}

private fun buildAttachmentUrl(guid: String, serverUrl: String, password: String): String {
  val baseUrl = if (serverUrl.startsWith("http")) serverUrl else "https://$serverUrl"
  return "$baseUrl/api/v1/attachment/$guid/download?password=$password"
}

private fun getFileIcon(mimeType: String?): androidx.compose.ui.graphics.vector.ImageVector {
  return when {
    mimeType?.startsWith("application/pdf") == true -> Icons.Default.PictureAsPdf
    mimeType?.contains("word") == true -> Icons.Default.Description
    mimeType?.contains("sheet") == true || mimeType?.contains("excel") == true -> Icons.Default.TableChart
    mimeType?.contains("zip") == true || mimeType?.contains("archive") == true -> Icons.Default.FolderZip
    else -> Icons.Default.InsertDriveFile
  }
}

private fun formatFileSize(bytes: Long): String {
  return when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
    else -> "${bytes / (1024 * 1024 * 1024)} GB"
  }
}
