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

    // Flags
    private var initYcharacter = false

    // Constants - Tối ưu: Đặt tốc độ thành companion object
    companion object {
        private const val MOVE_SPEED = 20
        private const val ANIMATION_SPEED = 250L
        private const val MIN_DISTANCE = 850
        private const val MAX_DISTANCE = 1000
        private const val DELAY_BEFORE_NEW_DISTANCE = 500L
    }

    // Screen and character dimensions
    private var screenWidth = 0
    private var screenHeight = 0
    private var characterWidth = 0
    private var characterHeight = 0

    // Movement state
    private var remainingDistance = 0
    private var moveRight = true
    private var facingRight = true
    private var isClimbing = false
    private var climbingUp = true
    private var rightWall = false

    // Animation frames
    private val walkFrames = listOf(R.drawable.walk_frame_1, R.drawable.walk_frame_2)
    private val climbFrames = listOf(R.drawable.walk_frame_1, R.drawable.walk_frame_2)
    private var currentFrame = 0

    // Tối ưu: Tách riêng logic di chuyển và animation
    private val updateRunnable = object : Runnable {
        override fun run() {
            if (overlayView != null) {
                moveCharacter()
                updateAnimation()
                handler.postDelayed(this, ANIMATION_SPEED)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        setupOverlay()
        handler.postDelayed({ setNewDistance() }, DELAY_BEFORE_NEW_DISTANCE)
    }

    private fun setupOverlay() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        getScreenSize()

        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.overlay_layout, null)

        overlayView?.let { view ->
            characterImage = view.findViewById(R.id.character_image_view)
            characterImage.setImageResource(R.drawable.walk_frame_1)

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
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = screenWidth / 2
                y = screenHeight - 40
            }

            windowManager.addView(view, params)

            // Tối ưu: Sử dụng ViewTreeObserver một cách an toàn hơn
            view.viewTreeObserver.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    characterWidth = view.width
                    characterHeight = view.height

                    if (!initYcharacter && characterHeight > 0) {
                        initYcharacter = true
                        val layoutParams = view.layoutParams as WindowManager.LayoutParams
                        layoutParams.y = screenHeight - characterHeight
                        windowManager.updateViewLayout(view, layoutParams)

                        // Bắt đầu animation sau khi đã setup xong
                        handler.post(updateRunnable)
                    }

                    // Remove listener để tránh gọi lại không cần thiết
                    view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            })
        } ?: run {
            stopSelf()
        }
    }

    private fun getScreenSize() {
        val displayMetrics = resources.displayMetrics
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels
    }

    private fun setNewDistance() {
        remainingDistance = Random.nextInt(MIN_DISTANCE, MAX_DISTANCE)
        moveRight = Random.nextBoolean()
    }

    // Tối ưu: Tách logic di chuyển thành các function nhỏ hơn
    private fun moveCharacter() {
        overlayView?.let { view ->
            val layoutParams = view.layoutParams as WindowManager.LayoutParams

            if (remainingDistance <= 0) {
                handler.postDelayed({ setNewDistance() }, DELAY_BEFORE_NEW_DISTANCE)
                return
            }

            if (isClimbing) {
                handleClimbing(layoutParams)
            } else {
                handleWalking(layoutParams)
            }

            windowManager.updateViewLayout(view, layoutParams)
        }
    }

    private fun handleClimbing(layoutParams: WindowManager.LayoutParams) {
        val currentY = layoutParams.y
        val moveDistance = minOf(MOVE_SPEED, remainingDistance)

        // Xác định hướng leo dựa trên vị trí tường
        climbingUp = climbingUp && ((moveRight && rightWall) || (!moveRight && !rightWall))
        val newY = if (climbingUp) currentY - moveDistance else currentY + moveDistance

        when {
            newY <= 0 -> {
                // Chạm trần - chuyển sang leo xuống
                climbingUp = false
                val excessDistance = moveDistance - currentY
                layoutParams.y = if (excessDistance > 0) excessDistance else 0
                remainingDistance -= moveDistance
            }
            newY >= screenHeight - characterHeight && !climbingUp -> {
                // Chạm đất - kết thúc leo
                layoutParams.y = screenHeight - characterHeight
                remainingDistance -= (moveDistance - (newY - (screenHeight - characterHeight)).coerceAtLeast(0))
                isClimbing = false
            }
            else -> {
                // Leo bình thường
                layoutParams.y = newY
                remainingDistance -= moveDistance
            }
        }
    }

    private fun handleWalking(layoutParams: WindowManager.LayoutParams) {
        val currentX = layoutParams.x
        val currentY = layoutParams.y
        val moveDistance = minOf(MOVE_SPEED, remainingDistance)
        val newX = if (moveRight) currentX + moveDistance else currentX - moveDistance

        when {
            newX <= 0 -> {
                // Chạm tường trái
                handleWallHit(layoutParams, currentX, currentY, 0, false, moveDistance)
            }
            newX >= screenWidth - characterWidth -> {
                // Chạm tường phải
                handleWallHit(layoutParams, currentX, currentY, screenWidth - characterWidth, true, moveDistance)
            }
            else -> {
                // Di chuyển bình thường
                layoutParams.x = newX
                remainingDistance -= moveDistance
                updateCharacterDirection()
            }
        }

        // Luôn ở đáy khi đi bộ
        if (!isClimbing) {
            layoutParams.y = screenHeight - characterHeight
        }
    }

    private fun handleWallHit(
        layoutParams: WindowManager.LayoutParams,
        currentX: Int,
        currentY: Int,
        wallX: Int,
        isRightWall: Boolean,
        moveDistance: Int
    ) {
        layoutParams.x = wallX
        val wallHitDistance = if (isRightWall) wallX - currentX else currentX - wallX
        val excessDistance = moveDistance - wallHitDistance
        remainingDistance -= wallHitDistance

        if (excessDistance > 0 && remainingDistance > 0) {
            // Chuyển sang leo tường
            startClimbing(layoutParams, currentY, excessDistance, isRightWall)
        }
    }

    private fun startClimbing(layoutParams: WindowManager.LayoutParams, currentY: Int, excessDistance: Int, isRightWall: Boolean) {
        isClimbing = true
        climbingUp = true
        rightWall = isRightWall
        facingRight = isRightWall
        characterImage.scaleX = if (facingRight) 1f else -1f

        val newY = currentY - excessDistance
        if (newY >= 0) {
            layoutParams.y = newY
            remainingDistance -= excessDistance
        } else {
            layoutParams.y = 0
            climbingUp = false
            remainingDistance -= (excessDistance - (0 - newY))
        }
    }

    private fun updateCharacterDirection() {
        facingRight = moveRight
        characterImage.scaleX = if (facingRight) 1f else -1f
    }

    private fun updateAnimation() {
        val frames = if (isClimbing) climbFrames else walkFrames
        currentFrame = (currentFrame + 1) % frames.size
        characterImage.setImageResource(frames[currentFrame])
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable)
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                // Handle case where view might already be removed
            }
            overlayView = null
        }
    }
}