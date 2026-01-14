package com.bluebubbles.messaging.ui.components

import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.bluebubbles.messaging.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

private const val TAG = "QrCodeScanner"

data class BlueBubblesQrConfig(
  val serverUrl: String,
  val password: String,
  val port: Int = 1234,
  val useHttps: Boolean = true
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun QrCodeScannerSheet(
  onDismiss: () -> Unit,
  onConfigScanned: (BlueBubblesQrConfig) -> Unit
) {
  val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = BackgroundDark
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      when {
        cameraPermissionState.status.isGranted -> {
          QrScannerCamera(
            onDismiss = onDismiss,
            onConfigScanned = onConfigScanned
          )
        }
        cameraPermissionState.status.shouldShowRationale -> {
          CameraPermissionRationale(
            onRequestPermission = { cameraPermissionState.launchPermissionRequest() },
            onDismiss = onDismiss
          )
        }
        else -> {
          LaunchedEffect(Unit) {
            cameraPermissionState.launchPermissionRequest()
          }
          CameraPermissionRequest(
            onRequestPermission = { cameraPermissionState.launchPermissionRequest() },
            onDismiss = onDismiss
          )
        }
      }
    }
  }
}

@Composable
private fun QrScannerCamera(
  onDismiss: () -> Unit,
  onConfigScanned: (BlueBubblesQrConfig) -> Unit
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  var flashEnabled by remember { mutableStateOf(false) }
  var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
  var hasScanned by remember { mutableStateOf(false) }
  var scanResult by remember { mutableStateOf<String?>(null) }
  var scanError by remember { mutableStateOf<String?>(null) }

  val executor = remember { Executors.newSingleThreadExecutor() }
  val scanner = remember { BarcodeScanning.getClient() }

  DisposableEffect(Unit) {
    onDispose {
      cameraProvider?.unbindAll()
      executor.shutdown()
      scanner.close()
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    AndroidView(
      modifier = Modifier.fillMaxSize(),
      factory = { ctx ->
        PreviewView(ctx).apply {
          layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
          )
          scaleType = PreviewView.ScaleType.FILL_CENTER

          val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
          cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()
            cameraProvider = provider

            val preview = Preview.Builder()
              .build()
              .also { it.setSurfaceProvider(surfaceProvider) }

            val imageAnalysis = ImageAnalysis.Builder()
              .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
              .build()
              .also { analysis ->
                analysis.setAnalyzer(executor) { imageProxy ->
                  if (hasScanned) {
                    imageProxy.close()
                    return@setAnalyzer
                  }

                  val mediaImage = imageProxy.image
                  if (mediaImage != null) {
                    val image = InputImage.fromMediaImage(
                      mediaImage,
                      imageProxy.imageInfo.rotationDegrees
                    )

                    scanner.process(image)
                      .addOnSuccessListener { barcodes ->
                        for (barcode in barcodes) {
                          if (barcode.valueType == Barcode.TYPE_TEXT ||
                            barcode.valueType == Barcode.TYPE_URL) {
                            barcode.rawValue?.let { value ->
                              if (!hasScanned) {
                                hasScanned = true
                                scanResult = value
                                parseQrConfig(value)?.let { config ->
                                  onConfigScanned(config)
                                } ?: run {
                                  scanError = "Invalid QR code format"
                                  hasScanned = false
                                }
                              }
                            }
                          }
                        }
                      }
                      .addOnFailureListener { e ->
                        Log.e(TAG, "Barcode scanning failed", e)
                      }
                      .addOnCompleteListener {
                        imageProxy.close()
                      }
                  } else {
                    imageProxy.close()
                  }
                }
              }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
              provider.unbindAll()
              val camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
              )

              camera.cameraControl.enableTorch(flashEnabled)
            } catch (e: Exception) {
              Log.e(TAG, "Camera binding failed", e)
            }
          }, ContextCompat.getMainExecutor(ctx))
        }
      }
    )

    // Scanner overlay
    ScannerOverlay(
      flashEnabled = flashEnabled,
      onFlashToggle = {
        flashEnabled = !flashEnabled
        cameraProvider?.let { provider ->
          try {
            val camera = provider.bindToLifecycle(
              lifecycleOwner,
              CameraSelector.DEFAULT_BACK_CAMERA
            )
            camera.cameraControl.enableTorch(flashEnabled)
          } catch (e: Exception) {
            Log.e(TAG, "Flash toggle failed", e)
          }
        }
      },
      onDismiss = onDismiss,
      errorMessage = scanError
    )
  }
}

