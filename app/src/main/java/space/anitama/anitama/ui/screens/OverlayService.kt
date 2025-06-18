package space.anitama.anitama.ui.screens

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
import space.anitama.anitama.R
import kotlin.random.Random

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private lateinit var characterImage: ImageView
    private val handler = Handler(Looper.getMainLooper())

    // Flags
    private var initYcharacter = false

    // Constants
    companion object {
        private const val MOVE_SPEED = 20
        private const val ANIMATION_SPEED = 250L
        private const val ANIMATION_SPEED_FALLING = 25L
        private const val FALL_SPEED = 500
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

    // Drag & Drop state
    private var isDragging = false
    private var isFalling = false
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var initialX = 0
    private var initialY = 0

    // Animation frames
    private val walkFrames = listOf(R.drawable.walk_frame_1, R.drawable.walk_frame_2)
    private val climbFrames = listOf(R.drawable.walk_frame_1, R.drawable.walk_frame_2)
    private val dragFrames =
        listOf(R.drawable.walk_frame_1) // Animation khi drag - có thể thay bằng hình riêng
    private val fallFrames =
        listOf(R.drawable.walk_frame_2) // Animation khi rơi - có thể thay bằng hình riêng
    private var currentFrame = 0

    private val updateRunnable = object : Runnable {
        override fun run() {
            if (overlayView != null) {
                when {
                    isFalling -> {
                        handleFalling()
                    }

                    !isDragging -> {
                        moveCharacter()
                        updateAnimation()
                    }

                    else -> updateDragAnimation()
                }
                if (!isFalling) handler.postDelayed(this, ANIMATION_SPEED)
                else handler.postDelayed(this, ANIMATION_SPEED_FALLING)
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
            setupTouchListener(view)

            view.viewTreeObserver.addOnGlobalLayoutListener(object :
                android.view.ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    characterWidth = view.width
                    characterHeight = view.height

                    if (!initYcharacter && characterHeight > 0) {
                        initYcharacter = true
                        val layoutParams = view.layoutParams as WindowManager.LayoutParams
                        layoutParams.y = screenHeight - characterHeight
                        windowManager.updateViewLayout(view, layoutParams)

                        handler.post(updateRunnable)
                    }

                    view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            })
        } ?: run {
            stopSelf()
        }
    }

    private fun setupTouchListener(view: View) {
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    handleTouchDown(event)
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    handleTouchMove(event)
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    handleTouchUp()
                    true
                }

                else -> false
            }
        }
    }

    private fun handleTouchDown(event: MotionEvent) {
        isDragging = true
        isFalling = false
        isClimbing = false

        initialTouchX = event.rawX
        initialTouchY = event.rawY

        overlayView?.let { view ->
            val layoutParams = view.layoutParams as WindowManager.LayoutParams
            initialX = layoutParams.x
            initialY = layoutParams.y
        }

        // Cập nhật params để cho phép touch
        overlayView?.let { view ->
            val layoutParams = view.layoutParams as WindowManager.LayoutParams
            layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            windowManager.updateViewLayout(view, layoutParams)
        }
    }

    private fun handleTouchMove(event: MotionEvent) {
        if (!isDragging) return

        val deltaX = (event.rawX - initialTouchX).toInt()
        val deltaY = (event.rawY - initialTouchY).toInt()

        overlayView?.let { view ->
            val layoutParams = view.layoutParams as WindowManager.LayoutParams
            val newX = (initialX + deltaX).coerceIn(0, screenWidth - characterWidth)
            val newY = (initialY + deltaY).coerceIn(0, screenHeight - characterHeight)

            layoutParams.x = newX
            layoutParams.y = newY

            windowManager.updateViewLayout(view, layoutParams)
        }
    }

    private fun handleTouchUp() {
        isDragging = false

        // Khôi phục params về trạng thái ban đầu
        overlayView?.let { view ->
            val layoutParams = view.layoutParams as WindowManager.LayoutParams
            layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE

            // Kiểm tra xem có cần rơi xuống không
            if (layoutParams.y < screenHeight - characterHeight) {
                isFalling = true
            } else {
                // Nếu đã ở đáy, tiếp tục di chuyển bình thường
                resetMovementState()
            }

            windowManager.updateViewLayout(view, layoutParams)
        }
    }

    private fun handleFalling() {
        overlayView?.let { view ->
            val layoutParams = view.layoutParams as WindowManager.LayoutParams
            val currentY = layoutParams.y
            val newY = currentY + FALL_SPEED

            if (newY >= screenHeight - characterHeight) {
                // Đã chạm đáy
                layoutParams.y = screenHeight - characterHeight
                isFalling = false
                resetMovementState()
            } else {
                // Tiếp tục rơi
                layoutParams.y = newY
            }

            windowManager.updateViewLayout(view, layoutParams)
        }

        // Cập nhật animation rơi
        updateFallAnimation()
    }

    private fun resetMovementState() {
        // Reset lại trạng thái di chuyển sau khi thả
        isClimbing = false
        climbingUp = true
        handler.postDelayed({ setNewDistance() }, DELAY_BEFORE_NEW_DISTANCE)
    }

    private fun getScreenSize() {
        val displayMetrics = resources.displayMetrics
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels
    }

    private fun setNewDistance() {
        if (!isDragging && !isFalling) {
            remainingDistance = Random.nextInt(MIN_DISTANCE, MAX_DISTANCE)
            moveRight = Random.nextBoolean()
        }
    }

    private fun moveCharacter() {
        if (isDragging || isFalling) return

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

        climbingUp = climbingUp && ((moveRight && rightWall) || (!moveRight && !rightWall))
        val newY = if (climbingUp) currentY - moveDistance else currentY + moveDistance

        when {
            newY <= 0 -> {
                climbingUp = false
                val excessDistance = moveDistance - currentY
                layoutParams.y = if (excessDistance > 0) excessDistance else 0
                remainingDistance -= moveDistance
            }

            newY >= screenHeight - characterHeight && !climbingUp -> {
                layoutParams.y = screenHeight - characterHeight
                remainingDistance -= (moveDistance - (newY - (screenHeight - characterHeight)).coerceAtLeast(
                    0
                ))
                isClimbing = false
            }

            else -> {
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
                handleWallHit(layoutParams, currentX, currentY, 0, false, moveDistance)
            }

            newX >= screenWidth - characterWidth -> {
                handleWallHit(
                    layoutParams,
                    currentX,
                    currentY,
                    screenWidth - characterWidth,
                    true,
                    moveDistance
                )
            }

            else -> {
                layoutParams.x = newX
                remainingDistance -= moveDistance
                updateCharacterDirection()
            }
        }

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
            startClimbing(layoutParams, currentY, excessDistance, isRightWall)
        }
    }

    private fun startClimbing(
        layoutParams: WindowManager.LayoutParams,
        currentY: Int,
        excessDistance: Int,
        isRightWall: Boolean
    ) {
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

    private fun updateDragAnimation() {
        // Animation khi đang drag
        currentFrame = (currentFrame + 1) % dragFrames.size
        characterImage.setImageResource(dragFrames[currentFrame])
    }

    private fun updateFallAnimation() {
        // Animation khi đang rơi
        currentFrame = (currentFrame + 1) % fallFrames.size
        characterImage.setImageResource(fallFrames[currentFrame])
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