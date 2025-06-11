package com.example.anitama

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween

// Data classes cho animation
data class AnimationPack(
    val name: String,
    val animations: List<Animation>
)

data class Animation(
    val name: String,
    val images: List<String>
)

// Hàm mock dữ liệu
fun getMockAnimationPacks(): List<AnimationPack> {
    return listOf(
        AnimationPack(
            name = "Pack 1",
            animations = (1..6).map { i ->
                Animation(
                    name = "Animation $i",
                    images = (1..3).map { j -> "url_pack1_anim${i}_img$j.png" }
                )
            }
        ),
        AnimationPack(
            name = "Pack 2",
            animations = (1..6).map { i ->
                Animation(
                    name = "Animation $i",
                    images = (1..3).map { j -> "url_pack2_anim${i}_img$j.png" }
                )
            }
        )
    )
}

// ViewModel để quản lý dữ liệu animation
class AnimationViewModel : ViewModel() {
    val animationPacks = getMockAnimationPacks()
}

class MainActivity : ComponentActivity() {
    private val settingsRepository by lazy { SettingsRepository(this) }

    private val overlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    startOverlayService()
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
                    painter = painterResource(id = R.drawable.ic_overlay),
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
                    painter = painterResource(id = R.drawable.ic_settings),
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
    settingsRepository: SettingsRepository,
    navController: NavController
) {
    var language by remember { mutableStateOf("Chưa chọn") }
    var isOverlayActive by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            settingsRepository.languageFlow.collectLatest { lang ->
                language = lang
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFB1B1),
                        Color(0xFF87CEEB),
                        Color(0xFFE6E6FA)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFE91E63),
                                Color(0xFF0288D1),
                                Color(0xFFAB47BC)
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Cấp quyền",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF333333)
                    )
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(30.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(
                                if (isOverlayActive) Color(0xFFE91E63) else Color(0xFFE0E0E0)
                            )
                            .clickable {
                                isOverlayActive = !isOverlayActive
                                if (isOverlayActive) onStartOverlay() else onStopOverlay()
                            }
                            .padding(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .align(if (isOverlayActive) Alignment.CenterEnd else Alignment.CenterStart)
                                .animateContentSize(
                                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                                )
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { navController.navigate("animationPacks") },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
            ) {
                Text("Thêm", color = Color.White)
            }
        }
    }
}

@Composable
fun AnimationPacksScreen(navController: NavController) {
    val viewModel: AnimationViewModel = viewModel()
    val packs = viewModel.animationPacks

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        items(packs) { pack ->
            Text(
                text = pack.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate("animationDetails/${pack.name}") }
                    .padding(16.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun AnimationDetailsScreen(navController: NavController, packName: String?) {
    val viewModel: AnimationViewModel = viewModel()
    val pack = viewModel.animationPacks.find { it.name == packName }

    if (pack != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Pack: ${pack.name}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn {
                items(pack.animations) { animation ->
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = animation.name,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Row {
                            animation.images.forEach { imageUrl ->
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .background(Color.Gray)
                                        .padding(4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        Text(
            text = "Pack không tìm thấy",
            modifier = Modifier.padding(16.dp)
        )
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