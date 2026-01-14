package com.bluebubbles.messaging.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bluebubbles.messaging.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * iMessage effect types
 */
enum class BubbleEffect(val id: String) {
  SLAM("com.apple.MobileSMS.expressivesend.impact"),
  LOUD("com.apple.MobileSMS.expressivesend.loud"),
  GENTLE("com.apple.MobileSMS.expressivesend.gentle"),
  INVISIBLE_INK("com.apple.MobileSMS.expressivesend.invisibleink");

  companion object {
    fun fromId(id: String?): BubbleEffect? = entries.find { it.id == id }
  }
}

enum class ScreenEffect(val id: String) {
  ECHO("com.apple.messages.effect.CKEchoEffect"),
  SPOTLIGHT("com.apple.messages.effect.CKSpotlightEffect"),
  BALLOONS("com.apple.messages.effect.CKHappyBirthdayEffect"),
  CONFETTI("com.apple.messages.effect.CKConfettiEffect"),
  LOVE("com.apple.messages.effect.CKHeartEffect"),
  LASERS("com.apple.messages.effect.CKLasersEffect"),
  FIREWORKS("com.apple.messages.effect.CKFireworksEffect"),
  CELEBRATION("com.apple.messages.effect.CKSparklesEffect");

  companion object {
    fun fromId(id: String?): ScreenEffect? = entries.find { it.id == id }
  }
}

// ==================== BUBBLE EFFECTS ====================

/**
 * Wrapper that applies bubble effects to message content
 */
@Composable
fun BubbleEffectWrapper(
  effect: BubbleEffect?,
  isNewMessage: Boolean,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit
) {
  when (effect) {
    BubbleEffect.SLAM -> SlamEffect(isNewMessage, modifier, content)
    BubbleEffect.LOUD -> LoudEffect(isNewMessage, modifier, content)
    BubbleEffect.GENTLE -> GentleEffect(isNewMessage, modifier, content)
    BubbleEffect.INVISIBLE_INK -> InvisibleInkEffect(modifier, content)
    null -> Box(modifier) { content() }
  }
}

@Composable
private fun SlamEffect(
  animate: Boolean,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit
) {
  var triggerAnimation by remember { mutableStateOf(animate) }

  val scale by animateFloatAsState(
    targetValue = if (triggerAnimation) 1f else 1f,
    animationSpec = if (triggerAnimation) {
      keyframes {
        durationMillis = 600
        1.5f at 0 using LinearEasing
        0.9f at 200 using FastOutSlowInEasing
        1.05f at 400 using FastOutSlowInEasing
        1f at 600 using FastOutSlowInEasing
      }
    } else {
      snap()
    },
    label = "slam_scale"
  )

  val offsetY by animateFloatAsState(
    targetValue = 0f,
    animationSpec = if (triggerAnimation) {
      keyframes {
        durationMillis = 600
        -50f at 0 using LinearEasing
        10f at 200 using FastOutSlowInEasing
        -5f at 400 using FastOutSlowInEasing
        0f at 600 using FastOutSlowInEasing
      }
    } else {
      snap()
    },
    label = "slam_offset"
  )

  LaunchedEffect(animate) {
    if (animate) {
      triggerAnimation = true
      delay(700)
      triggerAnimation = false
    }
  }

  Box(
    modifier = modifier
      .scale(scale)
      .offset(y = offsetY.dp)
  ) {
    content()
  }
}

@Composable
private fun LoudEffect(
  animate: Boolean,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit
) {
  val infiniteTransition = rememberInfiniteTransition(label = "loud")

  val shake by infiniteTransition.animateFloat(
    initialValue = -2f,
    targetValue = 2f,
    animationSpec = infiniteRepeatable(
      animation = tween(50, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "shake"
  )

  val scale by animateFloatAsState(
    targetValue = if (animate) 1.15f else 1f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessLow
    ),
    label = "loud_scale"
  )

  var shakeActive by remember { mutableStateOf(animate) }

  LaunchedEffect(animate) {
    if (animate) {
      shakeActive = true
      delay(1000)
      shakeActive = false
    }
  }

  Box(
    modifier = modifier
      .scale(scale)
      .offset(x = if (shakeActive) shake.dp else 0.dp)
  ) {
    content()
  }
}

@Composable
private fun GentleEffect(
  animate: Boolean,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit
) {
  val alpha by animateFloatAsState(
    targetValue = 1f,
    animationSpec = if (animate) {
      tween(durationMillis = 2000, easing = LinearEasing)
    } else {
      snap()
    },
    label = "gentle_alpha"
  )

  val scale by animateFloatAsState(
    targetValue = 1f,
    animationSpec = if (animate) {
      tween(durationMillis = 2000, easing = FastOutSlowInEasing)
    } else {
      snap()
    },
    label = "gentle_scale"
  )

  Box(
    modifier = modifier
      .alpha(if (animate) alpha else 1f)
      .scale(if (animate) scale else 1f)
  ) {
    content()
  }
}

@Composable
private fun InvisibleInkEffect(
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit
) {
  var isRevealed by remember { mutableStateOf(false) }

  val blurRadius by animateFloatAsState(
    targetValue = if (isRevealed) 0f else 20f,
    animationSpec = tween(500),
    label = "blur"
  )

  Box(
    modifier = modifier
      .blur(blurRadius.dp)
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null
      ) {
        isRevealed = !isRevealed
      }
  ) {
    content()

    if (!isRevealed) {
      // Sparkle overlay
      SparkleOverlay(
        modifier = Modifier.matchParentSize()
      )
    }
  }
}

