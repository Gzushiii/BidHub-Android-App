package com.cc106.bidhub.utils;

public class Config {
    // Backend API Base URL
    // Change this to your Render deployment URL
    public static final String API_BASE_URL = "https://bidhub-backend.onrender.com/api";
    
    // API Endpoints
    public static final String ENDPOINT_AUTH_REGISTER = "/auth/register";
    public static final String ENDPOINT_AUTH_LOGIN = "/auth/login";
    public static final String ENDPOINT_ITEMS = "/items";
    public static final String ENDPOINT_BIDS = "/bids/place";
    public static final String ENDPOINT_CREDITS_BALANCE = "/credits/balance";
    public static final String ENDPOINT_CREDITS_TRANSACTIONS = "/credits/transactions";
    public static final String ENDPOINT_CREDITS_PURCHASE = "/credits/purchase";
    public static final String ENDPOINT_CATEGORIES = "/categories";
    public static final String ENDPOINT_TOPUPS = "/topups";
    
    // SharedPreferences Keys
    public static final String PREFS_NAME = "BidHubPrefs";
    public static final String KEY_AUTH_TOKEN = "auth_token";
    public static final String KEY_USER_ID = "user_id";
    public static final String KEY_USER_EMAIL = "user_email";
    
    // Request Timeouts
    public static final int CONNECT_TIMEOUT = 30000; // 30 seconds
    public static final int READ_TIMEOUT = 30000; // 30 seconds
}

