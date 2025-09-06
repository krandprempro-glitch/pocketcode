package com.termux.app.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDialog
import com.termux.R

/**
 * 对话框基类 - 提供统一的样式和行为
 * 使用现代Material Design 3规范
 */
abstract class BaseDialog(
    context: Context,
    theme: Int = R.style.AppTheme_Dialog
) : AppCompatDialog(context, theme) {

    protected abstract fun getLayoutResource(): Int
    protected abstract fun initViews()
    protected abstract fun setupEventListeners()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setupWindow()
        setContentView(getLayoutResource())
        
        initViews()
        setupEventListeners()
        
        // 应用动画效果
        window?.setWindowAnimations(R.style.DialogAnimation)
    }

    private fun setupWindow() {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.apply {
            setGravity(android.view.Gravity.CENTER)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
            
            // 设置对话框位置和大小
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            
            // 设置对话框边距
            val params = attributes
            params.horizontalMargin = 0.05f // 5% margin on each side
            attributes = params
        }
    }

    /**
     * 设置对话框可取消性
     */
    protected fun setupCancelable(cancelable: Boolean, canceledOnTouchOutside: Boolean = true) {
        setCancelable(cancelable)
        setCanceledOnTouchOutside(canceledOnTouchOutside)
    }

    /**
     * 设置全屏对话框（用于复杂布局）
     */
    protected fun setFullscreen() {
        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
    }

    /**
     * 设置底部对话框样式
     */
    protected fun setBottomSheetStyle() {
        window?.apply {
            setGravity(android.view.Gravity.BOTTOM)
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setWindowAnimations(R.style.BottomSheetDialogAnimation)
        }
    }

    /**
     * 显示加载状态
     */
    protected fun showLoading(show: Boolean = true) {
        // 子类可以重写此方法来显示加载指示器
    }

    /**
     * 显示错误信息
     */
    protected fun showError(message: String) {
        // 子类可以重写此方法来显示错误信息
    }

    /**
     * 安全地关闭对话框
     */
    fun dismissSafely() {
        try {
            if (isShowing) {
                dismiss()
            }
        } catch (e: Exception) {
            // 忽略异常，防止崩溃
        }
    }
}
