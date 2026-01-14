package com.bluebubbles.messaging.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bluebubbles.messaging.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  onBackClick: () -> Unit,
  onServerSetupClick: () -> Unit
) {
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
            "Settings",
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
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Connection Section
      SettingsSection(title = "Connection") {
        SettingsItem(
          icon = Icons.Default.Cloud,
          title = "Server Setup",
          subtitle = "Configure BlueBubbles server connection",
          onClick = onServerSetupClick
        )
        SettingsItem(
          icon = Icons.Default.Notifications,
          title = "Push Notifications",
          subtitle = "Firebase Cloud Messaging settings",
          onClick = { }
        )
      }

      // Appearance Section
      SettingsSection(title = "Appearance") {
        SettingsItem(
          icon = Icons.Default.Palette,
          title = "Theme",
          subtitle = "BlueBubbles Dark",
          onClick = { }
        )
        SettingsItem(
          icon = Icons.Default.TextFields,
          title = "Font Size",
          subtitle = "Medium",
          onClick = { }
        )
      }

      // Privacy Section
      SettingsSection(title = "Privacy & Security") {
        SettingsItem(
          icon = Icons.Default.Lock,
          title = "App Lock",
          subtitle = "Require authentication to open app",
          onClick = { }
        )
        SettingsItem(
          icon = Icons.Default.Visibility,
          title = "Read Receipts",
          subtitle = "Let others know when you've read messages",
          onClick = { }
        )
      }

      // About Section
      SettingsSection(title = "About") {
        SettingsItem(
          icon = Icons.Default.Info,
          title = "Version",
          subtitle = "1.0.0",
          showArrow = false,
          onClick = { }
        )
        SettingsItem(
          icon = Icons.Default.Code,
          title = "Source Code",
          subtitle = "github.com/pdubbbbbs/bluebubbles-android",
          onClick = { }
        )
        SettingsItem(
          icon = Icons.Default.Policy,
          title = "Privacy Policy",
          subtitle = "View our privacy policy",
          onClick = { }
        )
      }

      // Developer Section
      SettingsSection(title = "Developer") {
        SettingsItem(
          icon = Icons.Default.BugReport,
          title = "Debug Logs",
          subtitle = "View and export debug logs",
          onClick = { }
        )
        SettingsItem(
          icon = Icons.Default.Refresh,
          title = "Clear Cache",
          subtitle = "Free up storage space",
          onClick = { }
        )
      }

      Spacer(modifier = Modifier.height(32.dp))

      // App info footer
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "BlueBubbles for Android",
          style = MaterialTheme.typography.bodyMedium,
          color = TextMuted
        )
        Text(
          text = "Made with love by Always Under, Inc.",
          style = MaterialTheme.typography.bodySmall,
          color = TextMuted
        )
      }
    }
  }
}

@Composable
fun SettingsSection(
  title: String,
  content: @Composable ColumnScope.() -> Unit
) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.SemiBold,
      color = CyanPrimary,
      modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
    )

    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(
        containerColor = CardBackground.copy(alpha = 0.6f)
      )
    ) {
      Column(
        modifier = Modifier.padding(vertical = 8.dp),
        content = content
      )
    }
  }
}

@Composable
fun SettingsItem(
  icon: ImageVector,
  title: String,
  subtitle: String,
  showArrow: Boolean = true,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 12.dp),
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(40.dp)
        .clip(RoundedCornerShape(10.dp))
        .background(
          Brush.linearGradient(
            listOf(CyanPrimary.copy(alpha = 0.2f), PurplePrimary.copy(alpha = 0.2f))
          )
        ),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = CyanPrimary,
        modifier = Modifier.size(22.dp)
      )
    }

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium,
        color = TextPrimary
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = TextMuted
      )
    }

    if (showArrow) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = TextMuted
      )
    }
  }
}
