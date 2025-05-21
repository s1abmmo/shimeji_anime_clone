package com.example.anitama

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val settingsRepository by lazy { SettingsRepository(this) }

    private val overlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    startOverlayService()
                } else {
                    // Có thể thêm thông báo nếu cần
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainScreen(
                    onStartOverlay = { checkAndRequestOverlayPermission() },
                    onStopOverlay = { stopOverlayService() },
                    settingsRepository = settingsRepository
                )
            }
        }
    }

    private fun checkAndRequestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                overlayPermissionLauncher.launch(intent)
            } else {
                startOverlayService()
            }
        } else {
            startOverlayService()
        }
    }

    private fun startOverlayService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            return
        }
        startService(Intent(this, OverlayService::class.java))
    }

    private fun stopOverlayService() {
        stopService(Intent(this, OverlayService::class.java))
    }
}

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
                OverlayScreen(onStartOverlay, onStopOverlay, settingsRepository)
            }
            composable("settings") {
                SettingsScreen()
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController, currentDestination: String) {
    NavigationBar(
        containerColor = Color.White,
        modifier = Modifier
            .shadow(8.dp, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
    ) {
        NavigationBarItem(
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_overlay), // Thay bằng icon overlay
                    contentDescription = "Overlay",
                    modifier = Modifier.size(32.dp)
                )
            },
            label = { Text("Overlay") },
            selected = currentDestination == "overlay",
            onClick = { navController.navigate("overlay") { popUpTo(navController.graph.startDestinationId) } }
        )
        NavigationBarItem(
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_settings), // Thay bằng icon settings
                    contentDescription = "Settings",
                    modifier = Modifier.size(32.dp)
                )
            },
            label = { Text("Settings") },
            selected = currentDestination == "settings",
            onClick = { navController.navigate("settings") { popUpTo(navController.graph.startDestinationId) } }
        )
    }
}

@Composable
fun OverlayScreen(
    onStartOverlay: () -> Unit,
    onStopOverlay: () -> Unit,
    settingsRepository: SettingsRepository
) {
    var language by remember { mutableStateOf("Chưa chọn") }

    LaunchedEffect(Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            settingsRepository.languageFlow.collectLatest { lang ->
                language = lang
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Overlay Control",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1976D2)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Ngôn ngữ: $language",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF424242)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onStartOverlay,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
        ) {
            Text("Bắt đầu Overlay", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = onStopOverlay,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(2.dp, Color(0xFF1976D2))
        ) {
            Text(
                "Dừng Overlay",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1976D2)
            )
        }
    }
}

@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1976D2)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Chức năng cài đặt sẽ được thêm sau",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF424242)
        )
    }
}