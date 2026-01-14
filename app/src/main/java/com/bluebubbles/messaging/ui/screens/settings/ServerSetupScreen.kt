package com.bluebubbles.messaging.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.bluebubbles.messaging.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSetupScreen(
  onBackClick: () -> Unit,
  onSetupComplete: () -> Unit
) {
  var serverUrl by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var port by remember { mutableStateOf("1234") }
  var useHttps by remember { mutableStateOf(true) }
  var showPassword by remember { mutableStateOf(false) }
  var isTestingConnection by remember { mutableStateOf(false) }
  var connectionStatus by remember { mutableStateOf<ConnectionTestResult?>(null) }

  Scaffold(
    topBar = {
      TopAppBar(
        navigationIcon = {
          IconButton(onClick = onBackClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
          }
        },
        title = {
          Text(
            "Server Setup",
            fontWeight = FontWeight.Bold
          )
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = BackgroundDark,
          titleContentColor = TextPrimary
        )
      )
    },
    containerColor = BackgroundDark
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .background(
          Brush.verticalGradient(
            colors = listOf(BackgroundDark, BackgroundPurple, BackgroundDark)
          )
        )
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
      // Header
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
          containerColor = CardBackground.copy(alpha = 0.6f)
        )
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Icon(
            imageVector = Icons.Default.Cloud,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = CyanPrimary
          )
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "Connect to BlueBubbles Server",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
          )
          Text(
            text = "Enter your server details to start messaging",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
          )
        }
      }

      // Server URL
      OutlinedTextField(
        value = serverUrl,
        onValueChange = { serverUrl = it },
        label = { Text("Server URL") },
        placeholder = { Text("yourserver.ngrok.io or 192.168.1.100") },
        leadingIcon = {
          Icon(Icons.Default.Language, null, tint = CyanPrimary)
        },
        modifier = Modifier.fillMaxWidth(),
        colors = outlinedTextFieldColors(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
      )

      // Port
      OutlinedTextField(
        value = port,
        onValueChange = { port = it.filter { c -> c.isDigit() } },
        label = { Text("Port") },
        leadingIcon = {
          Icon(Icons.Default.Router, null, tint = CyanPrimary)
        },
        modifier = Modifier.fillMaxWidth(),
        colors = outlinedTextFieldColors(),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
      )

      // Password
      OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Password") },
        leadingIcon = {
          Icon(Icons.Default.Lock, null, tint = CyanPrimary)
        },
        trailingIcon = {
          IconButton(onClick = { showPassword = !showPassword }) {
            Icon(
              if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
              null,
              tint = TextMuted
            )
          }
        },
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
        colors = outlinedTextFieldColors(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
      )

      // HTTPS Toggle
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Use HTTPS",
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary
          )
          Text(
            text = "Recommended for secure connections",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
          )
        }
        Switch(
          checked = useHttps,
          onCheckedChange = { useHttps = it },
          colors = SwitchDefaults.colors(
            checkedThumbColor = CyanPrimary,
            checkedTrackColor = CyanPrimary.copy(alpha = 0.3f)
          )
        )
      }

      // Connection Status
      connectionStatus?.let { status ->
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(
            containerColor = if (status.success) GreenAccent.copy(alpha = 0.1f)
            else RedAccent.copy(alpha = 0.1f)
          )
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = if (status.success) Icons.Default.CheckCircle else Icons.Default.Error,
              contentDescription = null,
              tint = if (status.success) GreenAccent else RedAccent
            )
            Column {
              Text(
                text = if (status.success) "Connected!" else "Connection Failed",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (status.success) GreenAccent else RedAccent
              )
              Text(
                text = status.message,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.weight(1f))

      // Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        OutlinedButton(
          onClick = {
            isTestingConnection = true
            // TODO: Implement actual connection test
            connectionStatus = ConnectionTestResult(
              success = serverUrl.isNotEmpty() && password.isNotEmpty(),
              message = if (serverUrl.isNotEmpty() && password.isNotEmpty())
                "Server v1.0.0 • macOS Sonoma 14.0"
              else "Please fill in all fields"
            )
            isTestingConnection = false
          },
          modifier = Modifier.weight(1f),
          enabled = !isTestingConnection,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.outlinedButtonColors(
            contentColor = CyanPrimary
          )
        ) {
          if (isTestingConnection) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              color = CyanPrimary,
              strokeWidth = 2.dp
            )
          } else {
            Text("Test Connection")
          }
        }

        Button(
          onClick = onSetupComplete,
          modifier = Modifier.weight(1f),
          enabled = connectionStatus?.success == true,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = CyanPrimary,
            contentColor = BackgroundDark,
            disabledContainerColor = PurpleDark,
            disabledContentColor = TextMuted
          )
        ) {
          Text("Save & Connect")
        }
      }
    }
  }
}

@Composable
fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
  focusedBorderColor = CyanPrimary,
  unfocusedBorderColor = PurpleDark,
  cursorColor = CyanPrimary,
  focusedLabelColor = CyanPrimary,
  unfocusedLabelColor = TextMuted,
  focusedTextColor = TextPrimary,
  unfocusedTextColor = TextPrimary
)

data class ConnectionTestResult(
  val success: Boolean,
  val message: String
)
