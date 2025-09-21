package com.cc106.bidhub;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import com.cc106.bidhub.toast.ToastHelper;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import android.content.Context;

public class SettingsActivity extends BaseActivity {

    // UI Components
    private Switch switchNotifications;
    private Switch switchBiometric;
    private Switch switchDarkMode;
    private Switch switchAutoBid;
    private Switch switchSoundEffects;
    private TextView textViewVersion;
    private TextView textViewAccountType;
    private CardView cardAccountSettings;
    private CardView cardPrivacySettings;
    private CardView cardNotificationSettings;
    private CardView cardAppSettings;
    private CardView cardAbout;
    private CardView cardHelp;
    private CardView cardLogout;
    
    // SharedPreferences
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
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
            getSupportActionBar().setTitle("Settings");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        // Initialize switches
        switchNotifications = findViewById(R.id.switch_notifications);
        switchBiometric = findViewById(R.id.switch_biometric);
        switchDarkMode = findViewById(R.id.switch_dark_mode);
        switchAutoBid = findViewById(R.id.switch_auto_bid);
        switchSoundEffects = findViewById(R.id.switch_sound_effects);
        
        // Initialize text views
        textViewVersion = findViewById(R.id.text_view_version);
        textViewAccountType = findViewById(R.id.text_view_account_type);
        
        // Initialize cards
        cardAccountSettings = findViewById(R.id.card_account_settings);
        cardPrivacySettings = findViewById(R.id.card_privacy_settings);
        cardNotificationSettings = findViewById(R.id.card_notification_settings);
        cardAppSettings = findViewById(R.id.card_app_settings);
        cardAbout = findViewById(R.id.card_about);
        cardHelp = findViewById(R.id.card_help);
        cardLogout = findViewById(R.id.card_logout);
        
        // Initialize preferences
        preferences = getSharedPreferences("bidhub_preferences", Context.MODE_PRIVATE);
        
        // Set version info
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            textViewVersion.setText("Version " + versionName);
        } catch (Exception e) {
            textViewVersion.setText("Version 1.0.0");
        }
    }
    
    /**
     * Load user preferences
     */
    private void loadPreferences() {
        // Load notification settings
        switchNotifications.setChecked(preferences.getBoolean("notifications_enabled", true));
        switchBiometric.setChecked(preferences.getBoolean("biometric_enabled", false));
        switchDarkMode.setChecked(preferences.getBoolean("dark_mode_enabled", false));
        switchAutoBid.setChecked(preferences.getBoolean("auto_bid_enabled", false));
        switchSoundEffects.setChecked(preferences.getBoolean("sound_effects_enabled", true));
        
        // Set account type
        textViewAccountType.setText("Standard Account");
    }
    
    /**
     * Set up click listeners
     */
    private void setupClickListeners() {
        // Switch listeners
        switchNotifications.setOnCheckedChangeListener(this::onNotificationToggle);
        switchBiometric.setOnCheckedChangeListener(this::onBiometricToggle);
        switchDarkMode.setOnCheckedChangeListener(this::onDarkModeToggle);
        switchAutoBid.setOnCheckedChangeListener(this::onAutoBidToggle);
        switchSoundEffects.setOnCheckedChangeListener(this::onSoundEffectsToggle);
        
        // Card click listeners
        cardAccountSettings.setOnClickListener(v -> openAccountSettings());
        cardPrivacySettings.setOnClickListener(v -> openPrivacySettings());
        cardNotificationSettings.setOnClickListener(v -> openNotificationSettings());
        cardAppSettings.setOnClickListener(v -> openAppSettings());
        cardAbout.setOnClickListener(v -> openAbout());
        cardHelp.setOnClickListener(v -> openHelp());
        cardLogout.setOnClickListener(v -> showLogoutDialog());
    }
    
    /**
     * Handle notification toggle
     */
    private void onNotificationToggle(CompoundButton buttonView, boolean isChecked) {
        preferences.edit().putBoolean("notifications_enabled", isChecked).apply();
        ToastHelper.showSuccess(this, isChecked ? "Notifications enabled" : "Notifications disabled");
    }
    
    /**
     * Handle biometric toggle
     */
    private void onBiometricToggle(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            // Check if biometric authentication is available
            // For now, just show a message
            ToastHelper.showInfo(this, "Biometric authentication will be available soon");
            switchBiometric.setChecked(false);
        } else {
            preferences.edit().putBoolean("biometric_enabled", false).apply();
            ToastHelper.showSuccess(this, "Biometric authentication disabled");
        }
    }
    
    /**
     * Handle dark mode toggle
     */
    private void onDarkModeToggle(CompoundButton buttonView, boolean isChecked) {
        preferences.edit().putBoolean("dark_mode_enabled", isChecked).apply();
        ToastHelper.showInfo(this, "Dark mode setting saved. Restart app to apply changes.");
    }
    
    /**
     * Handle auto bid toggle
     */
    private void onAutoBidToggle(CompoundButton buttonView, boolean isChecked) {
        preferences.edit().putBoolean("auto_bid_enabled", isChecked).apply();
        ToastHelper.showSuccess(this, isChecked ? "Auto bid enabled" : "Auto bid disabled");
    }
    
    /**
     * Handle sound effects toggle
     */
    private void onSoundEffectsToggle(CompoundButton buttonView, boolean isChecked) {
        preferences.edit().putBoolean("sound_effects_enabled", isChecked).apply();
        ToastHelper.showSuccess(this, isChecked ? "Sound effects enabled" : "Sound effects disabled");
    }
    
    /**
     * Open account settings
     */
    private void openAccountSettings() {
        Intent intent = new Intent(this, AccountSettingsActivity.class);
        intent.putExtra("USER_EMAIL", getCurrentUserEmail());
        startActivity(intent);
    }
    
    /**
     * Open privacy settings
     */
    private void openPrivacySettings() {
        Intent intent = new Intent(this, PrivacySettingsActivity.class);
        intent.putExtra("USER_EMAIL", getCurrentUserEmail());
        startActivity(intent);
    }
    
    /**
     * Open notification settings
     */
    private void openNotificationSettings() {
        Intent intent = new Intent(this, NotificationPreferencesActivity.class);
        intent.putExtra("USER_EMAIL", getCurrentUserEmail());
        startActivity(intent);
    }
    
    /**
     * Open app settings
     */
    private void openAppSettings() {
        Intent intent = new Intent(this, AppSettingsActivity.class);
        intent.putExtra("USER_EMAIL", getCurrentUserEmail());
        startActivity(intent);
    }
    
    /**
     * Open about dialog
     */
    private void openAbout() {
        Intent intent = new Intent(this, AboutAppActivity.class);
        startActivity(intent);
    }
    
    /**
     * Open help and support
     */
    private void openHelp() {
        Intent intent = new Intent(this, HelpSupportActivity.class);
        startActivity(intent);
    }
    
    /**
     * Show logout confirmation dialog
     */
    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Sign Out", (dialog, which) -> {
                    // Clear preferences
                    preferences.edit().clear().apply();
                    
                    // Navigate to login
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
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
        return false; // Settings is not in bottom navigation
    }
    
    @Override
    protected void setCurrentTabSelected() {
        // Settings is not in bottom navigation
    }
    
    @Override
    public String getCurrentUserEmail() {
        return getIntent().getStringExtra("USER_EMAIL");
    }
}
