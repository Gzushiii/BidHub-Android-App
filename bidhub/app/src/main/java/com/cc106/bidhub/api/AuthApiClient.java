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
                    Log.i(TAG, "=== LOGIN SUCCESS - PARSING USER DATA ===");
                    
                    // Extract user data with defensive type handling
                    String userId = String.valueOf(user.optInt("id", 0));
                    String username = user.optString("username", "");
                    String alias = user.optString("alias", "");
                    // Ensure credits is always parsed as double, not string
                    double credits = parseCreditsSafely(user);
                    
                    Log.i(TAG, "User ID: " + userId);
                    Log.i(TAG, "Username: " + username);
                    Log.i(TAG, "Email: " + email);
                    Log.i(TAG, String.format("Credits from response: %.2f", credits));
                    
                    // Store token first
                    prefsHelper.saveAuthToken(token);
                    Log.i(TAG, "Auth token saved");
                    
                    // Use UserRepository to update user data (centralized management)
                    com.cc106.bidhub.repository.UserRepository userRepo = 
                        com.cc106.bidhub.repository.UserRepository.getInstance(context);
                    userRepo.updateUserData(userId, email, username, alias, credits);
                    
                    // Immediately refresh credits from backend to ensure accuracy
                    // This runs in background, but we've already saved the value from login response
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
                            double backendCredits = parseCreditsSafely(balJson);
                            Log.i(TAG, String.format("Backend credits: %.2f (updating if different)", backendCredits));
                            // Update if different (backend is authoritative)
                            if (Math.abs(backendCredits - credits) > 0.01) {
                                userRepo.updateCreditsImmediately(backendCredits);
                                Log.i(TAG, "Credits updated from backend (was different from login response)");
                            }
                        } else {
                            Log.w(TAG, "Failed to fetch balance from backend, using login response value");
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Error fetching balance from backend: " + e.getMessage() + ", using login response value");
                    }
                    
                    Log.i(TAG, "=== LOGIN COMPLETE - USER DATA SAVED ===");
                    return new ApiResponse(true, "Login successful", jsonResponse);
                } else {
                    Log.e(TAG, "Invalid response format: token or user object missing");
                    return new ApiResponse(false, "Invalid response format", null);
                }
            } else {
                JSONObject errorResponse = new JSONObject(responseBody);
                String errorMessage = errorResponse.optString("error", "Login failed");
                Log.e(TAG, "Login failed - Response code: " + responseCode + ", Message: " + errorMessage);
                return new ApiResponse(false, errorMessage, null);
            }
            
        } catch (java.net.UnknownHostException e) {
            Log.e(TAG, "Login error: Unable to resolve host - " + e.getMessage(), e);
            return new ApiResponse(false, "Unable to connect to server. Please check your internet connection.", null);
        } catch (java.net.SocketTimeoutException e) {
            Log.e(TAG, "Login error: Connection timed out - " + e.getMessage(), e);
            return new ApiResponse(false, "Connection timed out. The server may be busy. Please try again.", null);
        } catch (java.io.IOException e) {
            Log.e(TAG, "Login error: Network I/O problem - " + e.getMessage(), e);
            return new ApiResponse(false, "Network error. Please check your internet connection and try again.", null);
        } catch (Exception e) {
            Log.e(TAG, "Login error: Unexpected error - " + e.getMessage(), e);
            return new ApiResponse(false, "An unexpected error occurred during login. Please try again.", null);
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

            // Debug logging
            Log.d(TAG, "Register request - Username: " + username + ", Email: " + email +
                      ", Phone: " + phoneNumber + ", Name: " + firstName + " " + lastName +
                      ", Alias: " + alias);

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
                    Log.i(TAG, "=== REGISTRATION SUCCESS - PARSING USER DATA ===");
                    
                    // Extract user data with defensive type handling
                    String userId = String.valueOf(user.optInt("id", 0));
                    String userAlias = user.optString("alias", "");
                    double credits = parseCreditsSafely(user);
                    
                    Log.i(TAG, "User ID: " + userId);
                    Log.i(TAG, "Username: " + username);
                    Log.i(TAG, "Email: " + email);
                    Log.i(TAG, String.format("Credits: %.2f", credits));
                    
                    // Store token first
                    prefsHelper.saveAuthToken(token);
                    Log.i(TAG, "Auth token saved");
                    
                    // Use UserRepository to update user data
                    com.cc106.bidhub.repository.UserRepository userRepo = 
                        com.cc106.bidhub.repository.UserRepository.getInstance(context);
                    userRepo.updateUserData(userId, email, username, userAlias, credits);
                    
                    Log.i(TAG, "=== REGISTRATION COMPLETE - USER DATA SAVED ===");
                    return new ApiResponse(true, "Registration successful", jsonResponse);
                } else {
                    Log.e(TAG, "Invalid response format: token or user object missing");
                    return new ApiResponse(false, "Invalid response format", null);
                }
            } else {
                JSONObject errorResponse = new JSONObject(responseBody);
                String errorMessage = errorResponse.optString("error", "Registration failed");
                return new ApiResponse(false, errorMessage, null);
            }
            
        } catch (java.net.UnknownHostException e) {
            Log.e(TAG, "Register error: Unable to resolve host - " + e.getMessage(), e);
            return new ApiResponse(false, "Unable to connect to server. Please check your internet connection.", null);
        } catch (java.net.SocketTimeoutException e) {
            Log.e(TAG, "Register error: Connection timed out - " + e.getMessage(), e);
            return new ApiResponse(false, "Connection timed out. The server may be busy. Please try again.", null);
        } catch (java.io.IOException e) {
            Log.e(TAG, "Register error: Network I/O problem - " + e.getMessage(), e);
            return new ApiResponse(false, "Network error. Please check your internet connection and try again.", null);
        } catch (Exception e) {
            Log.e(TAG, "Register error: Unexpected error - " + e.getMessage(), e);
            return new ApiResponse(false, "An unexpected error occurred during registration. Please try again.", null);
        }
    }
    
    /**
     * Safely parse credits from JSON object
     * Handles both numeric and string values, ensures always returns double
     * @param json JSON object containing credits field
     * @return Credits as double, or 0.0 if not found or invalid
     */
    private double parseCreditsSafely(JSONObject json) {
        try {
            // Try to get as double first (preferred)
            if (json.has("credits")) {
                Object creditsObj = json.get("credits");
                if (creditsObj instanceof Number) {
                    return ((Number) creditsObj).doubleValue();
                } else if (creditsObj instanceof String) {
                    // Handle string values like "100.00"
                    try {
                        return Double.parseDouble((String) creditsObj);
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "Failed to parse credits string: " + creditsObj);
                    }
                }
            }
            
            // Try balance field as fallback
            if (json.has("balance")) {
                Object balanceObj = json.get("balance");
                if (balanceObj instanceof Number) {
                    return ((Number) balanceObj).doubleValue();
                } else if (balanceObj instanceof String) {
                    try {
                        return Double.parseDouble((String) balanceObj);
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "Failed to parse balance string: " + balanceObj);
                    }
                }
            }
            
            // Try optDouble as last resort
            double credits = json.optDouble("credits", json.optDouble("balance", 0.0));
            Log.d(TAG, String.format("Parsed credits using optDouble: %.2f", credits));
            return credits;
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing credits: " + e.getMessage(), e);
            return 0.0;
        }
    }
}
