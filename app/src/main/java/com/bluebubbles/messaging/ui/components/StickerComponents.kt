package com.bluebubbles.messaging.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.bluebubbles.messaging.data.models.Attachment
import com.bluebubbles.messaging.ui.theme.*
import kotlin.math.roundToInt

/**
 * Data class for sticker positioning on messages
 */
data class StickerPlacement(
  val stickerGuid: String,
  val messageGuid: String,
  val xOffset: Float,
  val yOffset: Float,
  val rotation: Float = 0f,
  val scale: Float = 1f
)

/**
 * Displays a sticker attachment with optional drag/rotation capability
 */
@Composable
fun StickerView(
  attachment: Attachment,
  serverUrl: String,
  password: String,
  modifier: Modifier = Modifier,
  isDraggable: Boolean = false,
  initialOffset: Offset = Offset.Zero,
  onPositionChanged: ((Offset) -> Unit)? = null
) {
  var offset by remember { mutableStateOf(initialOffset) }
  var rotation by remember { mutableFloatStateOf(0f) }
  var scale by remember { mutableFloatStateOf(1f) }

  val animatedScale by animateFloatAsState(
    targetValue = scale,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessLow
    ),
    label = "sticker_scale"
  )

  Box(
    modifier = modifier
      .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
      .scale(animatedScale)
      .rotate(rotation)
      .then(
        if (isDraggable) {
          Modifier.pointerInput(Unit) {
            detectDragGestures(
              onDragStart = {
                scale = 1.1f
              },
              onDragEnd = {
                scale = 1f
                onPositionChanged?.invoke(offset)
              },
              onDrag = { change, dragAmount ->
                change.consume()
                offset = Offset(
                  x = offset.x + dragAmount.x,
                  y = offset.y + dragAmount.y
                )
              }
            )
          }
        } else Modifier
      )
  ) {
    val imageUrl = buildStickerUrl(serverUrl, attachment.guid, password)

    AsyncImage(
      model = ImageRequest.Builder(LocalContext.current)
        .data(imageUrl)
        .crossfade(true)
        .build(),
      contentDescription = "Sticker",
      modifier = Modifier
        .size(80.dp)
        .clip(RoundedCornerShape(8.dp)),
      contentScale = ContentScale.Fit
    )
  }
}

/**
 * Animated sticker that "peels" when long pressed
 */
@Composable
fun PeelableSticker(
  attachment: Attachment,
  serverUrl: String,
  password: String,
  onPeel: () -> Unit,
  modifier: Modifier = Modifier
) {
  var isPeeling by remember { mutableStateOf(false) }

  val peelRotation by animateFloatAsState(
    targetValue = if (isPeeling) -15f else 0f,
    animationSpec = tween(200),
    label = "peel_rotation"
  )

  val peelScale by animateFloatAsState(
    targetValue = if (isPeeling) 1.2f else 1f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy
    ),
    label = "peel_scale"
  )

  val shadowElevation by animateDpAsState(
    targetValue = if (isPeeling) 16.dp else 0.dp,
    animationSpec = tween(200),
    label = "shadow"
  )

  Box(
    modifier = modifier
      .scale(peelScale)
      .rotate(peelRotation)
      .clickable {
        isPeeling = true
        onPeel()
      }
  ) {
    Surface(
      shadowElevation = shadowElevation,
      shape = RoundedCornerShape(8.dp)
    ) {
      StickerView(
        attachment = attachment,
        serverUrl = serverUrl,
        password = password
      )
    }
  }
}

/**
 * Sticker picker sheet for browsing available stickers
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickerPickerSheet(
  stickers: List<Attachment>,
  serverUrl: String,
  password: String,
  onStickerSelected: (Attachment) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(
        Brush.verticalGradient(
          listOf(SurfaceDark, BackgroundDark)
        )
      )
      .padding(16.dp)
  ) {
    // Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Stickers",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
      )
      IconButton(onClick = onDismiss) {
        Icon(
          Icons.Default.Close,
          contentDescription = "Close",
          tint = TextMuted
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    if (stickers.isEmpty()) {
      // Empty state
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(200.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = "No stickers available",
            style = MaterialTheme.typography.bodyLarge,
            color = TextMuted
          )
          Text(
            text = "Stickers from iMessage will appear here",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
          )
        }
      }
    } else {
      // Sticker grid
      LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier
          .fillMaxWidth()
          .height(300.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(stickers) { sticker ->
          StickerGridItem(
            attachment = sticker,
            serverUrl = serverUrl,
            password = password,
            onClick = { onStickerSelected(sticker) }
          )
        }
      }
    }
  }
}

@Composable
private fun StickerGridItem(
  attachment: Attachment,
  serverUrl: String,
  password: String,
  onClick: () -> Unit
) {
  var isPressed by remember { mutableStateOf(false) }

  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.9f else 1f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy
    ),
    label = "press_scale"
  )

  Box(
    modifier = Modifier
      .aspectRatio(1f)
      .scale(scale)
      .clip(RoundedCornerShape(12.dp))
      .background(CardBackground)
      .clickable {
        isPressed = true
        onClick()
      }
      .padding(8.dp),
    contentAlignment = Alignment.Center
  ) {
    val imageUrl = buildStickerUrl(serverUrl, attachment.guid, password)

    AsyncImage(
      model = ImageRequest.Builder(LocalContext.current)
        .data(imageUrl)
        .crossfade(true)
        .build(),
      contentDescription = attachment.fileName ?: "Sticker",
      modifier = Modifier.fillMaxSize(),
      contentScale = ContentScale.Fit
    )
  }

  LaunchedEffect(isPressed) {
    if (isPressed) {
      kotlinx.coroutines.delay(100)
      isPressed = false
    }
  }
}

/**
 * Inline sticker display for message attachments
 */
@Composable
fun InlineSticker(
  attachment: Attachment,
  serverUrl: String,
  password: String,
  isFromMe: Boolean,
  modifier: Modifier = Modifier
) {
  val imageUrl = buildStickerUrl(serverUrl, attachment.guid, password)

  Box(
    modifier = modifier
      .size(120.dp)
      .padding(4.dp),
    contentAlignment = if (isFromMe) Alignment.CenterEnd else Alignment.CenterStart
  ) {
    AsyncImage(
      model = ImageRequest.Builder(LocalContext.current)
        .data(imageUrl)
        .crossfade(true)
        .build(),
      contentDescription = "Sticker",
      modifier = Modifier
        .fillMaxSize()
        .clip(RoundedCornerShape(8.dp)),
      contentScale = ContentScale.Fit
    )
  }
}

/**
 * Sticker pack info
 */
data class StickerPack(
  val name: String,
  val bundleId: String,
  val stickers: List<Attachment>
)

/**
 * Build sticker URL with authentication
 */
private fun buildStickerUrl(serverUrl: String, guid: String, password: String): String {
  val baseUrl = serverUrl.trimEnd('/')
  return "$baseUrl/api/v1/attachment/$guid/download?password=$password"
}
