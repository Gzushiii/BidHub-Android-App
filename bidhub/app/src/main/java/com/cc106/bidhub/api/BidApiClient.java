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
            connection.setInstanceFollowRedirects(false);
            
            // Set timeouts to handle Render cold starts (~50s cold start)
            connection.setConnectTimeout(90000); // 90 seconds for connection
            connection.setReadTimeout(90000);    // 90 seconds for read
            // Enable connection reuse
            connection.setRequestProperty("Connection", "keep-alive");
            
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
                // Parse error response with null safety
                JSONObject errorResponse = null;
                try {
                    if (responseBody != null && !responseBody.isEmpty()) {
                        errorResponse = new JSONObject(responseBody);
                    }
                } catch (org.json.JSONException e) {
                    Log.e(TAG, "Error parsing error response JSON: " + e.getMessage(), e);
                }
                
                String errorType = "bid_failed";
                String errorMessage = "Failed to place bid";
                
                if (errorResponse != null) {
                    errorType = errorResponse.optString("error", "bid_failed");
                    errorMessage = errorResponse.optString("message", "Failed to place bid");
                    String details = errorResponse.optString("details", "");
                    
                    // Check for database schema errors
                    if (errorMessage != null && (errorMessage.contains("Unknown column") || 
                        errorMessage.contains("current_bidder_id") || 
                        errorMessage.contains("field list"))) {
                        errorMessage = "Database schema error detected. The server database is missing required columns. This is a backend configuration issue that needs to be fixed on the server.";
                        Log.e(TAG, "Database schema error detected in bid placement: " + errorMessage);
                    }
                    // Provide more detailed error messages based on error type
                    else if ("bid_too_low".equals(errorType) || "bid_failed".equals(errorType) && 
                             (errorMessage.contains("must be higher") || errorMessage.contains("starting price"))) {
                        double requiredBid = errorResponse.optDouble("required_bid", 0);
                        double currentBid = errorResponse.optDouble("current_bid", 0);
                        double startingPrice = errorResponse.optDouble("starting_price", 0);
                        if (requiredBid > 0) {
                            errorMessage = String.format("Bid must be at least ₱%.2f. Current highest bid: ₱%.2f", 
                                requiredBid, currentBid > 0 ? currentBid : startingPrice);
                        }
                    } else if ("insufficient_credits".equals(errorType)) {
                        double required = errorResponse.optDouble("required", 0);
                        double available = errorResponse.optDouble("available", 0);
                        if (required > 0 && available >= 0) {
                            errorMessage = String.format("Insufficient credits. Required: ₱%.2f, Available: ₱%.2f", 
                                required, available);
                        }
                    } else if ("sql_error".equals(details) && errorMessage != null && 
                              (errorMessage.contains("Unknown column") || errorMessage.contains("field list"))) {
                        errorMessage = "Database schema error detected. The server database is missing required columns. This is a backend configuration issue that needs to be fixed on the server.";
                        Log.e(TAG, "SQL error detected in bid placement: " + errorMessage);
                    }
                }
                
                return new ApiResponse(false, errorMessage, errorResponse != null ? errorResponse.toString() : null);
            }
            
        } catch (java.net.UnknownHostException e) {
            Log.e(TAG, "Error placing bid: Unable to resolve host - " + e.getMessage(), e);
            return new ApiResponse(false, "Unable to connect to server. Please check your internet connection.", null);
        } catch (java.net.SocketTimeoutException e) {
            Log.e(TAG, "Error placing bid: Connection timed out - " + e.getMessage(), e);
            return new ApiResponse(false, "Connection timed out. The server may be busy. Please try again.", null);
        } catch (java.io.IOException e) {
            Log.e(TAG, "Error placing bid: Network I/O problem - " + e.getMessage(), e);
            return new ApiResponse(false, "Network error. Please check your internet connection and try again.", null);
        } catch (Exception e) {
            Log.e(TAG, "Error placing bid: Unexpected error - " + e.getMessage(), e);
            return new ApiResponse(false, "An unexpected error occurred. Please try again.", null);
        }
    }
    
    /**
     * Get user's bid history from backend
     */
    public static ApiResponse getBidHistory(String authToken, String status, int limit, int offset) {
        try {
            Log.i(TAG, "Fetching bid history - status: " + status + ", limit: " + limit + ", offset: " + offset);
            
            StringBuilder urlBuilder = new StringBuilder(BASE_URL + "/bids/history");
            boolean firstParam = true;
            
            if (status != null && !status.isEmpty()) {
                urlBuilder.append(firstParam ? "?" : "&").append("status=").append(status);
                firstParam = false;
            }
            if (limit > 0) {
                urlBuilder.append(firstParam ? "?" : "&").append("limit=").append(limit);
                firstParam = false;
            }
            if (offset > 0) {
                urlBuilder.append(firstParam ? "?" : "&").append("offset=").append(offset);
            }
            
            URL url = new URL(urlBuilder.toString());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + authToken);
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(60000);
            connection.setReadTimeout(60000);
            
            int responseCode = connection.getResponseCode();
            Log.i(TAG, "Bid history response code: " + responseCode);
            
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
            Log.i(TAG, "Bid history response: " + responseCode + " - " + responseBody);
            
            if (responseCode >= 200 && responseCode < 300) {
                JSONObject jsonResponse = new JSONObject(responseBody);
                return new ApiResponse(true, "Bid history fetched successfully", jsonResponse);
            } else {
                JSONObject errorResponse = new JSONObject(responseBody);
                String errorMessage = errorResponse.optString("message", "Failed to fetch bid history");
                return new ApiResponse(false, errorMessage, errorResponse);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error fetching bid history: " + e.getMessage(), e);
            return new ApiResponse(false, "An error occurred while fetching bid history: " + e.getMessage(), null);
        }
    }
    
    /**
     * Get bids for a specific item
     */
    public static ApiResponse getItemBids(String itemId, int limit, int offset) {
        try {
            Log.i(TAG, "Fetching bids for item: " + itemId);
            
            StringBuilder urlBuilder = new StringBuilder(BASE_URL + "/bids/item/" + itemId);
            boolean firstParam = true;
            
            if (limit > 0) {
                urlBuilder.append(firstParam ? "?" : "&").append("limit=").append(limit);
                firstParam = false;
            }
            if (offset > 0) {
                urlBuilder.append(firstParam ? "?" : "&").append("offset=").append(offset);
            }
            
            URL url = new URL(urlBuilder.toString());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(60000);
            connection.setReadTimeout(60000);
            
            int responseCode = connection.getResponseCode();
            Log.i(TAG, "Item bids response code: " + responseCode);
            
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
            Log.i(TAG, "Item bids response: " + responseCode + " - " + responseBody);
            
            if (responseCode >= 200 && responseCode < 300) {
                JSONObject jsonResponse = new JSONObject(responseBody);
                return new ApiResponse(true, "Item bids fetched successfully", jsonResponse);
            } else {
                JSONObject errorResponse = new JSONObject(responseBody);
                String errorMessage = errorResponse.optString("message", "Failed to fetch item bids");
                return new ApiResponse(false, errorMessage, errorResponse);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error fetching item bids: " + e.getMessage(), e);
            return new ApiResponse(false, "An error occurred while fetching item bids: " + e.getMessage(), null);
        }
    }
}

