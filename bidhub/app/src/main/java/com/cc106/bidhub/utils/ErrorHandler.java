package com.cc106.bidhub.utils;

import android.content.Context;
import android.view.View;
import android.widget.Toast;
import com.google.android.material.snackbar.Snackbar;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Centralized error handling utility for detailed logging and user feedback
 */
public class ErrorHandler {
    
    private static final String TAG = "ErrorHandler";
    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
    
    /**
     * Log error with detailed context
     * @param tag Log tag
     * @param message Error message
     * @param exception Exception object
     */
    public static void logError(String tag, String message, Throwable exception) {
        String timestamp = TIMESTAMP_FORMAT.format(new Date());
        String logMessage = String.format("[%s] %s", timestamp, message);
        
        if (exception != null) {
            android.util.Log.e(tag, logMessage, exception);
        } else {
            android.util.Log.e(tag, logMessage);
        }
    }
    
    /**
     * Log error with detailed context and additional data
     * @param tag Log tag
     * @param message Error message
     * @param exception Exception object
     * @param contextData Additional context data
     */
    public static void logError(String tag, String message, Throwable exception, String contextData) {
        String timestamp = TIMESTAMP_FORMAT.format(new Date());
        String logMessage = String.format("[%s] %s | Context: %s", timestamp, message, contextData);
        
        if (exception != null) {
            android.util.Log.e(tag, logMessage, exception);
        } else {
            android.util.Log.e(tag, logMessage);
        }
    }
    
    /**
     * Show detailed error to user with option to view details
     * @param context Android context
     * @param error Error message
     */
    public static void showDetailedError(Context context, String error) {
        if (context == null) {
            android.util.Log.w(TAG, "Context is null, cannot show error");
            return;
        }
        
        try {
            Toast.makeText(context, "❌ Error: " + error, Toast.LENGTH_LONG).show();
            android.util.Log.d(TAG, "Showed error to user: " + error);
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to show error to user", e);
        }
    }
    
    /**
     * Show detailed error with context data
     * @param context Android context
     * @param error Error message
     * @param contextData Additional context data
     */
    public static void showDetailedError(Context context, String error, String contextData) {
        if (context == null) {
            android.util.Log.w(TAG, "Context is null, cannot show error");
            return;
        }
        
        try {
            String fullError = String.format("%s | Context: %s", error, contextData);
            Toast.makeText(context, "❌ Error: " + fullError, Toast.LENGTH_LONG).show();
            android.util.Log.d(TAG, "Showed detailed error to user: " + fullError);
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to show detailed error to user", e);
        }
    }
    
    /**
     * Handle network-related errors
     * @param context Android context
     * @param operation Operation being performed
     * @param exception Network exception
     * @param requestData Request data (URL, parameters, etc.)
     */
    public static void handleNetworkError(Context context, String operation, Throwable exception, String requestData) {
        String errorMessage = String.format("🌐 Network error during %s", operation);
        String contextData = String.format("Request: %s | Exception: %s", 
            requestData != null ? requestData : "N/A", 
            exception != null ? exception.getMessage() : "Unknown");
        
        logError(TAG, errorMessage, exception, contextData);
        showDetailedError(context, "🌐 Network error occurred", operation);
    }
    
    /**
     * Handle database-related errors
     * @param context Android context
     * @param operation Database operation being performed
     * @param exception Database exception
     * @param queryData Query or table information
     */
    public static void handleDatabaseError(Context context, String operation, Throwable exception, String queryData) {
        String errorMessage = String.format("💾 Database error during %s", operation);
        String contextData = String.format("Query: %s | Exception: %s", 
            queryData != null ? queryData : "N/A", 
            exception != null ? exception.getMessage() : "Unknown");
        
        logError(TAG, errorMessage, exception, contextData);
        showDetailedError(context, "💾 Database error occurred", operation);
    }
    
    /**
     * Handle image loading errors
     * @param context Android context
     * @param imagePath Path to the image that failed to load
     * @param exception Image loading exception
     * @param operation Image operation (load, resize, etc.)
     */
    public static void handleImageError(Context context, String imagePath, Throwable exception, String operation) {
        String errorMessage = String.format("🖼️ Image loading error during %s", operation);
        String contextData = String.format("ImagePath: %s | Exception: %s", 
            imagePath != null ? imagePath : "N/A", 
            exception != null ? exception.getMessage() : "Unknown");
        
        logError(TAG, errorMessage, exception, contextData);
        showDetailedError(context, "🖼️ Image loading failed", operation);
    }
    
    /**
     * Handle adapter-related errors
     * @param context Android context
     * @param adapterName Name of the adapter
     * @param operation Operation being performed (bind, click, etc.)
     * @param exception Adapter exception
     * @param itemData Item data being processed
     */
    public static void handleAdapterError(Context context, String adapterName, String operation, 
                                        Throwable exception, String itemData) {
        String errorMessage = String.format("📋 Adapter error in %s during %s", adapterName, operation);
        String contextData = String.format("ItemData: %s | Exception: %s", 
            itemData != null ? itemData : "N/A", 
            exception != null ? exception.getMessage() : "Unknown");
        
        logError(TAG, errorMessage, exception, contextData);
        showDetailedError(context, "📋 Display error occurred", operation);
    }
    
