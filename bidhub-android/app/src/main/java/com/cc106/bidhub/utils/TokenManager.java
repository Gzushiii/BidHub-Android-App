package com.cc106.bidhub.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class TokenManager {
    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(Config.PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public static void saveToken(Context context, String token) {
        getPrefs(context).edit().putString(Config.KEY_AUTH_TOKEN, token).apply();
    }
    
    public static String getToken(Context context) {
        return getPrefs(context).getString(Config.KEY_AUTH_TOKEN, null);
    }
    
    public static void clearToken(Context context) {
        getPrefs(context).edit().remove(Config.KEY_AUTH_TOKEN).apply();
    }
    
    public static boolean isLoggedIn(Context context) {
        return getToken(context) != null;
    }
    
    public static void saveUserId(Context context, int userId) {
        getPrefs(context).edit().putInt(Config.KEY_USER_ID, userId).apply();
    }
    
    public static int getUserId(Context context) {
        return getPrefs(context).getInt(Config.KEY_USER_ID, -1);
    }
    
    public static void saveUserEmail(Context context, String email) {
        getPrefs(context).edit().putString(Config.KEY_USER_EMAIL, email).apply();
    }
    
    public static String getUserEmail(Context context) {
        return getPrefs(context).getString(Config.KEY_USER_EMAIL, null);
    }
    
    public static void clearAll(Context context) {
        getPrefs(context).edit().clear().apply();
    }
}

