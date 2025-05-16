package com.example.shimeji_anime_clone

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val OVERLAY_PERMISSION_REQ_CODE = 1234

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)
        } catch (e: Exception) {
            Toast.makeText(this, "Lỗi tải giao diện: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace() // This will help with debugging
            finish()
            return
        }

        val startButton = findViewById<Button>(R.id.startButton)
        val stopButton = findViewById<Button>(R.id.stopButton)

        startButton.setOnClickListener {
            if (checkOverlayPermission()) {
                startShimejiService()
            } else {
                requestOverlayPermission()
            }
        }

        stopButton.setOnClickListener {
            stopShimejiService()
        }
    }

    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE)
            } catch (e: Exception) {
                Toast.makeText(this, "Không thể mở cài đặt quyền overlay", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (checkOverlayPermission()) {
                startShimejiService()
            } else {
                Toast.makeText(this, "Quyền hiển thị trên các ứng dụng khác bị từ chối", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startShimejiService() {
        try {
            val intent = Intent(this, ShimejiService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            Toast.makeText(this, "Đã bắt đầu Shimeji", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Lỗi khi bắt đầu dịch vụ: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun stopShimejiService() {
        try {
            val intent = Intent(this, ShimejiService::class.java)
            stopService(intent)
            Toast.makeText(this, "Đã dừng Shimeji", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Lỗi khi dừng dịch vụ: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
}