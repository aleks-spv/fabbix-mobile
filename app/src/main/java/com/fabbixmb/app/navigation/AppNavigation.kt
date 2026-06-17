package com.fabbixmb.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fabbixmb.app.presentation.login.LoginScreen
import com.fabbixmb.app.presentation.main.MainScreen
import com.fabbixmb.app.presentation.servers.AddEditServerScreen
import com.fabbixmb.app.presentation.servers.ServerListScreen
import com.fabbixmb.app.presentation.settings.SettingsScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController, startDestination = Screen.ServerList.route) {
        composable(Screen.ServerList.route) {
            ServerListScreen(
                onAddServer = { navController.navigate(Screen.AddServer.route) },
                onEditServer = { id -> navController.navigate(Screen.EditServer.createRoute(id)) },
                onServerSelected = { id -> navController.navigate(Screen.Login.createRoute(id)) },
                onSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.AddServer.route) {
            AddEditServerScreen(serverId = null, onBack = { navController.popBackStack() })
        }

        composable(
            Screen.EditServer.route,
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: return@composable
            AddEditServerScreen(serverId = id, onBack = { navController.popBackStack() })
        }

        composable(
            Screen.Login.route,
            arguments = listOf(navArgument("serverId") { type = NavType.IntType })
        ) { backStackEntry ->
            val serverId = backStackEntry.arguments?.getInt("serverId") ?: return@composable
            LoginScreen(
                serverId = serverId,
                onLoginSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.ServerList.route) { inclusive = false }
                    }
                }
            )
        }

        composable(Screen.Main.route) {
            MainScreen(
                onNavigateToServers = {
                    navController.navigate(Screen.ServerList.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                },
                onSessionExpired = { serverId ->
                    navController.navigate(Screen.Login.createRoute(serverId)) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
