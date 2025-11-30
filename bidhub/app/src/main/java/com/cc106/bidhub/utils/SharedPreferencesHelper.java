package com.cc106.bidhub.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Helper class for managing SharedPreferences
 */
public class SharedPreferencesHelper {
    private static final String PREFS_NAME = "BidHubPrefs";
    private static final String KEY_AUTH_TOKEN = "auth_token";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_ALIAS = "alias";
    private static final String KEY_CREDITS = "credits";
    
    private SharedPreferences prefs;
    
    public SharedPreferencesHelper(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public void setAuthToken(String token) {
        Log.i("SharedPreferencesHelper", "=== SETTING AUTH TOKEN ===");
        Log.i("SharedPreferencesHelper", "Token: " + (token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "NULL"));
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply();
        Log.i("SharedPreferencesHelper", "Token saved to SharedPreferences");
    }
    
    public String getAuthToken() {
        String token = prefs.getString(KEY_AUTH_TOKEN, null);
        Log.i("SharedPreferencesHelper", "=== RETRIEVING AUTH TOKEN ===");
        Log.i("SharedPreferencesHelper", "Retrieved token: " + (token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "NULL"));
        return token;
    }
    
    public void setUserEmail(String email) {
        prefs.edit().putString(KEY_USER_EMAIL, email).apply();
    }
    
    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, null);
    }
    
    public void setUserId(String userId) {
        prefs.edit().putString(KEY_USER_ID, userId).apply();
    }
    
    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }
    
    public void setUsername(String username) {
        prefs.edit().putString(KEY_USERNAME, username).apply();
    }
    
    public String getUsername() {
        return prefs.getString(KEY_USERNAME, null);
    }
    
    public void setAlias(String alias) {
        prefs.edit().putString(KEY_ALIAS, alias).apply();
    }
    
    public String getAlias() {
        return prefs.getString(KEY_ALIAS, null);
    }
    
    /**
     * Save user credits to SharedPreferences
     * Ensures credits are always stored as double (converted to float for storage)
     * @param credits The credit balance to save (must be numeric)
     */
    public void setCredits(double credits) {
        double oldCredits = getCredits();
        float creditsFloat = (float) credits;
        prefs.edit().putFloat(KEY_CREDITS, creditsFloat).apply();
        Log.i("SharedPreferencesHelper", String.format("=== CREDITS UPDATED ==="));
        Log.i("SharedPreferencesHelper", String.format("Old balance: %.2f", oldCredits));
        Log.i("SharedPreferencesHelper", String.format("New balance: %.2f", credits));
        Log.i("SharedPreferencesHelper", String.format("Difference: %.2f", credits - oldCredits));
    }
    
    /**
     * Get user credits from SharedPreferences
     * Always returns a double value (converted from stored float)
     * @return The current credit balance, or 0.0 if not set
     */
    public double getCredits() {
        double credits = prefs.getFloat(KEY_CREDITS, 0.0f);
        Log.d("SharedPreferencesHelper", String.format("=== CREDITS RETRIEVED ==="));
        Log.d("SharedPreferencesHelper", String.format("Current balance: %.2f", credits));
        return credits;
    }
    
    /**
     * Alias method for setCredits - saves user credits
     * @param credits The credit balance to save
     */
    public void saveUserCredits(double credits) {
        setCredits(credits);
    }
    
    /**
     * Alias method for getCredits - gets user credits
     * @return The current credit balance
     */
    public double getUserCredits() {
        return getCredits();
    }
    
    // Alias methods for compatibility with AuthApiClient
    public void saveAuthToken(String token) {
        Log.i("SharedPreferencesHelper", "=== SAVE AUTH TOKEN (ALIAS) ===");
        Log.i("SharedPreferencesHelper", "Token: " + (token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "NULL"));
        setAuthToken(token);
    }
    
    public void saveUserEmail(String email) {
        setUserEmail(email);
    }
    
    public void saveUsername(String username) {
        setUsername(username);
    }
    
    public void clearAll() {
        prefs.edit().clear().apply();
    }

    public void logout() {
        clearAll();
    }
}
