package com.cc106.bidhub;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import com.cc106.bidhub.toast.ToastHelper;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.activity.OnBackPressedCallback;
import com.google.android.material.navigation.NavigationBarView;

public abstract class BaseActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener {

    protected NavigationBarView bottomNavigationView;
    private OnBackPressedCallback backPressedCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_with_bottom_nav);
        
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setOnItemSelectedListener(this);
        
        // Set the current tab as selected
        setCurrentTabSelected();
        
        // Setup back button handling
        setupBackButtonHandling();
    }
    
    /**
     * Setup back button handling for consistent navigation
     */
    private void setupBackButtonHandling() {
        backPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackPressed();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, backPressedCallback);
    }
    
    /**
     * Handle back button press - override in subclasses for custom behavior
     * Default behavior: finish activity or navigate to MainActivity
     */
    protected void handleBackPressed() {
        // If this is MainActivity, exit app on back press
        if (this instanceof MainActivity) {
            finishAffinity();
        } else {
            // For other activities, finish and return to previous
            finish();
        }
    }
    
    /**
     * Call this method after inflating content in child activities
     */
    protected void animateContentIn() {
        // Temporarily disabled to debug crash
        // View contentFrame = findViewById(R.id.content_frame);
        // if (contentFrame != null) {
        //     UIUtils.fadeInView(contentFrame, 300);
        // }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // This method will be overridden in MainActivity for fragment navigation
        return false;
    }

    /**
     * Navigates to the specified activity with smooth transitions
     */
    protected void navigateToActivity(Class<?> activityClass) {
        try {
            Intent intent = new Intent(this, activityClass);
            intent.putExtra("USER_EMAIL", getCurrentUserEmail());
            
            // Temporarily use simple navigation without custom animations
            startActivity(intent);
            finish();
        } catch (Exception e) {
            // Use new toast system
            ToastHelper.showError(this, "Navigation error: " + e.getMessage());
        }
    }
    
    /**
     * Shows error message with consistent styling
     */
    protected void showError(String message) {
        ToastHelper.showError(this, message);
    }
    
    /**
     * Shows success message with consistent styling
     */
    protected void showSuccess(String message) {
        ToastHelper.showSuccess(this, message);
    }
    
    /**
     * Handles loading states for UI elements
     */
    protected void setLoadingState(boolean isLoading) {
        // Override in subclasses for specific loading behavior
    }

    protected abstract boolean isCurrentActivity(int itemId);
    protected abstract void setCurrentTabSelected();
    public abstract String getCurrentUserEmail();
}
