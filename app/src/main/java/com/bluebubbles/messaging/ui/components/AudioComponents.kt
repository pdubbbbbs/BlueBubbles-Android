package com.bluebubbles.messaging.ui.components

import android.Manifest
import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.bluebubbles.messaging.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.sin
import kotlin.random.Random

/**
 * Audio recorder state
 */
data class AudioRecorderState(
  val isRecording: Boolean = false,
  val duration: Long = 0L,
  val amplitudes: List<Float> = emptyList(),
  val outputFile: File? = null
)

/**
 * Audio player state
 */
data class AudioPlayerState(
  val isPlaying: Boolean = false,
  val currentPosition: Long = 0L,
  val duration: Long = 0L,
  val waveform: List<Float> = emptyList()
)

/**
 * Voice message recorder with waveform visualization
 */
@Composable
fun VoiceMessageRecorder(
  onRecordingComplete: (Uri, Long) -> Unit,
  onCancel: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var state by remember { mutableStateOf(AudioRecorderState()) }
  var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
  var outputFile by remember { mutableStateOf<File?>(null) }

  // Recording pulse animation
  val infiniteTransition = rememberInfiniteTransition(label = "recording_pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.2f,
    animationSpec = infiniteRepeatable(
      animation = tween(500),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse"
  )

  DisposableEffect(Unit) {
    onDispose {
      recorder?.apply {
        try {
          stop()
          release()
        } catch (e: Exception) {
          // Ignore cleanup errors
        }
      }
    }
  }

  // Update duration while recording
  LaunchedEffect(state.isRecording) {
    while (state.isRecording) {
      delay(100)
      state = state.copy(
        duration = state.duration + 100,
        amplitudes = state.amplitudes + Random.nextFloat()
      )
    }
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(SurfaceDark)
      .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Waveform visualization
    if (state.isRecording) {
      AudioWaveformVisualizer(
        amplitudes = state.amplitudes,
        isRecording = true,
        modifier = Modifier
          .fillMaxWidth()
          .height(60.dp)
          .padding(horizontal = 16.dp)
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Duration display
      Text(
        text = formatDuration(state.duration),
        style = MaterialTheme.typography.titleMedium,
        color = RedAccent,
        fontWeight = FontWeight.Bold
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Controls
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Cancel button
      IconButton(
        onClick = {
          recorder?.apply {
            try {
              stop()
              release()
            } catch (e: Exception) {}
          }
          recorder = null
          outputFile?.delete()
          state = AudioRecorderState()
          onCancel()
        }
      ) {
        Icon(
          Icons.Default.Close,
          contentDescription = "Cancel",
          tint = TextMuted,
          modifier = Modifier.size(28.dp)
        )
      }

      // Record/Stop button
      Box(
        modifier = Modifier
          .size(64.dp)
          .scale(if (state.isRecording) pulseScale else 1f)
          .clip(CircleShape)
          .background(if (state.isRecording) RedAccent else CyanPrimary)
          .clickable {
            if (state.isRecording) {
              // Stop recording
              recorder?.apply {
                try {
                  stop()
                  release()
                } catch (e: Exception) {}
              }
              recorder = null
              state = state.copy(isRecording = false)

              outputFile?.let { file ->
                if (file.exists() && file.length() > 0) {
                  onRecordingComplete(Uri.fromFile(file), state.duration)
                }
              }
            } else {
              // Start recording
              val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
              outputFile = file

              recorder = createMediaRecorder(context, file).apply {
                try {
                  prepare()
                  start()
                  state = AudioRecorderState(
                    isRecording = true,
                    outputFile = file
                  )
                } catch (e: Exception) {
                  release()
                  recorder = null
                }
              }
            }
          },
        contentAlignment = Alignment.Center
      ) {
        Icon(
          if (state.isRecording) Icons.Default.Stop else Icons.Default.Mic,
          contentDescription = if (state.isRecording) "Stop" else "Record",
          tint = BackgroundDark,
          modifier = Modifier.size(32.dp)
        )
      }

      // Send button (only visible when recording)
      if (state.isRecording) {
        IconButton(
          onClick = {
            recorder?.apply {
              try {
                stop()
                release()
              } catch (e: Exception) {}
            }
            recorder = null
            state = state.copy(isRecording = false)

            outputFile?.let { file ->
              if (file.exists() && file.length() > 0) {
                onRecordingComplete(Uri.fromFile(file), state.duration)
              }
            }
          }
        ) {
          Icon(
            Icons.Default.Send,
            contentDescription = "Send",
            tint = CyanPrimary,
            modifier = Modifier.size(28.dp)
          )
        }
      } else {
        Spacer(modifier = Modifier.size(48.dp))
      }
    }
  }
}

/**
 * Audio message player with waveform and controls
 */
@Composable
fun AudioMessagePlayer(
  audioUrl: String,
  duration: Long? = null,
  isFromMe: Boolean,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var playerState by remember { mutableStateOf(AudioPlayerState()) }
  var player by remember { mutableStateOf<ExoPlayer?>(null) }

  // Generate fake waveform for visualization
  val waveform = remember {
    List(40) { Random.nextFloat() * 0.8f + 0.2f }
  }

  DisposableEffect(audioUrl) {
    val exoPlayer = ExoPlayer.Builder(context).build().apply {
      setMediaItem(MediaItem.fromUri(audioUrl))
      prepare()
      addListener(object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
          when (playbackState) {
            Player.STATE_READY -> {
              playerState = playerState.copy(
                duration = duration ?: this@apply.duration
              )
            }
            Player.STATE_ENDED -> {
              playerState = playerState.copy(
                isPlaying = false,
                currentPosition = 0
              )
              this@apply.seekTo(0)
            }
          }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
          playerState = playerState.copy(isPlaying = isPlaying)
        }
      })
    }
    player = exoPlayer

    onDispose {
      exoPlayer.release()
    }
  }

  // Update position while playing
  LaunchedEffect(playerState.isPlaying) {
    while (playerState.isPlaying) {
      player?.let {
        playerState = playerState.copy(currentPosition = it.currentPosition)
      }
      delay(50)
    }
  }

  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(if (isFromMe) CyanDark.copy(alpha = 0.3f) else CardBackground)
      .padding(12.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Play/Pause button
    Box(
      modifier = Modifier
        .size(44.dp)
        .clip(CircleShape)
        .background(if (isFromMe) BackgroundDark.copy(alpha = 0.5f) else CyanPrimary)
        .clickable {
          player?.let {
            if (it.isPlaying) {
              it.pause()
            } else {
              it.play()
            }
          }
        },
      contentAlignment = Alignment.Center
    ) {
      Icon(
        if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
        contentDescription = if (playerState.isPlaying) "Pause" else "Play",
        tint = if (isFromMe) TextPrimary else BackgroundDark,
        modifier = Modifier.size(24.dp)
      )
    }

    // Waveform and progress
    Column(modifier = Modifier.weight(1f)) {
      AudioWaveformProgress(
        waveform = waveform,
        progress = if (playerState.duration > 0) {
          playerState.currentPosition.toFloat() / playerState.duration
        } else 0f,
        isFromMe = isFromMe,
        modifier = Modifier
          .fillMaxWidth()
          .height(32.dp)
      )

      Spacer(modifier = Modifier.height(4.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = formatDuration(playerState.currentPosition),
          style = MaterialTheme.typography.labelSmall,
          color = if (isFromMe) BackgroundDark.copy(alpha = 0.7f) else TextMuted
        )
        Text(
          text = formatDuration(playerState.duration),
          style = MaterialTheme.typography.labelSmall,
          color = if (isFromMe) BackgroundDark.copy(alpha = 0.7f) else TextMuted
        )
      }
    }
  }
}

/**
 * Waveform visualization during recording
 */
@Composable
private fun AudioWaveformVisualizer(
  amplitudes: List<Float>,
  isRecording: Boolean,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "waveform")

  Canvas(modifier = modifier) {
    val barWidth = 4.dp.toPx()
    val spacing = 2.dp.toPx()
    val maxBars = ((size.width + spacing) / (barWidth + spacing)).toInt()
    val displayAmplitudes = amplitudes.takeLast(maxBars)

    displayAmplitudes.forEachIndexed { index, amplitude ->
      val barHeight = amplitude * size.height * 0.8f
      val x = index * (barWidth + spacing)

      drawRoundRect(
        color = if (isRecording) RedAccent else CyanPrimary,
        topLeft = Offset(x, (size.height - barHeight) / 2),
        size = Size(barWidth, barHeight.coerceAtLeast(4.dp.toPx())),
        cornerRadius = CornerRadius(2.dp.toPx())
      )
    }
  }
}

