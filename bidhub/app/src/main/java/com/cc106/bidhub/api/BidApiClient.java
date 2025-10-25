package com.cc106.bidhub.api;

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * API client for bid-related operations
 */
public class BidApiClient {
    private static final String TAG = "BidApiClient";
    private static final String BASE_URL = "https://bidhub-android-app.onrender.com/api";
    
    /**
     * Place a bid on an item
     */
    public static ApiResponse placeBid(String authToken, String itemId, double amount) {
        // This method should be called from a background thread
        // The calling code (BiddingEngine) already runs in a background thread
        try {
            Log.i(TAG, "=== BID API CLIENT DEBUG ===");
            Log.i(TAG, "Placing bid: " + amount + " on item: " + itemId);
            Log.i(TAG, "Auth token: " + (authToken != null ? authToken.substring(0, Math.min(20, authToken.length())) + "..." : "NULL"));
            Log.i(TAG, "Base URL: " + BASE_URL);
            
            URL url = new URL(BASE_URL + "/bids/place");
            Log.i(TAG, "Full URL: " + url.toString());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + authToken);
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            
            // Set timeouts to handle Render cold starts
            connection.setConnectTimeout(60000); // 60 seconds
            connection.setReadTimeout(60000);    // 60 seconds
            
            // Create request body
            JSONObject requestData = new JSONObject();
            requestData.put("item_id", itemId);
            requestData.put("amount", amount);
            
            String requestBody = requestData.toString();
            Log.i(TAG, "Request body: " + requestBody);
            
            // Send request
            OutputStream os = connection.getOutputStream();
            os.write(requestBody.getBytes("UTF-8"));
            os.close();
            Log.i(TAG, "Request sent successfully");
            
            // Get response
            int responseCode = connection.getResponseCode();
            Log.i(TAG, "Response code: " + responseCode);
            
            BufferedReader reader;
            
            if (responseCode >= 200 && responseCode < 300) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                Log.i(TAG, "Reading from input stream (success)");
            } else {
                reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                Log.i(TAG, "Reading from error stream (error)");
            }
            
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            String responseBody = response.toString();
            Log.i(TAG, "Bid placement response: " + responseCode + " - " + responseBody);
            
            if (responseCode >= 200 && responseCode < 300) {
                JSONObject jsonResponse = new JSONObject(responseBody);
                return new ApiResponse(true, jsonResponse.optString("message", "Bid placed successfully"), jsonResponse);
            } else {
                JSONObject errorResponse = new JSONObject(responseBody);
                String errorMessage = errorResponse.optString("error", "Failed to place bid");
                return new ApiResponse(false, errorMessage, null);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error placing bid", e);
            String errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "Network error or server unavailable";
            } else {
                errorMessage = "Network error: " + errorMessage;
            }
            return new ApiResponse(false, errorMessage, null);
        }
    }
}

