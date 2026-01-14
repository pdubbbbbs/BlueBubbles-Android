package com.bluebubbles.messaging.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bluebubbles.messaging.data.models.AssociatedMessage
import com.bluebubbles.messaging.data.models.ReactionType
import com.bluebubbles.messaging.ui.theme.*

@Composable
fun ReactionBubble(
  reactions: List<AssociatedMessage>,
  isFromMe: Boolean,
  modifier: Modifier = Modifier
) {
  if (reactions.isEmpty()) return

  // Group reactions by type
  val reactionCounts = reactions.groupBy { it.type }
    .mapValues { it.value.size }
    .toList()
    .sortedByDescending { it.second }

  Row(
    modifier = modifier
      .offset(
        x = if (isFromMe) (-8).dp else 8.dp,
        y = (-4).dp
      )
      .clip(RoundedCornerShape(12.dp))
      .background(
        Brush.linearGradient(
          listOf(
            CardBackground.copy(alpha = 0.95f),
            SurfaceDark.copy(alpha = 0.95f)
          )
        )
      )
      .shadow(2.dp, RoundedCornerShape(12.dp))
      .padding(horizontal = 6.dp, vertical = 2.dp),
    horizontalArrangement = Arrangement.spacedBy(2.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    reactionCounts.take(4).forEach { (type, count) ->
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        Text(
          text = type.emoji,
          fontSize = 12.sp
        )
        if (count > 1) {
          Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            fontSize = 10.sp
          )
        }
      }
    }
  }
}

@Composable
fun ReactionPicker(
  onReactionSelected: (ReactionType) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedReaction by remember { mutableStateOf<ReactionType?>(null) }

  AnimatedVisibility(
    visible = true,
    enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
    exit = scaleOut() + fadeOut()
  ) {
    Row(
      modifier = modifier
        .clip(RoundedCornerShape(24.dp))
        .background(
          Brush.linearGradient(
            listOf(CardBackground, SurfaceDark)
          )
        )
        .shadow(8.dp, RoundedCornerShape(24.dp))
        .padding(8.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      ReactionType.entries.forEach { reaction ->
        ReactionButton(
          reaction = reaction,
          isSelected = selectedReaction == reaction,
          onClick = {
            selectedReaction = reaction
            onReactionSelected(reaction)
            onDismiss()
          }
        )
      }
    }
  }
}

@Composable
private fun ReactionButton(
  reaction: ReactionType,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  val scale by animateFloatAsState(
    targetValue = if (isSelected) 1.2f else 1f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
    label = "scale"
  )

  Box(
    modifier = Modifier
      .size(40.dp)
      .clip(CircleShape)
      .background(
        if (isSelected) CyanPrimary.copy(alpha = 0.2f)
        else androidx.compose.ui.graphics.Color.Transparent
      )
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = reaction.emoji,
      fontSize = (20 * scale).sp
    )
  }
}

val ReactionType.emoji: String
  get() = when (this) {
    ReactionType.LOVE -> "\u2764\uFE0F" // ❤️
    ReactionType.LIKE -> "\uD83D\uDC4D" // 👍
    ReactionType.DISLIKE -> "\uD83D\uDC4E" // 👎
    ReactionType.LAUGH -> "\uD83D\uDE02" // 😂
    ReactionType.EMPHASIS -> "\u203C\uFE0F" // ‼️
    ReactionType.QUESTION -> "\u2753" // ❓
  }
