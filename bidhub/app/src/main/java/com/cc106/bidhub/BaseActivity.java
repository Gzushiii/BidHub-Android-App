package com.cc106.bidhub;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
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
        View contentFrame = findViewById(R.id.content_frame);
        if (contentFrame != null) {
            UIUtils.fadeInView(contentFrame, 300);
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        
        // Don't navigate if already on the current activity
        if (isCurrentActivity(itemId)) {
            return true;
        }
        
        if (itemId == R.id.nav_home) {
            if (!(this instanceof MainActivity)) {
                navigateToActivity(MainActivity.class);
            }
        } else if (itemId == R.id.nav_browse) {
            if (!(this instanceof BrowseActivity)) {
                navigateToActivity(BrowseActivity.class);
            }
        } else if (itemId == R.id.nav_post) {
            if (!(this instanceof PostActivity)) {
                navigateToActivity(PostActivity.class);
            }
        } else if (itemId == R.id.nav_credits) {
            if (!(this instanceof CreditsActivity)) {
                navigateToActivity(CreditsActivity.class);
            }
        } else if (itemId == R.id.nav_profile) {
            if (!(this instanceof ProfileActivity)) {
                navigateToActivity(ProfileActivity.class);
            }
        }
        
        return true;
    }

    /**
     * Navigates to the specified activity with smooth transitions
     */
    protected void navigateToActivity(Class<?> activityClass) {
        try {
            Intent intent = new Intent(this, activityClass);
            intent.putExtra("USER_EMAIL", getCurrentUserEmail());
            
            // Create activity options for smooth transition
            ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(
                this,
                R.anim.slide_in_right,
                R.anim.slide_out_left
            );
            
            startActivity(intent, options.toBundle());
            finish();
        } catch (Exception e) {
            UIUtils.showStyledToast(this, "Navigation error: " + e.getMessage(), true);
        }
    }
    
    /**
     * Shows error message with consistent styling
     */
    protected void showError(String message) {
        UIUtils.showStyledToast(this, message, true);
    }
    
    /**
     * Shows success message with consistent styling
     */
    protected void showSuccess(String message) {
        UIUtils.showStyledToast(this, message, false);
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
