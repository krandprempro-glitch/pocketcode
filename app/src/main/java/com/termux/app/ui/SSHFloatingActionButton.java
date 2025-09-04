package com.termux.app.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.termux.R;

/**
 * SSH连接配置的浮动按钮
 */
public class SSHFloatingActionButton extends RelativeLayout {
    
    private ImageView mFabIcon;
    private OnSSHFabClickListener mListener;
    private boolean mIsVisible = true;
    private float mLastTouchX;
    private float mLastTouchY;
    private boolean mIsDragging = false;
    private static final int CLICK_THRESHOLD = 10;
    
    public interface OnSSHFabClickListener {
        void onSSHFabClick();
    }

    public SSHFloatingActionButton(Context context) {
        this(context, null);
    }

    public SSHFloatingActionButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SSHFloatingActionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // 创建ImageView作为FAB图标
        mFabIcon = new ImageView(getContext());
        mFabIcon.setImageResource(R.drawable.ic_ssh);
        mFabIcon.setBackground(null);
        mFabIcon.setScaleType(ImageView.ScaleType.CENTER);
        mFabIcon.setPadding(8, 8, 8, 8);
        
        // 设置布局参数 - 极简化按钮尺寸
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
            (int) (24 * getResources().getDisplayMetrics().density),
            (int) (24 * getResources().getDisplayMetrics().density)
        );
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        layoutParams.setMargins(0, 
            (int) (16 * getResources().getDisplayMetrics().density),
            (int) (16 * getResources().getDisplayMetrics().density), 0);
        
        mFabIcon.setLayoutParams(layoutParams);
        
        // 添加触摸事件处理拖动和点击
        mFabIcon.setOnTouchListener(this::handleTouch);
        
        // 设置初始状态 - 移除阴影效果
        mFabIcon.setElevation(0);
        mFabIcon.setStateListAnimator(null); // 禁用默认状态动画
        
        addView(mFabIcon);
    }
    
    /**
     * 处理触摸事件，实现拖动和点击功能
     */
    private boolean handleTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastTouchX = event.getRawX();
                mLastTouchY = event.getRawY();
                mIsDragging = false;
                return true;
                
            case MotionEvent.ACTION_MOVE:
                float deltaX = event.getRawX() - mLastTouchX;
                float deltaY = event.getRawY() - mLastTouchY;
                
                // 判断是否开始拖动
                if (!mIsDragging && (Math.abs(deltaX) > CLICK_THRESHOLD || Math.abs(deltaY) > CLICK_THRESHOLD)) {
                    mIsDragging = true;
                }
                
                if (mIsDragging) {
                    // 更新按钮位置
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) getLayoutParams();
                    params.leftMargin = (int) (params.leftMargin + deltaX);
                    params.topMargin = (int) (params.topMargin + deltaY);
                    
                    // 限制在屏幕范围内
                    int screenWidth = getResources().getDisplayMetrics().widthPixels;
                    int screenHeight = getResources().getDisplayMetrics().heightPixels;
                    int buttonWidth = getWidth();
                    int buttonHeight = getHeight();
                    
                    params.leftMargin = Math.max(0, Math.min(params.leftMargin, screenWidth - buttonWidth));
                    params.topMargin = Math.max(0, Math.min(params.topMargin, screenHeight - buttonHeight));
                    
                    setLayoutParams(params);
                    
                    mLastTouchX = event.getRawX();
                    mLastTouchY = event.getRawY();
                }
                return true;
                
            case MotionEvent.ACTION_UP:
                if (!mIsDragging) {
                    // 如果没有拖动，则执行点击操作
                    animateClick();
                    if (mListener != null) {
                        mListener.onSSHFabClick();
                    }
                }
                mIsDragging = false;
                return true;
                
            default:
                return false;
        }
    }
    
    /**
     * 点击动画效果
     */
    private void animateClick() {
        mFabIcon.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(100)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mFabIcon.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(100)
                                .setListener(null)
                                .start();
                    }
                })
                .start();
    }
    
    /**
     * 显示FAB
     */
    public void show() {
        if (mIsVisible) return;
        
        mIsVisible = true;
        setVisibility(View.VISIBLE);
        mFabIcon.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(200)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(null)
                .start();
    }
    
    /**
     * 隐藏FAB
     */
    public void hide() {
        if (!mIsVisible) return;
        
        mIsVisible = false;
        mFabIcon.animate()
                .scaleX(0.0f)
                .scaleY(0.0f)
                .alpha(0.0f)
                .setDuration(200)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        setVisibility(View.GONE);
                    }
                })
                .start();
    }
    
    /**
     * 切换显示/隐藏状态
     */
    public void toggle() {
        if (mIsVisible) {
            hide();
        } else {
            show();
        }
    }
    
    public void setOnSSHFabClickListener(OnSSHFabClickListener listener) {
        mListener = listener;
    }
    
    public boolean isVisible() {
        return mIsVisible;
    }
}
