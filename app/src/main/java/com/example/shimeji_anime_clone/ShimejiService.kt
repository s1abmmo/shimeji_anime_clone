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
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.TranslateAnimation
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
    private val DRAG_THRESHOLD = 10f
    private val TAG = "ShimejiService"

    private val handler = Handler(Looper.getMainLooper())
    private val random = Random.Default

    private val movementRunnable = object : Runnable {
        override fun run() {
            if (!isDragging && !isAnimating) {
                randomMovement()
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
                        isDragging = false
                        stopAutomaticMovement()
                        false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = abs(event.rawX - initialTouchX)
                        val deltaY = abs(event.rawY - initialTouchY)
                        if (!isDragging && (deltaX > DRAG_THRESHOLD || deltaY > DRAG_THRESHOLD)) {
                            isDragging = true
                        }
                        if (isDragging) {
                            shimejiParams.x = initialX + (event.rawX - initialTouchX).toInt()
                            shimejiParams.y = initialY + (event.rawY - initialTouchY).toInt()
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
                        startAutomaticMovement()
                        false
                    }
                    else -> false
                }
            }

            // Thêm view vào WindowManager
            windowManager.addView(shimejiView, shimejiParams)
            Log.d(TAG, "ShimejiView added to window")

            // Bắt đầu chuyển động tự động
            startAutomaticMovement()
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

    private fun startAutomaticMovement() {
        handler.postDelayed(movementRunnable, 2000)
        Log.d(TAG, "Automatic movement started")
    }

    private fun stopAutomaticMovement() {
        handler.removeCallbacks(movementRunnable)
        Log.d(TAG, "Automatic movement stopped")
    }

    private fun randomMovement() {
        if (isAnimating) return
        updateDisplayMetrics()

        val action = random.nextInt(3)
        when (action) {
            0 -> jumpAnimation()
            1 -> walkAnimation()
            2 -> fallAnimation()
        }
        Log.d(TAG, "Random movement action: $action")
    }

    private fun jumpAnimation() {
        isAnimating = true

        val jumpHeight = -100f
        val animation = TranslateAnimation(0f, 0f, 0f, jumpHeight)
        animation.duration = 500
        animation.repeatCount = 1
        animation.repeatMode = Animation.REVERSE
        animation.interpolator = LinearInterpolator()

        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {
                Log.d(TAG, "Jump animation started")
                shimejiAnimationView.playAnimation()
            }
            override fun onAnimationEnd(p0: Animation?) {
                isAnimating = false
                Log.d(TAG, "Jump animation ended")
            }
            override fun onAnimationRepeat(p0: Animation?) {}
        })

        shimejiView?.post {
            shimejiAnimationView.startAnimation(animation)
        }
    }

    private fun walkAnimation() {
        isAnimating = true

        val moveX = (random.nextInt(300) - 150).toFloat()
        val animation = TranslateAnimation(0f, moveX, 0f, 0f)
        animation.duration = 1000
        animation.interpolator = LinearInterpolator()

        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {
                if (moveX < 0) {
                    shimejiAnimationView.scaleX = -1f
                } else {
                    shimejiAnimationView.scaleX = 1f
                }
                Log.d(TAG, "Walk animation started, direction: ${if (moveX < 0) "left" else "right"}")
                shimejiAnimationView.playAnimation()
            }

            override fun onAnimationEnd(p0: Animation?) {
                shimejiParams.x += moveX.toInt()
                if (shimejiParams.x < 0) shimejiParams.x = 0
                if (shimejiParams.x > displayMetrics.widthPixels - 150) {
                    shimejiParams.x = displayMetrics.widthPixels - 150
                }
                try {
                    windowManager.updateViewLayout(shimejiView, shimejiParams)
                    Log.d(TAG, "Walk animation ended, new X: ${shimejiParams.x}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating view after walk: ${e.message}")
                }
                isAnimating = false
            }

            override fun onAnimationRepeat(p0: Animation?) {}
        })

        shimejiView?.post {
            shimejiAnimationView.startAnimation(animation)
        }
    }

    private fun fallAnimation() {
        updateDisplayMetrics()
        if (shimejiParams.y < displayMetrics.heightPixels - 300) {
            isAnimating = true

            val fallDistance = 300f
            val animation = TranslateAnimation(0f, 0f, 0f, fallDistance)
            animation.duration = 800
            animation.interpolator = LinearInterpolator()

            animation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(p0: Animation?) {
                    Log.d(TAG, "Fall animation started")
                    shimejiAnimationView.playAnimation()
                }
                override fun onAnimationEnd(p0: Animation?) {
                    shimejiParams.y += fallDistance.toInt()
                    try {
                        windowManager.updateViewLayout(shimejiView, shimejiParams)
                        Log.d(TAG, "Fall animation ended, new Y: ${shimejiParams.y}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating view after fall: ${e.message}")
                    }
                    isAnimating = false
                }
                override fun onAnimationRepeat(p0: Animation?) {}
            })

            shimejiView?.post {
                shimejiAnimationView.startAnimation(animation)
            }
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "Service onDestroy")
        stopAutomaticMovement()
        try {
            shimejiView?.let { windowManager.removeView(it) }
            Log.d(TAG, "ShimejiView removed from window")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing view: ${e.message}")
        }
        super.onDestroy()
    }
}