@Composable
private fun ScannerOverlay(
  flashEnabled: Boolean,
  onFlashToggle: () -> Unit,
  onDismiss: () -> Unit,
  errorMessage: String?
) {
  Box(modifier = Modifier.fillMaxSize()) {
    // Semi-transparent overlay
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.5f))
    )

    // Top bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .statusBarsPadding(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(
        onClick = onDismiss,
        colors = IconButtonDefaults.iconButtonColors(
          containerColor = CardBackground.copy(alpha = 0.8f)
        )
      ) {
        Icon(
          Icons.Default.Close,
          contentDescription = "Close",
          tint = TextPrimary
        )
      }

      Text(
        text = "Scan QR Code",
        style = MaterialTheme.typography.titleMedium,
        color = TextPrimary,
        fontWeight = FontWeight.Bold
      )

      IconButton(
        onClick = onFlashToggle,
        colors = IconButtonDefaults.iconButtonColors(
          containerColor = if (flashEnabled) CyanPrimary else CardBackground.copy(alpha = 0.8f)
        )
      ) {
        Icon(
          if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
          contentDescription = "Flash",
          tint = if (flashEnabled) BackgroundDark else TextPrimary
        )
      }
    }

    // Scanner frame
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(40.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(
        modifier = Modifier
          .size(280.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(Color.Transparent)
          .border(
            width = 3.dp,
            color = CyanPrimary,
            shape = RoundedCornerShape(16.dp)
          )
      ) {
        // Corner highlights
        CornerAccents()
      }

      Spacer(modifier = Modifier.height(24.dp))

      Text(
        text = "Point your camera at the BlueBubbles\nserver QR code",
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary,
        textAlign = TextAlign.Center
      )

      AnimatedVisibility(
        visible = errorMessage != null,
        enter = fadeIn(),
        exit = fadeOut()
      ) {
        Card(
          modifier = Modifier.padding(top = 16.dp),
          colors = CardDefaults.cardColors(
            containerColor = RedAccent.copy(alpha = 0.2f)
          ),
          shape = RoundedCornerShape(8.dp)
        ) {
          Text(
            text = errorMessage ?: "",
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = RedAccent
          )
        }
      }
    }

    // Bottom hint
    Column(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(32.dp)
        .navigationBarsPadding(),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Card(
        colors = CardDefaults.cardColors(
          containerColor = CardBackground.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(12.dp)
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            Icons.Default.QrCodeScanner,
            contentDescription = null,
            tint = CyanPrimary,
            modifier = Modifier.size(24.dp)
          )
          Column {
            Text(
              text = "Find QR Code in BlueBubbles Server",
              style = MaterialTheme.typography.bodyMedium,
              color = TextPrimary,
              fontWeight = FontWeight.Medium
            )
            Text(
              text = "Settings → General → QR Code",
              style = MaterialTheme.typography.bodySmall,
              color = TextMuted
            )
          }
        }
      }
    }
  }
}

@Composable
private fun BoxScope.CornerAccents() {
  val cornerSize = 24.dp
  val strokeWidth = 4.dp
  val cornerColor = CyanPrimary

  // Top-left
  Box(
    modifier = Modifier
      .align(Alignment.TopStart)
      .padding(8.dp)
      .size(cornerSize)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(strokeWidth)
        .background(cornerColor, RoundedCornerShape(strokeWidth))
    )
    Box(
      modifier = Modifier
        .fillMaxHeight()
        .width(strokeWidth)
        .background(cornerColor, RoundedCornerShape(strokeWidth))
    )
  }

  // Top-right
  Box(
    modifier = Modifier
      .align(Alignment.TopEnd)
      .padding(8.dp)
      .size(cornerSize)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(strokeWidth)
        .background(cornerColor, RoundedCornerShape(strokeWidth))
    )
    Box(
      modifier = Modifier
        .fillMaxHeight()
        .width(strokeWidth)
        .align(Alignment.TopEnd)
        .background(cornerColor, RoundedCornerShape(strokeWidth))
    )
  }

  // Bottom-left
  Box(
    modifier = Modifier
      .align(Alignment.BottomStart)
      .padding(8.dp)
      .size(cornerSize)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(strokeWidth)
        .align(Alignment.BottomStart)
        .background(cornerColor, RoundedCornerShape(strokeWidth))
    )
    Box(
      modifier = Modifier
        .fillMaxHeight()
        .width(strokeWidth)
        .background(cornerColor, RoundedCornerShape(strokeWidth))
    )
  }

  // Bottom-right
  Box(
    modifier = Modifier
      .align(Alignment.BottomEnd)
      .padding(8.dp)
      .size(cornerSize)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(strokeWidth)
        .align(Alignment.BottomEnd)
        .background(cornerColor, RoundedCornerShape(strokeWidth))
    )
    Box(
      modifier = Modifier
        .fillMaxHeight()
        .width(strokeWidth)
        .align(Alignment.TopEnd)
        .background(cornerColor, RoundedCornerShape(strokeWidth))
    )
  }
}

