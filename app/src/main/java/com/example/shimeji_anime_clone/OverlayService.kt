package com.example.anitama

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import kotlin.random.Random

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private lateinit var characterImage: ImageView
    private val handler = Handler(Looper.getMainLooper())

    // Tốc độ
    private val MOVE_SPEED = 20 // Tốc độ di chuyển (pixel)
    private val ANIMATION_SPEED = 300L // Tốc độ đổi hình (ms)

    // Kích thước
    private var screenWidth = 0
    private var screenHeight = 0
    private var characterWidth = 0
    private var characterHeight = 0

    // Di chuyển
    private var targetX = 0 // Điểm đến
    private var facingRight = true // Hướng mặt

    // Hình ảnh đi bộ
    private val walkFrames = listOf(R.drawable.walk_frame_1, R.drawable.walk_frame_2)
    private var currentFrame = 0 // Hình hiện tại

    // Cập nhật di chuyển và hình ảnh
    private val updateRunnable = object : Runnable {
        override fun run() {
            moveCharacter() // Di chuyển nhân vật
            updateAnimation() // Cập nhật hình ảnh
            handler.postDelayed(this, ANIMATION_SPEED) // Lặp lại
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        setupOverlay() // Tạo nhân vật
        handler.postDelayed({ setNewTarget() }, 500) // Bắt đầu di chuyển
    }

    // Tạo nhân vật trên màn hình
    private fun setupOverlay() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        getScreenSize() // Lấy kích thước màn hình

        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.overlay_layout, null)

        overlayView?.let { view ->
            characterImage = view.findViewById(R.id.character_image_view)
            characterImage.setImageResource(R.drawable.walk_frame_1)
//            view.setBackgroundColor(0xFFFF0000.toInt()) // Màu đỏ

            val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )

            params.gravity = Gravity.TOP or Gravity.START
            params.x = screenWidth / 2 // Ở giữa
            params.y = screenHeight - 40 // Tạm đặt sát đáy (40dp)

            windowManager.addView(view, params)

            // Lấy kích thước nhân vật và đặt sát đáy
            view.viewTreeObserver.addOnGlobalLayoutListener {
                characterWidth = view.width
                characterHeight = view.height
                val layoutParams = view.layoutParams as WindowManager.LayoutParams
                layoutParams.y = screenHeight - characterHeight // Đặt sát đáy
                windowManager.updateViewLayout(view, layoutParams)
                view.viewTreeObserver.removeOnGlobalLayoutListener { }
            }

            handler.post(updateRunnable) // Bắt đầu cập nhật
        } ?: run {
            stopSelf() // Dừng nếu overlayView null
        }
    }

    // Lấy kích thước màn hình
    private fun getScreenSize() {
        val displayMetrics = resources.displayMetrics
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels
    }

    // Chọn điểm đến mới
    private fun setNewTarget() {
        overlayView?.let { view ->
            val layoutParams = view.layoutParams as WindowManager.LayoutParams
            val currentX = layoutParams.x
            val moveDistance = Random.nextInt(50, 500) // Di chuyển 50-200 pixel
            val moveRight = Random.nextBoolean()

            targetX = if (moveRight) currentX + moveDistance else currentX - moveDistance
            if (targetX < 0) targetX = 0 // Không ra ngoài trái
            if (targetX > screenWidth - characterWidth) targetX = screenWidth - characterWidth // Không ra ngoài phải

            facingRight = targetX > currentX // Cập nhật hướng mặt
            characterImage.scaleX = if (facingRight) 1f else -1f // Lật hình
        }
    }

    // Di chuyển nhân vật
    private fun moveCharacter() {
        overlayView?.let { view ->
            val layoutParams = view.layoutParams as WindowManager.LayoutParams
            val currentX = layoutParams.x

            if (Math.abs(currentX - targetX) < MOVE_SPEED) {
                layoutParams.x = targetX // Đến đích
                handler.postDelayed({ setNewTarget() }, 500) // Chọn điểm mới
            } else {
                val direction = if (targetX > currentX) MOVE_SPEED else -MOVE_SPEED
                layoutParams.x = currentX + direction // Di chuyển
            }

            layoutParams.y = screenHeight - characterHeight // Luôn sát đáy
            windowManager.updateViewLayout(view, layoutParams)
        }
    }

    // Cập nhật hình ảnh đi bộ
    private fun updateAnimation() {
        currentFrame = (currentFrame + 1) % walkFrames.size
        characterImage.setImageResource(walkFrames[currentFrame])
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable)
        overlayView?.let {
            windowManager.removeView(it)
            overlayView = null
        }
    }
}