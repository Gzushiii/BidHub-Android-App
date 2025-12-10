package com.cc106.bidhub.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.cc106.bidhub.toast.ToastHelper;
import com.cc106.bidhub.utils.SharedPreferencesHelper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import com.cc106.bidhub.credits.SimpleCreditManager;
import com.cc106.bidhub.credits.CreditPackage;
import com.cc106.bidhub.credits.CreditTransaction;
import com.cc106.bidhub.credits.CreditUIHelper;
import com.cc106.bidhub.payments.PaymentGateway;
import com.cc106.bidhub.payments.MockPaymentGateway;
import com.cc106.bidhub.adapters.TransactionHistoryAdapter;

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
    private SharedPreferencesHelper prefsHelper;
    private SimpleCreditManager creditManager;
    private PaymentGateway paymentGateway;
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
    
    // Request deduplication flags
    private boolean isTopupRequestInProgress = false;
    private boolean isSubmitRequestInProgress = false;

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
        
        // Test button (removed BuildConfig check for compatibility)
        // addTestButton();
    }
    
    public void loadCreditInformation() {
        android.util.Log.d("CreditsFragment", "loadCreditInformation called");
        
        if (userId == null) {
            android.util.Log.e("CreditsFragment", "userId is null");
            ToastHelper.showError(getContext(), "User not logged in");
            return;
        }
        
        android.util.Log.d("CreditsFragment", "userId: " + userId);
        
        try {
            // FIX: Show cached balance immediately for better UX, then refresh from server
            updateBalanceDisplay();
            
            // Load and display credit packages
            loadCreditPackages();
            
            // FIX: Refresh balance from server to ensure accuracy (async, updates UI when complete)
            // This ensures the displayed balance is always up-to-date after server response
            com.cc106.bidhub.utils.CreditBalanceManager.refreshBalance(
                getContext(),
                new com.cc106.bidhub.utils.CreditBalanceManager.BalanceUpdateCallback() {
                    @Override
                    public void onBalanceUpdated(double newBalance) {
                        if (getActivity() != null && !getActivity().isFinishing()) {
                            // Update UserRepository with confirmed value from backend
                            com.cc106.bidhub.repository.UserRepository userRepo = 
                                com.cc106.bidhub.repository.UserRepository.getInstance(getContext());
                            userRepo.updateCreditsImmediately(newBalance);
                            updateBalanceDisplay();
                            android.util.Log.d("CreditsFragment", String.format("Balance refreshed from server: %.2f", newBalance));
                        }
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        // Silent fail - already showing cached value
                        android.util.Log.w("CreditsFragment", "Failed to refresh balance from server: " + errorMessage);
                    }
                }
            );
            
            android.util.Log.d("CreditsFragment", "Credit information loaded successfully");
            
        } catch (Exception e) {
            android.util.Log.e("CreditsFragment", "Error loading credit information", e);
            ToastHelper.showError(getContext(), "Error loading credit information: " + e.getMessage());
        }
    }
    
    private void updateBalanceDisplay() {
        if (balanceAmount == null) return;

        // Use UserRepository as single source of truth
        com.cc106.bidhub.repository.UserRepository userRepo = 
            com.cc106.bidhub.repository.UserRepository.getInstance(getContext());
        double credits = userRepo.getCredits();
        
        android.util.Log.d("CreditsFragment", String.format("Updating balance display: %.2f", credits));
        balanceAmount.setText(creditManager.formatCurrency(credits));
    }
    
    private void refreshBalance() {
        // Fetch from backend and sync using CreditBalanceManager
        com.cc106.bidhub.utils.CreditBalanceManager.refreshBalance(
            getContext(),
            new com.cc106.bidhub.utils.CreditBalanceManager.BalanceUpdateCallback() {
                @Override
                public void onBalanceUpdated(double newBalance) {
                    if (getActivity() != null && !getActivity().isFinishing()) {
                        updateBalanceDisplay();
                        ToastHelper.showInfo(getContext(), "Balance updated");
                    }
                }
                
                @Override
                public void onError(String errorMessage) {
                    if (getActivity() != null && !getActivity().isFinishing()) {
                        ToastHelper.showError(getContext(), "Failed to refresh balance: " + errorMessage);
                    }
                }
            }
        );
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
        // Show GCash payment dialog
        showGCashPaymentDialog(pkg);
    }
    
    /**
     * Show GCash payment dialog with reference number input
     */
    private void showGCashPaymentDialog(CreditPackage pkg) {
        android.app.Dialog dialog = new android.app.Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_gcash_payment);
        dialog.setCancelable(true);
        
        // FIX: Set proper dialog window sizing
        android.view.Window window = dialog.getWindow();
        if (window != null) {
            android.view.WindowManager.LayoutParams params = window.getAttributes();
            
            // Set width to 90% of screen width, with max width of 400dp
            android.util.DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            int screenWidth = displayMetrics.widthPixels;
            int screenHeight = displayMetrics.heightPixels;
            int maxWidth = (int) (400 * displayMetrics.density); // 400dp in pixels
            int maxHeight = (int) (screenHeight * 0.85); // 85% of screen height
            int dialogWidth = Math.min((int) (screenWidth * 0.9), maxWidth);
            params.width = dialogWidth;
            
            // Set height to wrap_content with max height constraint
            params.height = android.view.WindowManager.LayoutParams.WRAP_CONTENT;
            
            // Center the dialog
            params.gravity = android.view.Gravity.CENTER;
            
            window.setAttributes(params);
            
            // Ensure dialog doesn't exceed screen height - set layout after attributes
            window.setLayout(dialogWidth, android.view.WindowManager.LayoutParams.WRAP_CONTENT);
            
            // Set background to solid surface color (not transparent)
            window.setBackgroundDrawableResource(R.color.surface);
        }
        
        // Initialize views
        android.widget.TextView tvPackageName = dialog.findViewById(R.id.tv_package_name);
        android.widget.TextView tvPackageCredits = dialog.findViewById(R.id.tv_package_credits);
        android.widget.TextView tvPackagePrice = dialog.findViewById(R.id.tv_package_price);
        android.widget.TextView tvGCashNumber = dialog.findViewById(R.id.tv_gcash_number);
        android.widget.TextView tvReferenceCode = dialog.findViewById(R.id.tv_reference_code);
        com.google.android.material.textfield.TextInputLayout tilReferenceNumber = dialog.findViewById(R.id.til_reference_number);
        com.google.android.material.textfield.TextInputEditText etReferenceNumber = dialog.findViewById(R.id.et_reference_number);
        android.widget.ProgressBar progressPayment = dialog.findViewById(R.id.progress_payment);
        com.google.android.material.button.MaterialButton btnCancel = dialog.findViewById(R.id.btn_cancel_payment);
        com.google.android.material.button.MaterialButton btnSubmit = dialog.findViewById(R.id.btn_submit_reference);
        
        // Set package details
        tvPackageName.setText(pkg.getName());
        tvPackageCredits.setText(creditManager.formatCurrency(pkg.getCredits()));
        tvPackagePrice.setText(creditManager.formatCurrency(pkg.getPrice()));
        
        // Set GCash number (from backend or default)
        tvGCashNumber.setText("+63 916 123 4567"); // TODO: Get from backend
        
        // Initialize top-up request
        int[] topupId = {0};
        String[] referenceCode = {""};
        
        // Store package info and current balance for validation
        final double packageAmount = pkg.getPrice();
        final double packageCredits = pkg.getCredits();
        com.cc106.bidhub.repository.UserRepository userRepo = 
            com.cc106.bidhub.repository.UserRepository.getInstance(getContext());
        final double currentBalanceBeforeTopup = userRepo.getCredits();
        
        android.util.Log.i("CreditsFragment", "=== TOP-UP INITIATED ===");
        android.util.Log.i("CreditsFragment", String.format("Current balance: %.2f", currentBalanceBeforeTopup));
        android.util.Log.i("CreditsFragment", String.format("Package amount: %.2f", packageAmount));
        android.util.Log.i("CreditsFragment", String.format("Package credits: %.2f", packageCredits));
        android.util.Log.i("CreditsFragment", String.format("Expected new balance: %.2f", currentBalanceBeforeTopup + packageCredits));
        
        // Start top-up request
        initiateTopupRequest(pkg.getPrice(), new TopupInitCallback() {
            @Override
            public void onSuccess(int topupIdValue, String refCode) {
                getActivity().runOnUiThread(() -> {
                    topupId[0] = topupIdValue;
                    referenceCode[0] = refCode;
                    tvReferenceCode.setText(refCode);
                    btnSubmit.setEnabled(true);
                });
            }
            
            @Override
            public void onError(String error) {
                getActivity().runOnUiThread(() -> {
                    ToastHelper.showError(getContext(), "Failed to initiate payment: " + error);
                    dialog.dismiss();
                });
            }
        });
        
        // Enable submit button when reference number is exactly 13 digits
        etReferenceNumber.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String ref = s != null ? s.toString().trim() : "";
                boolean isValid = isValidReferenceNumber(ref);
                btnSubmit.setEnabled(isValid && topupId[0] > 0);
                
                if (ref.length() > 0 && !isValid) {
                    if (ref.length() != 13) {
                        tilReferenceNumber.setError(getString(R.string.reference_must_be_13_digits));
                    } else if (!ref.matches("\\d+")) {
                        tilReferenceNumber.setError(getString(R.string.reference_numbers_only));
                    } else {
                        tilReferenceNumber.setError(getString(R.string.invalid_reference));
                    }
                } else {
                    tilReferenceNumber.setError(null);
                }
            }
            
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
        
        // Set input type to numbers only (maxLength is set in XML)
        etReferenceNumber.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        
        // Cancel button
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        // Submit button
        btnSubmit.setOnClickListener(v -> {
            String refNumber = etReferenceNumber.getText().toString().trim();
            if (!isValidReferenceNumber(refNumber)) {
                if (refNumber.length() != 13) {
                    tilReferenceNumber.setError(getString(R.string.reference_must_be_13_digits));
                } else if (!refNumber.matches("\\d+")) {
                    tilReferenceNumber.setError(getString(R.string.reference_numbers_only));
                } else {
                    tilReferenceNumber.setError(getString(R.string.invalid_reference));
                }
                return;
            }
            
            // Show loading
            progressPayment.setVisibility(android.view.View.VISIBLE);
            btnSubmit.setEnabled(false);
            btnCancel.setEnabled(false);
            
            // Submit reference number
            submitTopupReference(topupId[0], refNumber, new TopupSubmitCallback() {
                @Override
                public void onSuccess(double newBalance, String status) {
                    final String topupStatus = status; // Make final for use in inner class
                    getActivity().runOnUiThread(() -> {
                        android.util.Log.i("CreditsFragment", "=== TOP-UP SUCCESS CALLBACK ===");
                        android.util.Log.i("CreditsFragment", String.format("Balance value received: %.2f, Status: %s", newBalance, topupStatus));
                        
                        // Validate balance makes sense
                        double expectedBalance = currentBalanceBeforeTopup + packageCredits;
                        double balanceDifference = newBalance - currentBalanceBeforeTopup;
                        android.util.Log.i("CreditsFragment", String.format("Balance validation - Expected: %.2f, Received: %.2f, Difference: %.2f", 
                            expectedBalance, newBalance, balanceDifference));
                        
                        if (Math.abs(balanceDifference - packageCredits) > 0.01) {
                            android.util.Log.w("CreditsFragment", String.format(
                                "WARNING: Balance increment mismatch! Expected increment: %.2f, Actual increment: %.2f, Package credits: %.2f",
                                packageCredits, balanceDifference, packageCredits));
                            android.util.Log.w("CreditsFragment", "This may indicate a backend calculation issue. Frontend will display the value received from backend.");
                        }
                        
                        progressPayment.setVisibility(android.view.View.GONE);
                        dialog.dismiss();
                        
                        // Always refresh balance from backend to ensure accuracy
                        // This handles both CONFIRMED (immediate update) and UNDER_REVIEW (pending) cases
                        com.cc106.bidhub.utils.CreditBalanceManager.refreshBalance(
                            getContext(),
                            new com.cc106.bidhub.utils.CreditBalanceManager.BalanceUpdateCallback() {
                                @Override
                                public void onBalanceUpdated(double confirmedBalance) {
                                    if (getActivity() != null && !getActivity().isFinishing()) {
                                        // Update UserRepository with confirmed value from backend
                                        com.cc106.bidhub.repository.UserRepository userRepo = 
                                            com.cc106.bidhub.repository.UserRepository.getInstance(getContext());
                                        userRepo.updateCreditsImmediately(confirmedBalance);
                                        updateBalanceDisplay();
                                        android.util.Log.i("CreditsFragment", String.format("Balance confirmed from backend: %.2f", confirmedBalance));
                                        
                                        // Notify MainActivity to refresh credit displays in other fragments
                                        if (getActivity() instanceof com.cc106.bidhub.MainActivity) {
                                            ((com.cc106.bidhub.MainActivity) getActivity()).refreshCreditDisplays();
                                        }
                                        
                                        // CRITICAL: Refresh credit transaction history after balance update
                                        refreshCreditHistoryFromBackend();
                                        
                                        // Show appropriate message based on top-up status
                                        String message = getString(R.string.topup_processed_successfully);
                                        if ("CONFIRMED".equals(topupStatus)) {
                                            // Balance was updated immediately
                                            message = String.format("Top-up confirmed! Your balance is now ₱%.2f", confirmedBalance);
                                        } else if ("UNDER_REVIEW".equals(topupStatus)) {
                                            message = "Top-up submitted for review. Your balance will update once confirmed.";
                                        }
                                        ToastHelper.showSuccess(getContext(), message);
                                    }
                                }
                                
                                @Override
                                public void onError(String errorMessage) {
                                    // If refresh fails, still show success but use the value we have
                                    android.util.Log.w("CreditsFragment", "Backend refresh failed: " + errorMessage);
                                    updateBalanceDisplay();
                                    
                                    // Notify MainActivity to refresh credit displays even if backend refresh failed
                                    if (getActivity() != null && !getActivity().isFinishing() && getActivity() instanceof com.cc106.bidhub.MainActivity) {
                                        ((com.cc106.bidhub.MainActivity) getActivity()).refreshCreditDisplays();
                                    }
                                    
                                    // Still try to refresh history even if balance refresh failed
                                    refreshCreditHistoryFromBackend();
                                    
                                    // Show appropriate message based on status
                                    String message = getString(R.string.topup_processed_successfully);
                                    if ("UNDER_REVIEW".equals(topupStatus)) {
                                        message = "Top-up submitted for review. Your balance will update once confirmed.";
                                    }
                                    ToastHelper.showSuccess(getContext(), message);
                                }
                            }
                        );
                    });
                }
                
                @Override
                public void onError(String error) {
                    getActivity().runOnUiThread(() -> {
                        progressPayment.setVisibility(android.view.View.GONE);
                        btnSubmit.setEnabled(true);
                        btnCancel.setEnabled(true);
                        ToastHelper.showError(getContext(), "Failed to process top-up: " + error);
                    });
                }
            });
        });
        
        dialog.show();
    }
    
    /**
     * Interface for top-up initiation callback
     */
    private interface TopupInitCallback {
        void onSuccess(int topupId, String referenceCode);
        void onError(String error);
    }
    
    /**
     * Interface for top-up submission callback
     */
    private interface TopupSubmitCallback {
        void onSuccess(double newBalance, String status);
        void onError(String error);
    }
    
    /**
     * Validate 13-digit reference number
     * Must contain exactly 13 digits, numeric only
     */
    private boolean isValidReferenceNumber(String ref) {
        if (ref == null || ref.trim().isEmpty()) {
            return false;
        }
        String trimmed = ref.trim();
        return trimmed.length() == 13 && trimmed.matches("\\d+");
    }
    
    /**
     * Initiate top-up request with backend
     */
    private void initiateTopupRequest(double amount, TopupInitCallback callback) {
        // Prevent duplicate requests
        if (isTopupRequestInProgress) {
            android.util.Log.w("CreditsFragment", "Top-up request already in progress, ignoring duplicate request");
            callback.onError("A top-up request is already in progress. Please wait.");
            return;
        }
        
        isTopupRequestInProgress = true;
        
        new Thread(() -> {
            try {
                // Validate amount before sending
                if (amount < 100.0) {
                    isTopupRequestInProgress = false;
                    callback.onError("Amount too low. Minimum top-up is ₱100.00");
                    return;
                }
                if (amount > 50000.0) {
                    isTopupRequestInProgress = false;
                    callback.onError("Amount too high. Maximum top-up is ₱50,000.00");
                    return;
                }
                
                String token = prefsHelper.getAuthToken();
                if (token == null || token.isEmpty()) {
                    isTopupRequestInProgress = false;
                    callback.onError("Please log in again");
                    return;
                }
                
                android.util.Log.d("CreditsFragment", "Initiating top-up request: amount=" + amount);
                
                URL url = new URL(BASE_URL + "/topups");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                
                JSONObject body = new JSONObject();
                body.put("amount", amount);
                body.put("payment_method", "gcash");
                
                android.util.Log.d("CreditsFragment", "Request body: " + body.toString());
                
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes("UTF-8"));
                }
                
                int code = conn.getResponseCode();
                android.util.Log.d("CreditsFragment", "Response code: " + code);
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                    code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream()
                ));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                
                String responseBody = sb.toString();
                android.util.Log.d("CreditsFragment", "Response body: " + responseBody);
                
                if (code >= 200 && code < 300) {
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        if (json.has("topup_id") && json.has("generated_ref")) {
                            int topupId = json.getInt("topup_id");
                            String refCode = json.getString("generated_ref");
                            android.util.Log.d("CreditsFragment", "Top-up initiated successfully: id=" + topupId + ", ref=" + refCode);
                            isTopupRequestInProgress = false;
                            callback.onSuccess(topupId, refCode);
                        } else {
                            android.util.Log.e("CreditsFragment", "Missing fields in response: " + responseBody);
                            isTopupRequestInProgress = false;
                            callback.onError("Invalid response from server. Please try again.");
                        }
                    } catch (org.json.JSONException e) {
                        android.util.Log.e("CreditsFragment", "JSON parsing error: " + e.getMessage(), e);
                        isTopupRequestInProgress = false;
                        callback.onError("Invalid response format. Please try again.");
                    }
                } else {
                    // Parse error response
                    String errorMessage = "Failed to initiate payment";
                    try {
                        JSONObject errorJson = new JSONObject(responseBody);
                        errorMessage = errorJson.optString("error", "Failed to initiate payment");
                        String details = errorJson.optString("details", "");
                        if (!details.isEmpty()) {
                            errorMessage += ": " + details;
                        }
                    } catch (org.json.JSONException e) {
                        android.util.Log.e("CreditsFragment", "Error parsing error response: " + responseBody, e);
                        // Try to extract error from non-JSON response
                        if (responseBody != null && !responseBody.isEmpty()) {
                            errorMessage += ": " + responseBody;
                        } else {
                            errorMessage += " (HTTP " + code + ")";
                        }
                    }
                    android.util.Log.e("CreditsFragment", "Top-up initiation failed: " + errorMessage);
                    isTopupRequestInProgress = false;
                    callback.onError(errorMessage);
                }
            } catch (java.net.SocketTimeoutException e) {
                android.util.Log.e("CreditsFragment", "Request timeout", e);
                isTopupRequestInProgress = false;
                callback.onError("Request timed out. Please check your connection and try again.");
            } catch (java.net.UnknownHostException e) {
                android.util.Log.e("CreditsFragment", "Unknown host", e);
                isTopupRequestInProgress = false;
                callback.onError("Cannot connect to server. Please check your internet connection.");
            } catch (java.io.IOException e) {
                android.util.Log.e("CreditsFragment", "Network error", e);
                isTopupRequestInProgress = false;
                callback.onError("Network error: " + e.getMessage() + ". Please check your connection.");
            } catch (Exception e) {
                android.util.Log.e("CreditsFragment", "Unexpected error", e);
                isTopupRequestInProgress = false;
                callback.onError("Unexpected error: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Submit reference number for top-up - automatically processes and adds credits
     */
    private void submitTopupReference(int topupId, String referenceNumber, TopupSubmitCallback callback) {
        // Prevent duplicate requests
        if (isSubmitRequestInProgress) {
            android.util.Log.w("CreditsFragment", "Submit request already in progress, ignoring duplicate request");
            callback.onError("A submission is already in progress. Please wait.");
            return;
        }
        
        isSubmitRequestInProgress = true;
        
        new Thread(() -> {
            try {
                // Validate 13-digit reference number
                if (!isValidReferenceNumber(referenceNumber)) {
                    isSubmitRequestInProgress = false;
                    callback.onError("Reference number must contain exactly 13 digits (numbers only)");
                    return;
                }
                
                String token = prefsHelper.getAuthToken();
                if (token == null || token.isEmpty()) {
                    android.util.Log.e("CreditsFragment", "=== TOP-UP SUBMIT FAILED: NO AUTH TOKEN ===");
                    isSubmitRequestInProgress = false;
                    callback.onError("Please log in again");
                    return;
                }
                
                android.util.Log.i("CreditsFragment", "=== SUBMITTING TOP-UP REFERENCE ===");
                android.util.Log.i("CreditsFragment", "Top-up ID: " + topupId);
                android.util.Log.i("CreditsFragment", "Reference: " + referenceNumber);
                android.util.Log.d("CreditsFragment", "Auth token present: " + (token != null && !token.isEmpty()));
                android.util.Log.d("CreditsFragment", "Token preview: " + (token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "NULL"));
                
                URL url = new URL(BASE_URL + "/topups/" + topupId + "/submit");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                
                JSONObject body = new JSONObject();
                body.put("user_receipt_ref", referenceNumber.trim());
                
                android.util.Log.d("CreditsFragment", "Submit request body: " + body.toString());
                
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes("UTF-8"));
                }
                
                int code = conn.getResponseCode();
                android.util.Log.d("CreditsFragment", "Submit response code: " + code);
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                    code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream()
                ));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                
                String responseBody = sb.toString();
                android.util.Log.d("CreditsFragment", "Submit response body: " + responseBody);
                
                if (code >= 200 && code < 300) {
                    android.util.Log.i("CreditsFragment", "=== TOP-UP SUBMIT SUCCESS ===");
                    android.util.Log.d("CreditsFragment", "Response body: " + responseBody);
                    
                    // Parse success response to get new balance
                    double newBalance = 0.0;
                    boolean shouldUpdateBalance = false;
                    final String[] topupStatus = {""}; // Use array to make it effectively final for inner class
                    try {
                        JSONObject responseJson = new JSONObject(responseBody);
                        topupStatus[0] = responseJson.optString("status", "");
                        
                        android.util.Log.i("CreditsFragment", "Top-up status: " + topupStatus[0]);
                        
                        // Check if new_balance is present in response (regardless of status)
                        // This handles cases where backend returns balance even for UNDER_REVIEW
                        if (responseJson.has("new_balance")) {
                            Object balanceObj = responseJson.get("new_balance");
                            if (balanceObj instanceof Number) {
                                newBalance = ((Number) balanceObj).doubleValue();
                                shouldUpdateBalance = true;
                            } else if (balanceObj instanceof String) {
                                try {
                                    newBalance = Double.parseDouble((String) balanceObj);
                                    shouldUpdateBalance = true;
                                } catch (NumberFormatException e) {
                                    android.util.Log.w("CreditsFragment", "Failed to parse new_balance string: " + balanceObj);
                                }
                            }
                            
                            // Only update immediately if status is CONFIRMED
                            if (shouldUpdateBalance && "CONFIRMED".equals(topupStatus[0])) {
                                android.util.Log.i("CreditsFragment", String.format("Top-up CONFIRMED - New balance from API: %.2f", newBalance));
                                
                                // CRITICAL: Immediately update UserRepository and SharedPreferences
                                com.cc106.bidhub.repository.UserRepository userRepo = 
                                    com.cc106.bidhub.repository.UserRepository.getInstance(getContext());
                                double oldBalance = userRepo.getCredits();
                                userRepo.updateCreditsImmediately(newBalance);
                                
                                // Also update SharedPreferences immediately for consistency
                                com.cc106.bidhub.utils.CreditBalanceManager.updateBalanceImmediately(getContext(), newBalance);
                                
                                android.util.Log.i("CreditsFragment", String.format("Balance updated immediately: %.2f -> %.2f (Delta: %.2f)",
                                    oldBalance, newBalance, newBalance - oldBalance));
                                
                                // Log full transaction details for debugging
                                android.util.Log.i("CreditsFragment", "=== TOP-UP TRANSACTION DETAILS ===");
                                android.util.Log.i("CreditsFragment", String.format("Old balance: %.2f", oldBalance));
                                android.util.Log.i("CreditsFragment", String.format("New balance (from API): %.2f", newBalance));
                                android.util.Log.i("CreditsFragment", String.format("Balance change: %.2f", newBalance - oldBalance));
                                android.util.Log.i("CreditsFragment", "Note: If balance change doesn't match package amount, this indicates a backend calculation issue.");
                            } else if (shouldUpdateBalance && !"CONFIRMED".equals(topupStatus[0])) {
                                // Balance present but status is not CONFIRMED - log but don't update yet
                                android.util.Log.i("CreditsFragment", String.format("Top-up status is %s with balance %.2f - will refresh from backend to confirm", topupStatus[0], newBalance));
                                // Get current balance to pass to callback (don't change it yet)
                                com.cc106.bidhub.repository.UserRepository userRepo = 
                                    com.cc106.bidhub.repository.UserRepository.getInstance(getContext());
                                newBalance = userRepo.getCredits();
                                shouldUpdateBalance = false;
                            }
                        } else {
                            // No new_balance in response - status is UNDER_REVIEW or other
                            android.util.Log.i("CreditsFragment", "Top-up status is " + topupStatus[0] + " - no balance update, will refresh from backend");
                            // Get current balance to pass to callback (don't change it)
                            com.cc106.bidhub.repository.UserRepository userRepo = 
                                com.cc106.bidhub.repository.UserRepository.getInstance(getContext());
                            newBalance = userRepo.getCredits();
                            shouldUpdateBalance = false;
                        }
                        
                    } catch (org.json.JSONException e) {
                        android.util.Log.e("CreditsFragment", "Could not parse response", e);
                        android.util.Log.e("CreditsFragment", "Response body: " + responseBody);
                        // Get current balance as fallback
                        com.cc106.bidhub.repository.UserRepository userRepo = 
                            com.cc106.bidhub.repository.UserRepository.getInstance(getContext());
                        newBalance = userRepo.getCredits();
                        // Set status to empty string if parsing failed
                        if (topupStatus[0].isEmpty()) {
                            topupStatus[0] = "UNKNOWN";
                        }
                    }
                    isSubmitRequestInProgress = false;
                    callback.onSuccess(newBalance, topupStatus[0]);
                } else {
                    // Parse error response
                    String errorMessage = "Failed to process top-up";
                    try {
                        JSONObject errorJson = new JSONObject(responseBody);
                        errorMessage = errorJson.optString("error", "Failed to process top-up");
                        String details = errorJson.optString("details", "");
                        if (!details.isEmpty()) {
                            errorMessage += ": " + details;
                        }
                    } catch (org.json.JSONException e) {
                        android.util.Log.e("CreditsFragment", "Error parsing error response: " + responseBody, e);
                        if (responseBody != null && !responseBody.isEmpty()) {
                            errorMessage += ": " + responseBody;
                        } else {
                            errorMessage += " (HTTP " + code + ")";
                        }
                    }
                    android.util.Log.e("CreditsFragment", "Top-up processing failed: " + errorMessage);
                    isSubmitRequestInProgress = false;
                    callback.onError(errorMessage);
                }
            } catch (java.net.SocketTimeoutException e) {
                android.util.Log.e("CreditsFragment", "Submit request timeout", e);
                isSubmitRequestInProgress = false;
                callback.onError("Request timed out. Please check your connection and try again.");
            } catch (java.net.UnknownHostException e) {
                android.util.Log.e("CreditsFragment", "Unknown host", e);
                isSubmitRequestInProgress = false;
                callback.onError("Cannot connect to server. Please check your internet connection.");
            } catch (java.io.IOException e) {
                android.util.Log.e("CreditsFragment", "Network error", e);
                isSubmitRequestInProgress = false;
                callback.onError("Network error: " + e.getMessage() + ". Please check your connection.");
            } catch (Exception e) {
                android.util.Log.e("CreditsFragment", "Unexpected error", e);
                isSubmitRequestInProgress = false;
                callback.onError("Unexpected error: " + e.getMessage());
            }
        }).start();
    }
    
    private void processPayment(CreditPackage pkg, String paymentMethod) {
        paymentGateway.processPayment(userId, pkg.getPrice(), "PHP", 
            "Purchase: " + pkg.getName(), new PaymentGateway.PaymentCallback() {
                @Override
                public void onPaymentSuccess(String transactionId, String reference) {
                    getActivity().runOnUiThread(() -> {
                        // Refresh balance from backend after successful payment
                        com.cc106.bidhub.utils.CreditBalanceManager.refreshBalance(
                            getContext(),
                            new com.cc106.bidhub.utils.CreditBalanceManager.BalanceUpdateCallback() {
                                @Override
                                public void onBalanceUpdated(double newBalance) {
                                    if (getActivity() != null && !getActivity().isFinishing()) {
                                        updateBalanceDisplay();
                                        ToastHelper.showSuccess(getContext(), "Purchase successful!");
                                    }
                                }
                                
                                @Override
                                public void onError(String errorMessage) {
                                    if (getActivity() != null && !getActivity().isFinishing()) {
                                        ToastHelper.showSuccess(getContext(), "Purchase successful! Balance will update shortly.");
                                    }
                                }
                            }
                        );
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

    // ---------------- Backend Integration ----------------
    private static final String BASE_URL = "https://bidhub-android-app.onrender.com/api";

    private void fetchBalanceFromBackend() {
        new Thread(() -> {
            try {
                String token = prefsHelper.getAuthToken();
                if (token == null || token.isEmpty()) {
                    getActivity().runOnUiThread(() -> ToastHelper.showError(getContext(), "Please log in again"));
                    return;
                }

                URL url = new URL(BASE_URL + "/credits/balance");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int code = conn.getResponseCode();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                    code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream()
                ));
                StringBuilder sb = new StringBuilder();
                String line; while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                if (code >= 200 && code < 300) {
                    JSONObject json = new JSONObject(sb.toString());
                    double credits = json.optDouble("credits", 0.0);
                    // sync cache
                    prefsHelper.setCredits(credits);
                    getActivity().runOnUiThread(this::updateBalanceDisplay);
                } else {
                    getActivity().runOnUiThread(() -> ToastHelper.showError(getContext(), "Failed to fetch balance"));
                }
            } catch (Exception e) {
                getActivity().runOnUiThread(() -> ToastHelper.showError(getContext(), "Network error: " + e.getMessage()));
            }
        }).start();
    }

    private void purchaseCreditsBackend(int amount, String paymentMethod) {
        new Thread(() -> {
            try {
                String token = prefsHelper.getAuthToken();
                if (token == null || token.isEmpty()) {
                    getActivity().runOnUiThread(() -> ToastHelper.showError(getContext(), "Please log in again"));
                    return;
                }

                URL url = new URL(BASE_URL + "/credits/purchase");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("amount", amount);
                body.put("payment_method", paymentMethod);
                body.put("transaction_id", "MOB-" + System.currentTimeMillis());

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes("UTF-8"));
                }

                int code = conn.getResponseCode();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                    code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream()
                ));
                StringBuilder sb = new StringBuilder();
                String line; while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                if (code >= 200 && code < 300) {
                    // After successful purchase, refresh from backend to keep UI and cache in sync
                    com.cc106.bidhub.utils.CreditBalanceManager.refreshBalance(
                        getContext(),
                        new com.cc106.bidhub.utils.CreditBalanceManager.BalanceUpdateCallback() {
                            @Override
                            public void onBalanceUpdated(double newBalance) {
                                if (getActivity() != null && !getActivity().isFinishing()) {
                                    updateBalanceDisplay();
                                    ToastHelper.showSuccess(getContext(), "Purchase successful! ₱" + amount + " credits added.");
                                }
                            }
                            
                            @Override
                            public void onError(String errorMessage) {
                                if (getActivity() != null && !getActivity().isFinishing()) {
                                    ToastHelper.showSuccess(getContext(), "Purchase successful! Balance will update shortly.");
                                }
                            }
                        }
                    );
                } else {
                    getActivity().runOnUiThread(() -> ToastHelper.showError(getContext(), "Purchase failed"));
                }
            } catch (Exception e) {
                getActivity().runOnUiThread(() -> ToastHelper.showError(getContext(), "Network error: " + e.getMessage()));
            }
        }).start();
    }
    
    
    private void addTestButton() {
        // Only show test button if packagesContainer is available
        if (packagesContainer == null) {
            return;
        }
        
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
<<<<<<< Updated upstream
     * FIX: Load recent transactions from backend API
     * Fetches both credit transactions and top-ups to show complete history
     */
    private void loadRecentTransactions() {
        if (getContext() == null) {
            return;
        }
        
        // Fetch from backend API on background thread
        new Thread(() -> {
            try {
                SharedPreferencesHelper prefsHelper = new SharedPreferencesHelper(getContext());
                String token = prefsHelper.getAuthToken();
                
                if (token == null || token.isEmpty()) {
                    android.util.Log.w("CreditsFragment", "No auth token, cannot fetch transaction history");
                    return;
                }
                
                // Fetch credit transactions from API
                List<CreditTransaction> apiTransactions = fetchCreditTransactionsFromApi(token);
                
                // Fetch top-ups from API
                List<CreditTransaction> topupTransactions = fetchTopupsFromApi(token);
                
                // Combine and sort by date
                List<CreditTransaction> allTransactions = new ArrayList<>();
                allTransactions.addAll(apiTransactions);
                allTransactions.addAll(topupTransactions);
                
                // Sort by date descending (newest first)
                java.util.Collections.sort(allTransactions, (t1, t2) -> {
                    if (t1.getCreatedAt() == null && t2.getCreatedAt() == null) return 0;
                    if (t1.getCreatedAt() == null) return 1;
                    if (t2.getCreatedAt() == null) return -1;
                    return t2.getCreatedAt().compareTo(t1.getCreatedAt());
                });
                
                // Update transaction history
                transactionHistory = allTransactions;
                
                // Update UI on main thread
                if (getActivity() != null && !getActivity().isFinishing()) {
                    getActivity().runOnUiThread(() -> {
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
                    });
                }
                
            } catch (Exception e) {
                android.util.Log.e("CreditsFragment", "Error loading recent transactions from API: " + e.getMessage(), e);
                // Fallback to local transactions
                if (creditManager != null && userId != null) {
                    transactionHistory = creditManager.getTransactionHistory(userId);
                }
            }
        }).start();
    }
    
    /**
     * Fetch credit transactions from backend API
     */
    private List<CreditTransaction> fetchCreditTransactionsFromApi(String token) {
        List<CreditTransaction> transactions = new ArrayList<>();
        
        try {
            URL url = new URL(BASE_URL + "/credits/transactions?limit=50&offset=0");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            
            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                
                JSONObject json = new JSONObject(sb.toString());
                JSONArray transactionsArray = json.optJSONArray("transactions");
                
                if (transactionsArray != null) {
                    for (int i = 0; i < transactionsArray.length(); i++) {
                        JSONObject txJson = transactionsArray.getJSONObject(i);
                        CreditTransaction tx = new CreditTransaction();
                        tx.setTransactionId(txJson.optString("id", ""));
                        tx.setType(txJson.optString("type", ""));
                        tx.setAmount(txJson.optDouble("amount", 0.0));
                        tx.setDescription(txJson.optString("description", ""));
                        tx.setStatus(txJson.optString("status", ""));
                        tx.setReference(txJson.optString("reference", ""));
                        
                        // Parse created_at
                        if (txJson.has("created_at") && !txJson.isNull("created_at")) {
                            try {
                                String dateStr = txJson.getString("created_at");
                                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                                sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                                tx.setCreatedAt(sdf.parse(dateStr));
                            } catch (Exception e) {
                                android.util.Log.w("CreditsFragment", "Error parsing transaction date: " + e.getMessage());
                                tx.setCreatedAt(new Date());
                            }
                        } else {
                            tx.setCreatedAt(new Date());
                        }
                        
                        transactions.add(tx);
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.e("CreditsFragment", "Error fetching credit transactions from API: " + e.getMessage(), e);
        }
        
        return transactions;
    }
    
    /**
     * Fetch top-ups from backend API and convert to CreditTransaction format
     */
    private List<CreditTransaction> fetchTopupsFromApi(String token) {
        List<CreditTransaction> transactions = new ArrayList<>();
        
        try {
            URL url = new URL(BASE_URL + "/topups?limit=50&offset=0");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            
            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                
                JSONObject json = new JSONObject(sb.toString());
                JSONArray topupsArray = json.optJSONArray("topups");
                
                if (topupsArray != null) {
                    for (int i = 0; i < topupsArray.length(); i++) {
                        JSONObject topupJson = topupsArray.getJSONObject(i);
                        
                        // Only include confirmed top-ups as successful purchases
                        String status = topupJson.optString("status", "");
                        if (!"CONFIRMED".equals(status)) {
                            continue; // Skip pending/under review/rejected top-ups
                        }
                        
                        CreditTransaction tx = new CreditTransaction();
                        tx.setTransactionId("topup_" + topupJson.optInt("id", 0));
                        tx.setType(SimpleCreditManager.TRANSACTION_PURCHASE);
                        tx.setAmount(topupJson.optDouble("amount", 0.0));
                        tx.setDescription("Top-up via " + topupJson.optString("payment_method", "GCash"));
                        tx.setStatus("CONFIRMED");
                        tx.setReference(topupJson.optString("generated_ref", ""));
                        
                        // Parse created_at
                        if (topupJson.has("created_at") && !topupJson.isNull("created_at")) {
                            try {
                                String dateStr = topupJson.getString("created_at");
                                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                                sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                                tx.setCreatedAt(sdf.parse(dateStr));
                            } catch (Exception e) {
                                android.util.Log.w("CreditsFragment", "Error parsing topup date: " + e.getMessage());
                                tx.setCreatedAt(new Date());
                            }
                        } else {
                            tx.setCreatedAt(new Date());
                        }
                        
                        transactions.add(tx);
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.e("CreditsFragment", "Error fetching top-ups from API: " + e.getMessage(), e);
        }
        
        return transactions;
    }
    
    /**
     * Refresh credit transaction history from backend API
     * This method uses CreditBalanceManager for consistency with other parts of the app
     */
    private void refreshCreditHistoryFromBackend() {
        android.util.Log.d("CreditsFragment", "Refreshing credit transaction history from backend");
        
        com.cc106.bidhub.utils.CreditBalanceManager.refreshTransactionHistory(
            getContext(),
            new com.cc106.bidhub.utils.CreditBalanceManager.TransactionHistoryCallback() {
                @Override
                public void onHistoryUpdated(org.json.JSONArray transactions) {
                    if (getActivity() != null && !getActivity().isFinishing()) {
                        try {
                            // Convert JSONArray to List<CreditTransaction>
                            transactionHistory = new ArrayList<>();
                            if (transactions != null) {
                                for (int i = 0; i < transactions.length(); i++) {
                                    org.json.JSONObject txJson = transactions.getJSONObject(i);
                                    CreditTransaction tx = new CreditTransaction();
                                    
                                    // Map backend fields to CreditTransaction
                                    tx.setTransactionId(txJson.optString("id", String.valueOf(txJson.optInt("id", 0))));
                                    tx.setUserId(String.valueOf(txJson.optInt("user_id", 0)));
                                    tx.setType(txJson.optString("type", "unknown"));
                                    tx.setAmount(txJson.optDouble("amount", 0.0));
                                    tx.setStatus(txJson.optString("status", "completed"));
                                    tx.setPaymentMethod(txJson.optString("payment_method", ""));
                                    tx.setReference(txJson.optString("reference", ""));
                                    
                                    // Parse created_at timestamp
                                    String createdAtStr = txJson.optString("created_at", "");
                                    if (!createdAtStr.isEmpty()) {
                                        try {
                                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
                                            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                                            tx.setCreatedAt(sdf.parse(createdAtStr));
                                        } catch (Exception e) {
                                            android.util.Log.w("CreditsFragment", "Failed to parse created_at: " + createdAtStr, e);
                                            tx.setCreatedAt(new Date());
                                        }
                                    } else {
                                        tx.setCreatedAt(new Date());
                                    }
                                    
                                    // Set description based on type
                                    if ("purchase".equals(tx.getType())) {
                                        tx.setDescription("Top-up: ₱" + String.format("%.2f", Math.abs(tx.getAmount())));
                                    } else if ("bid".equals(tx.getType())) {
                                        tx.setDescription("Bid placed: ₱" + String.format("%.2f", Math.abs(tx.getAmount())));
                                    } else {
                                        tx.setDescription(txJson.optString("description", tx.getType()));
                                    }
                                    
                                    transactionHistory.add(tx);
                                }
                            }
                            
                            android.util.Log.i("CreditsFragment", "Loaded " + transactionHistory.size() + " transactions from backend");
                            
                            // Update UI with last transaction
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
                            android.util.Log.e("CreditsFragment", "Error parsing transaction history", e);
                        }
                    }
                }
                
                @Override
                public void onError(String errorMessage) {
                    android.util.Log.w("CreditsFragment", "Failed to refresh transaction history: " + errorMessage);
                    // Fallback to local storage if backend fails
                    if (creditManager != null && userId != null) {
                        try {
                            transactionHistory = creditManager.getTransactionHistory(userId);
                        } catch (Exception e) {
                            android.util.Log.e("CreditsFragment", "Error loading local transaction history", e);
                        }
                    }
                }
            }
        );
    }
    
    /**
     * Show enhanced transaction history dialog with proper adapter
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
        recyclerView.setPadding(0, 16, 0, 16);
        
        // Use TransactionHistoryAdapter for proper display
        TransactionHistoryAdapter adapter = new TransactionHistoryAdapter(transactionHistory);
        recyclerView.setAdapter(adapter);
        
        // Wrap RecyclerView in a container with proper height
        android.widget.FrameLayout container = new android.widget.FrameLayout(getContext());
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            (int) (getResources().getDisplayMetrics().heightPixels * 0.6) // 60% of screen height
        );
        recyclerView.setLayoutParams(params);
        container.addView(recyclerView);
        
        builder.setView(container);
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
    
    @Override
    public void onResume() {
        super.onResume();
        // Refresh balance when fragment becomes visible to ensure latest data
        if (userId != null) {
            loadCreditInformation();
        }
    }
}