@Composable
private fun SparkleOverlay(modifier: Modifier = Modifier) {
  val infiniteTransition = rememberInfiniteTransition(label = "sparkle")

  val sparkles = remember {
    List(20) {
      SparkleData(
        x = Random.nextFloat(),
        y = Random.nextFloat(),
        size = Random.nextFloat() * 4f + 2f,
        delay = Random.nextInt(500)
      )
    }
  }

  Canvas(modifier = modifier) {
    sparkles.forEach { sparkle ->
      val alpha = infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
          animation = tween(500, delayMillis = sparkle.delay),
          repeatMode = RepeatMode.Reverse
        ),
        label = "sparkle_alpha"
      )

      drawCircle(
        color = Color.White.copy(alpha = alpha.value),
        radius = sparkle.size,
        center = Offset(
          x = size.width * sparkle.x,
          y = size.height * sparkle.y
        )
      )
    }
  }
}

private data class SparkleData(
  val x: Float,
  val y: Float,
  val size: Float,
  val delay: Int
)

// ==================== SCREEN EFFECTS ====================

/**
 * Full-screen effect overlay
 */
@Composable
fun ScreenEffectOverlay(
  effect: ScreenEffect?,
  onEffectComplete: () -> Unit,
  modifier: Modifier = Modifier
) {
  if (effect == null) return

  var isPlaying by remember { mutableStateOf(true) }

  LaunchedEffect(effect) {
    delay(3000) // Play effect for 3 seconds
    isPlaying = false
    onEffectComplete()
  }

  if (isPlaying) {
    Box(
      modifier = modifier.fillMaxSize()
    ) {
      when (effect) {
        ScreenEffect.BALLOONS -> BalloonsEffect()
        ScreenEffect.CONFETTI -> ConfettiEffect()
        ScreenEffect.LOVE -> LoveEffect()
        ScreenEffect.FIREWORKS -> FireworksEffect()
        ScreenEffect.LASERS -> LasersEffect()
        ScreenEffect.CELEBRATION -> CelebrationEffect()
        ScreenEffect.ECHO -> {} // Handled differently
        ScreenEffect.SPOTLIGHT -> SpotlightEffect()
      }
    }
  }
}

@Composable
private fun BalloonsEffect() {
  val balloons = remember {
    List(15) {
      BalloonData(
        x = Random.nextFloat(),
        color = listOf(
          Color.Red, Color.Blue, Color.Yellow, Color.Green,
          Color.Magenta, CyanPrimary, PurplePrimary
        ).random(),
        speed = Random.nextFloat() * 2f + 1f,
        size = Random.nextFloat() * 30f + 40f
      )
    }
  }

  val infiniteTransition = rememberInfiniteTransition(label = "balloons")

  Canvas(modifier = Modifier.fillMaxSize()) {
    balloons.forEachIndexed { index, balloon ->
      val progress = infiniteTransition.animateFloat(
        initialValue = 1.2f,
        targetValue = -0.2f,
        animationSpec = infiniteRepeatable(
          animation = tween(
            durationMillis = (3000 / balloon.speed).toInt(),
            easing = LinearEasing
          )
        ),
        label = "balloon_$index"
      )

      val wobble = infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
          animation = tween(500, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Reverse
        ),
        label = "wobble_$index"
      )

      val centerX = size.width * balloon.x + wobble.value
      val centerY = size.height * progress.value

      // Balloon body
      drawOval(
        color = balloon.color,
        topLeft = Offset(centerX - balloon.size / 2, centerY - balloon.size / 2),
        size = androidx.compose.ui.geometry.Size(balloon.size, balloon.size * 1.2f)
      )

      // Balloon string
      drawLine(
        color = Color.Gray,
        start = Offset(centerX, centerY + balloon.size * 0.6f),
        end = Offset(centerX + wobble.value / 2, centerY + balloon.size * 1.2f),
        strokeWidth = 2f
      )
    }
  }
}

