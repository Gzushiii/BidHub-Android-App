package com.cc106.bidhub.utils;

import android.content.Context;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Centralized utility for managing credit balance updates across the app
 */
public class CreditBalanceManager {
    
    private static final String TAG = "CreditBalanceManager";
    private static final String BASE_URL = "https://bidhub-android-app.onrender.com/api";
    private static final String BALANCE_ENDPOINT = BASE_URL + "/credits/balance";
    private static final String TRANSACTIONS_ENDPOINT = BASE_URL + "/credits/transactions";
    
    /**
     * Refresh credit balance from backend and update SharedPreferences
     * @param context Application context
     * @param callback Callback to notify when balance is updated (runs on main thread)
     * @return true if refresh was initiated, false if already in progress or error
     */
    public static boolean refreshBalance(Context context, BalanceUpdateCallback callback) {
        if (context == null) {
            Log.e(TAG, "Context is null, cannot refresh balance");
            return false;
        }
        
        SharedPreferencesHelper prefsHelper = new SharedPreferencesHelper(context);
        String authToken = prefsHelper.getAuthToken();
        
        if (authToken == null || authToken.isEmpty()) {
            Log.w(TAG, "No auth token available, cannot refresh balance");
            if (callback != null) {
                callback.onError("Not authenticated");
            }
            return false;
        }
        
        // Run on background thread
        new Thread(() -> {
            try {
                URL url = new URL(BALANCE_ENDPOINT);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Authorization", "Bearer " + authToken);
                connection.setRequestProperty("Accept", "application/json");
                connection.setConnectTimeout(30000); // 30 seconds
                connection.setReadTimeout(30000);
                
                int responseCode = connection.getResponseCode();
                
                if (responseCode >= 200 && responseCode < 300) {
                    // Read response
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    // Parse JSON response
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    double newBalance = jsonResponse.optDouble("credits", 
                        jsonResponse.optDouble("balance", prefsHelper.getCredits()));
                    
                    // Update SharedPreferences
                    prefsHelper.setCredits(newBalance);
                    
                    Log.i(TAG, "Balance refreshed successfully: " + newBalance);
                    
                    // Notify callback on main thread if provided
                    if (callback != null) {
                        android.os.Handler mainHandler = new android.os.Handler(
                            android.os.Looper.getMainLooper());
                        mainHandler.post(() -> callback.onBalanceUpdated(newBalance));
                    }
                    
                } else {
                    Log.w(TAG, "Failed to refresh balance, status code: " + responseCode);
                    if (callback != null) {
                        android.os.Handler mainHandler = new android.os.Handler(
                            android.os.Looper.getMainLooper());
                        mainHandler.post(() -> callback.onError("Failed to refresh balance"));
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error refreshing balance", e);
                if (callback != null) {
                    android.os.Handler mainHandler = new android.os.Handler(
                        android.os.Looper.getMainLooper());
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        }).start();
        
        return true;
    }
    
    /**
     * Get current balance from SharedPreferences (cached value)
     * @param context Application context
     * @return Current cached balance
     */
    public static double getCurrentBalance(Context context) {
        if (context == null) {
            return 0.0;
        }
        SharedPreferencesHelper prefsHelper = new SharedPreferencesHelper(context);
        return prefsHelper.getCredits();
    }
    
    /**
     * Update balance in SharedPreferences immediately (for use when balance is confirmed from API)
     * This is useful when you receive a balance update from an API response (e.g., after top-up)
     * and want to update the cache immediately without making another API call.
     * @param context Application context
     * @param newBalance The new balance value to save
     */
    public static void updateBalanceImmediately(Context context, double newBalance) {
        if (context == null) {
            Log.w(TAG, "Context is null, cannot update balance");
            return;
        }
        SharedPreferencesHelper prefsHelper = new SharedPreferencesHelper(context);
        prefsHelper.setCredits(newBalance);
        Log.i(TAG, "Balance updated immediately in SharedPreferences: " + newBalance);
    }
    
    /**
     * Refresh credit transaction history from backend
     * @param context Application context
     * @param callback Callback to notify when history is updated (runs on main thread)
     * @param limit Maximum number of transactions to fetch (default: 50)
     * @param offset Offset for pagination (default: 0)
     * @return true if refresh was initiated, false if error
     */
    public static boolean refreshTransactionHistory(Context context, TransactionHistoryCallback callback, int limit, int offset) {
        if (context == null) {
            Log.e(TAG, "Context is null, cannot refresh transaction history");
            return false;
        }
        
        SharedPreferencesHelper prefsHelper = new SharedPreferencesHelper(context);
        String authToken = prefsHelper.getAuthToken();
        
        if (authToken == null || authToken.isEmpty()) {
            Log.w(TAG, "No auth token available, cannot refresh transaction history");
            if (callback != null) {
                callback.onError("Not authenticated");
            }
            return false;
        }
        
        // Run on background thread
        new Thread(() -> {
            try {
                StringBuilder urlBuilder = new StringBuilder(TRANSACTIONS_ENDPOINT);
                urlBuilder.append("?limit=").append(limit).append("&offset=").append(offset);
                URL url = new URL(urlBuilder.toString());
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Authorization", "Bearer " + authToken);
                connection.setRequestProperty("Accept", "application/json");
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(30000);
                
                int responseCode = connection.getResponseCode();
                
                if (responseCode >= 200 && responseCode < 300) {
                    // Read response
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    // Parse JSON response
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONArray transactionsArray = jsonResponse.optJSONArray("transactions");
                    
                    Log.i(TAG, "Transaction history refreshed successfully: " + 
                        (transactionsArray != null ? transactionsArray.length() : 0) + " transactions");
                    
                    // Notify callback on main thread if provided
                    if (callback != null) {
                        android.os.Handler mainHandler = new android.os.Handler(
                            android.os.Looper.getMainLooper());
                        mainHandler.post(() -> callback.onHistoryUpdated(transactionsArray));
                    }
                    
                } else {
                    Log.w(TAG, "Failed to refresh transaction history, status code: " + responseCode);
                    if (callback != null) {
                        android.os.Handler mainHandler = new android.os.Handler(
                            android.os.Looper.getMainLooper());
                        mainHandler.post(() -> callback.onError("Failed to refresh transaction history"));
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error refreshing transaction history", e);
                if (callback != null) {
                    android.os.Handler mainHandler = new android.os.Handler(
                        android.os.Looper.getMainLooper());
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        }).start();
        
        return true;
    }
    
    /**
     * Refresh credit transaction history from backend (with default limit/offset)
     * @param context Application context
     * @param callback Callback to notify when history is updated (runs on main thread)
     * @return true if refresh was initiated, false if error
     */
    public static boolean refreshTransactionHistory(Context context, TransactionHistoryCallback callback) {
        return refreshTransactionHistory(context, callback, 50, 0);
    }
    
    /**
     * Callback interface for balance updates
     */
    public interface BalanceUpdateCallback {
        /**
         * Called when balance is successfully updated
         * @param newBalance The new balance value
         */
        void onBalanceUpdated(double newBalance);
        
        /**
         * Called when an error occurs during refresh
         * @param errorMessage Error message
         */
        void onError(String errorMessage);
    }
    
    /**
     * Callback interface for transaction history updates
     */
    public interface TransactionHistoryCallback {
        /**
         * Called when transaction history is successfully updated
         * @param transactions JSONArray of transaction objects from backend
         */
        void onHistoryUpdated(org.json.JSONArray transactions);
        
        /**
         * Called when an error occurs during refresh
         * @param errorMessage Error message
         */
        void onError(String errorMessage);
    }
}

