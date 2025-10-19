package com.cc106.bidhub;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import com.cc106.bidhub.toast.ToastHelper;

public class SecuritySettingsActivity extends BaseActivity {

    private Switch switchTwoFactorAuth, switchBiometricLogin, switchAutoLock;
    private Switch switchLoginAlerts, switchDeviceManagement, switchSecureBrowsing;
    private Button buttonChangePassword, buttonManageDevices, buttonSecurityReport;
    private Button buttonEnable2FA, buttonBackupCodes, buttonSecurityQuestions;
    private TextView textViewSecurityStatus, textViewAuthSettings, textViewDeviceSettings;
    private String loggedInUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inflate the security settings content into the content frame
        getLayoutInflater().inflate(R.layout.activity_security_settings_content, findViewById(R.id.content_frame));
        
        // Get the logged-in user's email from the Intent
        loggedInUserEmail = getIntent().getStringExtra("USER_EMAIL");
        
        // Animate content in after inflation
        animateContentIn();
        
        // Initialize Views
        initializeViews();
        
        // Load security settings
        loadSecuritySettings();
        
        // Set up click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        // Security switches
        switchTwoFactorAuth = findViewById(R.id.switchTwoFactorAuth);
        switchBiometricLogin = findViewById(R.id.switchBiometricLogin);
        switchAutoLock = findViewById(R.id.switchAutoLock);
        switchLoginAlerts = findViewById(R.id.switchLoginAlerts);
        switchDeviceManagement = findViewById(R.id.switchDeviceManagement);
        switchSecureBrowsing = findViewById(R.id.switchSecureBrowsing);
        
        // Action buttons
        buttonChangePassword = findViewById(R.id.buttonChangePassword);
        buttonManageDevices = findViewById(R.id.buttonManageDevices);
        buttonSecurityReport = findViewById(R.id.buttonSecurityReport);
        buttonEnable2FA = findViewById(R.id.buttonEnable2FA);
        buttonBackupCodes = findViewById(R.id.buttonBackupCodes);
        buttonSecurityQuestions = findViewById(R.id.buttonSecurityQuestions);
        
        // Section headers
        textViewSecurityStatus = findViewById(R.id.textViewSecurityStatus);
        textViewAuthSettings = findViewById(R.id.textViewAuthSettings);
        textViewDeviceSettings = findViewById(R.id.textViewDeviceSettings);
    }

    private void loadSecuritySettings() {
        // Load security settings from database or use defaults
        // For now, we'll use default settings
        switchTwoFactorAuth.setChecked(false);
        switchBiometricLogin.setChecked(true);
        switchAutoLock.setChecked(true);
        switchLoginAlerts.setChecked(true);
        switchDeviceManagement.setChecked(true);
        switchSecureBrowsing.setChecked(true);
    }

    private void setupClickListeners() {
        // Security switches
        switchTwoFactorAuth.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                enableTwoFactorAuth();
            } else {
                disableTwoFactorAuth();
            }
        });
        
        switchBiometricLogin.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSecurityPreference("biometric_login", isChecked);
        });
        
        switchAutoLock.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSecurityPreference("auto_lock", isChecked);
        });
        
        switchLoginAlerts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSecurityPreference("login_alerts", isChecked);
        });
        
        switchDeviceManagement.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSecurityPreference("device_management", isChecked);
        });
        
        switchSecureBrowsing.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSecurityPreference("secure_browsing", isChecked);
        });
        
        // Action buttons
        buttonChangePassword.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChangePasswordActivity.class);
            intent.putExtra("USER_EMAIL", loggedInUserEmail);
            startActivity(intent);
        });
        
        buttonManageDevices.setOnClickListener(v -> {
            ToastHelper.showInfo(this, "Device management coming soon!");
        });
        
        buttonSecurityReport.setOnClickListener(v -> {
            ToastHelper.showInfo(this, "Security report coming soon!");
        });
        
        buttonEnable2FA.setOnClickListener(v -> {
            enableTwoFactorAuth();
        });
        
        buttonBackupCodes.setOnClickListener(v -> {
            ToastHelper.showInfo(this, "Backup codes coming soon!");
        });
        
        buttonSecurityQuestions.setOnClickListener(v -> {
            ToastHelper.showInfo(this, "Security questions coming soon!");
        });
    }

    private void saveSecurityPreference(String preference, boolean value) {
        // TODO: Implement proper preference saving to database
        // For now, just show a toast
        String message = preference.replace("_", " ").toUpperCase() + " " + (value ? "enabled" : "disabled");
        ToastHelper.showInfo(this, message);
    }
    
    private void enableTwoFactorAuth() {
        ToastHelper.showInfo(this, "Two-factor authentication setup coming soon!");
        // TODO: Implement 2FA setup flow
    }
    
    private void disableTwoFactorAuth() {
        ToastHelper.showInfo(this, "Two-factor authentication disabled");
        // TODO: Implement 2FA disable flow
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
