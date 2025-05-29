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

    // Flag để tránh setupOverlay bị gọi lại
//    private var isOverlaySetup = false
    private var initYcharacter = false;

    // Tốc độ
    private val MOVE_SPEED = 20 // Tốc độ di chuyển (pixel)
    private val ANIMATION_SPEED = 250L // Tốc độ đổi hình (ms)

    // Kích thước
    private var screenWidth = 0
    private var screenHeight = 0
    private var characterWidth = 0
    private var characterHeight = 0

    // Di chuyển
    private var remainingDistance = 0 // Khoảng cách còn lại cần di chuyển
    private var moveRight = true // Hướng di chuyển: true = phải, false = trái
    private var facingRight = true // Hướng mặt
    private var isClimbing = false // Đang leo tường
    private var climbingUp = true // Hướng leo: true = lên, false = xuống
    private var rightWall = false //true = tường phải, false = tường trái

    // Hình ảnh đi bộ và leo
    private val walkFrames = listOf(R.drawable.walk_frame_1, R.drawable.walk_frame_2)
    private val climbFrames =
        listOf(R.drawable.walk_frame_1, R.drawable.walk_frame_2) // Có thể thay bằng hình leo riêng
    private var currentFrame = 0 // Hình hiện tại

    private var count = 0

//    private var currentYPosition = 0 // Biến lưu trữ vị trí y hiện tại

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
//        if (!isOverlaySetup) {
        setupOverlay() // Tạo nhân vật
//        }
        handler.postDelayed({ setNewDistance() }, 500) // Bắt đầu di chuyển
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
//            isOverlaySetup = true // Đánh dấu đã setup xong

            // Lấy kích thước nhân vật và đặt sát đáy
            view.viewTreeObserver.addOnGlobalLayoutListener {
                characterWidth = view.width
                characterHeight = view.height
//                println("characterWidth $characterWidth characterHeight $characterHeight")
                val layoutParams = view.layoutParams as WindowManager.LayoutParams
                if (!initYcharacter) {
                    initYcharacter = true
//                    currentYPosition = screenHeight - characterHeight
                    layoutParams.y = screenHeight - characterHeight // Đặt sát đáy
                }
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

    // Đặt khoảng cách và hướng di chuyển mới
    private fun setNewDistance() {
        remainingDistance = Random.nextInt(850, 1000) // Khoảng cách 100-500 pixel
        moveRight = Random.nextBoolean() // Random hướng di chuyển
        println("set new remainingDistance $remainingDistance moveRight $moveRight")

//        // Cập nhật hướng mặt
//        facingRight = moveRight
//        characterImage.scaleX = if (facingRight) 1f else -1f
    }

    // Di chuyển nhân vật theo logic distance-based
    private fun moveCharacter() {
        overlayView?.let { view ->

            count++
//            println("moveCharacter count $count currentYPosition $currentYPosition")

            val layoutParams = view.layoutParams as WindowManager.LayoutParams
            val currentX = layoutParams.x
            val currentY = layoutParams.y

            // Nếu hết khoảng cách, đặt khoảng cách mới
            if (remainingDistance <= 0) {
                handler.postDelayed({ setNewDistance() }, 500)
                return
            }

            println("isClimbing $isClimbing")

            if (isClimbing) {

                // Đang leo tường
                val moveDistance = minOf(MOVE_SPEED, remainingDistance)
                climbingUp = climbingUp && ((moveRight && rightWall) || (!moveRight && !rightWall))
                val newY = if (climbingUp) currentY - moveDistance else currentY + moveDistance
                println("moveDistance $moveDistance currentY $currentY newY $newY")

                // Kiểm tra chạm trần hoặc chạm đất
                if (newY <= 0) {
                    // Chạm trần - chuyển sang leo xuống
                    climbingUp = false
//                    currentYPosition = 0
//                    println("set currentYPosition")
//                    layoutParams.y = currentYPosition
                    val excessDistance = moveDistance - (currentY - 0)
                    remainingDistance -= (moveDistance - excessDistance)
                    if (excessDistance > 0) {
                        layoutParams.y = excessDistance
                        remainingDistance -= excessDistance
                    }
                }

                if (newY >= screenHeight - characterHeight && !climbingUp) {
                    // Chạm đất - kết thúc leo, chuyển về đi bộ
//                    currentYPosition = screenHeight - characterHeight
//                    println("set currentYPosition $currentYPosition")
                    layoutParams.y = screenHeight - characterHeight
                    remainingDistance -= (moveDistance - (newY - (screenHeight - characterHeight)))
                    isClimbing = false
                } else {// Leo bình thường
//                    currentYPosition = newY
//                    println("set currentYPosition $currentYPosition")
                    layoutParams.y = newY
                    remainingDistance -= moveDistance
                }
            } else {
                // Đi bộ bình thường
                val moveDistance = minOf(MOVE_SPEED, remainingDistance)
                val newX = if (moveRight) currentX + moveDistance else currentX - moveDistance

                // Kiểm tra chạm tường
                if (newX <= 0) {
                    // Chạm tường trái
                    layoutParams.x = 0
                    val wallHitDistance = currentX - 0
                    val excessDistance = moveDistance - wallHitDistance
                    remainingDistance -= wallHitDistance

                    if (excessDistance > 0 && remainingDistance > 0) {
                        // Chuyển sang leo tường
                        isClimbing = true
                        climbingUp = true
                        facingRight = false // Mặt vào tường trái
                        characterImage.scaleX = -1f
                        rightWall = false

                        // Leo lên với khoảng cách dư
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

                }

                if (newX >= screenWidth - characterWidth) {
                    // Chạm tường phải
                    layoutParams.x = screenWidth - characterWidth
                    val wallHitDistance = (screenWidth - characterWidth) - currentX
                    val excessDistance = moveDistance - wallHitDistance
                    remainingDistance -= wallHitDistance

                    if (excessDistance > 0 && remainingDistance > 0) {
                        // Chuyển sang leo tường
                        isClimbing = true
                        climbingUp = true
                        facingRight = true // Mặt vào tường phải
                        characterImage.scaleX = 1f
                        rightWall = true

                        // Leo lên với khoảng cách dư
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

                } else {
                    // Di chuyển bình thường
                    layoutParams.x = newX
                    remainingDistance -= moveDistance

                    facingRight = moveRight
                    characterImage.scaleX = if (facingRight) 1f else -1f

                }

                // Luôn ở đáy khi đi bộ (trừ khi đang leo)
                if (!isClimbing) {
//                    currentYPosition = screenHeight - characterHeight
//                    println("set currentYPosition $currentYPosition")
                    layoutParams.y = screenHeight - characterHeight
                }
            }



            windowManager.updateViewLayout(view, layoutParams)
        }
    }

    // Cập nhật hình ảnh đi bộ/leo
    private fun updateAnimation() {
        val frames = if (isClimbing) climbFrames else walkFrames
        currentFrame = (currentFrame + 1) % frames.size
        characterImage.setImageResource(frames[currentFrame])
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