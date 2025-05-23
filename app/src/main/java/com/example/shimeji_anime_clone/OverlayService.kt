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
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import kotlin.random.Random

class OverlayService : Service() {

    // ===== CÁC BIẾN TỐC ĐỘ VÀ THÔNG SỐ =====
    private val ANIMATION_SPEED = 300L // Tốc độ animation (ms)
    private val MOVE_SPEED = 10 // Tốc độ di chuyển (pixel/frame)
    private val FALL_SPEED = 1000 // Tốc độ rơi (pixel/frame)
    private val SLEEP_DURATION = 3000L // Thời gian ngủ (ms)
    private val CLIMB_SPEED = 5 // Tốc độ leo trèo (pixel/frame)
    private val RANDOM_MOVE_MIN = 100 // Khoảng cách di chuyển tối thiểu
    private val RANDOM_MOVE_MAX = 300 // Khoảng cách di chuyển tối đa

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private lateinit var characterImageView: ImageView
    private val handler = Handler(Looper.getMainLooper())

    // Trạng thái character
    private var currentState = CharacterState.WALKING
    private var currentAnimation = AnimationType.WALK
    private var currentFrame = 0
    private var facingRight = true // Mặc định quay mặt về phải

    // Vị trí và di chuyển
    private var screenWidth = 0
    private var screenHeight = 0
    private var targetX = 0
    private var isMoving = false

    // Drag handling
    private var isDragging = false
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    // Animation frames - sử dụng các frame có sẵn từ code gốc
    private val walkFrames = listOf(R.drawable.walk_frame_1, R.drawable.walk_frame_2, R.drawable.walk_frame_3)
    private val runFrames = listOf(R.drawable.walk_frame_1, R.drawable.walk_frame_2, R.drawable.walk_frame_3) // Tạm dùng walk frames
    private val climbFrames = listOf(R.drawable.climb_frame_1, R.drawable.climb_frame_2, R.drawable.climb_frame_3)
    private val sleepFrames = listOf(R.drawable.walk_frame_1) // Tạm dùng walk frame đầu tiên
    private val dragFrames = listOf(R.drawable.walk_frame_1) // Tạm dùng walk frame đầu tiên
    private val fallFrames = listOf(R.drawable.walk_frame_1, R.drawable.walk_frame_2) // Tạm dùng walk frames

    private enum class CharacterState {
        WALKING, RUNNING, SLEEPING, CLIMBING, DRAGGING, FALLING
    }

    private enum class AnimationType {
        WALK, RUN, SLEEP, CLIMB, DRAG, FALL
    }

    private enum class ClimbSide {
        LEFT, RIGHT
    }

    private var currentClimbSide = ClimbSide.RIGHT

    // Animation runnable
    private val animationRunnable = object : Runnable {
        override fun run() {
            updateFrame()
            handleMovement()
            handler.postDelayed(this, ANIMATION_SPEED)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        initializeOverlay()
        // Delay một chút để đảm bảo overlay đã được tạo
        handler.postDelayed({
            startRandomBehavior()
        }, 500)
    }

    private fun initializeOverlay() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        getScreenDimensions() // Gọi trước khi sử dụng screenWidth/Height

        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.overlay_layout, null)

        overlayView?.let { view ->
            characterImageView = view.findViewById(R.id.character_image_view)

            val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            )

            params.gravity = Gravity.TOP or Gravity.START
            params.x = screenWidth / 2
            params.y = screenHeight - 200 // Vị trí viền dưới màn hình

