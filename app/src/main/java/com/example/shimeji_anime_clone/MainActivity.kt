package com.example.shimeji_anime_clone

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button

class MainActivity : AppCompatActivity() {

    private val overlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    // Quyền đã được cấp, khởi chạy service
                    startOverlayService()
                } else {
                    // Quyền chưa được cấp, thông báo cho người dùng
                    // Toast.makeText(this, "Quyền overlay chưa được cấp", Toast.LENGTH_SHORT).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Giả sử bạn có layout cho MainActivity

        // Nút để bắt đầu/dừng overlay (ví dụ)
        val startButton = findViewById<Button>(R.id.button_start_overlay) // Thay ID phù hợp
        startButton.setOnClickListener {
            checkAndRequestOverlayPermission()
        }

        val stopButton = findViewById<Button>(R.id.button_stop_overlay) // Thay ID phù hợp
        stopButton.setOnClickListener {
            stopOverlayService()
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
            startOverlayService() // Không cần quyền cho các phiên bản Android cũ hơn
        }
    }

    private fun startOverlayService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            // Toast.makeText(this, "Quyền overlay chưa được cấp", Toast.LENGTH_SHORT).show()
            return
        }
        startService(Intent(this, OverlayService::class.java))
    }

    private fun stopOverlayService() {
        stopService(Intent(this, OverlayService::class.java))
    }
}