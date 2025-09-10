package com.termux.app.floating.views

import android.animation.ValueAnimator
import android.content.Context
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import com.termux.shared.logger.Logger

/**
 * 可拖拽视图基类
 * 提供拖拽功能的基础视图组件（在Activity内部）
 */
abstract class DraggableView(context: Context) : FrameLayout(context) {
    
    companion object {
        private const val LOG_TAG = "DraggableView"
    }
    
    private var isDragging = false
    private var initialX = 0f
    private var initialY = 0f
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var parentContainer: ViewGroup? = null
    
    init {
        setupTouchListener()
        setupInitialPosition()
    }
    
    
    private fun setupInitialPosition() {
        // 设置初始位置：右上角
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            // 默认位置在右上角，距离边缘60dp
            topMargin = dpToPx(60)
            marginEnd = dpToPx(20)
            gravity = android.view.Gravity.TOP or android.view.Gravity.END
        }
    }
    
    private fun setupTouchListener() {
        setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> onTouchDown(event)
                MotionEvent.ACTION_MOVE -> onTouchMove(event)
                MotionEvent.ACTION_UP -> onTouchUp(event)
                else -> false
            }
        }
    }
    
    private fun onTouchDown(event: MotionEvent): Boolean {
        initialX = x
        initialY = y
        initialTouchX = event.rawX
        initialTouchY = event.rawY
        
        onDragStart()
        return true
    }
    
    private fun onTouchMove(event: MotionEvent): Boolean {
        if (!isDragging && (kotlin.math.abs(event.rawX - initialTouchX) > 10 ||
                    kotlin.math.abs(event.rawY - initialTouchY) > 10)) {
            isDragging = true
            onDragMove()
        }
        
        if (isDragging) {
            val newX = initialX + (event.rawX - initialTouchX)
            val newY = initialY + (event.rawY - initialTouchY)
            
            // 限制拖拽范围在父容器内
            parentContainer?.let { container ->
                val constrainedX = maxOf(0f, minOf(container.width - width.toFloat(), newX))
                val constrainedY = maxOf(0f, minOf(container.height - height.toFloat(), newY))
                
                x = constrainedX
                y = constrainedY
            }
            return true
        }
        
        return false
    }
    
    @Suppress("UNUSED_PARAMETER")
    private fun onTouchUp(event: MotionEvent): Boolean {
        if (isDragging) {
            isDragging = false
            snapToEdge()
            onDragEnd()
            return true
        } else {
            // 如果不是拖拽，则视为点击
            performClick()
            return true
        }
    }
    
    /**
     * 松手后自动吸附到边缘
     */
    private fun snapToEdge() {
        parentContainer?.let { container ->
            val targetX = if (x > container.width / 2) {
                // 吸附到右边缘
                container.width - width.toFloat()
            } else {
                // 吸附到左边缘
                0f
            }
            
            val animator = ValueAnimator.ofFloat(x, targetX)
            animator.duration = 200
            animator.interpolator = DecelerateInterpolator()
            animator.addUpdateListener { animation ->
                x = animation.animatedValue as Float
            }
            animator.start()
        }
    }
    
    // 抽象方法，子类实现
    protected abstract fun onDragStart()
    protected abstract fun onDragMove()
    protected abstract fun onDragEnd()
    
    // 工具方法
    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
    
    /**
     * 添加到Activity的容器中显示
     */
    fun show(container: ViewGroup) {
        try {
            parentContainer = container
            container.addView(this, layoutParams)
        } catch (e: Exception) {
            Logger.logError(LOG_TAG, "Failed to show floating view: ${e.message}")
        }
    }
    
    /**
     * 从容器中移除
     */
    open fun hide() {
        try {
            parentContainer?.removeView(this)
            parentContainer = null
        } catch (e: Exception) {
            Logger.logError(LOG_TAG, "Failed to hide floating view: ${e.message}")
        }
    }
}