package com.bluebubbles.messaging.data.models

data class ServerConfig(
  val serverUrl: String,
  val password: String,
  val useHttps: Boolean = true,
  val port: Int = 1234
) {
  val baseUrl: String
    get() = buildString {
      append(if (useHttps) "https://" else "http://")
      append(serverUrl)
      if (port != 443 && port != 80) {
        append(":$port")
      }
    }
}

data class ServerInfo(
  val serverVersion: String,
  val osVersion: String,
  val macOsVersion: String,
  val isConnected: Boolean,
  val proxyService: String?,
  val helperConnected: Boolean
)

data class ServerStatus(
  val isOnline: Boolean,
  val lastPingTime: Long,
  val latencyMs: Int,
  val fcmConnected: Boolean,
  val socketConnected: Boolean
)

sealed class ConnectionState {
  object Disconnected : ConnectionState()
  object Connecting : ConnectionState()
  object Connected : ConnectionState()
  data class Error(val message: String) : ConnectionState()
}
