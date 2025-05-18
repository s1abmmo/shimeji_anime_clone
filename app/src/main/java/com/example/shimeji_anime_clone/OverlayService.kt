package com.example.shimeji_anime_clone

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
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.annotation.DrawableRes

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private lateinit var characterImageView: ImageView
    private val handler = Handler(Looper.getMainLooper())
    private var currentAnimation = AnimationType.WALK
    private var currentFrame = 0
    private var isDragging = false
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    private val walkFrames = listOf(
        R.drawable.walk_frame_1,
        R.drawable.walk_frame_2,
        R.drawable.walk_frame_3
    )

    private val climbFrames = listOf(
        R.drawable.climb_frame_1,
        R.drawable.climb_frame_2,
        R.drawable.climb_frame_3
    )

    private enum class AnimationType {
        WALK, CLIMB
    }

    private val animationRunnable = object : Runnable {
        override fun run() {
            updateFrame()
            handler.postDelayed(this, 250) // 0.25 giây = 250 mili giây
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.overlay_layout, null) // Tạo file layout overlay_layout.xml

        overlayView?.let { view ->
            characterImageView = view.findViewById(R.id.character_image_view)

            val layoutFlag: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            )

            params.gravity = Gravity.TOP or Gravity.START
            params.x = 0
            params.y = 100 // Vị trí ban đầu

            windowManager.addView(view, params)
            startAnimation()

            // Cho phép di chuyển view
            view.setOnTouchListener { v, event ->
                val layoutParams = view.layoutParams as WindowManager.LayoutParams
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isDragging = false // Reset, chỉ set true khi có di chuyển đủ
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        true // Quan trọng: return true để nhận các event tiếp theo
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - initialTouchX
                        val dy = event.rawY - initialTouchY

                        // Chỉ coi là kéo nếu di chuyển một khoảng nhất định (ví dụ 5 pixel)
                        if (Math.abs(dx) > 5 || Math.abs(dy) > 5) {
                            isDragging = true
                        }

                        if (isDragging) {
                            layoutParams.x = initialX + dx.toInt()
                            layoutParams.y = initialY + dy.toInt()
                            windowManager.updateViewLayout(view, layoutParams)
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDragging) {
                            // Nếu không kéo mà chỉ click, thì đổi animation
                            switchAnimation()
                        }
                        isDragging = false
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun startAnimation() {
        currentFrame = 0
        handler.post(animationRunnable)
    }

    private fun stopAnimation() {
        handler.removeCallbacks(animationRunnable)
    }

    private fun updateFrame() {
        val frames = when (currentAnimation) {
            AnimationType.WALK -> walkFrames
            AnimationType.CLIMB -> climbFrames
        }
        currentFrame = (currentFrame + 1) % frames.size
        characterImageView.setImageResource(frames[currentFrame])
    }

    private fun switchAnimation() {
        currentAnimation = if (currentAnimation == AnimationType.WALK) {
            AnimationType.CLIMB
        } else {
            AnimationType.WALK
        }
        currentFrame = -1 // Để frame tiếp theo là frame đầu tiên của animation mới
        updateFrame() // Cập nhật ngay frame đầu tiên
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAnimation()
        overlayView?.let {
            windowManager.removeView(it)
            overlayView = null
        }
    }
}