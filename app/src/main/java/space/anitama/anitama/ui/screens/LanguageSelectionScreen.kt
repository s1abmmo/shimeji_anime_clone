package space.anitama.anitama.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.anitama.anitama.R

data class Language(
    val name: String,
    val flagResId: Int // Resource ID của icon cờ
)

@Composable
fun LanguageSelectionScreen(
    onLanguageSelected: (Language) -> Unit
) {
    // Danh sách ngôn ngữ mẫu
    val languages = listOf(
        Language("Vietnamese", R.drawable.ic_close),
        Language("English", R.drawable.ic_close),
        Language("French", R.drawable.ic_close),
        Language("Japanese", R.drawable.ic_close),
        Language("Spanish", R.drawable.ic_close)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Select Language",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(languages) { language ->
                LanguageItem(
                    language = language,
                    onClick = { onLanguageSelected(language) }
                )
            }
        }
    }
}

@Composable
fun LanguageItem(
    language: Language,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = language.flagResId),
                contentDescription = "${language.name} flag",
                modifier = Modifier
                    .size(40.dp)
                    .padding(end = 12.dp)
            )
            Text(
                text = language.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
        }
    }
}