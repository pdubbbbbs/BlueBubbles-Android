package com.bluebubbles.messaging.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bluebubbles.messaging.data.api.BlueBubblesApi
import com.bluebubbles.messaging.data.api.FcmRegisterRequest
import com.bluebubbles.messaging.data.models.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "server_config")

@Singleton
class ServerRepository @Inject constructor(
  @ApplicationContext private val context: Context,
  private val api: BlueBubblesApi
) {

  companion object {
    private val KEY_SERVER_URL = stringPreferencesKey("server_url")
    private val KEY_PASSWORD = stringPreferencesKey("password")
    private val KEY_USE_HTTPS = stringPreferencesKey("use_https")
    private val KEY_PORT = stringPreferencesKey("port")
  }

  val serverConfigFlow: Flow<ServerConfig?> = context.dataStore.data.map { prefs ->
    val url = prefs[KEY_SERVER_URL] ?: return@map null
    val password = prefs[KEY_PASSWORD] ?: return@map null
    ServerConfig(
      serverUrl = url,
      password = password,
      useHttps = prefs[KEY_USE_HTTPS]?.toBoolean() ?: true,
      port = prefs[KEY_PORT]?.toIntOrNull() ?: 1234
    )
  }

  suspend fun saveServerConfig(config: ServerConfig) {
    context.dataStore.edit { prefs ->
      prefs[KEY_SERVER_URL] = config.serverUrl
      prefs[KEY_PASSWORD] = config.password
      prefs[KEY_USE_HTTPS] = config.useHttps.toString()
      prefs[KEY_PORT] = config.port.toString()
    }
  }

  suspend fun getServerConfig(): ServerConfig? {
    return serverConfigFlow.first()
  }

  suspend fun clearServerConfig() {
    context.dataStore.edit { it.clear() }
  }

  suspend fun testConnection(): Result<ServerInfo> {
    return try {
      val response = api.getServerInfo()
      if (response.isSuccessful && response.body()?.data != null) {
        val data = response.body()!!.data!!
        Result.success(
          ServerInfo(
            serverVersion = data.serverVersion,
            osVersion = data.osVersion,
            macOsVersion = data.osVersion,
            isConnected = true,
            proxyService = data.proxyService,
            helperConnected = data.helperConnected
          )
        )
      } else {
        Result.failure(Exception("Connection test failed: ${response.message()}"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun ping(): Result<Boolean> {
    return try {
      val response = api.ping()
      Result.success(response.isSuccessful && response.body()?.data?.pong == true)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun registerFcmToken(deviceId: String, deviceName: String, token: String): Result<Unit> {
    return try {
      val request = FcmRegisterRequest(
        deviceId = deviceId,
        deviceName = deviceName,
        token = token
      )
      val response = api.registerFcmDevice(request)
      if (response.isSuccessful) {
        Result.success(Unit)
      } else {
        Result.failure(Exception("FCM registration failed: ${response.message()}"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
