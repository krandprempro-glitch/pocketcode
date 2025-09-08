package com.termux.app.bridge.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.*

/**
 * 动画管理器
 * 统一管理界面动画效果，提供常用的动画方法
 */
object AnimationManager {
    
    // 默认动画时长
    private const val DEFAULT_DURATION = 300L
    private const val FAST_DURATION = 150L
    private const val SLOW_DURATION = 500L
    
    /**
     * 淡入动画
     */
    @JvmStatic
    fun fadeIn(view: View, duration: Long = DEFAULT_DURATION, onComplete: (() -> Unit)? = null) {
        view.alpha = 0f
        view.visibility = View.VISIBLE
        
        view.animate()
            .alpha(1f)
            .setDuration(duration)
            .setInterpolator(DecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onComplete?.invoke()
                }
            })
            .start()
    }
    
    /**
     * 淡出动画
     */
    @JvmStatic
    fun fadeOut(view: View, duration: Long = DEFAULT_DURATION, onComplete: (() -> Unit)? = null) {
        view.animate()
            .alpha(0f)
            .setDuration(duration)
            .setInterpolator(AccelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = View.GONE
                    onComplete?.invoke()
                }
            })
            .start()
    }
    
    /**
     * 缩放弹出动画
     */
    @JvmStatic
    fun scaleIn(view: View, duration: Long = DEFAULT_DURATION, onComplete: (() -> Unit)? = null) {
        view.scaleX = 0.8f
        view.scaleY = 0.8f
        view.alpha = 0f
        view.visibility = View.VISIBLE
        
        view.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(duration)
            .setInterpolator(OvershootInterpolator(1.1f))
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onComplete?.invoke()
                }
            })
            .start()
    }
    
    /**
     * 缩放退出动画
     */
    @JvmStatic
    fun scaleOut(view: View, duration: Long = DEFAULT_DURATION, onComplete: (() -> Unit)? = null) {
        view.animate()
            .scaleX(0.8f)
            .scaleY(0.8f)
            .alpha(0f)
            .setDuration(duration)
            .setInterpolator(AccelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = View.GONE
                    view.scaleX = 1f
                    view.scaleY = 1f
                    onComplete?.invoke()
                }
            })
            .start()
    }
    
    /**
     * 从右侧滑入动画
     */
    @JvmStatic
    fun slideInFromRight(view: View, duration: Long = DEFAULT_DURATION, onComplete: (() -> Unit)? = null) {
        view.translationX = view.width.toFloat()
        view.visibility = View.VISIBLE
        
        view.animate()
            .translationX(0f)
            .setDuration(duration)
            .setInterpolator(DecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onComplete?.invoke()
                }
            })
            .start()
    }
    
    /**
     * 向右侧滑出动画
     */
    @JvmStatic
    fun slideOutToRight(view: View, duration: Long = DEFAULT_DURATION, onComplete: (() -> Unit)? = null) {
        view.animate()
            .translationX(view.width.toFloat())
            .setDuration(duration)
            .setInterpolator(AccelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = View.GONE
                    view.translationX = 0f
                    onComplete?.invoke()
                }
            })
            .start()
    }
    
    /**
     * 从左侧滑入动画
     */
    @JvmStatic
    fun slideInFromLeft(view: View, duration: Long = DEFAULT_DURATION, onComplete: (() -> Unit)? = null) {
        view.translationX = -view.width.toFloat()
        view.visibility = View.VISIBLE
        
        view.animate()
            .translationX(0f)
            .setDuration(duration)
            .setInterpolator(DecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onComplete?.invoke()
                }
            })
            .start()
    }
    
    /**
     * 从上方滑入动画
     */
    @JvmStatic
    fun slideInFromTop(view: View, duration: Long = DEFAULT_DURATION, onComplete: (() -> Unit)? = null) {
        view.translationY = -view.height.toFloat()
        view.visibility = View.VISIBLE
        
        view.animate()
            .translationY(0f)
            .setDuration(duration)
            .setInterpolator(DecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onComplete?.invoke()
                }
            })
            .start()
    }
    
    /**
     * 向上方滑出动画
     */
    @JvmStatic
    fun slideOutToTop(view: View, duration: Long = DEFAULT_DURATION, onComplete: (() -> Unit)? = null) {
        view.animate()
            .translationY(-view.height.toFloat())
            .setDuration(duration)
            .setInterpolator(AccelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = View.GONE
                    view.translationY = 0f
                    onComplete?.invoke()
                }
            })
            .start()
    }
    
    /**
     * 点击波纹效果
     */
    @JvmStatic
    fun rippleEffect(view: View, onComplete: (() -> Unit)? = null) {
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(FAST_DURATION)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(FAST_DURATION)
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                onComplete?.invoke()
                            }
                        })
                        .start()
                }
            })
            .start()
    }
    
    /**
     * 摇摆动画（错误提示）
     */
    @JvmStatic
    fun shake(view: View, intensity: Float = 10f, onComplete: (() -> Unit)? = null) {
        val animator = ObjectAnimator.ofFloat(view, "translationX", 0f, intensity, -intensity, intensity, -intensity, 0f)
        animator.duration = SLOW_DURATION
        animator.interpolator = DecelerateInterpolator()
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onComplete?.invoke()
            }
        })
        animator.start()
    }
    
    /**
     * 旋转动画
     */
    @JvmStatic
    fun rotate(view: View, degrees: Float, duration: Long = DEFAULT_DURATION, onComplete: (() -> Unit)? = null) {
        view.animate()
            .rotation(degrees)
            .setDuration(duration)
            .setInterpolator(LinearInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onComplete?.invoke()
                }
            })
            .start()
    }
    
    /**
     * 无限旋转动画
     */
    @JvmStatic
    fun rotateInfinite(view: View, duration: Long = 1000L): ObjectAnimator {
        val animator = ObjectAnimator.ofFloat(view, "rotation", 0f, 360f)
        animator.duration = duration
        animator.repeatCount = ValueAnimator.INFINITE
        animator.interpolator = LinearInterpolator()
        animator.start()
        return animator
    }
    
    /**
     * 停止无限旋转动画
     */
    @JvmStatic
    fun stopInfiniteRotation(animator: ObjectAnimator?, view: View) {
        animator?.cancel()
        view.rotation = 0f
    }
    
    /**
     * 弹跳动画
     */
    @JvmStatic
    fun bounce(view: View, onComplete: (() -> Unit)? = null) {
        val animator = ObjectAnimator.ofFloat(view, "translationY", 0f, -30f, 0f)
        animator.duration = SLOW_DURATION
        animator.interpolator = BounceInterpolator()
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onComplete?.invoke()
            }
        })
        animator.start()
    }
    
    /**
     * 闪烁动画
     */
    @JvmStatic
    fun blink(view: View, count: Int = 3, onComplete: (() -> Unit)? = null) {
        val animator = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f, 1f)
        animator.duration = FAST_DURATION * 2
        animator.repeatCount = count - 1
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                view.alpha = 1f
                onComplete?.invoke()
            }
        })
        animator.start()
    }
    
    /**
     * 组合动画：同时执行多个动画
     */
    @JvmStatic
    fun combineAnimations(view: View, duration: Long = DEFAULT_DURATION, onComplete: (() -> Unit)? = null, block: View.() -> Unit) {
        val animator = view.animate().setDuration(duration)
        view.block()
        animator.setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onComplete?.invoke()
            }
        }).start()
    }
    
    /**
     * 动画预设
     */
    enum class AnimationPreset {
        FADE_IN, FADE_OUT, SCALE_IN, SCALE_OUT, 
        SLIDE_IN_RIGHT, SLIDE_OUT_RIGHT, 
        SLIDE_IN_LEFT, SLIDE_IN_TOP, SLIDE_OUT_TOP,
        RIPPLE, SHAKE, BOUNCE, BLINK
    }
    
    /**
     * 执行预设动画
     */
    @JvmStatic
    fun animate(view: View, preset: AnimationPreset, duration: Long = DEFAULT_DURATION, onComplete: (() -> Unit)? = null) {
        when (preset) {
            AnimationPreset.FADE_IN -> fadeIn(view, duration, onComplete)
            AnimationPreset.FADE_OUT -> fadeOut(view, duration, onComplete)
            AnimationPreset.SCALE_IN -> scaleIn(view, duration, onComplete)
            AnimationPreset.SCALE_OUT -> scaleOut(view, duration, onComplete)
            AnimationPreset.SLIDE_IN_RIGHT -> slideInFromRight(view, duration, onComplete)
            AnimationPreset.SLIDE_OUT_RIGHT -> slideOutToRight(view, duration, onComplete)
            AnimationPreset.SLIDE_IN_LEFT -> slideInFromLeft(view, duration, onComplete)
            AnimationPreset.SLIDE_IN_TOP -> slideInFromTop(view, duration, onComplete)
            AnimationPreset.SLIDE_OUT_TOP -> slideOutToTop(view, duration, onComplete)
            AnimationPreset.RIPPLE -> rippleEffect(view, onComplete)
            AnimationPreset.SHAKE -> shake(view, onComplete = onComplete)
            AnimationPreset.BOUNCE -> bounce(view, onComplete)
            AnimationPreset.BLINK -> blink(view, onComplete = onComplete)
        }
    }
}