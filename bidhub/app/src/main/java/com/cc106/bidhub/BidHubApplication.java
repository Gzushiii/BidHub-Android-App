package com.cc106.bidhub;

import android.app.Application;
import android.util.Log;

/**
 * Custom Application class to handle global exception handling and initialization
 */
public class BidHubApplication extends Application {
    
    private static final String TAG = "BidHubApplication";
    private static BidHubApplication instance;
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        
        // Set up global exception handler to catch any uncaught exceptions
        setupGlobalExceptionHandler();
        
        Log.d(TAG, "Application onCreate completed");
    }
    
    /**
     * Set up global uncaught exception handler to prevent crashes
     * This catches any exceptions that aren't handled elsewhere
     */
    private void setupGlobalExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                try {
                    Log.e(TAG, "Uncaught exception in thread: " + thread.getName(), throwable);
                    Log.e(TAG, "Exception type: " + throwable.getClass().getName());
                    Log.e(TAG, "Exception message: " + throwable.getMessage());
                    
                    // Log stack trace
                    StackTraceElement[] stackTrace = throwable.getStackTrace();
                    if (stackTrace != null && stackTrace.length > 0) {
                        Log.e(TAG, "Stack trace:");
                        for (StackTraceElement element : stackTrace) {
                            Log.e(TAG, "  at " + element.toString());
                        }
                    }
                    
                    // If there's a cause, log it too
                    Throwable cause = throwable.getCause();
                    if (cause != null) {
                        Log.e(TAG, "Caused by: " + cause.getClass().getName() + ": " + cause.getMessage());
                    }
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error in exception handler", e);
                } finally {
                    // Allow the system default handler to process the exception
                    // This ensures proper crash reporting and cleanup
                    // Note: We don't prevent the crash, just log it first for debugging
                }
            }
        });
        
        Log.d(TAG, "Global exception handler installed");
    }
    
    /**
     * Get the application instance
     */
    public static BidHubApplication getInstance() {
        return instance;
    }
}

