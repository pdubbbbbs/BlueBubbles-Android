# BlueBubbles Android App - Development Progress

**Project:** BlueBubbles Android Client
**Status:** IN PROGRESS - Core Structure Complete
**Last Updated:** 2026-01-13
**Developer:** Philip S. Wright (pdubbbbbs)

---

## Project Overview

A native Android client for BlueBubbles iMessage bridge server, featuring:
- Modern Jetpack Compose UI
- MVVM architecture with Hilt DI
- Firebase Cloud Messaging for push notifications
- BlueGuard-inspired cyan/purple dark theme

---

## Completed Components

### Project Structure
- [x] Gradle configuration (Kotlin DSL)
- [x] App module with all dependencies
- [x] ProGuard rules for release builds

### Theme & Styling
- [x] Color palette matching BlueGuard (cyan/purple theme)
- [x] Typography system
- [x] Dark theme with gradient backgrounds
- [x] Adaptive launcher icons

### Data Layer
- [x] Data models (Conversation, Message, Participant, Attachment)
- [x] Server config models (ServerConfig, ServerInfo, ConnectionState)
- [x] BlueBubbles API interface (Retrofit)
- [x] API DTOs for JSON serialization
- [x] ChatRepository for conversation/message management
- [x] ServerRepository with DataStore persistence

### Dependency Injection
- [x] Hilt AppModule
- [x] OkHttpClient with logging
- [x] Retrofit instance

### ViewModels
- [x] ConversationsViewModel
- [x] ChatViewModel

### UI Screens
- [x] Navigation setup (NavHost)
- [x] ConversationsScreen (list, search, connection status)
- [x] ChatScreen (messages, input, reply)
- [x] SettingsScreen (categorized settings)
- [x] ServerSetupScreen (connection configuration)

### Firebase
- [x] BlueBubblesFirebaseService
- [x] Notification channels
- [x] Message handling (new-message, updated-message, etc.)

---

## Pending Tasks

### High Priority
- [ ] Room database for offline caching
- [ ] WebSocket connection for real-time updates
- [ ] QR code scanner for server setup
- [ ] Attachment sending/receiving

### Medium Priority
- [ ] Typing indicators
- [ ] Read receipts sync
- [ ] Contact integration
- [ ] Message search

### Low Priority
- [ ] Message reactions UI
- [ ] Message effects (slam, loud, etc.)
- [ ] Sticker support
- [ ] Audio messages

---

## Build Instructions

1. Open project in Android Studio Hedgehog or later
2. Copy `google-services.json` from Firebase setup to `app/` folder
3. Sync Gradle
4. Build and run on device/emulator

---

## File Structure

```
BlueBubbles-Android/
├── app/
│   ├── src/main/
│   │   ├── java/com/bluebubbles/messaging/
│   │   │   ├── BlueBubblesApp.kt
│   │   │   ├── MainActivity.kt
│   │   │   ├── data/
│   │   │   │   ├── api/
│   │   │   │   │   ├── BlueBubblesApi.kt
│   │   │   │   │   └── dto/ApiResponses.kt
│   │   │   │   ├── firebase/
│   │   │   │   │   └── BlueBubblesFirebaseService.kt
│   │   │   │   ├── models/
│   │   │   │   │   ├── Conversation.kt
│   │   │   │   │   └── ServerConfig.kt
│   │   │   │   └── repository/
│   │   │   │       ├── ChatRepository.kt
│   │   │   │       └── ServerRepository.kt
│   │   │   ├── di/
│   │   │   │   └── AppModule.kt
│   │   │   ├── ui/
│   │   │   │   ├── BlueBubblesNavHost.kt
│   │   │   │   ├── screens/
│   │   │   │   │   ├── chat/ChatScreen.kt
│   │   │   │   │   ├── conversations/ConversationsScreen.kt
│   │   │   │   │   └── settings/
│   │   │   │   │       ├── SettingsScreen.kt
│   │   │   │   │       └── ServerSetupScreen.kt
│   │   │   │   └── theme/
│   │   │   │       ├── Color.kt
│   │   │   │       ├── Theme.kt
│   │   │   │       └── Type.kt
│   │   │   └── viewmodel/
│   │   │       ├── ChatViewModel.kt
│   │   │       └── ConversationsViewModel.kt
│   │   ├── res/
│   │   │   ├── drawable/
│   │   │   ├── mipmap-anydpi-v26/
│   │   │   └── values/
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── SESSION_PROGRESS.md
```

---

## Technical Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin 1.9.21 |
| UI | Jetpack Compose |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt 2.48 |
| Networking | Retrofit + OkHttp |
| Storage | DataStore + Room (pending) |
| Push | Firebase Cloud Messaging |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 34 (Android 14) |

---

## Related Projects

- **BlueGuard iOS** - `/Users/dubs415/E/BlueGuard-iOS/` (Complete)
- **Firebase Setup Script** - `~/CLAUDE/bluebubbles-firebase-setup.sh`
- **BlueBubbles Proxmox Guide** - `/Users/dubs415/E/bluebubbles-proxmox-guide/`

---

## Next Session Tasks

1. Add Room database for offline message caching
2. Implement WebSocket connection for real-time updates
3. Add QR code scanner for easy server setup
4. Test with actual BlueBubbles server
