package com.fabbixmb.app.navigation

sealed class Screen(val route: String) {
    object ServerList : Screen("servers")
    object AddServer  : Screen("servers/add")
    object EditServer : Screen("servers/edit/{id}") {
        fun createRoute(id: Int) = "servers/edit/$id"
    }
    object Login : Screen("login/{serverId}") {
        fun createRoute(serverId: Int) = "login/$serverId"
    }
    object Main      : Screen("main")
    object Settings  : Screen("settings")
}