package com.cc106.bidhub;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

public class CreditsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);
        
        // Inflate credits content into the base layout
        View creditsContent = LayoutInflater.from(this).inflate(R.layout.activity_credits, null);
        ((android.widget.FrameLayout) findViewById(R.id.content_frame)).addView(creditsContent);
        
        // Set selected navigation item
        setSelectedNavItem(R.id.nav_credits);
        
        // TODO: Implement credits functionality
        Toast.makeText(this, "Credits Management - Coming Soon!", Toast.LENGTH_SHORT).show();
    }
}
