package com.cc106.bidhub.toast;

import android.content.Context;

/**
 * Examples of how to use the new Toast system
 * This class demonstrates various ways to show toast notifications
 */
public class ToastExamples {
    
    /**
     * Basic toast examples
     */
    public static void showBasicExamples(Context context) {
        // Simple message
        ToastHelper.show(context, "Hello World!");
        
        // Success message
        ToastHelper.showSuccess(context, "Operation completed successfully!");
        
        // Error message
        ToastHelper.showError(context, "Something went wrong!");
        
        // Warning message
        ToastHelper.showWarning(context, "Please check your input");
        
        // Info message
        ToastHelper.showInfo(context, "This is an informational message");
        
        // Loading message
        ToastHelper.showLoading(context, "Please wait...");
    }
    
    /**
     * Custom configuration examples
     */
    public static void showCustomExamples(Context context) {
        // Custom duration
        ToastConfig longConfig = new ToastConfig.Builder()
                .setType(ToastType.SUCCESS)
                .setDuration(ToastDuration.VERY_LONG)
                .build();
        ToastHelper.show(context, "This message will stay for 7 seconds", longConfig);
        
        // Custom position
        ToastConfig topConfig = new ToastConfig.Builder()
                .setType(ToastType.INFO)
                .setPosition(ToastPosition.TOP)
                .setAnimation(ToastAnimation.SLIDE_FROM_TOP)
                .build();
        ToastHelper.show(context, "Message at the top", topConfig);
        
        // Custom colors
        ToastConfig customConfig = new ToastConfig.Builder()
                .setType(ToastType.CUSTOM)
                .setCustomColors(android.graphics.Color.MAGENTA, android.graphics.Color.WHITE)
                .setCustomIcon(android.R.drawable.ic_dialog_info)
                .setAnimation(ToastAnimation.BOUNCE_IN_OUT)
                .build();
        ToastHelper.show(context, "Custom styled message", customConfig);
        
        // No haptic feedback
        ToastConfig silentConfig = new ToastConfig.Builder()
                .setType(ToastType.INFO)
                .setHapticFeedback(false)
                .build();
        ToastHelper.show(context, "Silent message", silentConfig);
        
        // Non-dismissible toast
        ToastConfig persistentConfig = new ToastConfig.Builder()
                .setType(ToastType.LOADING)
                .setDismissible(false)
                .setDuration(ToastDuration.VERY_LONG)
                .build();
        ToastHelper.show(context, "This cannot be dismissed by tapping", persistentConfig);
    }
    
    /**
     * Quick utility examples
     */
    public static void showQuickExamples(Context context) {
        // Quick success (short duration, fade animation)
        ToastHelper.quickSuccess(context, "Quick success!");
        
        // Quick error (short duration, slide animation)
        ToastHelper.quickError(context, "Quick error!");
        
        // Persistent loading
        ToastHelper.showPersistentLoading(context, "Processing...");
    }
    
    /**
     * Advanced usage examples
     */
    public static void showAdvancedExamples(Context context) {
        // Queue management
        ToastManager manager = ToastManager.getInstance(context);
        
        // Show multiple toasts (they will queue)
        manager.showSuccess("First message");
        manager.showInfo("Second message");
        manager.showWarning("Third message");
        
        // Dismiss current toast
        manager.dismiss();
        
        // Clear all pending toasts
        manager.clearQueue();
        
        // Custom animation sequence
        ToastConfig slideConfig = new ToastConfig.Builder()
                .setType(ToastType.SUCCESS)
                .setAnimation(ToastAnimation.SLIDE_FROM_LEFT)
                .setPosition(ToastPosition.TOP_LEFT)
                .build();
        ToastHelper.show(context, "Sliding from left", slideConfig);
        
        // Scale animation
        ToastConfig scaleConfig = new ToastConfig.Builder()
                .setType(ToastType.INFO)
                .setAnimation(ToastAnimation.SCALE_IN_OUT)
                .setPosition(ToastPosition.CENTER)
                .build();
        ToastHelper.show(context, "Scaling animation", scaleConfig);
    }
    
    /**
     * Preset configuration examples
     */
    public static void showPresetExamples(Context context) {
        // Use predefined configurations
        ToastHelper.show(context, "Success message", ToastConfig.success());
        ToastHelper.show(context, "Error message", ToastConfig.error());
        ToastHelper.show(context, "Warning message", ToastConfig.warning());
        ToastHelper.show(context, "Info message", ToastConfig.info());
        ToastHelper.show(context, "Loading message", ToastConfig.loading());
    }
}
