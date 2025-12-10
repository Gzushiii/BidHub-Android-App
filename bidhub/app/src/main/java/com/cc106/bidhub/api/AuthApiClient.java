package com.cc106.bidhub.api;

import android.content.Context;
import android.util.Log;
import com.cc106.bidhub.utils.SharedPreferencesHelper;
import com.cc106.bidhub.utils.NetworkUtils;
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
     * Includes retry logic for timeout errors
     */
    public ApiResponse login(String email, String password) {
        return loginWithRetry(email, password, 3);
    }
    
    /**
     * Login with retry logic for handling timeouts
     * @param email User email
     * @param password User password
     * @param maxRetries Maximum number of retry attempts
     * @return ApiResponse with login result
     */
    private ApiResponse loginWithRetry(String email, String password, int maxRetries) {
        // Check network connectivity before attempting login
        if (!NetworkUtils.isNetworkAvailable(context)) {
            Log.w(TAG, "Login attempted without network connectivity");
            return new ApiResponse(false, 
                "No internet connection detected. Please check your Wi-Fi or mobile data and try again.", 
                null);
        }
        
        int retryCount = 0;
        long baseDelayMs = 1000; // Start with 1 second delay
        
        while (retryCount < maxRetries) {
            try {
                URL url = new URL(LOGIN_ENDPOINT);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                
                // Set request method and headers
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);
                connection.setInstanceFollowRedirects(false);
                
                // Render free tier can cold start and take ~50s; increase timeouts accordingly
                // Using longer timeouts to handle cold starts
                connection.setConnectTimeout(90000); // 90 seconds for connection
                connection.setReadTimeout(90000);    // 90 seconds for read
                
                // Enable connection reuse
                connection.setRequestProperty("Connection", "keep-alive");
                
                // Create request body
                JSONObject requestData = new JSONObject();
                requestData.put("email", email);
                requestData.put("password", password);
                
                // Debug logging
                Log.d(TAG, "Login request attempt " + (retryCount + 1) + " - Email: " + email);
                
                // Send request
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = requestData.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }
                
                // Get response code - this can also timeout, so it's in the retry loop
                // Wrap in try-catch to handle timeouts during response reading
                int responseCode;
                try {
                    responseCode = connection.getResponseCode();
                    Log.d(TAG, "Login response code: " + responseCode);
                } catch (java.net.SocketTimeoutException e) {
                    // Timeout while reading response - retry
                    retryCount++;
                    Log.w(TAG, "Login response read timeout attempt " + retryCount + "/" + maxRetries);
                    
                    if (retryCount >= maxRetries) {
                        Log.e(TAG, "Login failed after " + maxRetries + " timeout attempts (response read)");
                        return new ApiResponse(false, 
                            "Connection timed out while reading response. The server may be slow. Please try again.", 
                            null);
                    }
                    
                    // Exponential backoff
                    long delayMs = baseDelayMs * (1L << (retryCount - 1));
                    Log.d(TAG, "Retrying login in " + delayMs + "ms...");
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return new ApiResponse(false, "Login interrupted. Please try again.", null);
                    }
                    continue; // Retry the entire request
                }
                
                // If we get here, the request succeeded - process response
                return processLoginResponse(connection, email, responseCode);
                
            } catch (java.net.SocketTimeoutException e) {
                retryCount++;
                Log.w(TAG, "Login timeout attempt " + retryCount + "/" + maxRetries + ": " + e.getMessage());
                
                if (retryCount >= maxRetries) {
                    Log.e(TAG, "Login failed after " + maxRetries + " timeout attempts");
                    return new ApiResponse(false, 
                        "Connection timed out after multiple attempts. The server may be starting up. Please wait a moment and try again.", 
                        null);
                }
                
                // Exponential backoff: 1s, 2s, 4s
                long delayMs = baseDelayMs * (1L << (retryCount - 1));
                Log.d(TAG, "Retrying login in " + delayMs + "ms...");
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return new ApiResponse(false, "Login interrupted. Please try again.", null);
                }
                
            } catch (java.net.UnknownHostException e) {
                Log.e(TAG, "Login error: Unable to resolve host - " + e.getMessage(), e);
                String errorMessage = NetworkUtils.getNetworkErrorMessage(context, e);
                return new ApiResponse(false, errorMessage, null);
            } catch (java.io.IOException e) {
                // Check if it's a connection timeout wrapped in IOException
                String errorMsg = e.getMessage();
                if (errorMsg != null && (errorMsg.contains("timeout") || errorMsg.contains("timed out"))) {
                    retryCount++;
                    Log.w(TAG, "Login connection error (timeout) attempt " + retryCount + "/" + maxRetries);
                    
                    if (retryCount >= maxRetries) {
                        return new ApiResponse(false, 
                            "Connection timed out after multiple attempts. Please check your internet connection and try again.", 
                            null);
                    }
                    
                    long delayMs = baseDelayMs * (1L << (retryCount - 1));
                    try {
                        Thread.sleep(delayMs);
                        continue; // Retry the request
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return new ApiResponse(false, "Login interrupted. Please try again.", null);
                    }
                } else {
                    Log.e(TAG, "Login error: Network I/O problem - " + e.getMessage(), e);
                    return new ApiResponse(false, "Network error. Please check your internet connection and try again.", null);
                }
            } catch (Exception e) {
                Log.e(TAG, "Login error: Unexpected error - " + e.getMessage(), e);
                return new ApiResponse(false, "An unexpected error occurred during login. Please try again.", null);
            }
        }
        
        // Should never reach here, but just in case
        return new ApiResponse(false, "Login failed after multiple attempts. Please try again.", null);
    }
    
    /**
     * Process login response after successful connection
     */
    private ApiResponse processLoginResponse(HttpURLConnection connection, String email, int responseCode) {
        try {
            
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
                    // Use shorter timeout since server should be warmed up after login
                    try {
                        URL balUrl = new URL(BASE_URL + "/credits/balance");
                        HttpURLConnection balConn = (HttpURLConnection) balUrl.openConnection();
                        balConn.setRequestMethod("GET");
                        balConn.setRequestProperty("Authorization", "Bearer " + token);
                        balConn.setRequestProperty("Connection", "keep-alive");
                        balConn.setConnectTimeout(20000); // 20 seconds - server should be warm
                        balConn.setReadTimeout(20000);
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
        } catch (Exception e) {
            Log.e(TAG, "Error processing login response: " + e.getMessage(), e);
            return new ApiResponse(false, "Error processing server response. Please try again.", null);
        }
    }
    
    /**
     * Register new user
     */
    public ApiResponse register(String username, String email, String password, 
                              String phoneNumber, String firstName, String lastName, String alias) {
        // Check network connectivity before attempting registration
        if (!NetworkUtils.isNetworkAvailable(context)) {
            Log.w(TAG, "Register attempted without network connectivity");
            return new ApiResponse(false, 
                "No internet connection detected. Please check your Wi-Fi or mobile data and try again.", 
                null);
        }
        
        try {
            URL url = new URL(REGISTER_ENDPOINT);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // Set request method and headers
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);
            connection.setInstanceFollowRedirects(false);
            // Render free tier can cold start and take ~50s; increase timeouts accordingly
            connection.setConnectTimeout(90000); // 90 seconds for connection
            connection.setReadTimeout(90000);    // 90 seconds for read
            // Enable connection reuse
            connection.setRequestProperty("Connection", "keep-alive");
            
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
            String errorMessage = NetworkUtils.getNetworkErrorMessage(context, e);
            return new ApiResponse(false, errorMessage, null);
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
