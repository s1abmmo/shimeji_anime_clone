package space.anitama.anitama.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import space.anitama.anitama.viewmodel.AnimationViewModel
import space.anitama.anitama.data.model.AnimationPack

@Composable
fun AnimationDetailsScreen(navController: NavController, modelName: String?) {
    val viewModel: AnimationViewModel = viewModel()
    val pack = viewModel.animationPacks.collectAsState().value.find { it.modelName == modelName }

    if (pack != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Model: ${pack.modelName}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            AsyncImage(
                model = "https://anitama.space${pack.pngDemoLink}",
                contentDescription = "Demo for ${pack.modelName}",
                modifier = Modifier.size(200.dp)
            )
            // Thêm các chi tiết khác nếu cần
        }
    } else {
        Text(
            text = "Model không tìm thấy",
            modifier = Modifier.padding(16.dp)
        )
    }
}