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
        if (balanceAmount == null) return;

        // Prefer server value stored in SharedPreferences, fallback to local manager
        double serverCredits = prefsHelper.getCredits();
        double display = serverCredits > 0 ? serverCredits : creditManager.getCreditBalance(userId);
        balanceAmount.setText(creditManager.formatCurrency(display));
    }
    
    private void refreshBalance() {
        // Fetch from backend and sync
        fetchBalanceFromBackend();
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
        
        // Enable submit button when reference number is entered
        etReferenceNumber.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean isValid = s != null && s.toString().trim().length() >= 4;
                btnSubmit.setEnabled(isValid && topupId[0] > 0);
                if (s != null && s.toString().trim().length() > 0 && s.toString().trim().length() < 4) {
                    tilReferenceNumber.setError(getString(R.string.invalid_reference));
                } else {
                    tilReferenceNumber.setError(null);
                }
            }
            
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
        
        // Cancel button
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        // Submit button
        btnSubmit.setOnClickListener(v -> {
            String refNumber = etReferenceNumber.getText().toString().trim();
            if (refNumber.length() < 4) {
                tilReferenceNumber.setError(getString(R.string.invalid_reference));
                return;
            }
            
            // Show loading
            progressPayment.setVisibility(android.view.View.VISIBLE);
            btnSubmit.setEnabled(false);
            btnCancel.setEnabled(false);
            
            // Submit reference number
            submitTopupReference(topupId[0], refNumber, new TopupSubmitCallback() {
                @Override
                public void onSuccess() {
                    getActivity().runOnUiThread(() -> {
                        progressPayment.setVisibility(android.view.View.GONE);
                        dialog.dismiss();
                        ToastHelper.showSuccess(getContext(), getString(R.string.payment_submitted));
                        // Refresh balance after a delay
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                            fetchBalanceFromBackend();
                        }, 2000);
                    });
                }
                
                @Override
                public void onError(String error) {
                    getActivity().runOnUiThread(() -> {
                        progressPayment.setVisibility(android.view.View.GONE);
                        btnSubmit.setEnabled(true);
                        btnCancel.setEnabled(true);
                        ToastHelper.showError(getContext(), "Failed to submit reference: " + error);
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
        void onSuccess();
        void onError(String error);
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
     * Submit reference number for top-up
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
                // Validate reference number
                if (referenceNumber == null || referenceNumber.trim().length() < 4) {
                    isSubmitRequestInProgress = false;
                    callback.onError("Reference number must be at least 4 characters");
                    return;
                }
                
                String token = prefsHelper.getAuthToken();
                if (token == null || token.isEmpty()) {
                    isSubmitRequestInProgress = false;
                    callback.onError("Please log in again");
                    return;
                }
                
                android.util.Log.d("CreditsFragment", "Submitting top-up reference: topupId=" + topupId + ", ref=" + referenceNumber);
                
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
                    android.util.Log.d("CreditsFragment", "Reference submitted successfully");
                    isSubmitRequestInProgress = false;
                    callback.onSuccess();
                } else {
                    // Parse error response
                    String errorMessage = "Failed to submit reference";
                    try {
                        JSONObject errorJson = new JSONObject(responseBody);
                        errorMessage = errorJson.optString("error", "Failed to submit reference");
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
                    android.util.Log.e("CreditsFragment", "Reference submission failed: " + errorMessage);
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
                        // Deprecated local add; we rely on backend now
                        fetchBalanceFromBackend();
                        ToastHelper.showSuccess(getContext(), "Purchase successful!");
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
                    fetchBalanceFromBackend();
                    getActivity().runOnUiThread(() -> ToastHelper.showSuccess(getContext(), "Purchase successful! ₱" + amount + " credits added."));
                } else {
                    getActivity().runOnUiThread(() -> ToastHelper.showError(getContext(), "Purchase failed"));
                }
            } catch (Exception e) {
                getActivity().runOnUiThread(() -> ToastHelper.showError(getContext(), "Network error: " + e.getMessage()));
            }
        }).start();
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