@Composable
private fun CameraPermissionRationale(
  onRequestPermission: () -> Unit,
  onDismiss: () -> Unit
) {
  PermissionContent(
    title = "Camera Permission Required",
    description = "To scan QR codes from your BlueBubbles server, we need access to your camera. This is only used for scanning and nothing is recorded.",
    buttonText = "Grant Permission",
    onButtonClick = onRequestPermission,
    onDismiss = onDismiss
  )
}

@Composable
private fun CameraPermissionRequest(
  onRequestPermission: () -> Unit,
  onDismiss: () -> Unit
) {
  PermissionContent(
    title = "Camera Access",
    description = "Scanning QR codes requires camera access. Please allow camera permission when prompted.",
    buttonText = "Continue",
    onButtonClick = onRequestPermission,
    onDismiss = onDismiss
  )
}

@Composable
private fun PermissionContent(
  title: String,
  description: String,
  buttonText: String,
  onButtonClick: () -> Unit,
  onDismiss: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    IconButton(
      onClick = onDismiss,
      modifier = Modifier.align(Alignment.Start)
    ) {
      Icon(Icons.Default.Close, "Close", tint = TextPrimary)
    }

    Spacer(modifier = Modifier.weight(1f))

    Icon(
      Icons.Default.QrCodeScanner,
      contentDescription = null,
      modifier = Modifier.size(80.dp),
      tint = CyanPrimary
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
      text = title,
      style = MaterialTheme.typography.headlineSmall,
      color = TextPrimary,
      fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(12.dp))

    Text(
      text = description,
      style = MaterialTheme.typography.bodyMedium,
      color = TextSecondary,
      textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.weight(1f))

    Button(
      onClick = onButtonClick,
      modifier = Modifier
        .fillMaxWidth()
        .height(56.dp),
      shape = RoundedCornerShape(16.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = CyanPrimary,
        contentColor = BackgroundDark
      )
    ) {
      Text(
        text = buttonText,
        fontWeight = FontWeight.SemiBold
      )
    }

    Spacer(modifier = Modifier.height(12.dp))

    TextButton(onClick = onDismiss) {
      Text(
        text = "Enter Manually Instead",
        color = TextMuted
      )
    }
  }
}

private fun parseQrConfig(rawValue: String): BlueBubblesQrConfig? {
  return try {
    // BlueBubbles QR format: bluebubbles://serverUrl:port?password=xxx
    // or JSON format: {"url":"...", "password":"...", "port":1234}
    when {
      rawValue.startsWith("bluebubbles://") -> {
        val uri = rawValue.removePrefix("bluebubbles://")
        val parts = uri.split("?")
        val hostPort = parts[0].split(":")
        val host = hostPort[0]
        val port = hostPort.getOrNull(1)?.toIntOrNull() ?: 1234
        val password = parts.getOrNull(1)
          ?.split("&")
          ?.find { it.startsWith("password=") }
          ?.removePrefix("password=")
          ?: ""

        BlueBubblesQrConfig(
          serverUrl = host,
          password = password,
          port = port,
          useHttps = !host.matches(Regex("^\\d+\\.\\d+\\.\\d+\\.\\d+$"))
        )
      }
      rawValue.startsWith("{") -> {
        // JSON format
        val json = org.json.JSONObject(rawValue)
        BlueBubblesQrConfig(
          serverUrl = json.optString("url", json.optString("server", "")),
          password = json.optString("password", ""),
          port = json.optInt("port", 1234),
          useHttps = json.optBoolean("https", true)
        )
      }
      rawValue.contains(":") && rawValue.contains("@") -> {
        // URL format: https://password@server:port
        val useHttps = rawValue.startsWith("https")
        val stripped = rawValue.removePrefix("https://").removePrefix("http://")
        val atIndex = stripped.indexOf("@")
        val password = if (atIndex > 0) stripped.substring(0, atIndex) else ""
        val hostPart = if (atIndex > 0) stripped.substring(atIndex + 1) else stripped
        val colonIndex = hostPart.lastIndexOf(":")
        val host = if (colonIndex > 0) hostPart.substring(0, colonIndex) else hostPart
        val port = if (colonIndex > 0) hostPart.substring(colonIndex + 1).toIntOrNull() ?: 1234 else 1234

        BlueBubblesQrConfig(
          serverUrl = host,
          password = password,
          port = port,
          useHttps = useHttps
        )
      }
      else -> null
    }
  } catch (e: Exception) {
    Log.e(TAG, "Failed to parse QR config", e)
    null
  }
}
