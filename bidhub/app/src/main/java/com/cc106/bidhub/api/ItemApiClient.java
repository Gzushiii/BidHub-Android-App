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
    private static final String BASE_URL = "https://bidhub-android-app.onrender.com/api";
    private static final String ITEMS_ENDPOINT = BASE_URL + "/items";
    
    private Context context;
    private SharedPreferencesHelper prefsHelper;
    
    public ItemApiClient(Context context) {
        this.context = context;
        this.prefsHelper = new SharedPreferencesHelper(context);
    }
    
    /**
     * Handle network exceptions and return appropriate error message
     */
    private ApiResponse handleNetworkException(Exception e, String operation) {
        if (e instanceof java.net.UnknownHostException) {
            Log.e(TAG, operation + ": Unable to resolve host - " + e.getMessage(), e);
            return new ApiResponse(false, "Unable to connect to server. Please check your internet connection.", null);
        } else if (e instanceof java.net.SocketTimeoutException) {
            Log.e(TAG, operation + ": Connection timed out - " + e.getMessage(), e);
            return new ApiResponse(false, "Connection timed out. The server may be busy. Please try again.", null);
        } else if (e instanceof java.io.IOException) {
            Log.e(TAG, operation + ": Network I/O problem - " + e.getMessage(), e);
            return new ApiResponse(false, "Network error. Please check your internet connection and try again.", null);
        } else {
            Log.e(TAG, operation + ": Unexpected error - " + e.getMessage(), e);
            return new ApiResponse(false, "An unexpected error occurred. Please try again.", null);
        }
    }
    
    /**
     * Create a new item via backend API (synchronous version for backward compatibility)
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
            // Send description if available, otherwise send empty string (backend will handle null)
            String description = itemData.getDescription();
            if (description == null || description.trim().isEmpty()) {
                requestData.put("description", ""); // Send empty string instead of null
            } else {
                requestData.put("description", description);
            }
            // Convert category_id from string to integer using mapping
            try {
                Integer categoryIdInt = com.cc106.bidhub.utils.CategoryMapping.toBackendCategoryId(itemData.getCategoryId());
                if (categoryIdInt == null) {
                    Log.e(TAG, "No mapping found for category_id: " + itemData.getCategoryId());
                    Log.e(TAG, "Available categories: " + com.cc106.bidhub.utils.CategoryMapping.getAllCategoryIds());
                    return new ApiResponse(false, "Category not found in mapping. Please update CategoryMapping class.", null);
                }
                requestData.put("category_id", categoryIdInt);
            } catch (Exception e) {
                Log.e(TAG, "Error mapping category_id: " + itemData.getCategoryId(), e);
                return new ApiResponse(false, "Invalid category ID mapping", null);
            }
            requestData.put("starting_price", itemData.getStartingPrice());
            requestData.put("reserve_price", itemData.getStartingPrice());
            requestData.put("duration_days", 7);
            // Don't send seller_email - backend gets seller from authenticated token
            // Set status based on whether this is a draft or active item
            requestData.put("status", "active");
            
            // Add images if available - should already be URLs from upload
            if (itemData.getImagePaths() != null && !itemData.getImagePaths().isEmpty()) {
                JSONArray imagesArray = new JSONArray();
                for (String imageUrl : itemData.getImagePaths()) {
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        imagesArray.put(imageUrl);
                    }
                }
                if (imagesArray.length() > 0) {
                    requestData.put("images", imagesArray);
                }
            }
            
            // Note: metadata, item_condition, and buy_now_price are not in the backend validator schema
            // These fields are not currently supported by the backend API
            // If needed, they should be added to the backend validator first
            
            // Make API call
            URL url = new URL(ITEMS_ENDPOINT);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + authToken);
            connection.setDoOutput(true);
            connection.setConnectTimeout(60000);
            connection.setReadTimeout(60000);
            
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
                // Parse error response for better error messages
                String errorMessage = "API error: " + responseCode;
                try {
                    if (response.length() > 0) {
                        org.json.JSONObject errorJson = new org.json.JSONObject(response.toString());
                        if (errorJson.has("error")) {
                            errorMessage = errorJson.getString("error");
                        }
                        if (errorJson.has("details")) {
                            Object details = errorJson.get("details");
                            if (details instanceof org.json.JSONArray) {
                                org.json.JSONArray detailsArray = (org.json.JSONArray) details;
                                if (detailsArray.length() > 0) {
                                    errorMessage += ": " + detailsArray.getString(0);
                                }
                            } else if (details instanceof String) {
                                errorMessage += ": " + details;
                            }
                        } else if (errorJson.has("message")) {
                            errorMessage += ": " + errorJson.getString("message");
                        }
                    }
                } catch (org.json.JSONException e) {
                    Log.w(TAG, "Could not parse error response", e);
                }
                Log.e(TAG, "API error: " + responseCode + " - " + response.toString());
                Log.e(TAG, "Request data was: " + requestData.toString());
                return new ApiResponse(false, errorMessage, response.toString());
            }
            
        } catch (Exception e) {
            return handleNetworkException(e, "Error creating item via API");
        }
    }
    
    /**
     * Create a new item via backend API (asynchronous version to prevent NetworkOnMainThreadException)
     */
    public void createItemAsync(ItemData itemData, String sellerEmail, ItemCreationCallback callback) {
        Log.i(TAG, "Creating item via API (async): " + itemData.getTitle());
        
        // Run on background thread to prevent NetworkOnMainThreadException
        new Thread(() -> {
            ApiResponse response = createItem(itemData, sellerEmail);
            
            // Call callback on main thread
            if (callback != null) {
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> callback.onResult(response));
            }
        }).start();
    }
    
    /**
     * Callback interface for async item creation
     */
    public interface ItemCreationCallback {
        void onResult(ApiResponse response);
    }

    /**
     * Check if an item exists on the server
     * @param itemId Item ID to check
     * @return ApiResponse with success/failure and item details if found
     */
    public ApiResponse checkItemExists(String itemId) {
        try {
            String authToken = prefsHelper.getAuthToken();
            if (authToken == null || authToken.isEmpty()) {
                return new ApiResponse(false, "Authentication token not found", null);
            }
            
            URL url = new URL(ITEMS_ENDPOINT + "/" + itemId);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + authToken);
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            
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
                Log.i(TAG, "Item exists on server: " + itemId);
                return new ApiResponse(true, "Item found", response.toString());
            } else {
                Log.w(TAG, "Item not found on server: " + itemId + " - " + responseCode);
                return new ApiResponse(false, "Item not found", response.toString());
            }
            
        } catch (Exception e) {
            return handleNetworkException(e, "Error checking item existence: " + itemId);
        }
    }
    
    /**
     * Create a draft item via backend API (synchronous version for backward compatibility)
     */
    public ApiResponse createDraftItem(ItemData itemData, String sellerEmail) {
        Log.i(TAG, "Creating draft item via API: " + itemData.getTitle());
        
        try {
            // Get auth token
            String authToken = prefsHelper.getAuthToken();
            if (authToken == null || authToken.isEmpty()) {
                return new ApiResponse(false, "Authentication token not found", null);
            }
            
            // Prepare request data for draft
            JSONObject requestData = new JSONObject();
            requestData.put("title", itemData.getTitle());
            // Send description if available, otherwise send empty string (backend will handle null)
            String description = itemData.getDescription();
            if (description == null || description.trim().isEmpty()) {
                requestData.put("description", ""); // Send empty string instead of null
            } else {
                requestData.put("description", description);
            }
            // Convert category_id from string to integer using mapping
            try {
                Integer categoryIdInt = com.cc106.bidhub.utils.CategoryMapping.toBackendCategoryId(itemData.getCategoryId());
                if (categoryIdInt == null) {
                    Log.e(TAG, "No mapping found for category_id: " + itemData.getCategoryId());
                    Log.e(TAG, "Available categories: " + com.cc106.bidhub.utils.CategoryMapping.getAllCategoryIds());
                    return new ApiResponse(false, "Category not found in mapping. Please update CategoryMapping class.", null);
                }
                requestData.put("category_id", categoryIdInt);
            } catch (Exception e) {
                Log.e(TAG, "Error mapping category_id: " + itemData.getCategoryId(), e);
                return new ApiResponse(false, "Invalid category ID mapping", null);
            }
            requestData.put("starting_price", itemData.getStartingPrice());
            requestData.put("reserve_price", itemData.getStartingPrice());
            requestData.put("duration_days", 7);
            // Don't send seller_email - backend gets seller from authenticated token
            requestData.put("status", "draft"); // Set as draft
            
            // Add images if available - should already be URLs from upload
            if (itemData.getImagePaths() != null && !itemData.getImagePaths().isEmpty()) {
                JSONArray imagesArray = new JSONArray();
                for (String imageUrl : itemData.getImagePaths()) {
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        imagesArray.put(imageUrl);
                    }
                }
                if (imagesArray.length() > 0) {
                    requestData.put("images", imagesArray);
                }
            }
            
            // Note: metadata, item_condition, and buy_now_price are not in the backend validator schema
            // These fields are not currently supported by the backend API
            // If needed, they should be added to the backend validator first
            
            // Make API call
            URL url = new URL(ITEMS_ENDPOINT);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + authToken);
            connection.setDoOutput(true);
            connection.setConnectTimeout(60000);
            connection.setReadTimeout(60000);
            
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
                Log.i(TAG, "Draft item created successfully via API");
                Log.d(TAG, "API Response: " + response.toString());
                return new ApiResponse(true, "Draft item created successfully", response.toString());
            } else {
                Log.e(TAG, "API error: " + responseCode + " - " + response.toString());
                Log.e(TAG, "Request data was: " + requestData.toString());
                return new ApiResponse(false, "API error: " + responseCode + " - " + response.toString(), response.toString());
            }
            
        } catch (Exception e) {
            return handleNetworkException(e, "Error creating draft item via API");
        }
    }
    
    /**
     * Create a draft item via backend API (asynchronous version to prevent NetworkOnMainThreadException)
     */
    public void createDraftItemAsync(ItemData itemData, String sellerEmail, ItemCreationCallback callback) {
        Log.i(TAG, "Creating draft item via API (async): " + itemData.getTitle());
        
        // Run on background thread to prevent NetworkOnMainThreadException
        new Thread(() -> {
            ApiResponse response = createDraftItem(itemData, sellerEmail);
            
            // Call callback on main thread
            if (callback != null) {
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> callback.onResult(response));
            }
        }).start();
    }
    
    /**
     * Publish a draft item
     */
    public ApiResponse publishDraftItem(String itemId, int durationDays) {
        Log.i(TAG, "Publishing draft item: " + itemId);
        
        try {
            // Get auth token
            String authToken = prefsHelper.getAuthToken();
            if (authToken == null || authToken.isEmpty()) {
                return new ApiResponse(false, "Authentication token not found", null);
            }
            
            // Prepare request data
            JSONObject requestData = new JSONObject();
            requestData.put("duration_days", durationDays);
            
            // Make API call
            URL url = new URL(ITEMS_ENDPOINT + "/" + itemId + "/publish");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + authToken);
            connection.setDoOutput(true);
            connection.setConnectTimeout(60000);
            connection.setReadTimeout(60000);
            
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
                Log.i(TAG, "Draft item published successfully via API");
                return new ApiResponse(true, "Item published successfully", response.toString());
            } else {
                Log.e(TAG, "API error: " + responseCode + " - " + response.toString());
                return new ApiResponse(false, "API error: " + responseCode + " - " + response.toString(), response.toString());
            }
            
        } catch (Exception e) {
            return handleNetworkException(e, "Publishing draft item");
        }
    }
    
    /**
     * Get items from backend API with retry logic for 5xx errors
     */
    public ApiResponse getItems(String categoryId, String search, Double minPrice, Double maxPrice, String sellerEmail, int limit, int offset) {
        Log.i(TAG, "Getting items from API");

        // Retry configuration
        final int MAX_RETRIES = 2;
        final int[] RETRY_DELAYS_MS = {200, 500}; // Exponential backoff: 200ms, 500ms

        int attempt = 0;
        ApiResponse lastResponse = null;

        while (attempt <= MAX_RETRIES) {
            try {
                if (attempt > 0) {
                    // Wait before retry
                    int delayMs = RETRY_DELAYS_MS[attempt - 1];
                    Log.i(TAG, "Retrying API call (attempt " + (attempt + 1) + "/" + (MAX_RETRIES + 1) + ") after " + delayMs + "ms delay");
                    Thread.sleep(delayMs);
                }

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
                connection.setConnectTimeout(60000);
                connection.setReadTimeout(60000);

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
                    Log.i(TAG, "Items retrieved successfully from API" + (attempt > 0 ? " (after " + attempt + " retries)" : ""));
                    return new ApiResponse(true, "Items retrieved successfully", response.toString());
                } else if (responseCode >= 500 && responseCode < 600) {
                    // Server error - retry
                    Log.w(TAG, "Server error: " + responseCode + " - " + response.toString());
                    lastResponse = new ApiResponse(false, "API error: " + responseCode, response.toString());

                    if (attempt < MAX_RETRIES) {
                        // Will retry
                        attempt++;
                        continue;
                    } else {
                        // Max retries reached
                        Log.e(TAG, "Max retries reached for API call, returning error");
                        return lastResponse;
                    }
                } else {
                    // Client error (4xx) - don't retry
                    Log.e(TAG, "API error: " + responseCode + " - " + response.toString());
                    return new ApiResponse(false, "API error: " + responseCode, response.toString());
                }

            } catch (InterruptedException e) {
                Log.e(TAG, "Retry sleep interrupted", e);
                Thread.currentThread().interrupt();
                return new ApiResponse(false, "Network error: " + e.getMessage(), null);
            } catch (Exception e) {
                Log.e(TAG, "Error getting items from API (attempt " + (attempt + 1) + ")", e);
                lastResponse = handleNetworkException(e, "Getting items");

                if (attempt < MAX_RETRIES) {
                    // Retry on network errors too
                    attempt++;
                    continue;
                } else {
                    return lastResponse;
                }
            }
        }

        // Should not reach here, but return last response if we do
        return lastResponse != null ? lastResponse : new ApiResponse(false, "Unknown error", null);
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
