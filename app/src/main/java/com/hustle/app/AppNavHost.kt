package com.hustle.app

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hustle.feature.auth.AuthRoute
import com.hustle.feature.auth.presentation.AuthScreen
import com.hustle.feature.demo.DemoRoute
import com.hustle.feature.demo.presentation.DemoScreen

@Composable
fun AppNavHost(
    startDestination: Any,
    isAuthenticated: Boolean,
    onLogout: () -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable<DemoRoute> {
            DemoScreen(
                isAuthenticated = isAuthenticated,
                onSignIn = {
                    navController.navigate(AuthRoute) {
                        launchSingleTop = true
                    }
                },
                onSignOut = {
                    onLogout()
                    navController.navigate(AuthRoute) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable<AuthRoute> {
            AuthScreen(
                onAuthenticated = {
                    navController.navigate(DemoRoute) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onContinueAsGuest = {
                    navController.navigate(DemoRoute) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
    }
}
