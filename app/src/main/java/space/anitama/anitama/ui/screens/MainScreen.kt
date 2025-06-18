package space.anitama.anitama.ui.screens

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.navigation.compose.*
import androidx.compose.runtime.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import space.anitama.anitama.ui.screens.AnimationPacksScreen
import space.anitama.anitama.data.repository.SettingsRepository
import space.anitama.anitama.ui.navigation.BottomNavigationBar

@Composable
fun MainScreen(
    onStartOverlay: () -> Unit,
    onStopOverlay: () -> Unit,
    settingsRepository: SettingsRepository
) {
    val navController = rememberNavController()
    val currentRoute by navController.currentBackStackEntryAsState()
    val currentDestination = currentRoute?.destination?.route ?: "overlay"

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController, currentDestination)
        },
        modifier = Modifier.background(
            Brush.verticalGradient(
                colors = listOf(Color(0xFFE3F2FD), Color(0xFFBBDEFB))
            )
        )
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "overlay",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("overlay") {
                OverlayScreen(onStartOverlay, onStopOverlay, settingsRepository, navController)
            }
            composable("settings") {
                SettingsScreen()
            }
            composable("animationPacks") {
                AnimationPacksScreen(navController)
            }
            composable("animationDetails/{packName}") { backStackEntry ->
                val packName = backStackEntry.arguments?.getString("packName")
                AnimationDetailsScreen(navController, packName)
            }
        }
    }
}