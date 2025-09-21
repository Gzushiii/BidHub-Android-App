package com.cc106.bidhub;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import com.cc106.bidhub.toast.ToastHelper;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import android.content.Context;

public class AppSettingsActivity extends BaseActivity {

    // UI Components
    private Switch switchAutoRefresh;
    private Switch switchDataSaver;
    private Switch switchAnalytics;
    private Switch switchCrashReporting;
    private Switch switchBetaFeatures;
    private TextView textViewCacheSize;
    private CardView cardClearCache;
    private CardView cardResetSettings;
    
    // SharedPreferences
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_settings);
        
        // Initialize UI
        initializeUI();
        
        // Load preferences
        loadPreferences();
        
        // Set up click listeners
        setupClickListeners();
    }
    
    /**
     * Initialize UI components
     */
    private void initializeUI() {
        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("App Settings");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        // Initialize switches
        switchAutoRefresh = findViewById(R.id.switch_auto_refresh);
        switchDataSaver = findViewById(R.id.switch_data_saver);
        switchAnalytics = findViewById(R.id.switch_analytics);
        switchCrashReporting = findViewById(R.id.switch_crash_reporting);
        switchBetaFeatures = findViewById(R.id.switch_beta_features);
        
        // Initialize text views
        textViewCacheSize = findViewById(R.id.text_view_cache_size);
        
        // Initialize cards
        cardClearCache = findViewById(R.id.card_clear_cache);
        cardResetSettings = findViewById(R.id.card_reset_settings);
        
        // Initialize preferences
        preferences = getSharedPreferences("bidhub_preferences", Context.MODE_PRIVATE);
        
        // Set cache size
        updateCacheSize();
    }
    
    /**
     * Load user preferences
     */
    private void loadPreferences() {
        switchAutoRefresh.setChecked(preferences.getBoolean("auto_refresh_enabled", true));
        switchDataSaver.setChecked(preferences.getBoolean("data_saver_enabled", false));
        switchAnalytics.setChecked(preferences.getBoolean("analytics_enabled", true));
        switchCrashReporting.setChecked(preferences.getBoolean("crash_reporting_enabled", true));
        switchBetaFeatures.setChecked(preferences.getBoolean("beta_features_enabled", false));
    }
    
    /**
     * Set up click listeners
     */
    private void setupClickListeners() {
        // Switch listeners
        switchAutoRefresh.setOnCheckedChangeListener(this::onAutoRefreshToggle);
        switchDataSaver.setOnCheckedChangeListener(this::onDataSaverToggle);
        switchAnalytics.setOnCheckedChangeListener(this::onAnalyticsToggle);
        switchCrashReporting.setOnCheckedChangeListener(this::onCrashReportingToggle);
        switchBetaFeatures.setOnCheckedChangeListener(this::onBetaFeaturesToggle);
        
        // Card click listeners
        cardClearCache.setOnClickListener(v -> showClearCacheDialog());
        cardResetSettings.setOnClickListener(v -> showResetSettingsDialog());
    }
    
    /**
     * Handle auto refresh toggle
     */
    private void onAutoRefreshToggle(CompoundButton buttonView, boolean isChecked) {
        preferences.edit().putBoolean("auto_refresh_enabled", isChecked).apply();
        ToastHelper.showSuccess(this, isChecked ? "Auto refresh enabled" : "Auto refresh disabled");
    }
    
    /**
     * Handle data saver toggle
     */
    private void onDataSaverToggle(CompoundButton buttonView, boolean isChecked) {
        preferences.edit().putBoolean("data_saver_enabled", isChecked).apply();
        ToastHelper.showSuccess(this, isChecked ? "Data saver enabled" : "Data saver disabled");
    }
    
    /**
     * Handle analytics toggle
     */
    private void onAnalyticsToggle(CompoundButton buttonView, boolean isChecked) {
        preferences.edit().putBoolean("analytics_enabled", isChecked).apply();
        ToastHelper.showSuccess(this, isChecked ? "Analytics enabled" : "Analytics disabled");
    }
    
    /**
     * Handle crash reporting toggle
     */
    private void onCrashReportingToggle(CompoundButton buttonView, boolean isChecked) {
        preferences.edit().putBoolean("crash_reporting_enabled", isChecked).apply();
        ToastHelper.showSuccess(this, isChecked ? "Crash reporting enabled" : "Crash reporting disabled");
    }
    
    /**
     * Handle beta features toggle
     */
    private void onBetaFeaturesToggle(CompoundButton buttonView, boolean isChecked) {
        preferences.edit().putBoolean("beta_features_enabled", isChecked).apply();
        ToastHelper.showInfo(this, isChecked ? "Beta features enabled" : "Beta features disabled");
    }
    
    /**
     * Show clear cache dialog
     */
    private void showClearCacheDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Clear Cache")
                .setMessage("This will clear all cached data. Are you sure?")
                .setPositiveButton("Clear", (dialog, which) -> {
                    clearCache();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    /**
     * Show reset settings dialog
     */
    private void showResetSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Reset Settings")
                .setMessage("This will reset all app settings to default values. Are you sure?")
                .setPositiveButton("Reset", (dialog, which) -> {
                    resetSettings();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    /**
     * Clear app cache
     */
    private void clearCache() {
        try {
            // Clear internal cache
            getCacheDir().delete();
            
            // Clear external cache if available
            if (getExternalCacheDir() != null) {
                getExternalCacheDir().delete();
            }
            
            ToastHelper.showSuccess(this, "Cache cleared successfully");
            updateCacheSize();
        } catch (Exception e) {
            ToastHelper.showError(this, "Failed to clear cache: " + e.getMessage());
        }
    }
    
    /**
     * Reset all settings to default
     */
    private void resetSettings() {
        try {
            preferences.edit().clear().apply();
            loadPreferences();
            ToastHelper.showSuccess(this, "Settings reset to default values");
        } catch (Exception e) {
            ToastHelper.showError(this, "Failed to reset settings: " + e.getMessage());
        }
    }
    
    /**
     * Update cache size display
     */
    private void updateCacheSize() {
        try {
            long cacheSize = getCacheDir().getTotalSpace();
            String sizeText = formatBytes(cacheSize);
            textViewCacheSize.setText(sizeText);
        } catch (Exception e) {
            textViewCacheSize.setText("Unknown");
        }
    }
    
    /**
     * Format bytes to human readable string
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected boolean isCurrentActivity(int itemId) {
        return false; // App settings is not in bottom navigation
    }
    
    @Override
    protected void setCurrentTabSelected() {
        // App settings is not in bottom navigation
    }
    
    @Override
    public String getCurrentUserEmail() {
        return getIntent().getStringExtra("USER_EMAIL");
    }
}
