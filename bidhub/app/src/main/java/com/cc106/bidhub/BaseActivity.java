package com.cc106.bidhub;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public abstract class BaseActivity extends AppCompatActivity {

    protected BottomNavigationView bottomNavigationView;
    protected SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "BidHubPrefs";
    private static final String KEY_DARK_MODE = "dark_mode_enabled";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before setContentView
        applyTheme();
        super.onCreate(savedInstanceState);
        
        // Initialize shared preferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        // Set up bottom navigation
        setupBottomNavigation();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_app_bar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_dark_mode) {
            toggleDarkMode();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupBottomNavigation() {
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        if (bottomNavigationView != null) {
            bottomNavigationView.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                Class<?> targetActivity = getTargetActivity(itemId);
                
                if (targetActivity != null && !this.getClass().equals(targetActivity)) {
                    Intent intent = new Intent(this, targetActivity);
                    startActivity(intent);
                    return true;
                }
                return false;
            });
        }
    }

    private Class<?> getTargetActivity(int itemId) {
        if (itemId == R.id.nav_home) {
            return MainActivity.class;
        } else if (itemId == R.id.nav_browse) {
            return BrowseActivity.class;
        } else if (itemId == R.id.nav_post) {
            return PostActivity.class;
        } else if (itemId == R.id.nav_credits) {
            return CreditsActivity.class;
        } else if (itemId == R.id.nav_profile) {
            return ProfileActivity.class;
        }
        return null;
    }

    private void applyTheme() {
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean(KEY_DARK_MODE, false);
        
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void toggleDarkMode() {
        boolean currentMode = sharedPreferences.getBoolean(KEY_DARK_MODE, false);
        boolean newMode = !currentMode;
        
        sharedPreferences.edit().putBoolean(KEY_DARK_MODE, newMode).apply();
        
        if (newMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            Toast.makeText(this, "Dark mode enabled", Toast.LENGTH_SHORT).show();
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            Toast.makeText(this, "Light mode enabled", Toast.LENGTH_SHORT).show();
        }
        
        // Recreate activity to apply theme changes
        recreate();
    }

    protected void setSelectedNavItem(int navItemId) {
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(navItemId);
        }
    }
}
