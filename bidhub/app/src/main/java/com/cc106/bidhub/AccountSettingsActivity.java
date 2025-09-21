package com.cc106.bidhub;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import com.cc106.bidhub.toast.ToastHelper;

public class AccountSettingsActivity extends BaseActivity {

    private Switch switchEmailNotifications, switchPushNotifications, switchBidAlerts, switchMarketingEmails;
    private Switch switchProfileVisibility, switchBiddingHistory, switchActivityStatus;
    private Button buttonChangePassword, buttonPrivacySettings, buttonAccountSecurity, buttonHelpSupport, buttonAboutApp;
    private TextView textViewAccountInfo, textViewNotificationSettings, textViewPrivacySettings, textViewAppSettings;
    private DatabaseHelper dbHelper;
    private String loggedInUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inflate the account settings content into the content frame
        getLayoutInflater().inflate(R.layout.activity_account_settings_content, findViewById(R.id.content_frame));
        
        // Get the logged-in user's email from the Intent
        loggedInUserEmail = getIntent().getStringExtra("USER_EMAIL");
        
        dbHelper = new DatabaseHelper(this);
        
        // Animate content in after inflation
        animateContentIn();
        
        // Initialize Views
        initializeViews();
        
        // Load user settings
        loadUserSettings();
        
        // Set up click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        // Notification switches
        switchEmailNotifications = findViewById(R.id.switchEmailNotifications);
        switchPushNotifications = findViewById(R.id.switchPushNotifications);
        switchBidAlerts = findViewById(R.id.switchBidAlerts);
        switchMarketingEmails = findViewById(R.id.switchMarketingEmails);
        
        // Privacy switches
        switchProfileVisibility = findViewById(R.id.switchProfileVisibility);
        switchBiddingHistory = findViewById(R.id.switchBiddingHistory);
        switchActivityStatus = findViewById(R.id.switchActivityStatus);
        
        // Action buttons
        buttonChangePassword = findViewById(R.id.buttonChangePassword);
        buttonPrivacySettings = findViewById(R.id.buttonPrivacySettings);
        buttonAccountSecurity = findViewById(R.id.buttonAccountSecurity);
        buttonHelpSupport = findViewById(R.id.buttonHelpSupport);
        buttonAboutApp = findViewById(R.id.buttonAboutApp);
        
        // Section headers
        textViewAccountInfo = findViewById(R.id.textViewAccountInfo);
        textViewNotificationSettings = findViewById(R.id.textViewNotificationSettings);
        textViewPrivacySettings = findViewById(R.id.textViewPrivacySettings);
        textViewAppSettings = findViewById(R.id.textViewAppSettings);
    }

    private void loadUserSettings() {
        // Load settings from database or use defaults
        // For now, we'll use default settings since we don't have a user_preferences table yet
        switchEmailNotifications.setChecked(true);
        switchPushNotifications.setChecked(true);
        switchBidAlerts.setChecked(true);
        switchMarketingEmails.setChecked(false);
        switchProfileVisibility.setChecked(true);
        switchBiddingHistory.setChecked(false);
        switchActivityStatus.setChecked(true);
    }

    private void setupClickListeners() {
        // Notification switches
        switchEmailNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveNotificationPreference("email_notifications", isChecked);
        });
        
        switchPushNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveNotificationPreference("push_notifications", isChecked);
        });
        
        switchBidAlerts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveNotificationPreference("bid_alerts", isChecked);
        });
        
        switchMarketingEmails.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveNotificationPreference("marketing_emails", isChecked);
        });
        
        // Privacy switches
        switchProfileVisibility.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePrivacyPreference("profile_visibility", isChecked);
        });
        
        switchBiddingHistory.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePrivacyPreference("bidding_history", isChecked);
        });
        
        switchActivityStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePrivacyPreference("activity_status", isChecked);
        });
        
        // Action buttons
        buttonChangePassword.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChangePasswordActivity.class);
            intent.putExtra("USER_EMAIL", loggedInUserEmail);
            startActivity(intent);
        });
        
        buttonPrivacySettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, PrivacySettingsActivity.class);
            intent.putExtra("USER_EMAIL", loggedInUserEmail);
            startActivity(intent);
        });
        
        buttonAccountSecurity.setOnClickListener(v -> {
            Intent intent = new Intent(this, AccountSecurityActivity.class);
            intent.putExtra("USER_EMAIL", loggedInUserEmail);
            startActivity(intent);
        });
        
        buttonHelpSupport.setOnClickListener(v -> {
            Intent intent = new Intent(this, HelpSupportActivity.class);
            intent.putExtra("USER_EMAIL", loggedInUserEmail);
            startActivity(intent);
        });
        
        buttonAboutApp.setOnClickListener(v -> {
            Intent intent = new Intent(this, AboutAppActivity.class);
            intent.putExtra("USER_EMAIL", loggedInUserEmail);
            startActivity(intent);
        });
    }

    private void saveNotificationPreference(String preference, boolean value) {
        // TODO: Implement proper preference saving to database
        // For now, just show a toast
        String message = preference.replace("_", " ").toUpperCase() + " " + (value ? "enabled" : "disabled");
        ToastHelper.showInfo(this, message);
    }
    
    private void savePrivacyPreference(String preference, boolean value) {
        // TODO: Implement proper preference saving to database
        // For now, just show a toast
        String message = preference.replace("_", " ").toUpperCase() + " " + (value ? "enabled" : "disabled");
        ToastHelper.showInfo(this, message);
    }

    @Override
    protected boolean isCurrentActivity(int itemId) {
        return false; // This is not a main navigation activity
    }

    @Override
    protected void setCurrentTabSelected() {
        // No tab selection for this activity
    }

    @Override
    public String getCurrentUserEmail() {
        return loggedInUserEmail;
    }
}
