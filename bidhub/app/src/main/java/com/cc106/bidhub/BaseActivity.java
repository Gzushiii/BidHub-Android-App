package com.cc106.bidhub;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import com.cc106.bidhub.toast.ToastHelper;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public abstract class BaseActivity extends AppCompatActivity implements BottomNavigationView.OnItemSelectedListener {

    protected BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_with_bottom_nav);
        
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setOnItemSelectedListener(this);
        
        // Set the current tab as selected
        setCurrentTabSelected();
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
    protected abstract String getCurrentUserEmail();
}
