package com.termux.app.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.termux.R;

/**
 * Light themed toast utility class
 * Provides light-styled toast messages for better UI consistency
 */
public class LightToast {
    
    /**
     * Create and show a light-themed short toast
     */
    public static void showShort(Context context, String message) {
        show(context, message, Toast.LENGTH_SHORT);
    }
    
    /**
     * Create and show a light-themed long toast
     */
    public static void showLong(Context context, String message) {
        show(context, message, Toast.LENGTH_LONG);
    }
    
    /**
     * Create and show a light-themed toast
     */
    public static void show(Context context, String message, int duration) {
        if (context == null || message == null) {
            return;
        }
        
        try {
            // Try to use custom layout for light theme
            LayoutInflater inflater = LayoutInflater.from(context);
            View layout = inflater.inflate(R.layout.toast_light, null);
            
            TextView text = layout.findViewById(R.id.toast_text);
            text.setText(message);
            
            Toast toast = new Toast(context);
            toast.setDuration(duration);
            toast.setView(layout);
            toast.show();
        } catch (Exception e) {
            // Fallback to standard toast if custom layout fails
            Toast.makeText(context, message, duration).show();
        }
    }
}