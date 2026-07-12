package io.nekohasekai.sagernet.widget

import android.animation.ValueAnimator
import android.content.Context
import android.database.ContentObserver
import android.graphics.Canvas
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.appcompat.content.res.AppCompatResources
import io.nekohasekai.sagernet.R
import kotlin.math.roundToInt
import kotlin.math.sin

class ConnectingCatView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val catDrawable = checkNotNull(
        AppCompatResources.getDrawable(context, R.mipmap.ic_launcher_foreground)
    ).mutate()
    private val animationScaleObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = updateAnimationState()
    }

    private var connecting = false
    private var observerRegistered = false
    private var animator: ValueAnimator? = null
    private var progress = 0f

    init {
        visibility = GONE
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        isClickable = false
        isFocusable = false
    }

    fun setConnecting(connecting: Boolean) {
        if (this.connecting == connecting) return
        this.connecting = connecting
        updateAnimationState()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        registerAnimationScaleObserver()
        updateAnimationState()
    }

    override fun onDetachedFromWindow() {
        unregisterAnimationScaleObserver()
        stopAnimation()
        visibility = GONE
        super.onDetachedFromWindow()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        updateAnimationState()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        val catSize = minOf(height.toFloat(), 96f * density)
        val wave = sin(progress * Math.PI * 10.0).toFloat()
        val left = -catSize + progress * (width + catSize)
        val top = (height - catSize) / 2f + wave * 4f * density
        val right = left + catSize
        val bottom = top + catSize
        val centerX = (left + right) / 2f
        val centerY = (top + bottom) / 2f
        val rotation = sin(progress * Math.PI * 10.0 + Math.PI / 2.0).toFloat() * 2f

        canvas.save()
        canvas.rotate(rotation, centerX, centerY)
        catDrawable.setBounds(
            left.roundToInt(),
            top.roundToInt(),
            right.roundToInt(),
            bottom.roundToInt(),
        )
        catDrawable.draw(canvas)
        canvas.restore()
    }

    private fun updateAnimationState() {
        val shouldAnimate = connecting && isAttachedToWindow &&
                windowVisibility == VISIBLE && systemAnimationsEnabled()
        if (shouldAnimate) {
            visibility = VISIBLE
            startAnimation()
        } else {
            stopAnimation()
            visibility = GONE
        }
    }

    private fun startAnimation() {
        if (animator != null) return
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 3600L
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun stopAnimation() {
        animator?.cancel()
        animator = null
        progress = 0f
        invalidate()
    }

    private fun systemAnimationsEnabled(): Boolean = if (Build.VERSION.SDK_INT >= 26) {
        ValueAnimator.areAnimatorsEnabled()
    } else {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) > 0f
    }

    private fun registerAnimationScaleObserver() {
        if (observerRegistered) return
        observerRegistered = runCatching {
            context.contentResolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
                false,
                animationScaleObserver,
            )
            true
        }.getOrDefault(false)
    }

    private fun unregisterAnimationScaleObserver() {
        if (!observerRegistered) return
        runCatching {
            context.contentResolver.unregisterContentObserver(animationScaleObserver)
        }
        observerRegistered = false
    }
}
