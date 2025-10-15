package com.cc106.bidhub.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Helper class for managing SharedPreferences
 */
public class SharedPreferencesHelper {
    private static final String PREFS_NAME = "BidHubPrefs";
    private static final String KEY_AUTH_TOKEN = "auth_token";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_ID = "user_id";
    
    private SharedPreferences prefs;
    
    public SharedPreferencesHelper(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public void setAuthToken(String token) {
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply();
    }
    
    public String getAuthToken() {
        return prefs.getString(KEY_AUTH_TOKEN, null);
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
    
    public void clearAll() {
        prefs.edit().clear().apply();
    }
}
