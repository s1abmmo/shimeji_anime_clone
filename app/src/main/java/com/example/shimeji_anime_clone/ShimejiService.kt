package com.example.shimeji_anime_clone

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.airbnb.lottie.LottieAnimationView
import kotlin.math.abs
import kotlin.random.Random

class ShimejiService : Service() {

    private lateinit var windowManager: WindowManager
    private var shimejiView: View? = null
    private lateinit var shimejiParams: WindowManager.LayoutParams
    private lateinit var displayMetrics: DisplayMetrics
    private lateinit var shimejiAnimationView: LottieAnimationView

    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f
    private var isDragging: Boolean = false
    private var isAnimating: Boolean = false
    private var isClimbing: Boolean = false
    private val DRAG_THRESHOLD = 10f
    private val TAG = "ShimejiService"
    private val MARGIN = 0 // Biến để tùy chỉnh lề (px), 0 để bám sát hoàn toàn

    private val handler = Handler(Looper.getMainLooper())
    private val random = Random.Default
    private var currentAnimation: String = "walk"

    // Danh sách các animation
    private val animations = mapOf(
        "climb" to "climb.json",
        "walk" to "walk.json",
        "fall" to "fall.json",
        "drag" to "drag.json"
    )

    private val movementRunnable = object : Runnable {
        override fun run() {
            if (!isDragging && !isAnimating) {
                if (isClimbing) {
                    randomClimb()
                } else {
                    randomWalk()
                }
            }
            handler.postDelayed(this, 3000)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate started")

        // Kiểm tra quyền overlay
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Log.e(TAG, "No overlay permission, stopping service")
            stopSelf()
            return
        }

        try {
            // Thiết lập foreground service
            createNotificationChannel()

            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            updateDisplayMetrics()

            // Tải layout
            shimejiView = LayoutInflater.from(this).inflate(R.layout.shimeji_layout, null)
            shimejiAnimationView = shimejiView!!.findViewById(R.id.shimejiAnimationView)

            // Set animation mặc định
            setAnimation("walk")

            // Thiết lập thông số cho cửa sổ overlay
            val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }

            shimejiParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )

            shimejiParams.gravity = Gravity.TOP or Gravity.START
            shimejiParams.x = 0
            shimejiParams.y = 100

