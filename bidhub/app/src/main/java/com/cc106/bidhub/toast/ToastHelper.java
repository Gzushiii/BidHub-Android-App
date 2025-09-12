package com.cc106.bidhub.toast;

import android.content.Context;

/**
 * Convenience class for easy toast access throughout the application
 */
public class ToastHelper {
    
    /**
     * Show a simple message toast
     */
    public static void show(Context context, String message) {
        ToastManager.getInstance(context).show(message);
    }
    
    /**
     * Show a success toast
     */
    public static void showSuccess(Context context, String message) {
        ToastManager.getInstance(context).showSuccess(message);
    }
    
    /**
     * Show an error toast
     */
    public static void showError(Context context, String message) {
        ToastManager.getInstance(context).showError(message);
    }
    
    /**
     * Show a warning toast
     */
    public static void showWarning(Context context, String message) {
        ToastManager.getInstance(context).showWarning(message);
    }
    
    /**
     * Show an info toast
     */
    public static void showInfo(Context context, String message) {
        ToastManager.getInstance(context).showInfo(message);
    }
    
    /**
     * Show a loading toast
     */
    public static void showLoading(Context context, String message) {
        ToastManager.getInstance(context).showLoading(message);
    }
    
    /**
     * Show a custom toast with configuration
     */
    public static void show(Context context, String message, ToastConfig config) {
        ToastManager.getInstance(context).show(message, config);
    }
    
    /**
     * Show a custom toast with type
     */
    public static void show(Context context, String message, ToastType type) {
        ToastManager.getInstance(context).show(message, type);
    }
    
    /**
     * Dismiss current toast
     */
    public static void dismiss(Context context) {
        ToastManager.getInstance(context).dismiss();
    }
    
    /**
     * Clear all pending toasts
     */
    public static void clearQueue(Context context) {
        ToastManager.getInstance(context).clearQueue();
    }
    
    /**
     * Show a quick success message
     */
    public static void quickSuccess(Context context, String message) {
        ToastConfig config = new ToastConfig.Builder()
                .setType(ToastType.SUCCESS)
                .setDuration(ToastDuration.SHORT)
                .setAnimation(ToastAnimation.FADE_IN_OUT)
                .setPosition(ToastPosition.TOP)
                .build();
        show(context, message, config);
    }
    
    /**
     * Show a quick error message
     */
    public static void quickError(Context context, String message) {
        ToastConfig config = new ToastConfig.Builder()
                .setType(ToastType.ERROR)
                .setDuration(ToastDuration.SHORT)
                .setAnimation(ToastAnimation.SLIDE_FROM_TOP)
                .setPosition(ToastPosition.TOP)
                .build();
        show(context, message, config);
    }
    
    /**
     * Show a persistent loading message
     */
    public static void showPersistentLoading(Context context, String message) {
        ToastConfig config = new ToastConfig.Builder()
                .setType(ToastType.LOADING)
                .setDuration(ToastDuration.VERY_LONG)
                .setAnimation(ToastAnimation.SCALE_IN_OUT)
                .setPosition(ToastPosition.CENTER)
                .setDismissible(false)
                .build();
        show(context, message, config);
    }
}
