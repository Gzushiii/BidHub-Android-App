package com.cc106.bidhub;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import com.cc106.bidhub.toast.ToastHelper;

public class PrivacySettingsActivity extends BaseActivity {

    private Switch switchProfileVisibility, switchBiddingHistory, switchActivityStatus;
    private Switch switchDataCollection, switchAnalytics, switchPersonalizedAds;
    private Switch switchLocationTracking, switchContactSync, switchDataSharing;
    private Button buttonDataExport, buttonDataDelete, buttonPrivacyPolicy;
    private TextView textViewPrivacyInfo, textViewDataControl, textViewDataSharing;
    private String loggedInUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inflate the privacy settings content into the content frame
        getLayoutInflater().inflate(R.layout.activity_privacy_settings_content, findViewById(R.id.content_frame));
        
        // Get the logged-in user's email from the Intent
        loggedInUserEmail = getIntent().getStringExtra("USER_EMAIL");
        
        // Animate content in after inflation
        animateContentIn();
        
        // Initialize Views
        initializeViews();
        
        // Load privacy settings
        loadPrivacySettings();
        
        // Set up click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        // Profile privacy switches
        switchProfileVisibility = findViewById(R.id.switchProfileVisibility);
        switchBiddingHistory = findViewById(R.id.switchBiddingHistory);
        switchActivityStatus = findViewById(R.id.switchActivityStatus);
        
        // Data control switches
        switchDataCollection = findViewById(R.id.switchDataCollection);
        switchAnalytics = findViewById(R.id.switchAnalytics);
        switchPersonalizedAds = findViewById(R.id.switchPersonalizedAds);
        
        // Data sharing switches
        switchLocationTracking = findViewById(R.id.switchLocationTracking);
        switchContactSync = findViewById(R.id.switchContactSync);
        switchDataSharing = findViewById(R.id.switchDataSharing);
        
        // Action buttons
        buttonDataExport = findViewById(R.id.buttonDataExport);
        buttonDataDelete = findViewById(R.id.buttonDataDelete);
        buttonPrivacyPolicy = findViewById(R.id.buttonPrivacyPolicy);
        
        // Section headers
        textViewPrivacyInfo = findViewById(R.id.textViewPrivacyInfo);
        textViewDataControl = findViewById(R.id.textViewDataControl);
        textViewDataSharing = findViewById(R.id.textViewDataSharing);
    }

    private void loadPrivacySettings() {
        // Load privacy settings from database or use defaults
        // For now, we'll use default settings
        switchProfileVisibility.setChecked(true);
        switchBiddingHistory.setChecked(false);
        switchActivityStatus.setChecked(true);
        switchDataCollection.setChecked(true);
        switchAnalytics.setChecked(true);
        switchPersonalizedAds.setChecked(false);
        switchLocationTracking.setChecked(false);
        switchContactSync.setChecked(false);
        switchDataSharing.setChecked(false);
    }

    private void setupClickListeners() {
        // Profile privacy switches
        switchProfileVisibility.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePrivacyPreference("profile_visibility", isChecked);
        });
        
        switchBiddingHistory.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePrivacyPreference("bidding_history", isChecked);
        });
        
        switchActivityStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePrivacyPreference("activity_status", isChecked);
        });
        
        // Data control switches
        switchDataCollection.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePrivacyPreference("data_collection", isChecked);
        });
        
        switchAnalytics.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePrivacyPreference("analytics", isChecked);
        });
        
        switchPersonalizedAds.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePrivacyPreference("personalized_ads", isChecked);
        });
        
        // Data sharing switches
        switchLocationTracking.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePrivacyPreference("location_tracking", isChecked);
        });
        
        switchContactSync.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePrivacyPreference("contact_sync", isChecked);
        });
        
        switchDataSharing.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePrivacyPreference("data_sharing", isChecked);
        });
        
        // Action buttons
        buttonDataExport.setOnClickListener(v -> {
            ToastHelper.showInfo(this, "Data export feature coming soon!");
        });
        
        buttonDataDelete.setOnClickListener(v -> {
            ToastHelper.showInfo(this, "Data deletion feature coming soon!");
        });
        
        buttonPrivacyPolicy.setOnClickListener(v -> {
            ToastHelper.showInfo(this, "Privacy policy will open in browser");
        });
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