            // Xử lý sự kiện chạm
            shimejiView?.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = shimejiParams.x
                        initialY = shimejiParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = true
                        isClimbing = false // Thoát trạng thái leo khi kéo
                        handler.removeCallbacks(movementRunnable)
                        setAnimation("drag")
                        false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = abs(event.rawX - initialTouchX)
                        val deltaY = abs(event.rawY - initialTouchY)
                        if (deltaX > DRAG_THRESHOLD || deltaY > DRAG_THRESHOLD) {
                            shimejiParams.x = initialX + (event.rawX - initialTouchX).toInt()
                            shimejiParams.y = initialY + (event.rawY - initialTouchY).toInt()
                            // Giới hạn trong màn hình với MARGIN
                            shimejiParams.x = shimejiParams.x.coerceIn(0, displayMetrics.widthPixels - MARGIN)
                            shimejiParams.y = shimejiParams.y.coerceIn(0, displayMetrics.heightPixels - MARGIN)
                            try {
                                windowManager.updateViewLayout(shimejiView, shimejiParams)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error updating view layout: ${e.message}")
                            }
                        }
                        false
                    }
                    MotionEvent.ACTION_UP -> {
                        isDragging = false
                        fallAnimation()
                        false
                    }
                    else -> false
                }
            }

            // Thêm view vào WindowManager
            windowManager.addView(shimejiView, shimejiParams)
            Log.d(TAG, "ShimejiView added to window")

            // Bắt đầu chuyển động ngẫu nhiên
            handler.postDelayed(movementRunnable, 2000)
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}")
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "shimeji_channel",
                "Dịch vụ Shimeji",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)

            val notification = Notification.Builder(this, "shimeji_channel")
                .setContentTitle("Shimeji đang chạy")
                .setContentText("Shimeji đang hoạt động trên màn hình")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build()

            try {
                startForeground(1, notification)
                Log.d(TAG, "Started as foreground service")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting foreground: ${e.message}")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand")
        return START_STICKY
    }

    private fun updateDisplayMetrics() {
        displayMetrics = DisplayMetrics()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val display = windowManager.currentWindowMetrics
            val bounds = display.bounds
            displayMetrics.widthPixels = bounds.width()
            displayMetrics.heightPixels = bounds.height()
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(displayMetrics)
        }
        Log.d(TAG, "Display metrics updated: ${displayMetrics.widthPixels}x${displayMetrics.heightPixels}")
    }

    private fun setAnimation(animationKey: String) {
        if (currentAnimation != animationKey) {
            val animationFile = animations[animationKey]
            if (animationFile != null) {
                shimejiAnimationView.setAnimation(animationFile)
                shimejiAnimationView.playAnimation()
                currentAnimation = animationKey
                Log.d(TAG, "Switched to animation: $animationKey")
            } else {
                Log.e(TAG, "Animation not found for key: $animationKey")
            }
        }
    }

    private fun randomWalk() {
        if (isAnimating || isDragging || isClimbing) return
        isAnimating = true
        setAnimation("walk")

        val moveX = if (random.nextBoolean()) 150f else -150f // Di chuyển ngẫu nhiên trái hoặc phải
        val targetX = (shimejiParams.x + moveX).coerceIn(0f, (displayMetrics.widthPixels - MARGIN).toFloat())
        val distance = abs(targetX - shimejiParams.x)
        val speed = 100f // 100px mỗi giây
        val duration = (distance / speed * 1000).toLong().coerceAtMost(5000) // Tối đa 5 giây
        val steps = 30 // 30 FPS
        val stepDuration = duration / steps
        val stepDistance = (targetX - shimejiParams.x) / steps

        var currentStep = 0

        // Cập nhật vị trí từ từ
        val walkRunnable = object : Runnable {
            override fun run() {
                if (currentStep < steps && !isDragging && !isClimbing) {
                    shimejiParams.x += stepDistance.toInt()
                    shimejiParams.x = shimejiParams.x.coerceIn(0, displayMetrics.widthPixels - MARGIN)
                    try {
                        windowManager.updateViewLayout(shimejiView, shimejiParams)
                        Log.d(TAG, "Walking, current X: ${shimejiParams.x}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating view during walk: ${e.message}")
                    }
                    currentStep++
                    handler.postDelayed(this, stepDuration)
                } else {
                    shimejiParams.x = targetX.toInt()
                    try {
                        windowManager.updateViewLayout(shimejiView, shimejiParams)
                        Log.d(TAG, "Walk animation ended, new X: ${shimejiParams.x}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating view after walk: ${e.message}")
                    }
                    isAnimating = false

                    // Kiểm tra chạm tường để chuyển sang trạng thái leo
                    if (shimejiParams.x <= 0 || shimejiParams.x >= displayMetrics.widthPixels - MARGIN) {
                        isClimbing = true
                        randomClimb()
                    } else {
                        setAnimation("walk")
                    }
                }
            }
        }

        // Lật hướng nhân vật
        shimejiAnimationView.scaleX = if (moveX < 0) -1f else 1f
        Log.d(TAG, "Walk animation started, direction: ${if (moveX < 0) "left" else "right"}")
        handler.post(walkRunnable)
    }

    private fun fallAnimation() {
        if (isAnimating || isDragging) return
        updateDisplayMetrics()
        val groundY = displayMetrics.heightPixels - MARGIN // Vị trí đáy màn hình

        if (shimejiParams.y < groundY) {
            isAnimating = true
            isClimbing = false // Thoát trạng thái leo khi rơi
            setAnimation("fall")

            val fallDistance = (groundY - shimejiParams.y).toFloat()
            val fallSpeed = 100f // 100px mỗi giây
            val duration = (fallDistance / fallSpeed * 1000).toLong().coerceAtMost(5000) // Tối đa 5 giây
            val steps = 30 // 30 FPS
            val stepDuration = duration / steps
            val stepDistance = fallDistance / steps

            var currentStep = 0

            // Cập nhật vị trí từ từ
            val fallRunnable = object : Runnable {
                override fun run() {
                    if (currentStep < steps && shimejiParams.y < groundY && !isDragging) {
                        shimejiParams.y += stepDistance.toInt()
                        if (shimejiParams.y > groundY) shimejiParams.y = groundY
                        try {
                            windowManager.updateViewLayout(shimejiView, shimejiParams)
                            Log.d(TAG, "Falling, current Y: ${shimejiParams.y}")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error updating view during fall: ${e.message}")
                        }
                        currentStep++
                        handler.postDelayed(this, stepDuration)
                    } else {
                        shimejiParams.y = groundY
                        try {
                            windowManager.updateViewLayout(shimejiView, shimejiParams)
                            Log.d(TAG, "Fall animation ended, new Y: ${shimejiParams.y}")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error updating view after fall: ${e.message}")
                        }
                        isAnimating = false
                        setAnimation("walk")
                        handler.postDelayed(movementRunnable, 2000)
                    }
                }
            }

            handler.post(fallRunnable)
        } else {
            isClimbing = false
            setAnimation("walk")
            handler.postDelayed(movementRunnable, 2000)
        }
    }

    private fun randomClimb() {
        if (isAnimating || isDragging || !isClimbing) return
        updateDisplayMetrics()
        val groundY = displayMetrics.heightPixels - MARGIN // Vị trí đáy màn hình
        val topY = 0

        // Nếu ở đỉnh màn hình, dừng lại
        if (shimejiParams.y <= topY) {
            isAnimating = false
            setAnimation("walk")
            return
        }

        isAnimating = true
        setAnimation("climb")

        val isLeftWall = shimejiParams.x <= 0
        val moveY = if (random.nextBoolean()) -150f else 150f // Lên hoặc xuống
        val targetY = (shimejiParams.y + moveY).coerceIn(topY.toFloat(), groundY.toFloat())
        val distance = abs(targetY - shimejiParams.y)
        val speed = 200f // 100px mỗi giây
        val duration = (distance / speed * 1000).toLong().coerceAtMost(5000) // Tối đa 5 giây
        val steps = 60 // 30 FPS
        val stepDuration = duration / steps
        val stepDistance = (targetY - shimejiParams.y) / steps

        var currentStep = 0

        // Cập nhật vị trí từ từ
        val climbRunnable = object : Runnable {
            override fun run() {
                if (currentStep < steps && !isDragging && isClimbing) {
                    shimejiParams.y += stepDistance.toInt()
                    shimejiParams.y = shimejiParams.y.coerceIn(topY, groundY)
                    try {
                        windowManager.updateViewLayout(shimejiView, shimejiParams)
                        Log.d(TAG, "Climbing, current Y: ${shimejiParams.y}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating view during climb: ${e.message}")
                    }
                    currentStep++
                    handler.postDelayed(this, stepDuration)
                } else {
                    shimejiParams.y = targetY.toInt()
                    try {
                        windowManager.updateViewLayout(shimejiView, shimejiParams)
                        Log.d(TAG, "Climb animation ended, new Y: ${shimejiParams.y}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating view after climb: ${e.message}")
                    }
                    isAnimating = false

                    // Nếu chạm đất, thoát trạng thái leo và cho phép walk
                    if (shimejiParams.y >= groundY) {
                        isClimbing = false
                        setAnimation("walk")
                    } else if (shimejiParams.y <= topY) {
                        setAnimation("walk") // Dừng ở đỉnh
                    } else {
                        setAnimation("climb")
                    }
                }
            }
        }

        // Lật hướng nhân vật
        shimejiAnimationView.scaleX = if (isLeftWall) 1f else -1f
        Log.d(TAG, "Climb animation started, direction: ${if (moveY < 0) "up" else "down"}")
        handler.post(climbRunnable)
    }

    override fun onDestroy() {
        Log.d(TAG, "Service onDestroy")
        handler.removeCallbacks(movementRunnable)
        try {
            shimejiView?.let { windowManager.removeView(it) }
            Log.d(TAG, "ShimejiView removed from window")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing view: ${e.message}")
        }
        super.onDestroy()
    }
}