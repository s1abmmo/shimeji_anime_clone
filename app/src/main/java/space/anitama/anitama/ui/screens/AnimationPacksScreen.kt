package space.anitama.anitama.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import space.anitama.anitama.viewmodel.AnimationViewModel
import space.anitama.anitama.data.model.AnimationPack
import space.anitama.anitama.R

@Composable
fun AnimationPacksScreen(navController: NavController) {
    val viewModel: AnimationViewModel = viewModel()
    val packs by viewModel.animationPacks.collectAsState()

    // Gọi API khi vào màn hình
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            viewModel.fetchAnimationPacks()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(packs, key = { it.index }) { pack ->
            AnimationPackItem(pack, navController)
        }
    }
}

@Composable
fun AnimationPackItem(pack: AnimationPack, navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(4.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White)
            .border(1.dp, Color.Black, RoundedCornerShape(4.dp))
            .clickable { navController.navigate("animationDetails/${pack.modelName}") }
    ) {
        // Ảnh nền từ pngBackgroundLink
        AsyncImage(
            model = "https://anitama.space${pack.pngBackgroundLink}",
            contentDescription = "Background for ${pack.modelName}",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cột 1: Ảnh demo
            AsyncImage(
                model = "https://anitama.space${pack.pngDemoLink}",
                contentDescription = "Demo for ${pack.modelName}",
                modifier = Modifier
                    .size(150.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Fit
            )

            // Cột 2: Nút download và tên model
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.End
            ) {
                // Nút download
                Button(
                    onClick = { /* Xử lý download zipUrl */ },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF0288D1), // Xanh
                                    Color(0xFFAB47BC), // Tím
                                    Color(0xFFE91E63)  // Hồng
                                )
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_download), // Giả định có icon download
                            contentDescription = "Download Icon",
                            tint = Color.White
                        )
                        Text(
                            text = "Download",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Thẻ tên
                Text(
                    text = pack.modelName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}