/**
 * Waveform with playback progress overlay
 */
@Composable
private fun AudioWaveformProgress(
  waveform: List<Float>,
  progress: Float,
  isFromMe: Boolean,
  modifier: Modifier = Modifier
) {
  Canvas(modifier = modifier) {
    val barWidth = 3.dp.toPx()
    val spacing = 2.dp.toPx()
    val totalWidth = waveform.size * (barWidth + spacing) - spacing

    waveform.forEachIndexed { index, amplitude ->
      val x = index * (barWidth + spacing)
      val barHeight = amplitude * size.height * 0.8f
      val isPlayed = (x / totalWidth) <= progress

      drawRoundRect(
        color = when {
          isPlayed && isFromMe -> BackgroundDark.copy(alpha = 0.9f)
          isPlayed -> CyanPrimary
          isFromMe -> BackgroundDark.copy(alpha = 0.4f)
          else -> TextMuted.copy(alpha = 0.5f)
        },
        topLeft = Offset(x, (size.height - barHeight) / 2),
        size = Size(barWidth, barHeight.coerceAtLeast(4.dp.toPx())),
        cornerRadius = CornerRadius(2.dp.toPx())
      )
    }
  }
}

/**
 * Compact voice message button for message input
 */
@Composable
fun VoiceMessageButton(
  onClick: () -> Unit,
  isRecordingMode: Boolean,
  modifier: Modifier = Modifier
) {
  val backgroundColor by animateColorAsState(
    targetValue = if (isRecordingMode) RedAccent else CyanPrimary,
    animationSpec = tween(200),
    label = "button_color"
  )

  IconButton(
    onClick = onClick,
    modifier = modifier
      .size(48.dp)
      .clip(CircleShape)
      .background(backgroundColor)
  ) {
    Icon(
      if (isRecordingMode) Icons.Default.Stop else Icons.Default.Mic,
      contentDescription = if (isRecordingMode) "Stop recording" else "Record voice message",
      tint = BackgroundDark
    )
  }
}

/**
 * Format duration in mm:ss format
 */
private fun formatDuration(millis: Long): String {
  val seconds = (millis / 1000) % 60
  val minutes = (millis / 1000) / 60
  return "%d:%02d".format(minutes, seconds)
}

/**
 * Create MediaRecorder with appropriate settings
 */
@Suppress("DEPRECATION")
private fun createMediaRecorder(context: Context, outputFile: File): MediaRecorder {
  return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    MediaRecorder(context)
  } else {
    MediaRecorder()
  }.apply {
    setAudioSource(MediaRecorder.AudioSource.MIC)
    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
    setAudioChannels(1)
    setAudioSamplingRate(44100)
    setAudioEncodingBitRate(96000)
    setOutputFile(outputFile.absolutePath)
  }
}
