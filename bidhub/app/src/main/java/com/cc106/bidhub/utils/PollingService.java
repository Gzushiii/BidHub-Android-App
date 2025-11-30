package com.cc106.bidhub.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Reusable polling service utility for periodic API calls
 * Lifecycle-aware and thread-safe implementation
 */
public class PollingService {
    private static final String TAG = "PollingService";
    
    private Handler handler;
    private Runnable pollingRunnable;
    private long interval;
    private boolean isPolling = false;
    private boolean isPaused = false;
    private final Runnable action;
    private final String serviceName;
    
    /**
     * Create a new polling service instance
     * @param serviceName Name for logging/debugging purposes
     * @param intervalMs Polling interval in milliseconds
     * @param action Runnable to execute on each poll (runs on background thread)
     */
    public PollingService(String serviceName, long intervalMs, Runnable action) {
        this.serviceName = serviceName != null ? serviceName : "PollingService";
        this.interval = intervalMs;
        this.action = action;
        this.handler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * Start polling. If already polling, restarts with current interval.
     */
    public synchronized void startPolling() {
        if (isPolling && !isPaused) {
            Log.d(TAG, serviceName + ": Already polling, ignoring start request");
            return;
        }
        
        stopPolling(); // Clean up any existing polling
        
        isPolling = true;
        isPaused = false;
        
        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isPolling || isPaused) {
                    return;
                }
                
                // Execute action on background thread
                new Thread(() -> {
                    try {
                        if (action != null) {
                            action.run();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, serviceName + ": Error executing polling action", e);
                    }
                    
                    // Schedule next poll if still active
                    if (isPolling && !isPaused) {
                        handler.postDelayed(this, interval);
                    }
                }).start();
            }
        };
        
        // Start immediately, then continue with interval
        handler.post(pollingRunnable);
        Log.d(TAG, serviceName + ": Polling started with interval " + interval + "ms");
    }
    
    /**
     * Stop polling permanently. Must call startPolling() again to resume.
     */
    public synchronized void stopPolling() {
        if (!isPolling) {
            return;
        }
        
        isPolling = false;
        isPaused = false;
        
        if (pollingRunnable != null && handler != null) {
            handler.removeCallbacks(pollingRunnable);
            pollingRunnable = null;
        }
        
        Log.d(TAG, serviceName + ": Polling stopped");
    }
    
    /**
     * Pause polling temporarily. Can be resumed with resumePolling().
     */
    public synchronized void pausePolling() {
        if (!isPolling || isPaused) {
            return;
        }
        
        isPaused = true;
        Log.d(TAG, serviceName + ": Polling paused");
    }
    
    /**
     * Resume polling after being paused.
     */
    public synchronized void resumePolling() {
        if (!isPolling || !isPaused) {
            return;
        }
        
        isPaused = false;
        
        // Restart polling with current interval
        if (pollingRunnable != null) {
            handler.post(pollingRunnable);
        }
        
        Log.d(TAG, serviceName + ": Polling resumed");
    }
    
    /**
     * Update polling interval. Takes effect on next poll cycle.
     * @param newIntervalMs New interval in milliseconds
     */
    public synchronized void updateInterval(long newIntervalMs) {
        if (newIntervalMs <= 0) {
            Log.w(TAG, serviceName + ": Invalid interval " + newIntervalMs + "ms, ignoring");
            return;
        }
        
        this.interval = newIntervalMs;
        Log.d(TAG, serviceName + ": Polling interval updated to " + interval + "ms");
        
        // If currently polling, restart to apply new interval
        if (isPolling && !isPaused) {
            stopPolling();
            startPolling();
        }
    }
    
    /**
     * Check if polling is currently active
     * @return true if polling is active and not paused
     */
    public synchronized boolean isPolling() {
        return isPolling && !isPaused;
    }
    
    /**
     * Check if polling service is paused
     * @return true if paused
     */
    public synchronized boolean isPaused() {
        return isPaused;
    }
    
    /**
     * Clean up resources. Call this when the service is no longer needed.
     */
    public synchronized void destroy() {
        stopPolling();
        handler = null;
        Log.d(TAG, serviceName + ": Polling service destroyed");
    }
}

