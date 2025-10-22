package com.cc106.bidhub.api;

import android.content.Context;
import android.util.Log;
import com.cc106.bidhub.utils.SharedPreferencesHelper;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * API Client for authentication-related backend operations
 */
public class AuthApiClient {
    private static final String TAG = "AuthApiClient";
    private static final String BASE_URL = "https://bidhub-android-app.onrender.com/api";
    private static final String LOGIN_ENDPOINT = BASE_URL + "/auth/login";
    private static final String REGISTER_ENDPOINT = BASE_URL + "/auth/register";
    
    private Context context;
    private SharedPreferencesHelper prefsHelper;
    
    public AuthApiClient(Context context) {
        this.context = context;
        this.prefsHelper = new SharedPreferencesHelper(context);
    }
    
    /**
     * Login user with email and password
     */
    public ApiResponse login(String email, String password) {
        try {
            URL url = new URL(LOGIN_ENDPOINT);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // Set request method and headers
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);
            // Render free tier can cold start and take ~50s; increase timeouts accordingly
            connection.setConnectTimeout(60000);
            connection.setReadTimeout(60000);
            
            // Create request body
            JSONObject requestData = new JSONObject();
            requestData.put("email", email);
            requestData.put("password", password);
            
            // Debug logging
            Log.d(TAG, "Login request - Email: " + email + ", Password length: " + password.length());
            
            // Send request
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestData.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            
            // Get response
            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Login response code: " + responseCode);
            
            BufferedReader reader;
            if (responseCode >= 200 && responseCode < 300) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            } else {
                reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            }
            
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            String responseBody = response.toString();
            Log.d(TAG, "Login response: " + responseBody);
            
            if (responseCode >= 200 && responseCode < 300) {
                JSONObject jsonResponse = new JSONObject(responseBody);
                String token = jsonResponse.optString("token", "");
                JSONObject user = jsonResponse.optJSONObject("user");
                
                if (!token.isEmpty() && user != null) {
                    // Store token and user info
                    prefsHelper.saveAuthToken(token);
                    prefsHelper.saveUserEmail(email);
                    prefsHelper.saveUsername(user.optString("username", ""));
                    prefsHelper.setUserId(String.valueOf(user.optInt("id", 0)));
                    prefsHelper.setAlias(user.optString("alias", ""));
                    // Immediately refresh credits from backend to avoid stale cache
                    try {
                        URL balUrl = new URL(BASE_URL + "/credits/balance");
                        HttpURLConnection balConn = (HttpURLConnection) balUrl.openConnection();
                        balConn.setRequestMethod("GET");
                        balConn.setRequestProperty("Authorization", "Bearer " + token);
                        balConn.setConnectTimeout(60000);
                        balConn.setReadTimeout(60000);
                        int balCode = balConn.getResponseCode();
                        if (balCode >= 200 && balCode < 300) {
                            BufferedReader br = new BufferedReader(new InputStreamReader(balConn.getInputStream()));
                            StringBuilder sb = new StringBuilder();
                            String ln; while ((ln = br.readLine()) != null) sb.append(ln); br.close();
                            JSONObject balJson = new JSONObject(sb.toString());
                            double credits = balJson.optDouble("credits", balJson.optDouble("balance", 0.0));
                            prefsHelper.setCredits(credits);
                        } else {
                            prefsHelper.setCredits(user.optDouble("credits", 0.0));
                        }
                    } catch (Exception ignore) {
                        prefsHelper.setCredits(user.optDouble("credits", 0.0));
                    }
                    
                    return new ApiResponse(true, "Login successful", jsonResponse);
                } else {
                    return new ApiResponse(false, "Invalid response format", null);
                }
            } else {
                JSONObject errorResponse = new JSONObject(responseBody);
                String errorMessage = errorResponse.optString("error", "Login failed");
                Log.e(TAG, "Login failed - Response code: " + responseCode + ", Message: " + errorMessage);
                return new ApiResponse(false, errorMessage, null);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Login error", e);
            return new ApiResponse(false, "Network error: " + e.getMessage(), null);
        }
    }
    
    /**
     * Register new user
     */
    public ApiResponse register(String username, String email, String password, 
                              String phoneNumber, String firstName, String lastName, String alias) {
        try {
            URL url = new URL(REGISTER_ENDPOINT);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // Set request method and headers
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);
            // Render free tier can cold start and take ~50s; increase timeouts accordingly
            connection.setConnectTimeout(60000);
            connection.setReadTimeout(60000);
            
            // Create request body
            JSONObject requestData = new JSONObject();
            requestData.put("username", username);
            requestData.put("email", email);
            requestData.put("password", password);
            requestData.put("phone_number", phoneNumber);
            requestData.put("first_name", firstName);
            requestData.put("last_name", lastName);
            requestData.put("alias", alias);
            
            // Send request
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestData.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            
            // Get response
            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Register response code: " + responseCode);
            
            BufferedReader reader;
            if (responseCode >= 200 && responseCode < 300) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            } else {
                reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            }
            
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            String responseBody = response.toString();
            Log.d(TAG, "Register response: " + responseBody);
            
            if (responseCode >= 200 && responseCode < 300) {
                JSONObject jsonResponse = new JSONObject(responseBody);
                String token = jsonResponse.optString("token", "");
                JSONObject user = jsonResponse.optJSONObject("user");
                
                if (!token.isEmpty() && user != null) {
                    // Store token and user info
                    prefsHelper.saveAuthToken(token);
                    prefsHelper.saveUserEmail(email);
                    prefsHelper.saveUsername(username);
                    prefsHelper.setUserId(String.valueOf(user.optInt("id", 0)));
                    prefsHelper.setAlias(user.optString("alias", ""));
                    prefsHelper.setCredits(user.optDouble("credits", 0.0));
                    
                    return new ApiResponse(true, "Registration successful", jsonResponse);
                } else {
                    return new ApiResponse(false, "Invalid response format", null);
                }
            } else {
                JSONObject errorResponse = new JSONObject(responseBody);
                String errorMessage = errorResponse.optString("error", "Registration failed");
                return new ApiResponse(false, errorMessage, null);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Register error", e);
            return new ApiResponse(false, "Network error: " + e.getMessage(), null);
        }
    }
}
