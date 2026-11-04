package com.example.buddygotchi

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

object BuddyOverlayManager {
    private var activeOverlayView: View? = null
    private var autoDismissRunnable: Runnable? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun canDrawOverlays(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun requestOverlayPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                val fallbackIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            }
        }
    }

    fun showPopupAlert(
        context: Context,
        taskId: String = "test_alert",
        taskTitle: String = "Task Due!",
        taskCategory: String = "REMINDER",
        taskTime: Long = System.currentTimeMillis()
    ) {
        if (!canDrawOverlays(context)) return

        mainHandler.post {
            dismissActiveOverlay(context)

            val appContext = context.applicationContext
            val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return@post

            val density = appContext.resources.displayMetrics.density
            fun dp(value: Float): Int = (value * density).toInt()

            // 1. Equipped Buddy Thumbnail
            AccountAndInventoryManager.ensureInitialized(appContext)
            val equippedPetId = AccountAndInventoryManager.equippedPetIdState.value
            val buddyBitmap = Buddy3DManager.getBuddyScreenshotBitmap(appContext, equippedPetId)

            // 2. Build Overlay Layout programmatically (clean & 100% lifecycle-free)
            val rootLayout = FrameLayout(appContext)

            val container = LinearLayout(appContext).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12f), dp(10f), dp(12f), dp(10f))

                // Dark pill background with NothingRed border
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#F0141414"))
                    cornerRadius = dp(24f).toFloat()
                    setStroke(dp(1.5f), Color.parseColor("#D71921")) // NothingRed
                }
                elevation = dp(16f).toFloat()
            }

            // Speech Bubble / Text column
            val textContainer = LinearLayout(appContext).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(dp(175f), LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = dp(10f)
                }
            }

            // Header row with RED DOT + "TASK DUE!" + Close button
            val headerRow = LinearLayout(appContext).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val dot = View(appContext).apply {
                layoutParams = LinearLayout.LayoutParams(dp(7f), dp(7f)).apply {
                    marginEnd = dp(6f)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#D71921"))
                }
            }

            val headerText = TextView(appContext).apply {
                text = "${taskCategory.uppercase()} DUE"
                setTextColor(Color.parseColor("#D71921"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                typeface = Typeface.MONOSPACE
                paint.isFakeBoldText = true
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val closeButton = TextView(appContext).apply {
                text = "✕"
                setTextColor(Color.parseColor("#8E8E8E"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(dp(6f), dp(2f), dp(6f), dp(2f))
                setOnClickListener {
                    dismissActiveOverlay(appContext)
                }
            }

            headerRow.addView(dot)
            headerRow.addView(headerText)
            headerRow.addView(closeButton)

            // Title
            val titleText = TextView(appContext).apply {
                text = taskTitle.ifBlank { "Time for your task!" }
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                paint.isFakeBoldText = true
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(2f)
                }
            }

            // Action prompt
            val openPrompt = TextView(appContext).apply {
                text = "TAP TO OPEN APP →"
                setTextColor(Color.parseColor("#A8A8A8"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f)
                typeface = Typeface.MONOSPACE
                paint.isFakeBoldText = true
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(4f)
                }
            }

            textContainer.addView(headerRow)
            textContainer.addView(titleText)
            textContainer.addView(openPrompt)

            // Buddy Avatar ImageView
            val buddyView = ImageView(appContext).apply {
                layoutParams = LinearLayout.LayoutParams(dp(56f), dp(56f))
                scaleType = ImageView.ScaleType.FIT_CENTER
                if (buddyBitmap != null) {
                    setImageBitmap(buddyBitmap)
                } else {
                    setImageResource(R.mipmap.ic_launcher)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#1F1F1F"))
                    setStroke(dp(1f), Color.parseColor("#333333"))
                }
                setPadding(dp(2f), dp(2f), dp(2f), dp(2f))
            }

            container.addView(textContainer)
            container.addView(buddyView)
            rootLayout.addView(container)

            // Tap anywhere on the container to open MainActivity
            container.setOnClickListener {
                val launchIntent = appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)?.apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                }
                if (launchIntent != null) {
                    appContext.startActivity(launchIntent)
                }
                dismissActiveOverlay(appContext)
            }

            // Window Layout Params
            val layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
                x = dp(14f)
                y = 0
            }

            // Vertical dragging support
            var initialY = 0
            var initialTouchY = 0f
            rootLayout.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialY = layoutParams.y
                        initialTouchY = event.rawY
                        false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dy = (event.rawY - initialTouchY).toInt()
                        layoutParams.y = initialY + dy
                        try {
                            windowManager.updateViewLayout(rootLayout, layoutParams)
                        } catch (_: Exception) {}
                        false
                    }
                    else -> false
                }
            }

            // Slide in animation from right with overshoot bounce
            rootLayout.translationX = dp(280f).toFloat()
            rootLayout.alpha = 0f

            try {
                windowManager.addView(rootLayout, layoutParams)
                activeOverlayView = rootLayout

                rootLayout.animate()
                    .translationX(0f)
                    .alpha(1f)
                    .setDuration(450)
                    .setInterpolator(OvershootInterpolator(1.15f))
                    .start()

                // Phone vibration on popup
                triggerAlertVibration(appContext)

                // Auto dismiss after 18 seconds
                autoDismissRunnable = Runnable {
                    dismissActiveOverlay(appContext)
                }
                mainHandler.postDelayed(autoDismissRunnable!!, 18000L)
            } catch (e: Exception) {
                activeOverlayView = null
            }
        }
    }

    fun dismissActiveOverlay(context: Context) {
        autoDismissRunnable?.let { mainHandler.removeCallbacks(it) }
        autoDismissRunnable = null

        val view = activeOverlayView ?: return
        activeOverlayView = null

        mainHandler.post {
            try {
                val windowManager = context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                view.animate()
                    .translationX(600f)
                    .alpha(0f)
                    .setDuration(250)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            try {
                                windowManager.removeView(view)
                            } catch (_: Exception) {}
                        }
                    })
                    .start()
            } catch (_: Exception) {}
        }
    }

    fun triggerAlertVibration(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            val pattern = longArrayOf(0, 350, 150, 350, 150, 500)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, -1)
            }
        } catch (_: Exception) {}
    }
}
