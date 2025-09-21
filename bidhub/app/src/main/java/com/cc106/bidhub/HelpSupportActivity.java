package com.cc106.bidhub;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.cc106.bidhub.toast.ToastHelper;

public class HelpSupportActivity extends BaseActivity {

    private Button buttonContactSupport, buttonEmailSupport, buttonReportBug, buttonFeatureRequest;
    private Button buttonFAQ, buttonUserGuide, buttonTermsOfService, buttonPrivacyPolicy;
    private LinearLayout faqContainer;
    private String loggedInUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inflate the help support content into the content frame
        getLayoutInflater().inflate(R.layout.activity_help_support_content, findViewById(R.id.content_frame));
        
        // Get the logged-in user's email from the Intent
        loggedInUserEmail = getIntent().getStringExtra("USER_EMAIL");
        
        // Animate content in after inflation
        animateContentIn();
        
        // Initialize Views
        initializeViews();
        
        // Set up click listeners
        setupClickListeners();
        
        // Load FAQ items
        loadFAQItems();
    }

    private void initializeViews() {
        // Contact buttons
        buttonContactSupport = findViewById(R.id.buttonContactSupport);
        buttonEmailSupport = findViewById(R.id.buttonEmailSupport);
        buttonReportBug = findViewById(R.id.buttonReportBug);
        buttonFeatureRequest = findViewById(R.id.buttonFeatureRequest);
        
        // Help buttons
        buttonFAQ = findViewById(R.id.buttonFAQ);
        buttonUserGuide = findViewById(R.id.buttonUserGuide);
        buttonTermsOfService = findViewById(R.id.buttonTermsOfService);
        buttonPrivacyPolicy = findViewById(R.id.buttonPrivacyPolicy);
        
        // FAQ container
        faqContainer = findViewById(R.id.faqContainer);
    }

    private void setupClickListeners() {
        // Contact buttons
        buttonContactSupport.setOnClickListener(v -> {
            ToastHelper.showInfo(this, "Opening support chat...");
        });
        
        buttonEmailSupport.setOnClickListener(v -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:support@bidhub.com"));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "BidHub Support Request");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "Please describe your issue:\n\n");
            startActivity(Intent.createChooser(emailIntent, "Send Email"));
        });
        
        buttonReportBug.setOnClickListener(v -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:bugs@bidhub.com"));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Bug Report - BidHub App");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "Please describe the bug:\n\nSteps to reproduce:\n1.\n2.\n3.\n\nExpected behavior:\n\nActual behavior:\n\nDevice info:\n");
            startActivity(Intent.createChooser(emailIntent, "Report Bug"));
        });
        
        buttonFeatureRequest.setOnClickListener(v -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:features@bidhub.com"));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Feature Request - BidHub App");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "Please describe your feature request:\n\nWhy would this feature be useful?\n\nHow should it work?\n\n");
            startActivity(Intent.createChooser(emailIntent, "Request Feature"));
        });
        
        // Help buttons
        buttonFAQ.setOnClickListener(v -> {
            toggleFAQVisibility();
        });
        
        buttonUserGuide.setOnClickListener(v -> {
            ToastHelper.showInfo(this, "User guide will open in browser");
        });
        
        buttonTermsOfService.setOnClickListener(v -> {
            ToastHelper.showInfo(this, "Terms of service will open in browser");
        });
        
        buttonPrivacyPolicy.setOnClickListener(v -> {
            ToastHelper.showInfo(this, "Privacy policy will open in browser");
        });
    }

    private void loadFAQItems() {
        // Add FAQ items dynamically
        String[] faqQuestions = {
            "How do I place a bid?",
            "How do I add credits to my account?",
            "What happens if I win an auction?",
            "How do I change my password?",
            "Can I cancel a bid?",
            "How do I contact support?",
            "Is my personal information secure?",
            "How do I delete my account?"
        };
        
        String[] faqAnswers = {
            "To place a bid, browse items, select one you like, and tap the 'Place Bid' button. Enter your bid amount and confirm.",
            "You can add credits through the Credits section. We accept various payment methods including GCash and Maya.",
            "If you win an auction, you'll receive a notification and can redeem your item using the provided redemption code.",
            "Go to Settings > Account Settings > Change Password to update your password securely.",
            "Bids cannot be cancelled once placed to maintain auction integrity. Please bid carefully.",
            "You can contact support through this help section, email, or the in-app chat feature.",
            "Yes, we use industry-standard encryption and follow strict privacy policies to protect your data.",
            "Account deletion can be requested through the Privacy Settings section or by contacting support."
        };
        
        for (int i = 0; i < faqQuestions.length; i++) {
            addFAQItem(faqQuestions[i], faqAnswers[i]);
        }
    }

    private void addFAQItem(String question, String answer) {
        // Create FAQ item layout
        LinearLayout faqItem = new LinearLayout(this);
        faqItem.setOrientation(LinearLayout.VERTICAL);
        faqItem.setPadding(16, 12, 16, 12);
        faqItem.setBackground(getResources().getDrawable(R.drawable.ripple_effect));
        faqItem.setClickable(true);
        faqItem.setFocusable(true);
        
        // Question text
        TextView questionText = new TextView(this);
        questionText.setText(question);
        questionText.setTextColor(getResources().getColor(R.color.text_primary));
        questionText.setTextSize(16);
        questionText.setTypeface(null, android.graphics.Typeface.BOLD);
        questionText.setPadding(0, 0, 0, 8);
        
        // Answer text (initially hidden)
        TextView answerText = new TextView(this);
        answerText.setText(answer);
        answerText.setTextColor(getResources().getColor(R.color.text_secondary));
        answerText.setTextSize(14);
        answerText.setVisibility(View.GONE);
        answerText.setPadding(0, 0, 0, 8);
        
        // Add views to FAQ item
        faqItem.addView(questionText);
        faqItem.addView(answerText);
        
        // Set click listener to toggle answer visibility
        faqItem.setOnClickListener(v -> {
            if (answerText.getVisibility() == View.GONE) {
                answerText.setVisibility(View.VISIBLE);
            } else {
                answerText.setVisibility(View.GONE);
            }
        });
        
        // Add to container
        faqContainer.addView(faqItem);
    }

    private void toggleFAQVisibility() {
        if (faqContainer.getVisibility() == View.GONE) {
            faqContainer.setVisibility(View.VISIBLE);
            buttonFAQ.setText("Hide FAQ");
        } else {
            faqContainer.setVisibility(View.GONE);
            buttonFAQ.setText("Show FAQ");
        }
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
