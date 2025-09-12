package com.cc106.bidhub;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.cc106.bidhub.toast.ToastHelper;
import com.cc106.bidhub.toast.ToastConfig;
import com.cc106.bidhub.toast.ToastType;
import com.cc106.bidhub.toast.ToastDuration;
import com.cc106.bidhub.toast.ToastPosition;
import com.cc106.bidhub.toast.ToastAnimation;

/**
 * Test activity to demonstrate the new toast notification system
 * This activity can be used to test all toast features
 */
public class ToastTestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Create a simple layout for testing
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);
        
        // Add test buttons
        addTestButton(layout, "Basic Success", () -> ToastHelper.showSuccess(this, "Success message!"));
        addTestButton(layout, "Basic Error", () -> ToastHelper.showError(this, "Error message!"));
        addTestButton(layout, "Basic Warning", () -> ToastHelper.showWarning(this, "Warning message!"));
        addTestButton(layout, "Basic Info", () -> ToastHelper.showInfo(this, "Info message!"));
        addTestButton(layout, "Basic Loading", () -> ToastHelper.showLoading(this, "Loading..."));
        
        addTestButton(layout, "Quick Success", () -> ToastHelper.quickSuccess(this, "Quick success!"));
        addTestButton(layout, "Quick Error", () -> ToastHelper.quickError(this, "Quick error!"));
        
        addTestButton(layout, "Custom Duration", () -> {
            ToastConfig config = new ToastConfig.Builder()
                    .setType(ToastType.SUCCESS)
                    .setCustomDuration(10000) // 10 seconds
                    .build();
            ToastHelper.show(this, "This will show for 10 seconds", config);
        });
        
        addTestButton(layout, "Top Position", () -> {
            ToastConfig config = new ToastConfig.Builder()
                    .setType(ToastType.INFO)
                    .setPosition(ToastPosition.TOP)
                    .setAnimation(ToastAnimation.SLIDE_FROM_TOP)
                    .build();
            ToastHelper.show(this, "Message at top", config);
        });
        
        addTestButton(layout, "Center Position", () -> {
            ToastConfig config = new ToastConfig.Builder()
                    .setType(ToastType.WARNING)
                    .setPosition(ToastPosition.CENTER)
                    .setAnimation(ToastAnimation.SCALE_IN_OUT)
                    .build();
            ToastHelper.show(this, "Message in center", config);
        });
        
        addTestButton(layout, "Bounce Animation", () -> {
            ToastConfig config = new ToastConfig.Builder()
                    .setType(ToastType.SUCCESS)
                    .setAnimation(ToastAnimation.BOUNCE_IN_OUT)
                    .build();
            ToastHelper.show(this, "Bouncy message!", config);
        });
        
        addTestButton(layout, "Custom Colors", () -> {
            ToastConfig config = new ToastConfig.Builder()
                    .setType(ToastType.CUSTOM)
                    .setCustomColors(android.graphics.Color.MAGENTA, android.graphics.Color.WHITE)
                    .setCustomIcon(android.R.drawable.ic_dialog_info)
                    .build();
            ToastHelper.show(this, "Custom colored message", config);
        });
        
        addTestButton(layout, "No Haptic", () -> {
            ToastConfig config = new ToastConfig.Builder()
                    .setType(ToastType.INFO)
                    .setHapticFeedback(false)
                    .build();
            ToastHelper.show(this, "No vibration", config);
        });
        
        addTestButton(layout, "Non-dismissible", () -> {
            ToastConfig config = new ToastConfig.Builder()
                    .setType(ToastType.LOADING)
                    .setDismissible(false)
                    .setDuration(ToastDuration.VERY_LONG)
                    .build();
            ToastHelper.show(this, "Cannot dismiss by tapping", config);
        });
        
        addTestButton(layout, "Queue Test", () -> {
            ToastHelper.showSuccess(this, "First message");
            ToastHelper.showInfo(this, "Second message");
            ToastHelper.showWarning(this, "Third message");
        });
        
        addTestButton(layout, "Dismiss Current", () -> ToastHelper.dismiss(this));
        addTestButton(layout, "Clear Queue", () -> ToastHelper.clearQueue(this));
        
        setContentView(layout);
    }
    
    private void addTestButton(LinearLayout layout, String text, Runnable onClick) {
        Button button = new Button(this);
        button.setText(text);
        button.setOnClickListener(v -> onClick.run());
        button.setPadding(20, 20, 20, 20);
        layout.addView(button);
    }
}
