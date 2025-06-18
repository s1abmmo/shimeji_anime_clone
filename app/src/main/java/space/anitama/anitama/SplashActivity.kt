package space.anitama.anitama

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import space.anitama.anitama.ui.MainActivity
import space.anitama.anitama.data.repository.SettingsRepository

class SplashActivity : ComponentActivity() {
    private val settingsRepository by lazy { SettingsRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Cài đặt Splash Screen trước super.onCreate
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Biến để kiểm soát thời gian hiển thị
        var isReadyToNavigate = false
        var isLanguageChecked = false

        // Giữ Splash Screen cho đến khi cả hai điều kiện được thỏa mãn
        splashScreen.setKeepOnScreenCondition { !isReadyToNavigate }

        // Kiểm tra ngôn ngữ và trì hoãn tối thiểu 3 giây
        CoroutineScope(Dispatchers.Main).launch {
            // Kiểm tra trạng thái ngôn ngữ
            val isLanguageSet = settingsRepository.isLanguageSetFlow.first()
            isLanguageChecked = true

            // Đợi tối thiểu 3 giây kể từ khi bắt đầu
            delay(3000)

            // Cả hai điều kiện đã thỏa mãn, cho phép chuyển màn hình
            isReadyToNavigate = true

            // Điều hướng dựa trên trạng thái ngôn ngữ
            val intent = if (isLanguageSet) {
                Intent(this@SplashActivity, MainActivity::class.java)
            } else {
                Intent(this@SplashActivity, LanguageSelectionActivity::class.java)
            }

            startActivity(intent)
            finish()
        }
    }
}