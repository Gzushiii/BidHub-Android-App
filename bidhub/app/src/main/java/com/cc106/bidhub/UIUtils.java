package com.cc106.bidhub;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.widget.Button;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

public class UIUtils {
    
    private static final int HAPTIC_FEEDBACK_DURATION = 50;
    private static final int BUTTON_ANIMATION_DURATION = 100;
    
    /**
     * Provides haptic feedback for button interactions
     */
    public static void provideHapticFeedback(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                VibrationEffect effect = VibrationEffect.createOneShot(
                    HAPTIC_FEEDBACK_DURATION, 
                    VibrationEffect.DEFAULT_AMPLITUDE
                );
                vibrator.vibrate(effect);
            }
        } else {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(HAPTIC_FEEDBACK_DURATION);
            }
        }
    }
    
    /**
     * Applies button press animation with haptic feedback
     */
    public static void animateButtonPress(Button button, Runnable onClickAction) {
        // Provide haptic feedback
        provideHapticFeedback(button.getContext());
        
        // Create press animation
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(button, "scaleX", 1.0f, 0.95f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(button, "scaleY", 1.0f, 0.95f);
        
        AnimatorSet pressAnim = new AnimatorSet();
        pressAnim.playTogether(scaleX, scaleY);
        pressAnim.setDuration(BUTTON_ANIMATION_DURATION);
        
        // Create release animation
        ObjectAnimator scaleXRelease = ObjectAnimator.ofFloat(button, "scaleX", 0.95f, 1.0f);
        ObjectAnimator scaleYRelease = ObjectAnimator.ofFloat(button, "scaleY", 0.95f, 1.0f);
        
        AnimatorSet releaseAnim = new AnimatorSet();
        releaseAnim.playTogether(scaleXRelease, scaleYRelease);
        releaseAnim.setDuration(BUTTON_ANIMATION_DURATION);
        
        // Chain animations
        pressAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                releaseAnim.start();
            }
        });
        
        releaseAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (onClickAction != null) {
                    onClickAction.run();
                }
            }
        });
        
        pressAnim.start();
    }
    
    /**
     * Shows a styled toast message with better positioning
     */
    public static void showStyledToast(Context context, String message, boolean isError) {
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        
        // Position toast at top for better visibility
        toast.setGravity(android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL, 0, 200);
        
        // Style the toast background
        View toastView = toast.getView();
        if (toastView != null) {
            if (isError) {
                toastView.setBackgroundColor(ContextCompat.getColor(context, R.color.error_red));
            } else {
                toastView.setBackgroundColor(ContextCompat.getColor(context, R.color.primary_blue));
            }
            toastView.setPadding(32, 16, 32, 16);
        }
        
        toast.show();
    }
    
    /**
     * Creates a fade-in animation for views
     */
    public static void fadeInView(View view, int duration) {
        view.setAlpha(0f);
        view.animate()
            .alpha(1f)
            .setDuration(duration)
            .setInterpolator(new DecelerateInterpolator())
            .start();
    }
    
    /**
     * Creates a fade-out animation for views
     */
    public static void fadeOutView(View view, int duration, Runnable onComplete) {
        view.animate()
            .alpha(0f)
            .setDuration(duration)
            .setInterpolator(new AccelerateInterpolator())
            .withEndAction(onComplete)
            .start();
    }
    
    /**
     * Creates a loading animation for buttons
     */
    public static void showButtonLoading(Button button, boolean isLoading) {
        if (isLoading) {
            button.setEnabled(false);
            button.setText("Loading...");
            // Add rotation animation
            ObjectAnimator rotation = ObjectAnimator.ofFloat(button, "rotation", 0f, 360f);
            rotation.setDuration(1000);
            rotation.setRepeatCount(ObjectAnimator.INFINITE);
            rotation.start();
        } else {
            button.setEnabled(true);
            button.setText("Submit"); // Reset to original text
            button.animate().rotation(0f).setDuration(200).start();
        }
    }
}
