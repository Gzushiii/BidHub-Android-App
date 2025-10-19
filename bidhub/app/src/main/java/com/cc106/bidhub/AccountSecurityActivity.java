package com.cc106.bidhub;

import android.os.Bundle;
import android.widget.TextView;
import com.cc106.bidhub.toast.ToastHelper;

public class AccountSecurityActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inflate the account security content into the content frame
        getLayoutInflater().inflate(R.layout.activity_account_security_content, findViewById(R.id.content_frame));
        
        // Animate content in after inflation
        animateContentIn();
        
        // Initialize Views
        TextView textViewComingSoon = findViewById(R.id.textViewComingSoon);
        textViewComingSoon.setText("Account Security - Coming Soon!\n\nThis feature will include:\n• Two-factor authentication\n• Login history\n• Device management\n• Security alerts");
    }

    @Override
    protected boolean isCurrentActivity(int itemId) {
        return false;
    }

    @Override
    protected void setCurrentTabSelected() {
        // No tab selection for this activity
    }

    @Override
    public String getCurrentUserEmail() {
        return getIntent().getStringExtra("USER_EMAIL");
    }
}
