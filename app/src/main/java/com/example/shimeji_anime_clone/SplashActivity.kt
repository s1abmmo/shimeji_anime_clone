package com.example.anitama

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SplashActivity : ComponentActivity() {
    private val settingsRepository by lazy { SettingsRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Cài đặt Splash Screen trước super.onCreate
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Giữ Splash Screen cho đến khi kiểm tra xong
        var keepSplashOnScreen = true
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }

        // Kiểm tra trạng thái ngôn ngữ
        CoroutineScope(Dispatchers.Main).launch {
            val isLanguageSet = settingsRepository.isLanguageSetFlow.first()

            // Điều hướng dựa trên trạng thái ngôn ngữ
            val intent = if (isLanguageSet) {
                Intent(this@SplashActivity, MainActivity::class.java)
            } else {
                Intent(this@SplashActivity, LanguageSelectionActivity::class.java)
            }

            // Tắt Splash Screen và chuyển màn hình
            keepSplashOnScreen = false
            startActivity(intent)
            finish()
        }
    }
}