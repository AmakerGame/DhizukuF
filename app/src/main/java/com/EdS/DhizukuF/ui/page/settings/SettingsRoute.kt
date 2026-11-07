package com.EdS.DhizukuF.ui.page.settings

sealed class SettingsRoute(val route: String) {
    data object Home : SettingsRoute("home")
    data object AppManagement : SettingsRoute("app_management")
    data object UserManagement : SettingsRoute("user_management")
    data object AccountManagement : SettingsRoute("account_management/{id}") {
        fun route(userId: Int) = "account_management/$userId"
    }
    data object Settings : SettingsRoute("settings")
    data object Security : SettingsRoute("security")
    data object Logs : SettingsRoute("logs")
    data object Customization : SettingsRoute("customization")
    data object LogSettings : SettingsRoute("settings/logs")
    data object OssLicenses : SettingsRoute("oss_licenses")
    data object Plugins : SettingsRoute("plugins")
    /** Plugin-provided Home/Settings entry. Args are URL-encoded. */
    data object PluginEntry : SettingsRoute("plugin_entry?title={title}&subtitle={subtitle}&route={route}") {
        fun route(title: String, subtitle: String, route: String): String {
            fun enc(s: String) = java.net.URLEncoder.encode(s, Charsets.UTF_8.name())
            return "plugin_entry?title=${enc(title)}&subtitle=${enc(subtitle)}&route=${enc(route)}"
        }
    }
    data object Activate : SettingsRoute("activate/{mode}") {
        enum class Mode {
            Dhizuku,
            Shizuku;
        }

        fun route(mode: Mode) = "activate/${mode.name}"
    }
}
