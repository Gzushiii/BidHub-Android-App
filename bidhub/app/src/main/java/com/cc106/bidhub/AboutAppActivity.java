package com.cc106.bidhub;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.cc106.bidhub.toast.ToastHelper;

public class AboutAppActivity extends BaseActivity {

    private TextView textViewAppName, textViewVersion, textViewBuildNumber, textViewDescription;
    private TextView textViewDeveloper, textViewCopyright, textViewAppInfo;
    private Button buttonRateApp, buttonShareApp, buttonCheckUpdates, buttonOpenSource;
    private Button buttonTermsOfService, buttonPrivacyPolicy, buttonContactUs;
    private String loggedInUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inflate the about app content into the content frame
        getLayoutInflater().inflate(R.layout.activity_about_app_content, findViewById(R.id.content_frame));
        
        // Get the logged-in user's email from the Intent
        loggedInUserEmail = getIntent().getStringExtra("USER_EMAIL");
        
        // Animate content in after inflation
        animateContentIn();
        
        // Initialize Views
        initializeViews();
        
        // Load app information
        loadAppInformation();
        
        // Set up click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        // App info text views
        textViewAppName = findViewById(R.id.textViewAppName);
        textViewVersion = findViewById(R.id.textViewVersion);
        textViewBuildNumber = findViewById(R.id.textViewBuildNumber);
        textViewDescription = findViewById(R.id.textViewDescription);
        textViewDeveloper = findViewById(R.id.textViewDeveloper);
        textViewCopyright = findViewById(R.id.textViewCopyright);
        textViewAppInfo = findViewById(R.id.textViewAppInfo);
        
        // Action buttons
        buttonRateApp = findViewById(R.id.buttonRateApp);
        buttonShareApp = findViewById(R.id.buttonShareApp);
        buttonCheckUpdates = findViewById(R.id.buttonCheckUpdates);
        buttonOpenSource = findViewById(R.id.buttonOpenSource);
        buttonTermsOfService = findViewById(R.id.buttonTermsOfService);
        buttonPrivacyPolicy = findViewById(R.id.buttonPrivacyPolicy);
        buttonContactUs = findViewById(R.id.buttonContactUs);
    }

    private void loadAppInformation() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String versionName = packageInfo.versionName;
            int versionCode = packageInfo.versionCode;
            
            textViewAppName.setText("BidHub");
            textViewVersion.setText("Version " + versionName);
            textViewBuildNumber.setText("Build " + versionCode);
            textViewDescription.setText("A modern auction platform that connects buyers and sellers through secure, anonymous bidding. Experience the thrill of competitive bidding while maintaining your privacy.");
            textViewDeveloper.setText("Developed by CC106 Group 5");
            textViewCopyright.setText("© 2024 BidHub. All rights reserved.");
            textViewAppInfo.setText("BidHub revolutionizes the auction experience with cutting-edge technology, ensuring fair play and user privacy. Our platform provides a secure environment for competitive bidding across various categories.");
            
        } catch (PackageManager.NameNotFoundException e) {
            textViewAppName.setText("BidHub");
            textViewVersion.setText("Version 1.0.0");
            textViewBuildNumber.setText("Build 1");
            textViewDescription.setText("A modern auction platform that connects buyers and sellers through secure, anonymous bidding.");
            textViewDeveloper.setText("Developed by CC106 Group 5");
            textViewCopyright.setText("© 2024 BidHub. All rights reserved.");
            textViewAppInfo.setText("BidHub revolutionizes the auction experience with cutting-edge technology.");
        }
    }

    private void setupClickListeners() {
        // Rate App
        buttonRateApp.setOnClickListener(v -> {
            try {
                Intent rateIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName()));
                startActivity(rateIntent);
            } catch (Exception e) {
                Intent rateIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName()));
                startActivity(rateIntent);
            }
        });
        
        // Share App
        buttonShareApp.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Check out BidHub!");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "I've been using BidHub for auctions and it's amazing! Download it from: https://play.google.com/store/apps/details?id=" + getPackageName());
            startActivity(Intent.createChooser(shareIntent, "Share BidHub"));
        });
        
        // Check Updates
        buttonCheckUpdates.setOnClickListener(v -> {
            ToastHelper.showInfo(this, "Checking for updates...");
            // TODO: Implement actual update checking logic
        });
        
        // Open Source
        buttonOpenSource.setOnClickListener(v -> {
            Intent openSourceIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/bidhub/android"));
            startActivity(openSourceIntent);
        });
        
        // Terms of Service
        buttonTermsOfService.setOnClickListener(v -> {
            Intent termsIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://bidhub.com/terms"));
            startActivity(termsIntent);
        });
        
        // Privacy Policy
        buttonPrivacyPolicy.setOnClickListener(v -> {
            Intent privacyIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://bidhub.com/privacy"));
            startActivity(privacyIntent);
        });
        
        // Contact Us
        buttonContactUs.setOnClickListener(v -> {
            Intent contactIntent = new Intent(Intent.ACTION_SENDTO);
            contactIntent.setData(Uri.parse("mailto:contact@bidhub.com"));
            contactIntent.putExtra(Intent.EXTRA_SUBJECT, "BidHub App - Contact");
            contactIntent.putExtra(Intent.EXTRA_TEXT, "Hello BidHub team,\n\n");
            startActivity(Intent.createChooser(contactIntent, "Contact Us"));
        });
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
