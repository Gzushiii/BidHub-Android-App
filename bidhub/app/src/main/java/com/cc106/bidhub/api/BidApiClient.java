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
        try {
            Log.i(TAG, "Placing bid: " + amount + " on item: " + itemId);
            
            URL url = new URL(BASE_URL + "/bids/place");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + authToken);
            connection.setDoOutput(true);
            
            // Create request body
            JSONObject requestData = new JSONObject();
            requestData.put("item_id", itemId);
            requestData.put("amount", amount);
            
            // Send request
            OutputStream os = connection.getOutputStream();
            os.write(requestData.toString().getBytes("UTF-8"));
            os.close();
            
            // Get response
            int responseCode = connection.getResponseCode();
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
            return new ApiResponse(false, "Network error: " + e.getMessage(), null);
        }
    }
}
