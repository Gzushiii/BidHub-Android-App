package com.cc106.bidhub;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.cc106.bidhub.toast.ToastHelper;
import com.cc106.bidhub.credits.CreditManager;
import com.cc106.bidhub.credits.CreditPackage;
import com.cc106.bidhub.credits.CreditTransaction;

import java.util.List;

public class CreditsActivity extends BaseActivity {

    private String loggedInUserEmail;
    private CreditManager creditManager;
    private TextView balanceTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inflate the credits content into the content frame
        getLayoutInflater().inflate(R.layout.activity_credits_content, findViewById(R.id.content_frame));
        
        // Get the logged-in user's email from the Intent
        loggedInUserEmail = getIntent().getStringExtra("USER_EMAIL");
        
        // Initialize credit manager
        creditManager = new CreditManager(this);
        
        // Animate content in after inflation
        animateContentIn();
        
        // Initialize UI components
        initializeUI();
        
        // Load and display credit information
        loadCreditInformation();
    }
    
    private void initializeUI() {
        // Find balance text view (assuming it exists in the layout)
        balanceTextView = findViewById(R.id.balance_text_view);
        
        // If balance text view doesn't exist, create a simple one
        if (balanceTextView == null) {
            balanceTextView = new TextView(this);
            balanceTextView.setText("Credit Balance: Loading...");
        }
    }
    
    private void loadCreditInformation() {
        if (loggedInUserEmail == null) {
            ToastHelper.showError(this, "User not logged in");
            return;
        }
        
        try {
            // Get user ID from email (simplified - in real app, you'd have a proper user ID)
            String userId = getUserIdFromEmail(loggedInUserEmail);
            
            if (userId != null) {
                // Get current balance
                double balance = creditManager.getCreditBalance(userId);
                
                // Update UI
                if (balanceTextView != null) {
                    balanceTextView.setText(String.format("Credit Balance: ₱%.2f", balance));
                }
                
                // Get available packages
                List<CreditPackage> packages = creditManager.getAvailablePackages();
                ToastHelper.showInfo(this, "Available packages: " + packages.size());
                
                // Get recent transactions
                List<CreditTransaction> transactions = creditManager.getTransactionHistory(userId);
                ToastHelper.showInfo(this, "Recent transactions: " + transactions.size());
                
            } else {
                ToastHelper.showError(this, "Unable to load user information");
            }
            
        } catch (Exception e) {
            ToastHelper.showError(this, "Error loading credit information: " + e.getMessage());
        }
    }
    
    private String getUserIdFromEmail(String email) {
        // Simplified implementation - in real app, you'd query the database
        // For now, return a mock user ID
        return "user_" + email.hashCode();
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
    public String getCurrentUserEmail() {
        return loggedInUserEmail;
    }
}