private data class BalloonData(
  val x: Float,
  val color: Color,
  val speed: Float,
  val size: Float
)

@Composable
private fun ConfettiEffect() {
  val confetti = remember {
    List(100) {
      ConfettiPiece(
        x = Random.nextFloat(),
        color = listOf(
          Color.Red, Color.Blue, Color.Yellow, Color.Green,
          Color.Magenta, CyanPrimary, PurplePrimary, Color.White
        ).random(),
        speed = Random.nextFloat() * 2f + 0.5f,
        rotation = Random.nextFloat() * 360f,
        size = Random.nextFloat() * 8f + 4f
      )
    }
  }

  val infiniteTransition = rememberInfiniteTransition(label = "confetti")

  Canvas(modifier = Modifier.fillMaxSize()) {
    confetti.forEachIndexed { index, piece ->
      val progress = infiniteTransition.animateFloat(
        initialValue = -0.1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
          animation = tween(
            durationMillis = (2000 / piece.speed).toInt(),
            easing = LinearEasing
          )
        ),
        label = "confetti_$index"
      )

      val rotation = infiniteTransition.animateFloat(
        initialValue = piece.rotation,
        targetValue = piece.rotation + 360f,
        animationSpec = infiniteRepeatable(
          animation = tween(1000, easing = LinearEasing)
        ),
        label = "rotation_$index"
      )

      val wobble = sin(progress.value * 10 + index) * 20f

      rotate(rotation.value, Offset(size.width * piece.x + wobble, size.height * progress.value)) {
        drawRect(
          color = piece.color,
          topLeft = Offset(
            size.width * piece.x + wobble - piece.size / 2,
            size.height * progress.value - piece.size / 2
          ),
          size = androidx.compose.ui.geometry.Size(piece.size, piece.size * 0.6f)
        )
      }
    }
  }
}

private data class ConfettiPiece(
  val x: Float,
  val color: Color,
  val speed: Float,
  val rotation: Float,
  val size: Float
)

@Composable
private fun LoveEffect() {
  val hearts = remember {
    List(20) {
      HeartData(
        x = Random.nextFloat(),
        speed = Random.nextFloat() * 1.5f + 0.5f,
        size = Random.nextFloat() * 30f + 20f,
        alpha = Random.nextFloat() * 0.5f + 0.5f
      )
    }
  }

  val infiniteTransition = rememberInfiniteTransition(label = "love")

  Canvas(modifier = Modifier.fillMaxSize()) {
    hearts.forEachIndexed { index, heart ->
      val progress = infiniteTransition.animateFloat(
        initialValue = 1.1f,
        targetValue = -0.1f,
        animationSpec = infiniteRepeatable(
          animation = tween(
            durationMillis = (4000 / heart.speed).toInt(),
            easing = LinearEasing
          )
        ),
        label = "heart_$index"
      )

      val wobble = sin(progress.value * 5 + index) * 30f

      drawHeart(
        color = Color.Red.copy(alpha = heart.alpha),
        center = Offset(size.width * heart.x + wobble, size.height * progress.value),
        size = heart.size
      )
    }
  }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHeart(
  color: Color,
  center: Offset,
  size: Float
) {
  val path = Path().apply {
    moveTo(center.x, center.y + size * 0.3f)
    cubicTo(
      center.x - size * 0.5f, center.y - size * 0.3f,
      center.x - size * 0.5f, center.y - size * 0.6f,
      center.x, center.y - size * 0.3f
    )
    cubicTo(
      center.x + size * 0.5f, center.y - size * 0.6f,
      center.x + size * 0.5f, center.y - size * 0.3f,
      center.x, center.y + size * 0.3f
    )
    close()
  }
  drawPath(path, color)
}

private data class HeartData(
  val x: Float,
  val speed: Float,
  val size: Float,
  val alpha: Float
)

@Composable
private fun FireworksEffect() {
  val fireworks = remember {
    List(5) {
      FireworkData(
        x = Random.nextFloat() * 0.8f + 0.1f,
        y = Random.nextFloat() * 0.5f + 0.2f,
        color = listOf(
          Color.Red, Color.Yellow, Color.Green, CyanPrimary, PurplePrimary, Color.White
        ).random(),
        delay = Random.nextInt(1000)
      )
    }
  }

  val infiniteTransition = rememberInfiniteTransition(label = "fireworks")

  Canvas(modifier = Modifier.fillMaxSize()) {
    fireworks.forEachIndexed { index, firework ->
      val progress = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
          animation = tween(1500, delayMillis = firework.delay, easing = FastOutSlowInEasing)
        ),
        label = "firework_$index"
      )

      val alpha = 1f - progress.value

      // Draw explosion particles
      for (i in 0 until 12) {
        val angle = (i * 30f) * (Math.PI / 180f)
        val radius = progress.value * 100f

        drawCircle(
          color = firework.color.copy(alpha = alpha),
          radius = 4f,
          center = Offset(
            size.width * firework.x + (cos(angle) * radius).toFloat(),
            size.height * firework.y + (sin(angle) * radius).toFloat()
          )
        )
      }
    }
  }
}

