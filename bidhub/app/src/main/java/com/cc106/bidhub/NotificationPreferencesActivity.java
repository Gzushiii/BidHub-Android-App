package com.cc106.bidhub;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import com.cc106.bidhub.toast.ToastHelper;

public class NotificationPreferencesActivity extends BaseActivity {

    private Switch switchEmailNotifications, switchPushNotifications, switchBidAlerts;
    private Switch switchMarketingEmails, switchAuctionUpdates, switchPaymentNotifications;
    private Switch switchSystemUpdates, switchSecurityAlerts, switchWeeklyDigest;
    private Button buttonTestNotification, buttonNotificationHistory, buttonResetDefaults;
    private TextView textViewEmailSettings, textViewPushSettings, textViewAlertSettings;
    private String loggedInUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inflate the notification preferences content into the content frame
        getLayoutInflater().inflate(R.layout.activity_notification_preferences_content, findViewById(R.id.content_frame));
        
        // Get the logged-in user's email from the Intent
        loggedInUserEmail = getIntent().getStringExtra("USER_EMAIL");
        
        // Animate content in after inflation
        animateContentIn();
        
        // Initialize Views
        initializeViews();
        
        // Load notification preferences
        loadNotificationPreferences();
        
        // Set up click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        // Email notification switches
        switchEmailNotifications = findViewById(R.id.switchEmailNotifications);
        switchMarketingEmails = findViewById(R.id.switchMarketingEmails);
        switchWeeklyDigest = findViewById(R.id.switchWeeklyDigest);
        
        // Push notification switches
        switchPushNotifications = findViewById(R.id.switchPushNotifications);
        switchBidAlerts = findViewById(R.id.switchBidAlerts);
        switchAuctionUpdates = findViewById(R.id.switchAuctionUpdates);
        
        // Alert switches
        switchPaymentNotifications = findViewById(R.id.switchPaymentNotifications);
        switchSystemUpdates = findViewById(R.id.switchSystemUpdates);
        switchSecurityAlerts = findViewById(R.id.switchSecurityAlerts);
        
        // Action buttons
        buttonTestNotification = findViewById(R.id.buttonTestNotification);
        buttonNotificationHistory = findViewById(R.id.buttonNotificationHistory);
        buttonResetDefaults = findViewById(R.id.buttonResetDefaults);
        
        // Section headers
        textViewEmailSettings = findViewById(R.id.textViewEmailSettings);
        textViewPushSettings = findViewById(R.id.textViewPushSettings);
        textViewAlertSettings = findViewById(R.id.textViewAlertSettings);
    }

    private void loadNotificationPreferences() {
        // Load notification preferences from database or use defaults
        // For now, we'll use default settings
        switchEmailNotifications.setChecked(true);
        switchPushNotifications.setChecked(true);
        switchBidAlerts.setChecked(true);
        switchMarketingEmails.setChecked(false);
        switchAuctionUpdates.setChecked(true);
        switchPaymentNotifications.setChecked(true);
        switchSystemUpdates.setChecked(true);
        switchSecurityAlerts.setChecked(true);
        switchWeeklyDigest.setChecked(false);
    }

    private void setupClickListeners() {
        // Email notification switches
        switchEmailNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveNotificationPreference("email_notifications", isChecked);
        });
        
        switchMarketingEmails.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveNotificationPreference("marketing_emails", isChecked);
        });
        
        switchWeeklyDigest.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveNotificationPreference("weekly_digest", isChecked);
        });
        
        // Push notification switches
        switchPushNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveNotificationPreference("push_notifications", isChecked);
        });
        
        switchBidAlerts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveNotificationPreference("bid_alerts", isChecked);
        });
        
        switchAuctionUpdates.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveNotificationPreference("auction_updates", isChecked);
        });
        
        // Alert switches
        switchPaymentNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveNotificationPreference("payment_notifications", isChecked);
        });
        
        switchSystemUpdates.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveNotificationPreference("system_updates", isChecked);
        });
        
        switchSecurityAlerts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveNotificationPreference("security_alerts", isChecked);
        });
        
        // Action buttons
        buttonTestNotification.setOnClickListener(v -> {
            ToastHelper.showInfo(this, "Test notification sent!");
        });
        
        buttonNotificationHistory.setOnClickListener(v -> {
            ToastHelper.showInfo(this, "Notification history coming soon!");
        });
        
        buttonResetDefaults.setOnClickListener(v -> {
            resetToDefaults();
        });
    }

    private void saveNotificationPreference(String preference, boolean value) {
        // TODO: Implement proper preference saving to database
        // For now, just show a toast
        String message = preference.replace("_", " ").toUpperCase() + " " + (value ? "enabled" : "disabled");
        ToastHelper.showInfo(this, message);
    }
    
    private void resetToDefaults() {
        // Reset all switches to default values
        switchEmailNotifications.setChecked(true);
        switchPushNotifications.setChecked(true);
        switchBidAlerts.setChecked(true);
        switchMarketingEmails.setChecked(false);
        switchAuctionUpdates.setChecked(true);
        switchPaymentNotifications.setChecked(true);
        switchSystemUpdates.setChecked(true);
        switchSecurityAlerts.setChecked(true);
        switchWeeklyDigest.setChecked(false);
        
        ToastHelper.showSuccess(this, "Notification preferences reset to defaults");
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
