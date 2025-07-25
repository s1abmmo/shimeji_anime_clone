package space.anitama.anitama

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import space.anitama.anitama.ui.screens.LanguageSelectionScreen
import space.anitama.anitama.ui.MainActivity
import space.anitama.anitama.data.repository.SettingsRepository

class LanguageSelectionActivity : ComponentActivity() {
    private val settingsRepository by lazy { SettingsRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                LanguageSelectionScreen(
                    onLanguageSelected = { language ->
                        // Lưu ngôn ngữ và chuyển sang MainActivity
                        CoroutineScope(Dispatchers.IO).launch {
                            settingsRepository.saveLanguage(language.name.lowercase())
                            startActivity(Intent(this@LanguageSelectionActivity, MainActivity::class.java))
                            finish()
                        }
                    }
                )
            }
        }
    }
}