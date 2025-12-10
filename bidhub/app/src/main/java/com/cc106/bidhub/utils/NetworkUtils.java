package com.cc106.bidhub.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

/**
 * Utility class for network connectivity checks
 */
public class NetworkUtils {
    private static final String TAG = "NetworkUtils";
    
    /**
     * Check if device has active internet connection
     * @param context Application context
     * @return true if connected to internet, false otherwise
     */
    public static boolean isNetworkAvailable(Context context) {
        if (context == null) {
            return false;
        }
        
        ConnectivityManager connectivityManager = 
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager == null) {
            return false;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) {
                return false;
            }
            
            NetworkCapabilities capabilities = 
                connectivityManager.getNetworkCapabilities(network);
            
            if (capabilities == null) {
                return false;
            }
            
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                   capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        } else {
            // For older Android versions
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
    }
    
    /**
     * Get a user-friendly error message based on network status
     * @param context Application context
     * @param exception The exception that occurred
     * @return User-friendly error message
     */
    public static String getNetworkErrorMessage(Context context, Exception exception) {
        if (!isNetworkAvailable(context)) {
            return "No internet connection detected. Please check your Wi-Fi or mobile data and try again.";
        }
        
        if (exception instanceof java.net.UnknownHostException) {
            String message = exception.getMessage();
            if (message != null && message.contains("Unable to resolve host")) {
                return "Unable to reach the server. This could be due to:\n" +
                       "• No internet connection\n" +
                       "• DNS server issues\n" +
                       "• Server may be temporarily unavailable\n\n" +
                       "Please check your internet connection and try again.";
            }
            return "Unable to connect to server. Please check your internet connection.";
        }
        
        if (exception instanceof java.net.SocketTimeoutException) {
            return "Connection timed out. The server may be busy or starting up. Please try again in a moment.";
        }
        
        if (exception instanceof java.io.IOException) {
            String message = exception.getMessage();
            if (message != null && (message.contains("timeout") || message.contains("timed out"))) {
                return "Connection timed out. Please check your internet connection and try again.";
            }
            return "Network error occurred. Please check your internet connection and try again.";
        }
        
        return "Network error. Please check your internet connection and try again.";
    }
}

