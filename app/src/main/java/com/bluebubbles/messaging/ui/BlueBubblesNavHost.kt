package com.bluebubbles.messaging.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bluebubbles.messaging.ui.screens.chat.ChatScreen
import com.bluebubbles.messaging.ui.screens.conversations.ConversationsScreen
import com.bluebubbles.messaging.ui.screens.search.SearchScreen
import com.bluebubbles.messaging.ui.screens.settings.SettingsScreen
import com.bluebubbles.messaging.ui.screens.settings.ServerSetupScreen

sealed class Screen(val route: String) {
  object Conversations : Screen("conversations")
  object Chat : Screen("chat/{chatGuid}") {
    fun createRoute(chatGuid: String) = "chat/$chatGuid"
  }
  object Search : Screen("search")
  object Settings : Screen("settings")
  object ServerSetup : Screen("server_setup")
}

@Composable
fun BlueBubblesNavHost() {
  val navController = rememberNavController()

  NavHost(
    navController = navController,
    startDestination = Screen.Conversations.route
  ) {
    composable(Screen.Conversations.route) {
      ConversationsScreen(
        onConversationClick = { chatGuid ->
          navController.navigate(Screen.Chat.createRoute(chatGuid))
        },
        onSettingsClick = {
          navController.navigate(Screen.Settings.route)
        },
        onSearchClick = {
          navController.navigate(Screen.Search.route)
        }
      )
    }

    composable(Screen.Search.route) {
      SearchScreen(
        onBackClick = { navController.popBackStack() },
        onMessageClick = { chatGuid, _ ->
          navController.navigate(Screen.Chat.createRoute(chatGuid))
        }
      )
    }

    composable(
      route = Screen.Chat.route,
      arguments = listOf(
        navArgument("chatGuid") { type = NavType.StringType }
      )
    ) {
      ChatScreen(
        onBackClick = { navController.popBackStack() }
      )
    }

    composable(Screen.Settings.route) {
      SettingsScreen(
        onBackClick = { navController.popBackStack() },
        onServerSetupClick = { navController.navigate(Screen.ServerSetup.route) }
      )
    }

    composable(Screen.ServerSetup.route) {
      ServerSetupScreen(
        onBackClick = { navController.popBackStack() },
        onSetupComplete = {
          navController.popBackStack(Screen.Conversations.route, inclusive = false)
        }
      )
    }
  }
}
