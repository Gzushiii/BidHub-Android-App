package com.cc106.bidhub.utils;

import android.content.Context;
import android.util.Log;
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
}

