package com.termux.app.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import com.termux.R

/**
 * 连接状态显示组件
 * 显示SSH/SFTP连接状态，支持动画效果
 */
class ConnectionStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val statusIcon: ImageView
    private val statusText: TextView
    private var currentStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED
    private var rotateAnimation: Animation? = null

    enum class ConnectionStatus {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        ERROR
    }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(resources.getDimensionPixelSize(R.dimen.connection_status_padding))

        // 创建状态图标
        statusIcon = ImageView(context).apply {
            val size = resources.getDimensionPixelSize(R.dimen.connection_status_icon_size)
            layoutParams = LayoutParams(size, size).apply {
                setMargins(0, 0, resources.getDimensionPixelSize(R.dimen.connection_status_icon_margin), 0)
            }
        }

        // 创建状态文本
        statusText = TextView(context).apply {
            textSize = 12f
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }

        addView(statusIcon)
        addView(statusText)

        // 初始化为未连接状态
        updateStatus(ConnectionStatus.DISCONNECTED, "未连接")
    }

    fun updateStatus(status: ConnectionStatus, message: String = "") {
        if (currentStatus == status && message.isEmpty()) return
        
        currentStatus = status
        
        // 停止之前的动画
        statusIcon.clearAnimation()
        rotateAnimation?.cancel()
        
        when (status) {
            ConnectionStatus.DISCONNECTED -> {
                setStatusIcon(android.R.drawable.presence_offline)
                setStatusText(message.ifEmpty { "未连接" })
                setStatusColor(R.color.connection_status_disconnected)
            }
            
            ConnectionStatus.CONNECTING -> {
                setStatusIcon(R.drawable.ic_connecting)
                setStatusText(message.ifEmpty { "连接中..." })
                setStatusColor(R.color.connection_status_connecting)
                startConnectingAnimation()
            }
            
            ConnectionStatus.CONNECTED -> {
                setStatusIcon(android.R.drawable.presence_online)
                setStatusText(message.ifEmpty { "已连接" })
                setStatusColor(R.color.connection_status_connected)
                // 连接成功时显示脉冲动画
                startPulseAnimation()
            }
            
            ConnectionStatus.ERROR -> {
                setStatusIcon(android.R.drawable.presence_busy)
                setStatusText(message.ifEmpty { "连接错误" })
                setStatusColor(R.color.connection_status_error)
                // 错误时显示震动动画
                startShakeAnimation()
            }
        }
    }

    fun setConnectionInfo(serverInfo: String) {
        if (currentStatus == ConnectionStatus.CONNECTED) {
            statusText.text = "已连接 - $serverInfo"
        }
    }

    private fun setStatusIcon(drawableRes: Int) {
        statusIcon.setImageResource(drawableRes)
    }

    private fun setStatusText(text: String) {
        statusText.text = text
    }

    private fun setStatusColor(colorRes: Int) {
        val color = ContextCompat.getColor(context, colorRes)
        statusIcon.setColorFilter(color)
        statusText.setTextColor(color)
    }

    private fun startConnectingAnimation() {
        rotateAnimation = RotateAnimation(
            0f, 360f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 1000
            repeatCount = Animation.INFINITE
            repeatMode = Animation.RESTART
        }
        
        statusIcon.startAnimation(rotateAnimation)
    }

    private fun startPulseAnimation() {
        val animator = ValueAnimator.ofFloat(1f, 0.7f, 1f).apply {
            duration = 800
            repeatCount = 2
            addUpdateListener { animation ->
                val scale = animation.animatedValue as Float
                statusIcon.scaleX = scale
                statusIcon.scaleY = scale
                statusIcon.alpha = 0.7f + (scale - 0.7f) * 2f
            }
        }
        animator.start()
    }

    private fun startShakeAnimation() {
        val animator = ValueAnimator.ofFloat(-10f, 10f, -5f, 5f, 0f).apply {
            duration = 500
            addUpdateListener { animation ->
                val translationX = animation.animatedValue as Float
                statusIcon.translationX = translationX
            }
        }
        animator.start()
    }

    /**
     * 获取当前连接状态
     */
    fun getCurrentStatus(): ConnectionStatus = currentStatus

    /**
     * 检查是否已连接
     */
    fun isConnected(): Boolean = currentStatus == ConnectionStatus.CONNECTED

    /**
     * 检查是否正在连接
     */
    fun isConnecting(): Boolean = currentStatus == ConnectionStatus.CONNECTING

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 清理动画
        statusIcon.clearAnimation()
        rotateAnimation?.cancel()
    }
}