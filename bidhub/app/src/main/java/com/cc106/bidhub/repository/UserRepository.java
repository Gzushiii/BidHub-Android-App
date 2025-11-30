package com.cc106.bidhub.repository;

import android.content.Context;
import android.util.Log;
import com.cc106.bidhub.utils.SharedPreferencesHelper;
import com.cc106.bidhub.utils.CreditBalanceManager;

/**
 * Centralized repository for user data management
 * Provides single source of truth for user information including credits
 * Synchronizes SharedPreferences, API responses, and UI components
 */
public class UserRepository {
    private static final String TAG = "UserRepository";
    private static UserRepository instance;
    private SharedPreferencesHelper prefsHelper;
    private Context context;
    
    // Current user data cache
    private String userId;
    private String userEmail;
    private String username;
    private String alias;
    private double credits;
    private boolean isDataLoaded = false;
    
    private UserRepository(Context context) {
        this.context = context.getApplicationContext();
        this.prefsHelper = new SharedPreferencesHelper(this.context);
        loadUserDataFromPreferences();
    }
    
    /**
     * Get singleton instance of UserRepository
     * @param context Application context
     * @return UserRepository instance
     */
    public static synchronized UserRepository getInstance(Context context) {
        if (instance == null) {
            instance = new UserRepository(context);
        }
        return instance;
    }
    
    /**
     * Load user data from SharedPreferences
     * Called on initialization and when data needs to be refreshed
     */
    public void loadUserDataFromPreferences() {
        try {
            userId = prefsHelper.getUserId();
            userEmail = prefsHelper.getUserEmail();
            username = prefsHelper.getUsername();
            alias = prefsHelper.getAlias();
            credits = prefsHelper.getCredits();
            isDataLoaded = true;
            
            Log.i(TAG, "=== USER DATA LOADED FROM PREFERENCES ===");
            Log.i(TAG, "User ID: " + userId);
            Log.i(TAG, "Email: " + userEmail);
            Log.i(TAG, "Username: " + username);
            Log.i(TAG, "Alias: " + alias);
            Log.i(TAG, String.format("Credits: %.2f", credits));
        } catch (Exception e) {
            Log.e(TAG, "Error loading user data from preferences", e);
            isDataLoaded = false;
        }
    }
    
    /**
     * Update credits immediately (for use with API responses)
     * Updates both SharedPreferences and in-memory cache
     * @param newCredits The new credit balance
     */
    public void updateCreditsImmediately(double newCredits) {
        double oldCredits = this.credits;
        this.credits = newCredits;
        prefsHelper.setCredits(newCredits);
        
        Log.i(TAG, "=== CREDITS UPDATED IMMEDIATELY ===");
        Log.i(TAG, String.format("Old: %.2f -> New: %.2f (Delta: %.2f)", 
            oldCredits, newCredits, newCredits - oldCredits));
    }
    
    /**
     * Refresh credits from backend API
     * Updates SharedPreferences and in-memory cache
     * @param callback Callback to notify when refresh completes
     */
    public void refreshCreditsFromBackend(CreditBalanceManager.BalanceUpdateCallback callback) {
        Log.i(TAG, "=== REFRESHING CREDITS FROM BACKEND ===");
        CreditBalanceManager.refreshBalance(context, new CreditBalanceManager.BalanceUpdateCallback() {
            @Override
            public void onBalanceUpdated(double newBalance) {
                credits = newBalance;
                isDataLoaded = true;
                Log.i(TAG, String.format("Credits refreshed from backend: %.2f", newBalance));
                if (callback != null) {
                    callback.onBalanceUpdated(newBalance);
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Failed to refresh credits from backend: " + errorMessage);
                if (callback != null) {
                    callback.onError(errorMessage);
                }
            }
        });
    }
    
    /**
     * Update user data from login/registration response
     * @param userId User ID
     * @param email User email
     * @param username Username
     * @param alias User alias
     * @param credits Initial credits
     */
    public void updateUserData(String userId, String email, String username, String alias, double credits) {
        this.userId = userId;
        this.userEmail = email;
        this.username = username;
        this.alias = alias;
        this.credits = credits;
        
        // Save to SharedPreferences
        prefsHelper.setUserId(userId);
        prefsHelper.setUserEmail(email);
        prefsHelper.setUsername(username);
        prefsHelper.setAlias(alias);
        prefsHelper.setCredits(credits);
        
        isDataLoaded = true;
        
        Log.i(TAG, "=== USER DATA UPDATED ===");
        Log.i(TAG, "User ID: " + userId);
        Log.i(TAG, "Email: " + email);
        Log.i(TAG, "Username: " + username);
        Log.i(TAG, "Alias: " + alias);
        Log.i(TAG, String.format("Credits: %.2f", credits));
    }
    
    /**
     * Clear all user data (for logout)
     */
    public void clearUserData() {
        userId = null;
        userEmail = null;
        username = null;
        alias = null;
        credits = 0.0;
        isDataLoaded = false;
        
        prefsHelper.clearAll();
        
        Log.i(TAG, "=== USER DATA CLEARED ===");
    }
    
    // Getters
    public String getUserId() {
        if (!isDataLoaded) {
            loadUserDataFromPreferences();
        }
        return userId;
    }
    
    public String getUserEmail() {
        if (!isDataLoaded) {
            loadUserDataFromPreferences();
        }
        return userEmail;
    }
    
    public String getUsername() {
        if (!isDataLoaded) {
            loadUserDataFromPreferences();
        }
        return username;
    }
    
    public String getAlias() {
        if (!isDataLoaded) {
            loadUserDataFromPreferences();
        }
        return alias;
    }
    
    /**
     * Get current credits
     * Always returns the latest value from SharedPreferences
     * @return Current credit balance
     */
    public double getCredits() {
        // Always read from SharedPreferences to ensure latest value
        credits = prefsHelper.getCredits();
        return credits;
    }
    
    /**
     * Check if user data is loaded
     * @return true if data is loaded, false otherwise
     */
    public boolean isDataLoaded() {
        return isDataLoaded;
    }
    
    /**
     * Force reload user data from SharedPreferences
     * Useful when data may have been updated elsewhere
     */
    public void reloadUserData() {
        loadUserDataFromPreferences();
    }
}

