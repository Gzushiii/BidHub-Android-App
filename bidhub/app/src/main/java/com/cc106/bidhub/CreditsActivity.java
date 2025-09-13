package com.cc106.bidhub;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.cc106.bidhub.toast.ToastHelper;
import com.cc106.bidhub.credits.SimpleCreditManager;
import com.cc106.bidhub.credits.CreditPackage;
import com.cc106.bidhub.credits.CreditUIHelper;
import com.cc106.bidhub.credits.PaymentGateway;
import com.cc106.bidhub.credits.TestPaymentGateway;

import java.util.List;

public class CreditsActivity extends BaseActivity {

    private String loggedInUserEmail;
    private String userId;
    private SimpleCreditManager creditManager;
    private PaymentGateway paymentGateway;
    private TextView balanceTextView;
    private LinearLayout packagesLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inflate the credits content into the content frame
        getLayoutInflater().inflate(R.layout.activity_credits_content, findViewById(R.id.content_frame));
        
        // Get the logged-in user's email from the Intent
        loggedInUserEmail = getIntent().getStringExtra("USER_EMAIL");
        
        // Initialize credit manager and payment gateway
        creditManager = new SimpleCreditManager(this);
        paymentGateway = new TestPaymentGateway();
        
        // Get user ID from email
        userId = creditManager.getUserIdFromEmail(loggedInUserEmail);
        
        // Animate content in after inflation
        animateContentIn();
        
        // Initialize UI
        initializeUI();
        
        // Load and display credit information
        loadCreditInformation();
    }
    
    private void initializeUI() {
        // Create a simple UI programmatically since the layout is basic
        LinearLayout contentLayout = findViewById(R.id.content_frame);
        
        // Create balance display
        balanceTextView = new TextView(this);
        balanceTextView.setText("Loading balance...");
        balanceTextView.setTextSize(18);
        balanceTextView.setTextColor(getResources().getColor(android.R.color.black));
        balanceTextView.setPadding(24, 24, 24, 16);
        
        // Create packages layout
        packagesLayout = new LinearLayout(this);
        packagesLayout.setOrientation(LinearLayout.VERTICAL);
        packagesLayout.setPadding(24, 16, 24, 24);
        
        // Add views to content
        if (contentLayout != null) {
            contentLayout.addView(balanceTextView);
            contentLayout.addView(packagesLayout);
        }
    }
    
    private void loadCreditInformation() {
        if (userId == null) {
            ToastHelper.showError(this, "User not logged in");
            return;
        }
        
        try {
            // Update balance display
            updateBalanceDisplay();
            
            // Load and display credit packages
            loadCreditPackages();
            
            // Show initial information
            CreditUIHelper.showBalanceInfo(this, userId, creditManager);
            
        } catch (Exception e) {
            ToastHelper.showError(this, "Error loading credit information: " + e.getMessage());
        }
    }
    
    private void updateBalanceDisplay() {
        if (balanceTextView != null) {
            double balance = creditManager.getCreditBalance(userId);
            balanceTextView.setText("Credit Balance: " + creditManager.formatCurrency(balance));
        }
    }
    
    private void loadCreditPackages() {
        if (packagesLayout == null) return;
        
        packagesLayout.removeAllViews();
        
        List<CreditPackage> packages = creditManager.getAvailablePackages();
        
        // Add title
        TextView titleView = new TextView(this);
        titleView.setText("Available Credit Packages:");
        titleView.setTextSize(16);
        titleView.setTextColor(getResources().getColor(android.R.color.black));
        titleView.setPadding(0, 0, 0, 16);
        packagesLayout.addView(titleView);
        
        // Add package buttons
        for (CreditPackage pkg : packages) {
            Button packageButton = createPackageButton(pkg);
            packagesLayout.addView(packageButton);
        }
        
        // Add action buttons
        addActionButtons();
    }
    
    private Button createPackageButton(CreditPackage pkg) {
        Button button = new Button(this);
        button.setText(pkg.getName() + "\n" + 
                      creditManager.formatCurrency(pkg.getCredits()) + " credits for " + 
                      creditManager.formatCurrency(pkg.getPrice()));
        button.setPadding(16, 16, 16, 16);
        button.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        
        // Set margin
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) button.getLayoutParams();
        params.setMargins(0, 0, 0, 16);
        button.setLayoutParams(params);
        
        button.setOnClickListener(v -> purchaseCredits(pkg));
        
        return button;
    }
    
    private void addActionButtons() {
        // Refresh button
        Button refreshButton = new Button(this);
        refreshButton.setText("Refresh Balance");
        refreshButton.setOnClickListener(v -> {
            updateBalanceDisplay();
            CreditUIHelper.showBalanceInfo(this, userId, creditManager);
        });
        packagesLayout.addView(refreshButton);
        
        // Transaction history button
        Button historyButton = new Button(this);
        historyButton.setText("View Transaction History");
        historyButton.setOnClickListener(v -> CreditUIHelper.showTransactionHistory(this, userId, creditManager));
        packagesLayout.addView(historyButton);
        
        // Available packages button
        Button packagesButton = new Button(this);
        packagesButton.setText("View All Packages");
        packagesButton.setOnClickListener(v -> CreditUIHelper.showAvailablePackages(this, creditManager));
        packagesLayout.addView(packagesButton);
    }
    
    private void purchaseCredits(CreditPackage pkg) {
        if (userId == null) {
            ToastHelper.showError(this, "User not logged in");
            return;
        }
        
        // Show payment options
        String[] paymentMethods = {"Test Payment", "GCash (Coming Soon)", "Maya (Coming Soon)"};
        
        // For now, just use test payment
        processPayment(pkg, TestPaymentGateway.PAYMENT_METHOD_TEST);
    }
    
    private void processPayment(CreditPackage pkg, String paymentMethod) {
        paymentGateway.processPayment(userId, pkg.getPrice(), "PHP", 
            "Purchase: " + pkg.getName(), new PaymentGateway.PaymentCallback() {
                @Override
                public void onPaymentSuccess(String transactionId, String reference) {
                    runOnUiThread(() -> {
                        // Add credits to account
                        boolean success = creditManager.addCredits(userId, pkg.getCredits(), SimpleCreditManager.TRANSACTION_PURCHASE);
                        if (success) {
                            updateBalanceDisplay();
                            CreditUIHelper.showPurchaseSuccess(CreditsActivity.this, pkg);
                        } else {
                            CreditUIHelper.showPurchaseFailure(CreditsActivity.this, "Failed to add credits to account");
                        }
                    });
                }
                
                @Override
                public void onPaymentFailed(String errorCode, String errorMessage) {
                    runOnUiThread(() -> {
                        CreditUIHelper.showPurchaseFailure(CreditsActivity.this, errorMessage);
                    });
                }
                
                @Override
                public void onPaymentCancelled() {
                    runOnUiThread(() -> {
                        ToastHelper.showInfo(CreditsActivity.this, "Payment cancelled");
                    });
                }
            });
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
