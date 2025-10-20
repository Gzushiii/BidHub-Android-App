package com.cc106.bidhub.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.cc106.bidhub.toast.ToastHelper;
import com.cc106.bidhub.credits.SimpleCreditManager;
import com.cc106.bidhub.credits.CreditPackage;
import com.cc106.bidhub.credits.CreditTransaction;
import com.cc106.bidhub.credits.CreditUIHelper;
import com.cc106.bidhub.payments.PaymentGateway;
import com.cc106.bidhub.payments.MockPaymentGateway;
import com.cc106.bidhub.utils.SharedPreferencesHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cc106.bidhub.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CreditsFragment extends Fragment {

    private String loggedInUserEmail;
    private String userId;
    private SimpleCreditManager creditManager;
    private PaymentGateway paymentGateway;
    private SharedPreferencesHelper prefsHelper;
    private TextView balanceAmount;
    private TextView balanceStatus;
    private LinearLayout packagesContainer;
    private Button btnRefreshBalance;
    private Button btnTransactionHistory;
    
    // Enhanced UI components
    private TextView tvCreditUsage;
    private TextView tvLastTransaction;
    private LinearLayout layoutPromotionalBanner;
    private RecyclerView rvTransactionHistory;
    private List<CreditTransaction> transactionHistory;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_credits_shop, container, false);
        
        // Get the logged-in user's email from arguments
        if (getArguments() != null) {
            loggedInUserEmail = getArguments().getString("USER_EMAIL");
        }
        
        // Initialize credit manager and payment gateway
        creditManager = new SimpleCreditManager(getContext());
        paymentGateway = new MockPaymentGateway();
        prefsHelper = new SharedPreferencesHelper(getContext());
        
        // Get user ID from email
        userId = creditManager.getUserIdFromEmail(loggedInUserEmail);
        
        // If user not found in database, create a test user for demo purposes
        if (userId == null && loggedInUserEmail != null) {
            android.util.Log.w("CreditsFragment", "User not found in database, creating test user");
            userId = "test_user_" + System.currentTimeMillis();
            // Add some test credits
            creditManager.addCredits(userId, 100.0, SimpleCreditManager.TRANSACTION_PURCHASE);
        }
        
        // Initialize UI
        initializeUI(view);
        
        // Load and display credit information
        loadCreditInformation();
        
        // Load credit usage analytics
        loadCreditUsageAnalytics();
        
        // Load recent transactions
        loadRecentTransactions();
        
        return view;
    }
    
    private void initializeUI(View view) {
        // Initialize UI components
        balanceAmount = view.findViewById(R.id.balance_amount);
        balanceStatus = view.findViewById(R.id.balance_status);
        packagesContainer = view.findViewById(R.id.packages_container);
        btnRefreshBalance = view.findViewById(R.id.btn_refresh_balance);
        btnTransactionHistory = view.findViewById(R.id.btn_transaction_history);
        
        // Debug logging
        android.util.Log.d("CreditsFragment", "balanceAmount: " + (balanceAmount != null ? "found" : "null"));
        android.util.Log.d("CreditsFragment", "packagesContainer: " + (packagesContainer != null ? "found" : "null"));
        
        // Set up click listeners
        if (btnRefreshBalance != null) {
            btnRefreshBalance.setOnClickListener(v -> refreshBalance());
        }
        if (btnTransactionHistory != null) {
            btnTransactionHistory.setOnClickListener(v -> showTransactionHistory());
        }
        
        // Add test button for debugging (remove in production)
        addTestButton();
    }
    
    private void loadCreditInformation() {
        android.util.Log.d("CreditsFragment", "loadCreditInformation called");
        
        if (userId == null) {
            android.util.Log.e("CreditsFragment", "userId is null");
            ToastHelper.showError(getContext(), "User not logged in");
            return;
        }
        
        android.util.Log.d("CreditsFragment", "userId: " + userId);
        
        try {
            // Update balance display
            updateBalanceDisplay();
            
            // Load and display credit packages
            loadCreditPackages();
            
            android.util.Log.d("CreditsFragment", "Credit information loaded successfully");
            
        } catch (Exception e) {
            android.util.Log.e("CreditsFragment", "Error loading credit information", e);
            ToastHelper.showError(getContext(), "Error loading credit information: " + e.getMessage());
        }
    }
    
    private void updateBalanceDisplay() {
        if (balanceAmount != null) {
            double balance = creditManager.getCreditBalance(userId);
            String newBalance = creditManager.formatCurrency(balance);
            balanceAmount.setText(newBalance);
        }
    }
    
    private void refreshBalance() {
        updateBalanceDisplay();
        ToastHelper.showInfo(getContext(), "Balance refreshed");
    }
    
    private void loadCreditPackages() {
        android.util.Log.d("CreditsFragment", "loadCreditPackages called");
        
        if (packagesContainer == null) {
            android.util.Log.e("CreditsFragment", "packagesContainer is null");
            return;
        }
        
        android.util.Log.d("CreditsFragment", "packagesContainer found, removing all views");
        packagesContainer.removeAllViews();
        
        List<CreditPackage> packages = creditManager.getAvailablePackages();
        android.util.Log.d("CreditsFragment", "Found " + packages.size() + " packages");
        
        for (int i = 0; i < packages.size(); i++) {
            CreditPackage pkg = packages.get(i);
            android.util.Log.d("CreditsFragment", "Creating card for package: " + pkg.getName());
            View packageView = createPackageCard(pkg);
            packagesContainer.addView(packageView);
            
            // Add staggered animation
            packageView.setAlpha(0f);
            packageView.setTranslationY(50f);
            packageView.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(300)
                    .setStartDelay(i * 100)
                    .start();
        }
        
        android.util.Log.d("CreditsFragment", "All package cards created and added");
    }
    
    private View createPackageCard(CreditPackage pkg) {
        View cardView = LayoutInflater.from(getContext()).inflate(R.layout.item_credit_package, packagesContainer, false);
        
        // Set package details
        TextView packageName = cardView.findViewById(R.id.package_name);
        TextView packageDescription = cardView.findViewById(R.id.package_description);
        TextView packageCredits = cardView.findViewById(R.id.package_credits);
        TextView packagePrice = cardView.findViewById(R.id.package_price);
        TextView packageOriginalPrice = cardView.findViewById(R.id.package_original_price);
        TextView packageDiscount = cardView.findViewById(R.id.package_discount);
        TextView packageBadge = cardView.findViewById(R.id.package_badge);
        Button btnPurchase = cardView.findViewById(R.id.btn_purchase);
        
        packageName.setText(pkg.getName());
        packageDescription.setText(pkg.getDescription());
        packageCredits.setText(creditManager.formatCurrency(pkg.getCredits()));
        packagePrice.setText(creditManager.formatCurrency(pkg.getPrice()));
        
        // Show discount if applicable
        if (pkg.getDiscountPercentage() > 0) {
            double originalPrice = pkg.getPrice() / (1 - pkg.getDiscountPercentage() / 100);
            packageOriginalPrice.setText(creditManager.formatCurrency(originalPrice));
            packageOriginalPrice.setVisibility(View.VISIBLE);
            packageDiscount.setText("Save " + (int)pkg.getDiscountPercentage() + "%");
            packageDiscount.setVisibility(View.VISIBLE);
        }
        
        // Show popular badge for premium packages
        if (pkg.getPackageId() == 3) { // Premium Pack
            packageBadge.setVisibility(View.VISIBLE);
        }
        
        // Set up purchase button
        btnPurchase.setOnClickListener(v -> showPurchaseConfirmation(pkg));
        
        return cardView;
    }
    
    private void showPurchaseConfirmation(CreditPackage pkg) {
        // Simple confirmation for now
        ToastHelper.showInfo(getContext(), "Purchase: " + pkg.getName() + " for " + creditManager.formatCurrency(pkg.getPrice()));
        
        // Process payment
        processPayment(pkg, "MOCK_PAYMENT");
    }
    
    private void processPayment(CreditPackage pkg, String paymentMethod) {
        paymentGateway.processPayment(userId, pkg.getPrice(), "PHP", 
            "Purchase: " + pkg.getName(), new PaymentGateway.PaymentCallback() {
                @Override
                public void onPaymentSuccess(String transactionId, String reference) {
                    getActivity().runOnUiThread(() -> {
                        // Add credits to account
                        boolean success = creditManager.addCredits(userId, pkg.getCredits(), SimpleCreditManager.TRANSACTION_PURCHASE);
                        if (success) {
                            // Update SharedPreferences to sync with other parts of the app
                            double newBalance = creditManager.getCreditBalance(userId);
                            prefsHelper.setCredits(newBalance);
                            
                            updateBalanceDisplay();
                            
                            // Refresh ProfileFragment if MainActivity is available
                            if (getActivity() instanceof com.cc106.bidhub.MainActivity) {
                                ((com.cc106.bidhub.MainActivity) getActivity()).refreshProfileFragment();
                            }
                            
                            ToastHelper.showSuccess(getContext(), "Purchase successful! " + creditManager.formatCurrency(pkg.getCredits()) + " credits added.");
                        } else {
                            ToastHelper.showError(getContext(), "Failed to add credits to account");
                        }
                    });
                }
                
                @Override
                public void onPaymentFailed(String errorCode, String errorMessage) {
                    getActivity().runOnUiThread(() -> {
                        ToastHelper.showError(getContext(), "Purchase failed: " + errorMessage);
                    });
                }
                
                @Override
                public void onPaymentCancelled() {
                    getActivity().runOnUiThread(() -> {
                        ToastHelper.showInfo(getContext(), "Payment cancelled");
                    });
                }
            });
    }
    
    
    private void addTestButton() {
        if (packagesContainer != null) {
            Button testButton = new Button(getContext());
            testButton.setText("🧪 Test Credits System");
            testButton.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_light));
            testButton.setTextColor(getResources().getColor(android.R.color.white));
            testButton.setPadding(16, 16, 16, 16);
            testButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) testButton.getLayoutParams();
            params.setMargins(0, 16, 0, 0);
            testButton.setLayoutParams(params);
            
            testButton.setOnClickListener(v -> {
                // Run a simple test
                ToastHelper.showInfo(getContext(), "Credits system is working! Balance: " + creditManager.formatCurrency(creditManager.getCreditBalance(userId)));
            });
            
            packagesContainer.addView(testButton);
        }
    }
    
    public void updateUserEmail(String email) {
        this.loggedInUserEmail = email;
    }
    
    /**
     * Load credit usage analytics
     */
    private void loadCreditUsageAnalytics() {
        if (creditManager == null || userId == null) {
            return;
        }
        
        try {
            // Get transaction history for analytics
            List<CreditTransaction> transactions = creditManager.getTransactionHistory(userId);
            
            // Calculate usage statistics
            double totalPurchased = 0;
            double totalUsed = 0;
            int purchaseCount = 0;
            int usageCount = 0;
            
            for (CreditTransaction transaction : transactions) {
                if (transaction.getType().equals(SimpleCreditManager.TRANSACTION_PURCHASE)) {
                    totalPurchased += Math.abs(transaction.getAmount());
                    purchaseCount++;
                } else if (transaction.getType().equals(SimpleCreditManager.TRANSACTION_BID)) {
                    totalUsed += Math.abs(transaction.getAmount());
                    usageCount++;
                }
            }
            
            // Update UI with analytics
            if (tvCreditUsage != null) {
                String usageText = String.format("Used %s of %s credits (%d transactions)", 
                    creditManager.formatCurrency(totalUsed),
                    creditManager.formatCurrency(totalPurchased),
                    usageCount);
                tvCreditUsage.setText(usageText);
            }
            
        } catch (Exception e) {
            android.util.Log.e("CreditsFragment", "Error loading credit analytics: " + e.getMessage(), e);
        }
    }
    
    /**
     * Load recent transactions
     */
    private void loadRecentTransactions() {
        if (creditManager == null || userId == null) {
            return;
        }
        
        try {
            transactionHistory = creditManager.getTransactionHistory(userId);
            
            // Show last transaction info
            if (!transactionHistory.isEmpty() && tvLastTransaction != null) {
                CreditTransaction lastTransaction = transactionHistory.get(0);
                String lastTransactionText = String.format("Last: %s %s on %s",
                    lastTransaction.getType(),
                    creditManager.formatCurrency(Math.abs(lastTransaction.getAmount())),
                    new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(lastTransaction.getCreatedAt())
                );
                tvLastTransaction.setText(lastTransactionText);
            }
            
        } catch (Exception e) {
            android.util.Log.e("CreditsFragment", "Error loading recent transactions: " + e.getMessage(), e);
        }
    }
    
    /**
     * Show enhanced transaction history dialog
     */
    private void showTransactionHistory() {
        if (transactionHistory == null || transactionHistory.isEmpty()) {
            ToastHelper.showInfo(getContext(), "No transaction history available");
            return;
        }
        
        // Create a custom dialog with RecyclerView for transaction history
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setTitle("Transaction History");
        
        // Create RecyclerView for transactions
        RecyclerView recyclerView = new RecyclerView(getContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // TODO: Create TransactionAdapter for better display
        // For now, show simple list
        StringBuilder historyText = new StringBuilder();
        for (CreditTransaction transaction : transactionHistory) {
            historyText.append(String.format("%s: %s %s\n",
                new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(transaction.getCreatedAt()),
                transaction.getType(),
                creditManager.formatCurrency(transaction.getAmount())
            ));
        }
        
        TextView textView = new TextView(getContext());
        textView.setText(historyText.toString());
        textView.setPadding(32, 32, 32, 32);
        textView.setTextSize(14);
        
        builder.setView(textView);
        builder.setPositiveButton("Close", null);
        builder.show();
    }
    
    /**
     * Show promotional banner for credit packages
     */
    private void showPromotionalBanner() {
        if (layoutPromotionalBanner != null) {
            // TODO: Implement promotional banner with special offers
            android.util.Log.d("CreditsFragment", "Showing promotional banner");
        }
    }
}
