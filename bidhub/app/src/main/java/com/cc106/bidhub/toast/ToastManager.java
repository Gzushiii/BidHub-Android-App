package com.cc106.bidhub.toast;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.cc106.bidhub.R;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Advanced Toast Manager with queue management, animations, and customization
 */
public class ToastManager {
    private static ToastManager instance;
    private final Context context;
    private final ConcurrentLinkedQueue<ToastItem> toastQueue;
    private final Handler mainHandler;
    private ToastItem currentToast;
    private boolean isShowing = false;

    private ToastManager(Context context) {
        this.context = context.getApplicationContext();
        this.toastQueue = new ConcurrentLinkedQueue<>();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized ToastManager getInstance(Context context) {
        if (instance == null) {
            instance = new ToastManager(context);
        }
        return instance;
    }

    /**
     * Show a toast with default configuration
     */
    public void show(String message) {
        show(message, ToastConfig.info());
    }

    /**
     * Show a toast with specific type
     */
    public void show(String message, ToastType type) {
        show(message, new ToastConfig.Builder().setType(type).build());
    }

    /**
     * Show a toast with custom configuration
     */
    public void show(String message, ToastConfig config) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }

        ToastItem toastItem = new ToastItem(message, config);
        toastQueue.offer(toastItem);
        processQueue();
    }

    /**
     * Show success toast
     */
    public void showSuccess(String message) {
        show(message, ToastConfig.success());
    }

    /**
     * Show error toast
     */
    public void showError(String message) {
        show(message, ToastConfig.error());
    }

    /**
     * Show warning toast
     */
    public void showWarning(String message) {
        show(message, ToastConfig.warning());
    }

    /**
     * Show info toast
     */
    public void showInfo(String message) {
        show(message, ToastConfig.info());
    }

    /**
     * Show loading toast
     */
    public void showLoading(String message) {
        show(message, ToastConfig.loading());
    }

    /**
     * Dismiss current toast
     */
    public void dismiss() {
        if (currentToast != null && currentToast.toast != null) {
            currentToast.toast.cancel();
            currentToast = null;
            isShowing = false;
            processQueue();
        }
    }

    /**
     * Clear all pending toasts
     */
    public void clearQueue() {
        toastQueue.clear();
        dismiss();
    }

    /**
     * Process the toast queue
     */
    private void processQueue() {
        if (isShowing || toastQueue.isEmpty()) {
            return;
        }

        ToastItem nextToast = toastQueue.poll();
        if (nextToast != null) {
            showToast(nextToast);
        }
    }

    /**
     * Show a specific toast item
     */
    private void showToast(ToastItem toastItem) {
        isShowing = true;
        currentToast = toastItem;

        // Create custom toast view
        View toastView = createToastView(toastItem.message, toastItem.config);
        
        // Create toast
        Toast toast = new Toast(context);
        toast.setView(toastView);
        toast.setGravity(
            toastItem.config.getPosition().getGravity(),
            toastItem.config.getPosition().getXOffset(),
            toastItem.config.getPosition().getYOffset()
        );

        // Set duration
        int duration = toastItem.config.getDuration() == ToastDuration.CUSTOM 
            ? toastItem.config.getCustomDurationMs() 
            : toastItem.config.getDuration().getDurationMs();

        toast.setDuration(duration > 0 ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
        toastItem.toast = toast;

        // Provide haptic feedback
        if (toastItem.config.isHapticFeedbackEnabled()) {
            provideHapticFeedback();
        }

        // Show toast
        toast.show();

        // Auto-dismiss after duration
        if (duration > 0) {
            mainHandler.postDelayed(() -> {
                if (currentToast == toastItem) {
                    dismiss();
                }
            }, duration);
        }
    }

    /**
     * Create custom toast view
     */
    private View createToastView(String message, ToastConfig config) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View toastView = inflater.inflate(R.layout.toast_custom, null);

        // Get views
        LinearLayout container = toastView.findViewById(R.id.toast_container);
        ImageView iconView = toastView.findViewById(R.id.toast_icon);
        TextView messageView = toastView.findViewById(R.id.toast_message);

        // Set message
        messageView.setText(message);

        // Set colors
        int backgroundColor = config.getCustomBackgroundColor() != 0 
            ? config.getCustomBackgroundColor() 
            : ContextCompat.getColor(context, config.getType().getBackgroundColorRes());
        
        int textColor = config.getCustomTextColor() != 0 
            ? config.getCustomTextColor() 
            : ContextCompat.getColor(context, config.getType().getTextColorRes());

        container.setBackgroundColor(backgroundColor);
        messageView.setTextColor(textColor);

        // Set icon
        if (config.isShowIcon()) {
            int iconRes = config.getCustomIcon() != 0 
                ? config.getCustomIcon() 
                : config.getType().getIconRes();
            
            if (iconRes != 0) {
                Drawable icon = ContextCompat.getDrawable(context, iconRes);
                iconView.setImageDrawable(icon);
                iconView.setVisibility(View.VISIBLE);
            } else {
                iconView.setVisibility(View.GONE);
            }
        } else {
            iconView.setVisibility(View.GONE);
        }

        // Set click listener for dismissible toasts
        if (config.isDismissible()) {
            toastView.setOnClickListener(v -> dismiss());
        }

        return toastView;
    }

    /**
     * Provide haptic feedback
     */
    private void provideHapticFeedback() {
        try {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    VibrationEffect effect = VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE);
                    vibrator.vibrate(effect);
                } else {
                    vibrator.vibrate(50);
                }
            }
        } catch (Exception e) {
            // Haptic feedback is optional
        }
    }

    /**
     * Toast item wrapper
     */
    private static class ToastItem {
        final String message;
        final ToastConfig config;
        Toast toast;

        ToastItem(String message, ToastConfig config) {
            this.message = message;
            this.config = config;
        }
    }
}
