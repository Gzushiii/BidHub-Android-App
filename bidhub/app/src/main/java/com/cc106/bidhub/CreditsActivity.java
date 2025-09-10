package com.cc106.bidhub;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

public class CreditsActivity extends BaseActivity {

    private String loggedInUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inflate the credits content into the content frame
        getLayoutInflater().inflate(R.layout.activity_credits_content, findViewById(R.id.content_frame));
        
        // Get the logged-in user's email from the Intent
        loggedInUserEmail = getIntent().getStringExtra("USER_EMAIL");
        
        // Animate content in after inflation
        animateContentIn();
        
        // TODO: Implement credits functionality
        showSuccess("Credits Management - Coming Soon!");
    }

    @Override
    protected boolean isCurrentActivity(int itemId) {
        return itemId == R.id.nav_credits;
    }

    @Override
    protected void setCurrentTabSelected() {
        bottomNavigationView.setSelectedItemId(R.id.nav_credits);
    }

    @Override
    protected String getCurrentUserEmail() {
        return loggedInUserEmail;
    }
}
