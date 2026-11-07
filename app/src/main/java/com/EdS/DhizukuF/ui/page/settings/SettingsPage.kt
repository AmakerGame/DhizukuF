package com.EdS.DhizukuF.ui.page.settings

import com.EdS.DhizukuF.ui.page.settings.logs.LogSettingsPage
import com.EdS.DhizukuF.ui.page.settings.oss.OssLicensesPage

import com.EdS.DhizukuF.ui.page.settings.customization.CustomizationPage
import com.EdS.DhizukuF.ui.page.settings.logs.LogsPage

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument

import com.EdS.DhizukuF.ui.page.settings.activate.ActivatePage
import com.EdS.DhizukuF.ui.page.settings.account_manager.AccountManagerPage
import com.EdS.DhizukuF.ui.page.settings.app_management.AppManagementPage
import com.EdS.DhizukuF.ui.page.settings.home.HomePage
import com.EdS.DhizukuF.ui.page.settings.security.SecurityPage
import com.EdS.DhizukuF.plugin.ui.PluginsPage
import com.EdS.DhizukuF.plugin.ui.PluginEntryPage
import java.net.URLDecoder
import com.EdS.DhizukuF.ui.page.settings.settings.SettingsPage
import com.EdS.DhizukuF.ui.page.settings.user_manager.UserManagerPage

@Composable
fun SettingsPage(windowInsets: WindowInsets) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = SettingsRoute.Home.route,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300)) +
                    fadeIn(tween(300))
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300)) +
                    fadeOut(tween(300))
        },
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300)) +
                    fadeIn(tween(300))
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300)) +
                    fadeOut(tween(300))
        }
    ) {
        composable(route = SettingsRoute.Home.route) {
            HomePage(
                windowInsets = windowInsets,
                navController = navController
            )
        }
        composable(route = SettingsRoute.AppManagement.route) {
            AppManagementPage(
                windowInsets = windowInsets,
                navController = navController
            )
        }
        composable(route = SettingsRoute.UserManagement.route) {
            UserManagerPage(
                windowInsets = windowInsets,
                onNavigateToAccount = { userId ->
                    navController.navigate(SettingsRoute.AccountManagement.route(userId))
                },
                onBack = navController::navigateUp
            )
        }
        composable(
            route = SettingsRoute.AccountManagement.route,
            arguments = listOf(
                navArgument("id") {
                    type = NavType.IntType
                }
            )
        ) {
            val userId = it.arguments?.getInt("id")
            if (userId == null) {
                navController.navigateUp()
                return@composable
            }
            AccountManagerPage(
                windowInsets = windowInsets,
                userId = userId,
                onBack = navController::navigateUp
            )
        }
        composable(route = SettingsRoute.Security.route) {
            SecurityPage(
                windowInsets = windowInsets,
                navController = navController
            )
        }
        composable(route = SettingsRoute.Plugins.route) {
            PluginsPage(
                onBack = navController::navigateUp
            )
        }
        composable(
            route = "plugin_entry?title={title}&subtitle={subtitle}&route={route}",
            arguments = listOf(
                navArgument("title") { type = NavType.StringType; defaultValue = "" },
                navArgument("subtitle") { type = NavType.StringType; defaultValue = "" },
                navArgument("route") { type = NavType.StringType; defaultValue = "" }
            )
        ) { entry ->
            fun dec(s: String?) = URLDecoder.decode(s ?: "", Charsets.UTF_8.name())
            PluginEntryPage(
                title = dec(entry.arguments?.getString("title")),
                subtitle = dec(entry.arguments?.getString("subtitle")),
                route = dec(entry.arguments?.getString("route")),
                onBack = navController::navigateUp
            )
        }
        composable(route = SettingsRoute.Customization.route) {
            CustomizationPage(
                windowInsets = windowInsets,
                navController = navController
            )
        }
        composable(route = SettingsRoute.Logs.route) {
            LogsPage(
                windowInsets = windowInsets,
                navController = navController
            )
        }
        composable(route = SettingsRoute.LogSettings.route) {
            LogSettingsPage(
                windowInsets = windowInsets,
                navController = navController
            )
        }
        composable(route = SettingsRoute.OssLicenses.route) {
            OssLicensesPage(
                windowInsets = windowInsets,
                navController = navController
            )
        }
        composable(route = SettingsRoute.Settings.route) {
            SettingsPage(
                windowInsets = windowInsets,
                navController = navController
            )
        }
        composable(route = SettingsRoute.Activate.route) {
            val mode = it.arguments?.getString("mode")?.let { name ->
                SettingsRoute.Activate.Mode.valueOf(name)
            } ?: SettingsRoute.Activate.Mode.Dhizuku

            ActivatePage(
                windowInsets = windowInsets,
                navController = navController,
                mode = mode
            )
        }
    }
}