private data class FireworkData(
  val x: Float,
  val y: Float,
  val color: Color,
  val delay: Int
)

@Composable
private fun LasersEffect() {
  val infiniteTransition = rememberInfiniteTransition(label = "lasers")

  val laserColors = listOf(Color.Red, Color.Green, CyanPrimary, PurplePrimary)

  Canvas(modifier = Modifier.fillMaxSize()) {
    laserColors.forEachIndexed { index, color ->
      val offset = infiniteTransition.animateFloat(
        initialValue = -size.width,
        targetValue = size.width * 2,
        animationSpec = infiniteRepeatable(
          animation = tween(1000, delayMillis = index * 200, easing = LinearEasing)
        ),
        label = "laser_$index"
      )

      // Draw laser beams
      drawLine(
        brush = Brush.horizontalGradient(
          listOf(Color.Transparent, color, color, Color.Transparent)
        ),
        start = Offset(offset.value, size.height * (0.2f + index * 0.2f)),
        end = Offset(offset.value + size.width, size.height * (0.3f + index * 0.15f)),
        strokeWidth = 8f
      )
    }
  }
}

@Composable
private fun CelebrationEffect() {
  val sparkles = remember {
    List(50) {
      CelebrationSparkle(
        x = Random.nextFloat(),
        y = Random.nextFloat(),
        size = Random.nextFloat() * 6f + 2f,
        color = listOf(
          Color.Yellow, Color.White, CyanPrimary, PurplePrimary
        ).random(),
        delay = Random.nextInt(500)
      )
    }
  }

  val infiniteTransition = rememberInfiniteTransition(label = "celebration")

  Canvas(modifier = Modifier.fillMaxSize()) {
    sparkles.forEachIndexed { index, sparkle ->
      val alpha = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
          animation = tween(300, delayMillis = sparkle.delay),
          repeatMode = RepeatMode.Reverse
        ),
        label = "sparkle_$index"
      )

      val scale = infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
          animation = tween(300, delayMillis = sparkle.delay),
          repeatMode = RepeatMode.Reverse
        ),
        label = "scale_$index"
      )

      // Draw 4-pointed star
      drawStar(
        color = sparkle.color.copy(alpha = alpha.value),
        center = Offset(size.width * sparkle.x, size.height * sparkle.y),
        size = sparkle.size * scale.value
      )
    }
  }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStar(
  color: Color,
  center: Offset,
  size: Float
) {
  val path = Path().apply {
    moveTo(center.x, center.y - size)
    lineTo(center.x + size * 0.3f, center.y - size * 0.3f)
    lineTo(center.x + size, center.y)
    lineTo(center.x + size * 0.3f, center.y + size * 0.3f)
    lineTo(center.x, center.y + size)
    lineTo(center.x - size * 0.3f, center.y + size * 0.3f)
    lineTo(center.x - size, center.y)
    lineTo(center.x - size * 0.3f, center.y - size * 0.3f)
    close()
  }
  drawPath(path, color)
}

private data class CelebrationSparkle(
  val x: Float,
  val y: Float,
  val size: Float,
  val color: Color,
  val delay: Int
)

@Composable
private fun SpotlightEffect() {
  val infiniteTransition = rememberInfiniteTransition(label = "spotlight")

  val spotlightX = infiniteTransition.animateFloat(
    initialValue = 0.3f,
    targetValue = 0.7f,
    animationSpec = infiniteRepeatable(
      animation = tween(2000, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "spotlight_x"
  )

  Canvas(modifier = Modifier.fillMaxSize()) {
    // Draw dark overlay with spotlight cutout
    drawRect(
      brush = Brush.radialGradient(
        colors = listOf(
          Color.Transparent,
          Color.Black.copy(alpha = 0.7f)
        ),
        center = Offset(size.width * spotlightX.value, size.height * 0.5f),
        radius = size.width * 0.3f
      )
    )
  }
}