            windowManager.addView(view, params)
            setupTouchListener(view)
            startAnimation()
        }
    }

    private fun getScreenDimensions() {
        val displayMetrics = resources.displayMetrics
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels
    }

    private fun setupTouchListener(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startDragging(event)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isDragging) {
                        updateDragPosition(event, view)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isDragging) {
                        stopDragging()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun startDragging(event: MotionEvent) {
        isDragging = true
        currentState = CharacterState.DRAGGING
        currentAnimation = AnimationType.DRAG
        initialTouchX = event.rawX
        initialTouchY = event.rawY
        isMoving = false
    }

    private fun updateDragPosition(event: MotionEvent, view: View) {
        val layoutParams = view.layoutParams as WindowManager.LayoutParams
        layoutParams.x = (event.rawX - initialTouchX + layoutParams.x).toInt()
        layoutParams.y = (event.rawY - initialTouchY + layoutParams.y).toInt()
        windowManager.updateViewLayout(view, layoutParams)
        initialTouchX = event.rawX
        initialTouchY = event.rawY
    }

    private fun stopDragging() {
        isDragging = false
        currentState = CharacterState.FALLING
        currentAnimation = AnimationType.FALL
        startFalling()
    }

    private fun startFalling() {
        val fallRunnable = object : Runnable {
            override fun run() {
                overlayView?.let { view ->
                    val layoutParams = view.layoutParams as WindowManager.LayoutParams
                    layoutParams.y += FALL_SPEED

                    // Kiểm tra chạm đáy màn hình
                    if (layoutParams.y >= screenHeight - 200) {
                        layoutParams.y = screenHeight - 200
                        windowManager.updateViewLayout(view, layoutParams)
                        landOnGround()
                        return
                    }

                    windowManager.updateViewLayout(view, layoutParams)
                    handler.postDelayed(this, 50)
                }
            }
        }
        handler.post(fallRunnable)
    }

    private fun landOnGround() {
        currentState = CharacterState.WALKING
        currentAnimation = AnimationType.WALK
        startRandomBehavior()
    }

    private fun startRandomBehavior() {
        if (currentState == CharacterState.CLIMBING || currentState == CharacterState.DRAGGING || currentState == CharacterState.FALLING) {
            return
        }

        val randomAction = Random.nextInt(1, 4) // 1: walk, 2: run, 3: sleep
        when (randomAction) {
            1 -> startWalking()
            2 -> startRunning()
            3 -> startSleeping()
        }
    }

    private fun startWalking() {
        currentState = CharacterState.WALKING
        currentAnimation = AnimationType.WALK
        setRandomTarget()
        isMoving = true
    }

    private fun startRunning() {
        currentState = CharacterState.RUNNING
        currentAnimation = AnimationType.RUN
        setRandomTarget()
        isMoving = true
    }

    private fun startSleeping() {
        currentState = CharacterState.SLEEPING
        currentAnimation = AnimationType.SLEEP
        isMoving = false

        handler.postDelayed({
            if (currentState == CharacterState.SLEEPING) {
                startRandomBehavior()
            }
        }, SLEEP_DURATION)
    }

    private fun setRandomTarget() {
        overlayView?.let { view ->
            val layoutParams = view.layoutParams as WindowManager.LayoutParams
            val currentX = layoutParams.x
            val moveDistance = Random.nextInt(RANDOM_MOVE_MIN, RANDOM_MOVE_MAX)
            val moveRight = Random.nextBoolean()

            targetX = if (moveRight) {
                (currentX + moveDistance).coerceAtMost(screenWidth - 100)
            } else {
                (currentX - moveDistance).coerceAtLeast(0)
            }

            // Cập nhật hướng mặt
            facingRight = targetX > currentX
            updateImageOrientation()
        }
    }

    private fun handleMovement() {
        if (!isMoving || isDragging) return

        overlayView?.let { view ->
            val layoutParams = view.layoutParams as WindowManager.LayoutParams
            val currentX = layoutParams.x

            when (currentState) {
                CharacterState.WALKING, CharacterState.RUNNING -> {
                    val speed = if (currentState == CharacterState.RUNNING) MOVE_SPEED * 2 else MOVE_SPEED

                    if (Math.abs(currentX - targetX) < speed) {
                        layoutParams.x = targetX
                        windowManager.updateViewLayout(view, layoutParams)
                        checkBoundaries()
                        isMoving = false

                        // Sau khi dừng, bắt đầu hành vi ngẫu nhiên mới
                        handler.postDelayed({ startRandomBehavior() }, 500)
                    } else {
                        layoutParams.x += if (targetX > currentX) speed else -speed
                        windowManager.updateViewLayout(view, layoutParams)
                    }
                }
                CharacterState.CLIMBING -> {
                    handleClimbing(layoutParams)
                }
                else -> { }
            }
        }
    }

    private fun checkBoundaries() {
        overlayView?.let { view ->
            val layoutParams = view.layoutParams as WindowManager.LayoutParams

            // Kiểm tra chạm viền trái hoặc phải
            if (layoutParams.x <= 0) {
                startClimbing(ClimbSide.LEFT)
            } else if (layoutParams.x >= screenWidth - 100) {
                startClimbing(ClimbSide.RIGHT)
            }
        }
    }

    private fun startClimbing(side: ClimbSide) {
        currentState = CharacterState.CLIMBING
        currentAnimation = AnimationType.CLIMB
        currentClimbSide = side
        facingRight = side == ClimbSide.RIGHT
        updateImageOrientation()
        isMoving = true

        // Leo lên một khoảng ngẫu nhiên rồi dừng
        val climbHeight = Random.nextInt(200, 500)
        handler.postDelayed({
            if (currentState == CharacterState.CLIMBING) {
                stopClimbing()
            }
        }, (climbHeight / CLIMB_SPEED * ANIMATION_SPEED).toLong())
    }

    private fun handleClimbing(layoutParams: WindowManager.LayoutParams) {
        layoutParams.y -= CLIMB_SPEED

        // Giữ khít với viền
        when (currentClimbSide) {
            ClimbSide.LEFT -> layoutParams.x = 0
            ClimbSide.RIGHT -> layoutParams.x = screenWidth - 100
        }

        windowManager.updateViewLayout(overlayView!!, layoutParams)
    }

    private fun stopClimbing() {
        currentState = CharacterState.FALLING
        currentAnimation = AnimationType.FALL
        startFalling()
    }

    private fun updateImageOrientation() {
        characterImageView.scaleX = if (facingRight) 1f else -1f
    }

    private fun updateFrame() {
        val frames = when (currentAnimation) {
            AnimationType.WALK -> walkFrames
            AnimationType.RUN -> runFrames
            AnimationType.SLEEP -> sleepFrames
            AnimationType.CLIMB -> climbFrames
            AnimationType.DRAG -> dragFrames
            AnimationType.FALL -> fallFrames
        }

        if (frames.isNotEmpty()) {
            if (currentState != CharacterState.SLEEPING || frames.size > 1) {
                currentFrame = (currentFrame + 1) % frames.size
            }

            try {
                characterImageView.setImageResource(frames[currentFrame])
            } catch (e: Exception) {
                // Fallback to first walk frame if error
                characterImageView.setImageResource(R.drawable.walk_frame_1)
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

    override fun onDestroy() {
        super.onDestroy()
        stopAnimation()
        overlayView?.let {
            windowManager.removeView(it)
            overlayView = null
        }
    }
}