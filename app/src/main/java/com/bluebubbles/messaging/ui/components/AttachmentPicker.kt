package com.bluebubbles.messaging.ui.components

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.bluebubbles.messaging.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class SelectedAttachment(
  val uri: Uri,
  val mimeType: String?,
  val fileName: String?,
  val size: Long?
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AttachmentPickerSheet(
  onDismiss: () -> Unit,
  onAttachmentsSelected: (List<SelectedAttachment>) -> Unit
) {
  val context = LocalContext.current
  var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

  // Photo picker
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
  ) { uris ->
    if (uris.isNotEmpty()) {
      val attachments = uris.map { uri ->
        SelectedAttachment(
          uri = uri,
          mimeType = context.contentResolver.getType(uri),
          fileName = getFileName(context, uri),
          size = getFileSize(context, uri)
        )
      }
      onAttachmentsSelected(attachments)
    }
    onDismiss()
  }

  // Camera launcher
  val cameraLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.TakePicture()
  ) { success ->
    if (success && tempPhotoUri != null) {
      val attachment = SelectedAttachment(
        uri = tempPhotoUri!!,
        mimeType = "image/jpeg",
        fileName = "photo_${System.currentTimeMillis()}.jpg",
        size = null
      )
      onAttachmentsSelected(listOf(attachment))
    }
    onDismiss()
  }

  // File picker
  val filePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenMultipleDocuments()
  ) { uris ->
    if (uris.isNotEmpty()) {
      val attachments = uris.map { uri ->
        SelectedAttachment(
          uri = uri,
          mimeType = context.contentResolver.getType(uri),
          fileName = getFileName(context, uri),
          size = getFileSize(context, uri)
        )
      }
      onAttachmentsSelected(attachments)
    }
    onDismiss()
  }

  // Camera permission
  val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

  Surface(
    modifier = Modifier.fillMaxWidth(),
    color = SurfaceDark,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      // Handle bar
      Box(
        modifier = Modifier
          .align(Alignment.CenterHorizontally)
          .width(40.dp)
          .height(4.dp)
          .clip(RoundedCornerShape(2.dp))
          .background(TextMuted)
      )

      Spacer(modifier = Modifier.height(20.dp))

      Text(
        text = "Add Attachment",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
      )

      Spacer(modifier = Modifier.height(20.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
      ) {
        AttachmentOption(
          icon = Icons.Default.Image,
          label = "Photos",
          gradientColors = listOf(PurplePrimary, PurpleDark),
          onClick = {
            photoPickerLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
            )
          }
        )

        AttachmentOption(
          icon = Icons.Default.CameraAlt,
          label = "Camera",
          gradientColors = listOf(CyanPrimary, CyanDark),
          onClick = {
            if (cameraPermission.status.isGranted) {
              tempPhotoUri = createImageUri(context)
              tempPhotoUri?.let { cameraLauncher.launch(it) }
            } else {
              cameraPermission.launchPermissionRequest()
            }
          }
        )

        AttachmentOption(
          icon = Icons.Default.InsertDriveFile,
          label = "Files",
          gradientColors = listOf(GreenAccent, CyanDark),
          onClick = {
            filePickerLauncher.launch(arrayOf("*/*"))
          }
        )

        AttachmentOption(
          icon = Icons.Default.LocationOn,
          label = "Location",
          gradientColors = listOf(AmberAccent, RedAccent),
          onClick = {
            // TODO: Location picker
            onDismiss()
          }
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      TextButton(
        onClick = onDismiss,
        modifier = Modifier.align(Alignment.CenterHorizontally)
      ) {
        Text("Cancel", color = TextMuted)
      }

      Spacer(modifier = Modifier.height(8.dp))
    }
  }
}

@Composable
private fun AttachmentOption(
  icon: ImageVector,
  label: String,
  gradientColors: List<androidx.compose.ui.graphics.Color>,
  onClick: () -> Unit
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.clickable(onClick = onClick)
  ) {
    Box(
      modifier = Modifier
        .size(60.dp)
        .clip(CircleShape)
        .background(Brush.linearGradient(gradientColors)),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = TextPrimary,
        modifier = Modifier.size(28.dp)
      )
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = TextSecondary
    )
  }
}

private fun createImageUri(context: Context): Uri? {
  val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
  val imageFileName = "JPEG_${timeStamp}_"
  val storageDir = context.cacheDir
  val imageFile = File.createTempFile(imageFileName, ".jpg", storageDir)

  return FileProvider.getUriForFile(
    context,
    "${context.packageName}.fileprovider",
    imageFile
  )
}

private fun getFileName(context: Context, uri: Uri): String? {
  return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
    cursor.moveToFirst()
    if (nameIndex >= 0) cursor.getString(nameIndex) else null
  }
}

private fun getFileSize(context: Context, uri: Uri): Long? {
  return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
    val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
    cursor.moveToFirst()
    if (sizeIndex >= 0) cursor.getLong(sizeIndex) else null
  }
}
