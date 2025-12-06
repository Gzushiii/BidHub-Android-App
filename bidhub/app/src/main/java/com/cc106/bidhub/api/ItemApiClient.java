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
     * Validate if a string is a valid URI (for image URLs)
     * Backend uses Joi.uri() validator which requires proper URI format
     */
    private boolean isValidUri(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        try {
            // Basic URI validation: must have protocol://host format
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                return false;
            }
            
            // Try to parse as URI to ensure it's valid
            java.net.URI uri = new java.net.URI(url);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            
            // Must have http or https scheme and a host
            if ((scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) ||
                (host == null || host.isEmpty())) {
                return false;
            }
            
            // Additional check: URL should not contain spaces or invalid characters
            if (url.contains(" ") || url.length() > 500) { // Backend max is 500 chars
                return false;
            }
            
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Invalid URI format: " + url, e);
            return false;
        }
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
     * Create a new item via backend API (synchronous version with retry logic for 500 errors)
     */
    public ApiResponse createItem(ItemData itemData, String sellerEmail) {
        Log.i(TAG, "Creating item via API: " + itemData.getTitle());
        
        // Retry configuration for 500 errors (transient server errors)
        final int MAX_RETRIES = 2;
        final int[] RETRY_DELAYS_MS = {500, 1000}; // Exponential backoff: 500ms, 1000ms
        
        int attempt = 0;
        ApiResponse lastResponse = null;
        
        while (attempt <= MAX_RETRIES) {
            try {
                if (attempt > 0) {
                    // Wait before retry
                    int delayMs = RETRY_DELAYS_MS[attempt - 1];
                    Log.i(TAG, "Retrying createItem API call (attempt " + (attempt + 1) + "/" + (MAX_RETRIES + 1) + ") after " + delayMs + "ms delay");
                    Thread.sleep(delayMs);
                }
                
                // Get auth token
                String authToken = prefsHelper.getAuthToken();
                if (authToken == null || authToken.isEmpty()) {
                    return new ApiResponse(false, "Authentication token not found", null);
                }
                
                // FIX: Prepare request data with validation
                JSONObject requestData = new JSONObject();
                
                // Validate and set title (required, min 3 chars)
                String title = itemData.getTitle();
                if (title == null || title.trim().isEmpty()) {
                    Log.e(TAG, "Title is null or empty");
                    return new ApiResponse(false, "Title is required. Please enter a title.", null);
                }
                if (title.trim().length() < 3) {
                    Log.e(TAG, "Title is too short: " + title.length() + " characters");
                    return new ApiResponse(false, "Title must be at least 3 characters long.", null);
                }
                requestData.put("title", title.trim());
                
                // Send description if available, otherwise send empty string (backend will handle null)
                String description = itemData.getDescription();
                if (description == null || description.trim().isEmpty()) {
                    requestData.put("description", ""); // Send empty string instead of null
                } else {
                    // Backend validates: if description provided, must be at least 10 chars
                    if (description.trim().length() < 10) {
                        Log.w(TAG, "Description provided but too short (" + description.length() + " chars), sending empty string");
                        requestData.put("description", ""); // Send empty if too short
                    } else {
                        requestData.put("description", description.trim());
                    }
                }
                // FIX: Convert category_id from string to integer using mapping
                // CategoryMapping already has fallback to category 10 (Others) if mapping not found
                try {
                    String categoryId = itemData.getCategoryId();
                    if (categoryId == null || categoryId.isEmpty()) {
                        Log.e(TAG, "Category ID is null or empty");
                        return new ApiResponse(false, "Category is required. Please select a category.", null);
                    }
                    
                    Integer categoryIdInt = com.cc106.bidhub.utils.CategoryMapping.toBackendCategoryId(categoryId);
                    if (categoryIdInt == null) {
                        // CategoryMapping should never return null (has fallback), but handle it just in case
                        Log.w(TAG, "Category mapping returned null for: " + categoryId + ", using fallback category 10");
                        categoryIdInt = 10; // Fallback to "Others" category
                    }
                    requestData.put("category_id", categoryIdInt);
                    Log.d(TAG, "Mapped category: " + categoryId + " -> " + categoryIdInt);
                } catch (Exception e) {
                    Log.e(TAG, "Error mapping category_id: " + itemData.getCategoryId(), e);
                    return new ApiResponse(false, "Invalid category. Please select a valid category.", null);
                }
                
                // FIX: Validate starting price before sending
                double startingPrice = itemData.getStartingPrice();
                if (startingPrice < 0.01) {
                    Log.e(TAG, "Starting price is too low: " + startingPrice);
                    return new ApiResponse(false, "Starting price must be at least ₱0.01", null);
                }
                if (startingPrice > 999999.99) {
                    Log.e(TAG, "Starting price is too high: " + startingPrice);
                    return new ApiResponse(false, "Starting price cannot exceed ₱999,999.99", null);
                }
                // FIX: Ensure starting_price is sent as a number (not string)
                requestData.put("starting_price", startingPrice);
                requestData.put("reserve_price", startingPrice); // Reserve price defaults to starting price
                // FIX: duration_days is REQUIRED by backend - ensure it's always sent as integer
                requestData.put("duration_days", 7); // Default duration (backend requires 1-30, default 7)
                // Don't send seller_email - backend gets seller from authenticated token
                // Set status based on whether this is a draft or active item
                requestData.put("status", "active");
                
                // FIX: Add images if available - validate URLs as valid URIs (backend uses Joi.uri() validator)
                if (itemData.getImagePaths() != null && !itemData.getImagePaths().isEmpty()) {
                    JSONArray imagesArray = new JSONArray();
                    for (String imageUrl : itemData.getImagePaths()) {
                        if (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.equals("null")) {
                            // FIX: Validate URL as proper URI (backend Joi validator requires valid URI)
                            // Check for proper URI format: protocol://host/path
                            String trimmedUrl = imageUrl.trim();
                            if (isValidUri(trimmedUrl)) {
                                imagesArray.put(trimmedUrl);
                                Log.d(TAG, "Valid image URL added: " + trimmedUrl);
                            } else {
                                Log.w(TAG, "Skipping invalid image URL (not a valid URI): " + imageUrl);
                            }
                        }
                    }
                    if (imagesArray.length() > 0) {
                        // Backend allows max 10 images
                        if (imagesArray.length() > 10) {
                            Log.w(TAG, "Too many images (" + imagesArray.length() + "), limiting to 10");
                            JSONArray limitedArray = new JSONArray();
                            for (int i = 0; i < 10; i++) {
                                limitedArray.put(imagesArray.get(i));
                            }
                            requestData.put("images", limitedArray);
                        } else {
                            requestData.put("images", imagesArray);
                        }
                        Log.d(TAG, "Including " + imagesArray.length() + " image(s) in request");
                    } else {
                        Log.d(TAG, "No valid image URLs to include in request");
                    }
                } else {
                    Log.d(TAG, "No images provided for item");
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
                // Render free tier can cold start and take ~50s; increase timeouts accordingly
                connection.setConnectTimeout(90000); // 90 seconds for connection
                connection.setReadTimeout(90000);    // 90 seconds for read
                // Enable connection reuse
                connection.setRequestProperty("Connection", "keep-alive");
                
                // FIX: Validate and log request data before sending for debugging
                String requestJson = requestData.toString();
                
                // Validate JSON is properly formatted before sending
                try {
                    // Try to parse the JSON to ensure it's valid
                    new org.json.JSONObject(requestJson);
                    Log.d(TAG, "Request JSON validated successfully");
                } catch (org.json.JSONException e) {
                    Log.e(TAG, "Invalid JSON generated for request", e);
                    Log.e(TAG, "Request data: " + requestData.toString());
                    return new ApiResponse(false, "Internal error: Invalid request data format", null);
                }
                
                Log.d(TAG, "=== SENDING ITEM CREATION REQUEST ===");
                Log.d(TAG, "Request JSON: " + requestJson);
                Log.d(TAG, "Request URL: " + ITEMS_ENDPOINT);
                Log.d(TAG, "Request Fields:");
                Log.d(TAG, "  - title: " + requestData.optString("title", "null"));
                Log.d(TAG, "  - description: " + (requestData.has("description") ? requestData.optString("description", "empty") : "not set"));
                Log.d(TAG, "  - category_id: " + requestData.optInt("category_id", -1));
                Log.d(TAG, "  - starting_price: " + requestData.optDouble("starting_price", -1));
                Log.d(TAG, "  - reserve_price: " + requestData.optDouble("reserve_price", -1));
                Log.d(TAG, "  - duration_days: " + requestData.optInt("duration_days", -1));
                Log.d(TAG, "  - status: " + requestData.optString("status", "null"));
                Log.d(TAG, "  - images: " + (requestData.has("images") ? requestData.optJSONArray("images").length() + " image(s)" : "none"));
                Log.d(TAG, "=====================================");
                
                // Send request
                OutputStream os = connection.getOutputStream();
                os.write(requestJson.getBytes("UTF-8"));
                os.flush();
                os.close();
                
                // Get response
                int responseCode = connection.getResponseCode();
                
                // Log response headers for debugging
                Log.d(TAG, "Response Code: " + responseCode);
                Log.d(TAG, "Response Message: " + connection.getResponseMessage());
                
                // Read response body (success or error)
                StringBuilder response = new StringBuilder();
                BufferedReader reader = null;
                
                try {
                    if (responseCode >= 200 && responseCode < 300) {
                        reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                    } else {
                        // Try to read error stream, but handle case where it might be null
                        java.io.InputStream errorStream = connection.getErrorStream();
                        if (errorStream != null) {
                            reader = new BufferedReader(new InputStreamReader(errorStream, "UTF-8"));
                        } else {
                            // If error stream is null, try reading from input stream (some servers send errors there)
                            try {
                                reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                            } catch (Exception e) {
                                Log.w(TAG, "Could not read error stream or input stream", e);
                                // Use empty response
                            }
                        }
                    }
                    
                    if (reader != null) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error reading response body", e);
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (Exception closeEx) {
                            // Ignore
                        }
                    }
                }
                
                String responseBody = response.toString();
                if (responseBody.isEmpty()) {
                    Log.w(TAG, "Empty response body received");
                }
            
            if (responseCode >= 200 && responseCode < 300) {
                Log.i(TAG, "Item created successfully via API" + (attempt > 0 ? " (after " + attempt + " retries)" : ""));
                Log.d(TAG, "API Response: " + responseBody);
                return new ApiResponse(true, "Item created successfully", responseBody);
            } else if (responseCode >= 500 && responseCode < 600) {
                // Server error (500-599) - retry
                String fullErrorDetails = responseBody;
                
                // Enhanced logging for server errors
                Log.e(TAG, "=== SERVER ERROR (500) - ATTEMPT " + (attempt + 1) + "/" + (MAX_RETRIES + 1) + " ===");
                Log.e(TAG, "HTTP Status Code: " + responseCode);
                Log.e(TAG, "Response Message: " + connection.getResponseMessage());
                Log.e(TAG, "Error Response Body: " + fullErrorDetails);
                Log.e(TAG, "Request JSON: " + requestJson);
                Log.e(TAG, "Request URL: " + ITEMS_ENDPOINT);
                Log.e(TAG, "==========================================");
                
                lastResponse = new ApiResponse(false, "Server error: " + responseCode, fullErrorDetails);
                
                if (attempt < MAX_RETRIES) {
                    // Will retry
                    attempt++;
                    continue;
                } else {
                    // Max retries reached - parse error for better message
                    String errorMessage = parseServerError(fullErrorDetails, responseCode);
                    Log.e(TAG, "=== MAX RETRIES REACHED - FINAL ERROR ===");
                    Log.e(TAG, "Final Error Message: " + errorMessage);
                    Log.e(TAG, "All " + (MAX_RETRIES + 1) + " attempts failed with 500 error");
                    Log.e(TAG, "This indicates a persistent backend server issue");
                    Log.e(TAG, "==========================================");
                    return new ApiResponse(false, errorMessage, fullErrorDetails);
                }
            } else {
                // Client errors (4xx) - don't retry, parse error immediately
                String fullErrorDetails = responseBody;
                String errorMessage = parseClientError(fullErrorDetails, responseCode);
                
                // Enhanced logging for debugging
                Log.e(TAG, "=== ITEM POSTING FAILED (CLIENT ERROR) ===");
                Log.e(TAG, "HTTP Status Code: " + responseCode);
                Log.e(TAG, "Response Message: " + connection.getResponseMessage());
                Log.e(TAG, "Error Message: " + errorMessage);
                Log.e(TAG, "Full Error Response: " + fullErrorDetails);
                Log.e(TAG, "Request Data: " + requestData.toString());
                Log.e(TAG, "Request URL: " + ITEMS_ENDPOINT);
                Log.e(TAG, "==========================================");
                
                return new ApiResponse(false, errorMessage, fullErrorDetails);
            }
            
            } catch (InterruptedException e) {
                Log.e(TAG, "Retry sleep interrupted", e);
                Thread.currentThread().interrupt();
                return new ApiResponse(false, "Network error: " + e.getMessage(), null);
            } catch (Exception e) {
                Log.e(TAG, "Error creating item via API (attempt " + (attempt + 1) + "): " + itemData.getTitle(), e);
                lastResponse = handleNetworkException(e, "Error creating item via API");
                
                // Retry on network errors too
                if (attempt < MAX_RETRIES) {
                    attempt++;
                    continue;
                } else {
                    return lastResponse;
                }
            }
        }
        
        // Should not reach here, but return last response if we do
        return lastResponse != null ? lastResponse : new ApiResponse(false, "Unknown error creating item", null);
    }
    
    /**
     * Parse server error (500-599) responses for better error messages
     */
    private String parseServerError(String errorResponse, int statusCode) {
        String errorMessage = "Server error occurred. Please try again.";
        
        try {
            if (errorResponse != null && errorResponse.length() > 0) {
                org.json.JSONObject errorJson = new org.json.JSONObject(errorResponse);
                
                // Check for detailed error message
                String rawError = null;
                if (errorJson.has("message")) {
                    rawError = errorJson.getString("message");
                } else if (errorJson.has("error")) {
                    rawError = errorJson.getString("error");
                }
                
                // Check for SQL/database errors in the error message or details
                String errorText = rawError != null ? rawError.toLowerCase() : "";
                String detailsText = "";
                if (errorJson.has("details")) {
                    Object details = errorJson.get("details");
                    if (details instanceof String) {
                        detailsText = ((String) details).toLowerCase();
                    }
                }
                
                // Detect database schema errors
                if (errorText.contains("unknown column") || detailsText.contains("unknown column") ||
                    errorText.contains("bad field") || detailsText.contains("bad field") ||
                    errorText.contains("sqlstate") || detailsText.contains("sqlstate") ||
                    errorText.contains("er_bad_field") || detailsText.contains("er_bad_field")) {
                    errorMessage = "Database schema error detected. The server database is missing required columns. " +
                                 "This is a backend configuration issue that needs to be fixed on the server.";
                    Log.e(TAG, "=== DATABASE SCHEMA ERROR DETECTED ===");
                    Log.e(TAG, "The backend database schema is missing required columns.");
                    Log.e(TAG, "Error details: " + errorResponse);
                    Log.e(TAG, "This requires a backend database migration to fix.");
                    Log.e(TAG, "=====================================");
                } else if (errorText.contains("failed to create item") || 
                          (rawError != null && rawError.equals("Failed to create item"))) {
                    // Generic "Failed to create item" - check if we can get more details
                    if (errorJson.has("details")) {
                        Object details = errorJson.get("details");
                        if (details instanceof String && !((String) details).isEmpty()) {
                            String detailsStr = (String) details;
                            // Check if details contain SQL errors
                            if (detailsStr.toLowerCase().contains("unknown column") ||
                                detailsStr.toLowerCase().contains("bad field")) {
                                errorMessage = "Database schema error: " + detailsStr;
                            } else {
                                errorMessage = "Server error while creating item: " + detailsStr;
                            }
                        } else {
                            errorMessage = "Server error while creating item. The server may be temporarily unavailable. Please try again in a moment.";
                        }
                    } else {
                        errorMessage = "Server error while creating item. The server may be temporarily unavailable. Please try again in a moment.";
                    }
                } else if (rawError != null) {
                    errorMessage = rawError;
                }
                
                // Add details if available and not already included
                if (errorJson.has("details") && !errorMessage.contains("Database schema")) {
                    Object details = errorJson.get("details");
                    if (details instanceof String && !((String) details).isEmpty()) {
                        String detailsStr = (String) details;
                        // Only append if it's not a SQL error (already handled above)
                        if (!detailsStr.toLowerCase().contains("unknown column") &&
                            !detailsStr.toLowerCase().contains("bad field")) {
                            errorMessage += ": " + detailsStr;
                        }
                    }
                }
            }
        } catch (org.json.JSONException e) {
            Log.w(TAG, "Could not parse server error response as JSON: " + errorResponse, e);
            // Check if error response contains SQL error keywords even if not JSON
            String errorLower = errorResponse.toLowerCase();
            if (errorLower.contains("unknown column") || errorLower.contains("bad field") ||
                errorLower.contains("sqlstate") || errorLower.contains("er_bad_field")) {
                errorMessage = "Database schema error detected. The server database is missing required columns. " +
                             "This is a backend configuration issue that needs to be fixed on the server.";
                Log.e(TAG, "=== DATABASE SCHEMA ERROR DETECTED (non-JSON response) ===");
                Log.e(TAG, "Error response: " + errorResponse);
                Log.e(TAG, "=========================================================");
            }
        }
        
        return errorMessage;
    }
    
    /**
     * Parse client error (400-499) responses for better error messages
     */
    private String parseClientError(String errorResponse, int statusCode) {
        String errorMessage = "Failed to post item";
        
        try {
            if (errorResponse != null && errorResponse.length() > 0) {
                org.json.JSONObject errorJson = new org.json.JSONObject(errorResponse);
                
                // Prioritize message field for user-friendly errors
                if (errorJson.has("message")) {
                    errorMessage = errorJson.getString("message");
                } else if (errorJson.has("error")) {
                    errorMessage = errorJson.getString("error");
                }
                
                // Add validation details if available
                if (errorJson.has("details")) {
                    Object details = errorJson.get("details");
                    StringBuilder detailsBuilder = new StringBuilder();
                    
                    if (details instanceof org.json.JSONArray) {
                        org.json.JSONArray detailsArray = (org.json.JSONArray) details;
                        for (int i = 0; i < detailsArray.length(); i++) {
                            if (i > 0) detailsBuilder.append("; ");
                            detailsBuilder.append(detailsArray.getString(i));
                        }
                    } else if (details instanceof String) {
                        detailsBuilder.append((String) details);
                    }
                    
                    String detailsStr = detailsBuilder.toString();
                    if (!detailsStr.isEmpty()) {
                        errorMessage += ": " + detailsStr;
                    }
                }
            } else {
                // No response body - use HTTP status code message
                switch (statusCode) {
                    case 400:
                        errorMessage = "Invalid request. Please check your input fields.";
                        break;
                    case 401:
                        errorMessage = "Authentication failed. Please log in again.";
                        break;
                    case 403:
                        errorMessage = "You don't have permission to perform this action.";
                        break;
                    case 404:
                        errorMessage = "Server endpoint not found. Please try again later.";
                        break;
                    default:
                        errorMessage = "Failed to post item (HTTP " + statusCode + ")";
                }
            }
        } catch (org.json.JSONException e) {
            Log.w(TAG, "Could not parse error response as JSON: " + errorResponse, e);
            // If response is not JSON, use it as-is if it's not empty
            if (errorResponse != null && !errorResponse.trim().isEmpty()) {
                errorMessage = errorResponse;
            }
        }
        
        return errorMessage;
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
            connection.setInstanceFollowRedirects(false);
            // Render free tier can cold start and take ~50s; increase timeouts accordingly
            connection.setConnectTimeout(90000); // 90 seconds for connection
            connection.setReadTimeout(90000);    // 90 seconds for read
            // Enable connection reuse
            connection.setRequestProperty("Connection", "keep-alive");
            
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
            
            if (responseCode >= 200 && responseCode < 300) {
                // Parse JSON response to verify item actually exists
                try {
                    org.json.JSONObject jsonResponse = new org.json.JSONObject(responseBody);
                    
                    // Backend returns: { success: true, item: {...}, correlationId: ... }
                    // OR direct item object: { id: ..., title: ..., ... }
                    boolean hasItem = jsonResponse.has("item") || jsonResponse.has("id") || jsonResponse.has("uuid_id");
                    boolean hasSuccess = jsonResponse.has("success") && jsonResponse.getBoolean("success");
                    
                    if (hasItem || hasSuccess) {
                        Log.i(TAG, "Item exists on server: " + itemId);
                        return new ApiResponse(true, "Item found", responseBody);
                    } else {
                        Log.w(TAG, "Item response missing item data: " + itemId);
                        return new ApiResponse(false, "Item data not found in response", responseBody);
                    }
                } catch (org.json.JSONException e) {
                    Log.e(TAG, "Error parsing item existence check response", e);
                    // If we can't parse JSON but got 200, assume item exists
                    Log.i(TAG, "Item exists on server (unparseable response): " + itemId);
                    return new ApiResponse(true, "Item found", responseBody);
                }
            } else {
                // Parse error response for better error messages
                String errorMessage = "Item not found";
                try {
                    if (responseBody != null && !responseBody.isEmpty()) {
                        org.json.JSONObject errorJson = new org.json.JSONObject(responseBody);
                        errorMessage = errorJson.optString("message", errorJson.optString("error", "Item not found"));
                    }
                } catch (org.json.JSONException e) {
                    Log.w(TAG, "Could not parse error response", e);
                }
                
                Log.w(TAG, "Item not found on server: " + itemId + " - " + responseCode + " - " + errorMessage);
                return new ApiResponse(false, errorMessage, responseBody);
            }
            
        } catch (Exception e) {
            return handleNetworkException(e, "Error checking item existence: " + itemId);
        }
    }
    
    /**
     * Get a single item by ID with full details from the backend API
     * This method uses the proper endpoint /items/{itemId} with retry logic for 500 errors
     * @param itemId Item ID to fetch
     * @return ApiResponse with success/failure and full item details if found
     */
    public ApiResponse getItemById(String itemId) {
        Log.i(TAG, "Fetching item by ID from API: " + itemId);
        
        // Retry configuration for 500 errors
        final int MAX_RETRIES = 2;
        final int[] RETRY_DELAYS_MS = {200, 500}; // Exponential backoff: 200ms, 500ms
        
        int attempt = 0;
        ApiResponse lastResponse = null;
        
        while (attempt <= MAX_RETRIES) {
            try {
                if (attempt > 0) {
                    // Wait before retry
                    int delayMs = RETRY_DELAYS_MS[attempt - 1];
                    Log.i(TAG, "Retrying getItemById API call (attempt " + (attempt + 1) + "/" + (MAX_RETRIES + 1) + ") after " + delayMs + "ms delay");
                    Thread.sleep(delayMs);
                }
                
                String authToken = prefsHelper.getAuthToken();
                if (authToken == null || authToken.isEmpty()) {
                    return new ApiResponse(false, "Authentication token not found", null);
                }
                
                // FIX: Use proper endpoint /items/{itemId} instead of fetching all items
                // Add cache-busting parameter for real-time updates
                String urlString = ITEMS_ENDPOINT + "/" + itemId + "?_t=" + System.currentTimeMillis();
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Authorization", "Bearer " + authToken);
                // FIX: Add cache-control headers to ensure fresh data for real-time updates
                connection.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
                connection.setRequestProperty("Pragma", "no-cache");
                connection.setRequestProperty("Expires", "0");
                connection.setInstanceFollowRedirects(false);
                // Render free tier can cold start and take ~50s; increase timeouts accordingly
                connection.setConnectTimeout(90000); // 90 seconds for connection
                connection.setReadTimeout(90000);    // 90 seconds for read
                // Enable connection reuse
                connection.setRequestProperty("Connection", "keep-alive");
                
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
                    // Success - parse the item response
                    Log.i(TAG, "Item fetched successfully from API: " + itemId + (attempt > 0 ? " (after " + attempt + " retries)" : ""));
                    try {
                        // Response might be direct item object or wrapped in {item: {...}}
                        org.json.JSONObject jsonResponse = new org.json.JSONObject(response.toString());
                        org.json.JSONObject itemJson;
                        
                        if (jsonResponse.has("item")) {
                            itemJson = jsonResponse.getJSONObject("item");
                        } else if (jsonResponse.has("id") || jsonResponse.has("uuid_id")) {
                            // Response is the item object itself
                            itemJson = jsonResponse;
                        } else {
                            Log.e(TAG, "Unexpected response format: " + response.toString());
                            return new ApiResponse(false, "Unexpected response format from API", response.toString());
                        }
                        
                        // Wrap in standard format for consistency
                        org.json.JSONObject itemResponse = new org.json.JSONObject();
                        itemResponse.put("item", itemJson);
                        return new ApiResponse(true, "Item found", itemResponse.toString());
                    } catch (org.json.JSONException e) {
                        Log.e(TAG, "Error parsing API response", e);
                        return new ApiResponse(false, "Error parsing API response: " + e.getMessage(), response.toString());
                    }
                } else if (responseCode >= 500 && responseCode < 600) {
                    // Server error (500-599) - retry
                    Log.w(TAG, "Server error fetching item: " + responseCode + " - " + response.toString());
                    lastResponse = new ApiResponse(false, "Server error: " + responseCode, response.toString());
                    
                    if (attempt < MAX_RETRIES) {
                        // Will retry
                        attempt++;
                        continue;
                    } else {
                        // Max retries reached
                        Log.e(TAG, "Max retries reached for getItemById, returning error");
                        return lastResponse;
                    }
                } else if (responseCode == 404) {
                    // Item not found - don't retry
                    Log.w(TAG, "Item not found on server: " + itemId + " - 404");
                    return new ApiResponse(false, "Item not found", response.toString());
                } else {
                    // Other client errors (4xx) - don't retry
                    Log.w(TAG, "API error fetching item: " + responseCode + " - " + response.toString());
                    return new ApiResponse(false, "API error: " + responseCode, response.toString());
                }
                
            } catch (InterruptedException e) {
                Log.e(TAG, "Retry sleep interrupted", e);
                Thread.currentThread().interrupt();
                return new ApiResponse(false, "Network error: " + e.getMessage(), null);
            } catch (Exception e) {
                Log.e(TAG, "Error fetching item by ID (attempt " + (attempt + 1) + "): " + itemId, e);
                lastResponse = handleNetworkException(e, "Error fetching item by ID: " + itemId);
                
                // Retry on network errors too
                if (attempt < MAX_RETRIES) {
                    attempt++;
                    continue;
                } else {
                    return lastResponse;
                }
            }
        }
        
        // Should not reach here, but return last response if we do
        return lastResponse != null ? lastResponse : new ApiResponse(false, "Unknown error fetching item", null);
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
            connection.setInstanceFollowRedirects(false);
            // Render free tier can cold start and take ~50s; increase timeouts accordingly
            connection.setConnectTimeout(90000); // 90 seconds for connection
            connection.setReadTimeout(90000);    // 90 seconds for read
            // Enable connection reuse
            connection.setRequestProperty("Connection", "keep-alive");
            
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
            connection.setInstanceFollowRedirects(false);
            // Render free tier can cold start and take ~50s; increase timeouts accordingly
            connection.setConnectTimeout(90000); // 90 seconds for connection
            connection.setReadTimeout(90000);    // 90 seconds for read
            // Enable connection reuse
            connection.setRequestProperty("Connection", "keep-alive");
            
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
        return getItems(categoryId, search, minPrice, maxPrice, sellerEmail, limit, offset, false);
    }
    
    /**
     * Get items from backend API with retry logic for 5xx errors
     * @param forceRefresh If true, adds cache-busting parameter to ensure fresh data
     */
    public ApiResponse getItems(String categoryId, String search, Double minPrice, Double maxPrice, String sellerEmail, int limit, int offset, boolean forceRefresh) {
        Log.i(TAG, "Getting items from API" + (forceRefresh ? " (FORCE REFRESH)" : ""));

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
                
                // FIX: Add cache-busting parameter to ensure fresh data when forceRefresh is true
                // This prevents browsers/proxies from returning cached responses
                if (forceRefresh) {
                    urlBuilder.append("&_t=").append(System.currentTimeMillis());
                    Log.d(TAG, "Added cache-busting parameter for force refresh");
                }

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
                // FIX: Add cache-control headers to ensure fresh data for real-time updates
                if (forceRefresh) {
                    connection.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
                    connection.setRequestProperty("Pragma", "no-cache");
                    connection.setRequestProperty("Expires", "0");
                    Log.d(TAG, "Added cache-control headers for force refresh");
                }
                // Render free tier can cold start and take ~50s; increase timeouts accordingly
                connection.setConnectTimeout(90000); // 90 seconds for connection
                connection.setReadTimeout(90000);    // 90 seconds for read
                // Enable connection reuse
                connection.setRequestProperty("Connection", "keep-alive");

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
