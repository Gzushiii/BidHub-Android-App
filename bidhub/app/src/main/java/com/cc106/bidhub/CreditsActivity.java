package com.cc106.bidhub;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.cc106.bidhub.toast.ToastHelper;
import com.cc106.bidhub.credits.SimpleCreditManager;
import com.cc106.bidhub.credits.CreditPackage;
import com.cc106.bidhub.credits.CreditTransaction;
import com.cc106.bidhub.credits.CreditUIHelper;
import com.cc106.bidhub.credits.PaymentGateway;
import com.cc106.bidhub.credits.TestPaymentGateway;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CreditsActivity extends BaseActivity {

    private String loggedInUserEmail;
    private String userId;
    private SimpleCreditManager creditManager;
    private PaymentGateway paymentGateway;
    private TextView balanceAmount;
    private TextView balanceStatus;
    private LinearLayout packagesContainer;
    private Button btnRefreshBalance;
    private Button btnTransactionHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inflate the credits shop content into the content frame
        android.util.Log.d("CreditsActivity", "Inflating credits shop layout");
        View contentView = getLayoutInflater().inflate(R.layout.test_credits_simple, null);
        ViewGroup contentFrame = findViewById(R.id.content_frame);
        android.util.Log.d("CreditsActivity", "Content frame found: " + (contentFrame != null));
        
        if (contentFrame != null) {
            contentFrame.addView(contentView);
            android.util.Log.d("CreditsActivity", "Content view added to frame");
        } else {
            android.util.Log.e("CreditsActivity", "Content frame is null!");
        }
        
        // Get the logged-in user's email from the Intent
        loggedInUserEmail = getIntent().getStringExtra("USER_EMAIL");
        
        // Initialize credit manager and payment gateway
        creditManager = new SimpleCreditManager(this);
        paymentGateway = new TestPaymentGateway();
        
        // Get user ID from email
        userId = creditManager.getUserIdFromEmail(loggedInUserEmail);
        
        // If user not found in database, create a test user for demo purposes
        if (userId == null && loggedInUserEmail != null) {
            android.util.Log.w("CreditsActivity", "User not found in database, creating test user");
            userId = "test_user_" + System.currentTimeMillis();
            // Add some test credits
            creditManager.addCredits(userId, 100.0, SimpleCreditManager.TRANSACTION_PURCHASE);
        }
        
        // Animate content in after inflation
        animateContentIn();
        
        // Initialize UI
        initializeUI();
        
        // Load and display credit information
        loadCreditInformation();
    }
    
    private void initializeUI() {
        // Test simple button first
        Button testButton = findViewById(R.id.test_button);
        if (testButton != null) {
            android.util.Log.d("CreditsActivity", "Test button found!");
            testButton.setOnClickListener(v -> {
                android.util.Log.d("CreditsActivity", "Test button clicked!");
                ToastHelper.showInfo(this, "Test button works! Layout is loading correctly.");
            });
        } else {
            android.util.Log.e("CreditsActivity", "Test button not found!");
        }
        
        // Initialize UI components
        balanceAmount = findViewById(R.id.balance_amount);
        balanceStatus = findViewById(R.id.balance_status);
        packagesContainer = findViewById(R.id.packages_container);
        btnRefreshBalance = findViewById(R.id.btn_refresh_balance);
        btnTransactionHistory = findViewById(R.id.btn_transaction_history);
        
        // Debug logging
        android.util.Log.d("CreditsActivity", "balanceAmount: " + (balanceAmount != null ? "found" : "null"));
        android.util.Log.d("CreditsActivity", "packagesContainer: " + (packagesContainer != null ? "found" : "null"));
        
        // Set up click listeners
        if (btnRefreshBalance != null) {
            btnRefreshBalance.setOnClickListener(v -> refreshBalance());
        }
        if (btnTransactionHistory != null) {
            btnTransactionHistory.setOnClickListener(v -> showTransactionHistory());
        }
        
        // Add test button only in debug builds
        if (com.cc106.bidhub.BuildConfig.DEBUG) {
            addTestButton();
        }
    }
    
    private void loadCreditInformation() {
        android.util.Log.d("CreditsActivity", "loadCreditInformation called");
        
        if (userId == null) {
            android.util.Log.e("CreditsActivity", "userId is null");
            ToastHelper.showError(this, "User not logged in");
            return;
        }
        
        android.util.Log.d("CreditsActivity", "userId: " + userId);
        
        try {
            // Update balance display
            updateBalanceDisplay();
            
            // Load and display credit packages
            loadCreditPackages();
            
            android.util.Log.d("CreditsActivity", "Credit information loaded successfully");
            
        } catch (Exception e) {
            android.util.Log.e("CreditsActivity", "Error loading credit information", e);
            ToastHelper.showError(this, "Error loading credit information: " + e.getMessage());
        }
    }
    
    private void updateBalanceDisplay() {
        if (balanceAmount != null) {
            // Use SharedPreferences (updated by CreditBalanceManager) as source of truth
            com.cc106.bidhub.utils.SharedPreferencesHelper prefsHelper = 
                new com.cc106.bidhub.utils.SharedPreferencesHelper(this);
            double balance = prefsHelper.getCredits();
            
            // Fallback to local manager if SharedPreferences is empty
            if (balance <= 0) {
                balance = creditManager.getCreditBalance(userId);
            }
            
            String newBalance = creditManager.formatCurrency(balance);
            
            // Animate balance update
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(balanceAmount, "scaleX", 1.0f, 1.2f, 1.0f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(balanceAmount, "scaleY", 1.0f, 1.2f, 1.0f);
            scaleX.setDuration(200);
            scaleY.setDuration(200);
            
            scaleX.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    balanceAmount.setText(newBalance);
                }
            });
            
            scaleX.start();
            scaleY.start();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh balance from backend when activity resumes
        refreshBalanceFromBackend();
    }
    
    private void refreshBalanceFromBackend() {
        // First update UI with cached value
        updateBalanceDisplay();
        
        // Then refresh from backend
        com.cc106.bidhub.utils.CreditBalanceManager.refreshBalance(
            this,
            new com.cc106.bidhub.utils.CreditBalanceManager.BalanceUpdateCallback() {
                @Override
                public void onBalanceUpdated(double newBalance) {
                    updateBalanceDisplay();
                }
                
                @Override
                public void onError(String errorMessage) {
                    // Silent fail - already showing cached value
                }
            }
        );
    }
    
    private void refreshBalance() {
        refreshBalanceFromBackend();
        ToastHelper.showInfo(this, "Balance refreshed");
    }
    
    private void loadCreditPackages() {
        android.util.Log.d("CreditsActivity", "loadCreditPackages called");
        
        if (packagesContainer == null) {
            android.util.Log.e("CreditsActivity", "packagesContainer is null");
            return;
        }
        
        android.util.Log.d("CreditsActivity", "packagesContainer found, removing all views");
        packagesContainer.removeAllViews();
        
        List<CreditPackage> packages = creditManager.getAvailablePackages();
        android.util.Log.d("CreditsActivity", "Found " + packages.size() + " packages");
        
        for (int i = 0; i < packages.size(); i++) {
            CreditPackage pkg = packages.get(i);
            android.util.Log.d("CreditsActivity", "Creating card for package: " + pkg.getName());
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
        
        android.util.Log.d("CreditsActivity", "All package cards created and added");
    }
    
    private void addTestButton() {
        if (packagesContainer != null) {
            Button testButton = new Button(this);
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
                android.content.Intent intent = new android.content.Intent(this, CreditsTestActivity.class);
                startActivity(intent);
            });
            
            packagesContainer.addView(testButton);
        }
    }
    
    private View createPackageCard(CreditPackage pkg) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.item_credit_package, packagesContainer, false);
        
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
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_purchase_confirmation);
        
        // Set package details
        TextView packageName = dialog.findViewById(R.id.package_name);
        TextView packageCredits = dialog.findViewById(R.id.package_credits);
        TextView packagePrice = dialog.findViewById(R.id.package_price);
        TextView paymentMethod = dialog.findViewById(R.id.payment_method);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel);
        Button btnConfirm = dialog.findViewById(R.id.btn_confirm);
        
        packageName.setText(pkg.getName());
        packageCredits.setText(creditManager.formatCurrency(pkg.getCredits()));
        packagePrice.setText(creditManager.formatCurrency(pkg.getPrice()));
        paymentMethod.setText("Test Payment");
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            processPayment(pkg, TestPaymentGateway.PAYMENT_METHOD_TEST);
        });
        
        dialog.show();
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
    
    private void showTransactionHistory() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_transaction_history);
        
        RecyclerView recyclerView = dialog.findViewById(R.id.recycler_transactions);
        View emptyState = dialog.findViewById(R.id.empty_state);
        Button btnClose = dialog.findViewById(R.id.btn_close);
        
        List<CreditTransaction> transactions = creditManager.getTransactionHistory(userId);
        
        if (transactions.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
            
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(new TransactionAdapter(transactions));
        }
        
        btnClose.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
    
    // Transaction Adapter
    private class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {
        private List<CreditTransaction> transactions;
        private SimpleDateFormat dateFormat;
        
        public TransactionAdapter(List<CreditTransaction> transactions) {
            this.transactions = transactions;
            this.dateFormat = new SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault());
        }
        
        @Override
        public TransactionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
            return new TransactionViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(TransactionViewHolder holder, int position) {
            CreditTransaction transaction = transactions.get(position);
            
            holder.transactionType.setText(transaction.getType().toUpperCase());
            holder.transactionDescription.setText(transaction.getDescription());
            holder.transactionDate.setText(dateFormat.format(transaction.getCreatedAt()));
            holder.transactionAmount.setText(creditManager.formatCurrency(transaction.getAmount()));
            holder.transactionStatus.setText(transaction.getStatus());
            
            // Set icon and colors based on transaction type
            if (transaction.getAmount() > 0) {
                holder.transactionIcon.setText("+");
                holder.transactionAmount.setTextColor(getResources().getColor(R.color.success_color));
            } else {
                holder.transactionIcon.setText("-");
                holder.transactionAmount.setTextColor(getResources().getColor(R.color.error_red));
            }
            
            // Set status color
            if (SimpleCreditManager.STATUS_COMPLETED.equals(transaction.getStatus())) {
                holder.transactionStatus.setTextColor(getResources().getColor(R.color.success_color));
            } else if (SimpleCreditManager.STATUS_FAILED.equals(transaction.getStatus())) {
                holder.transactionStatus.setTextColor(getResources().getColor(R.color.error_red));
            } else {
                holder.transactionStatus.setTextColor(getResources().getColor(R.color.warning_yellow));
            }
        }
        
        @Override
        public int getItemCount() {
            return transactions.size();
        }
        
        class TransactionViewHolder extends RecyclerView.ViewHolder {
            TextView transactionIcon;
            TextView transactionType;
            TextView transactionDescription;
            TextView transactionDate;
            TextView transactionAmount;
            TextView transactionStatus;
            
            public TransactionViewHolder(View itemView) {
                super(itemView);
                transactionIcon = itemView.findViewById(R.id.transaction_icon);
                transactionType = itemView.findViewById(R.id.transaction_type);
                transactionDescription = itemView.findViewById(R.id.transaction_description);
                transactionDate = itemView.findViewById(R.id.transaction_date);
                transactionAmount = itemView.findViewById(R.id.transaction_amount);
                transactionStatus = itemView.findViewById(R.id.transaction_status);
            }
        }
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
