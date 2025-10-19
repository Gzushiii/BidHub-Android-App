package com.cc106.bidhub.api;

import android.content.Context;
import android.util.Log;
import com.cc106.bidhub.items.ItemData;
import com.cc106.bidhub.utils.SharedPreferencesHelper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * API Client for item-related backend operations
 */
public class ItemApiClient {
    private static final String TAG = "ItemApiClient";
    private static final String BASE_URL = "http://192.168.18.136:3000/api";
    private static final String ITEMS_ENDPOINT = BASE_URL + "/items";
    
    private Context context;
    private SharedPreferencesHelper prefsHelper;
    
    public ItemApiClient(Context context) {
        this.context = context;
        this.prefsHelper = new SharedPreferencesHelper(context);
    }
    
    /**
     * Create a new item via backend API
     */
    public ApiResponse createItem(ItemData itemData, String sellerEmail) {
        Log.i(TAG, "Creating item via API: " + itemData.getTitle());
        
        try {
            // Get auth token
            String authToken = prefsHelper.getAuthToken();
            if (authToken == null || authToken.isEmpty()) {
                return new ApiResponse(false, "Authentication token not found", null);
            }
            
            // Prepare request data
            JSONObject requestData = new JSONObject();
            requestData.put("title", itemData.getTitle());
            requestData.put("description", itemData.getDescription());
            requestData.put("category_id", itemData.getCategoryId());
            requestData.put("starting_price", itemData.getStartingPrice());
            requestData.put("reserve_price", itemData.getStartingPrice()); // Use starting price as reserve
            requestData.put("duration_days", 7); // Default 7 days
            requestData.put("seller_email", sellerEmail); // Add seller email for database
            
            // Add images if available
            if (itemData.getImagePaths() != null && !itemData.getImagePaths().isEmpty()) {
                JSONArray imagesArray = new JSONArray();
                for (String imagePath : itemData.getImagePaths()) {
                    imagesArray.put(imagePath);
                }
                requestData.put("images", imagesArray);
            }
            
            // Add metadata if available
            if (itemData.getMetadata() != null && !itemData.getMetadata().isEmpty()) {
                requestData.put("metadata", itemData.getMetadata());
            }
            
            // Add condition if available
            if (itemData.getCondition() != null && !itemData.getCondition().isEmpty()) {
                requestData.put("item_condition", itemData.getCondition());
            }
            
            // Add buy now price if available
            if (itemData.getBuyNowPrice() > 0) {
                requestData.put("buy_now_price", itemData.getBuyNowPrice());
            }
            
            // Make API call
            URL url = new URL(ITEMS_ENDPOINT);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + authToken);
            connection.setDoOutput(true);
            
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
            
            if (responseCode >= 200 && responseCode < 300) {
                Log.i(TAG, "Item created successfully via API");
                Log.d(TAG, "API Response: " + response.toString());
                return new ApiResponse(true, "Item created successfully", response.toString());
            } else {
                Log.e(TAG, "API error: " + responseCode + " - " + response.toString());
                Log.e(TAG, "Request data was: " + requestData.toString());
                return new ApiResponse(false, "API error: " + responseCode + " - " + response.toString(), response.toString());
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating item via API", e);
            return new ApiResponse(false, "Network error: " + e.getMessage(), null);
        }
    }
    
    /**
     * Get items from backend API
     */
    public ApiResponse getItems(String categoryId, String search, Double minPrice, Double maxPrice, String sellerEmail, int limit, int offset) {
        Log.i(TAG, "Getting items from API");
        
        try {
            // Build query parameters
            StringBuilder urlBuilder = new StringBuilder(ITEMS_ENDPOINT);
            urlBuilder.append("?limit=").append(limit).append("&offset=").append(offset);
            
            if (categoryId != null && !categoryId.isEmpty()) {
                urlBuilder.append("&category_id=").append(categoryId);
            }
            if (search != null && !search.isEmpty()) {
                urlBuilder.append("&search=").append(search);
            }
            if (minPrice != null) {
                urlBuilder.append("&min_price=").append(minPrice);
            }
            if (maxPrice != null) {
                urlBuilder.append("&max_price=").append(maxPrice);
            }
            if (sellerEmail != null && !sellerEmail.isEmpty()) {
                urlBuilder.append("&seller_email=").append(sellerEmail);
            }
            
            // Get auth token
            String authToken = prefsHelper.getAuthToken();
            if (authToken == null || authToken.isEmpty()) {
                return new ApiResponse(false, "Authentication token not found", null);
            }
            
            // Make API call
            URL url = new URL(urlBuilder.toString());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + authToken);
            
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
            
            if (responseCode >= 200 && responseCode < 300) {
                Log.i(TAG, "Items retrieved successfully from API");
                return new ApiResponse(true, "Items retrieved successfully", response.toString());
            } else {
                Log.e(TAG, "API error: " + responseCode + " - " + response.toString());
                return new ApiResponse(false, "API error: " + responseCode, response.toString());
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting items from API", e);
            return new ApiResponse(false, "Network error: " + e.getMessage(), null);
        }
    }
    
    /**
     * API Response wrapper class
     */
    public static class ApiResponse {
        private boolean success;
        private String message;
        private String data;
        
        public ApiResponse(boolean success, String message, String data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getData() { return data; }
    }
}