    /**
     * Handle API-related errors
     * @param context Android context
     * @param apiEndpoint API endpoint being called
     * @param responseCode HTTP response code
     * @param exception API exception
     * @param requestData Request parameters
     */
    public static void handleApiError(Context context, String apiEndpoint, int responseCode, 
                                    Throwable exception, String requestData) {
        String errorMessage = String.format("🔌 API error calling %s", apiEndpoint);
        String contextData = String.format("ResponseCode: %d | Request: %s | Exception: %s", 
            responseCode, 
            requestData != null ? requestData : "N/A", 
            exception != null ? exception.getMessage() : "Unknown");
        
        logError(TAG, errorMessage, exception, contextData);
        showDetailedError(context, "🔌 API call failed", apiEndpoint);
    }
    
    /**
     * Handle initialization errors
     * @param context Android context
     * @param componentName Name of the component being initialized
     * @param exception Initialization exception
     * @param initData Initialization data
     */
    public static void handleInitError(Context context, String componentName, Throwable exception, String initData) {
        String errorMessage = String.format("⚙️ Initialization error for %s", componentName);
        String contextData = String.format("InitData: %s | Exception: %s", 
            initData != null ? initData : "N/A", 
            exception != null ? exception.getMessage() : "Unknown");
        
        logError(TAG, errorMessage, exception, contextData);
        showDetailedError(context, "⚙️ Initialization failed", componentName);
    }
    
    /**
     * Log warning with context
     * @param tag Log tag
     * @param message Warning message
     * @param contextData Additional context data
     */
    public static void logWarning(String tag, String message, String contextData) {
        String timestamp = TIMESTAMP_FORMAT.format(new Date());
        String logMessage = String.format("[%s] ⚠️ WARNING: %s | Context: %s", timestamp, message, contextData);
        android.util.Log.w(tag, logMessage);
    }
    
    /**
     * Log info with context
     * @param tag Log tag
     * @param message Info message
     * @param contextData Additional context data
     */
    public static void logInfo(String tag, String message, String contextData) {
        String timestamp = TIMESTAMP_FORMAT.format(new Date());
        String logMessage = String.format("[%s] ℹ️ INFO: %s | Context: %s", timestamp, message, contextData);
        android.util.Log.i(tag, logMessage);
    }
    
    /**
     * Show success message with emoji
     * @param context Android context
     * @param message Success message
     */
    public static void showSuccess(Context context, String message) {
        if (context == null) {
            android.util.Log.w(TAG, "Context is null, cannot show success message");
            return;
        }
        
        try {
            Toast.makeText(context, "✅ " + message, Toast.LENGTH_SHORT).show();
            android.util.Log.d(TAG, "Showed success to user: " + message);
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to show success to user", e);
        }
    }
    
    /**
     * Show loading message with emoji
     * @param context Android context
     * @param message Loading message
     */
    public static void showLoading(Context context, String message) {
        if (context == null) {
            android.util.Log.w(TAG, "Context is null, cannot show loading message");
            return;
        }
        
        try {
            Toast.makeText(context, "⏳ " + message, Toast.LENGTH_SHORT).show();
            android.util.Log.d(TAG, "Showed loading to user: " + message);
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to show loading to user", e);
        }
    }
    
    /**
     * Show error with Snackbar and retry action
     * @param view Root view for Snackbar
     * @param error Error message
     * @param retryAction Action to perform on retry
     */
    public static void showErrorWithRetry(View view, String error, Runnable retryAction) {
        if (view == null) {
            android.util.Log.w(TAG, "View is null, cannot show Snackbar");
            return;
        }
        
        try {
            Snackbar snackbar = Snackbar.make(view, "❌ " + error, Snackbar.LENGTH_LONG);
            if (retryAction != null) {
                snackbar.setAction("Retry", v -> {
                    try {
                        retryAction.run();
                    } catch (Exception e) {
                        android.util.Log.e(TAG, "Error in retry action", e);
                    }
                });
                snackbar.setActionTextColor(android.graphics.Color.WHITE);
            }
            snackbar.show();
            android.util.Log.d(TAG, "Showed error Snackbar: " + error);
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to show error Snackbar", e);
        }
    }
    
    /**
     * Show success with Snackbar and action
     * @param view Root view for Snackbar
     * @param message Success message
     * @param actionText Text for action button
     * @param action Action to perform
     */
    public static void showSuccessWithAction(View view, String message, String actionText, Runnable action) {
        if (view == null) {
            android.util.Log.w(TAG, "View is null, cannot show Snackbar");
            return;
        }
        
        try {
            Snackbar snackbar = Snackbar.make(view, "✅ " + message, Snackbar.LENGTH_LONG);
            if (action != null && actionText != null) {
                snackbar.setAction(actionText, v -> {
                    try {
                        action.run();
                    } catch (Exception e) {
                        android.util.Log.e(TAG, "Error in action", e);
                    }
                });
                snackbar.setActionTextColor(android.graphics.Color.WHITE);
            }
            snackbar.show();
            android.util.Log.d(TAG, "Showed success Snackbar: " + message);
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to show success Snackbar", e);
        }
    }
    
    /**
     * Show info with Snackbar
     * @param view Root view for Snackbar
     * @param message Info message
     */
    public static void showInfoSnackbar(View view, String message) {
        if (view == null) {
            android.util.Log.w(TAG, "View is null, cannot show Snackbar");
            return;
        }
        
        try {
            Snackbar snackbar = Snackbar.make(view, "ℹ️ " + message, Snackbar.LENGTH_SHORT);
            snackbar.show();
            android.util.Log.d(TAG, "Showed info Snackbar: " + message);
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to show info Snackbar", e);
        }
    }
}
