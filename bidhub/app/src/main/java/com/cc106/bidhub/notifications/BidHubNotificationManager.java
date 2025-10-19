package com.cc106.bidhub.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.cc106.bidhub.MainActivity;
import com.cc106.bidhub.R;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Comprehensive Notification Management System
 * Handles push notifications, local notifications, and notification preferences
 */
public class BidHubNotificationManager {
    private static final String TAG = "NotificationManager";
    private static BidHubNotificationManager instance;
    
    // Notification Channels
    private static final String CHANNEL_BID_UPDATES = "bid_updates";
    private static final String CHANNEL_AUCTION_ALERTS = "auction_alerts";
    private static final String CHANNEL_PAYMENT_NOTIFICATIONS = "payment_notifications";
    private static final String CHANNEL_SYSTEM_NOTIFICATIONS = "system_notifications";
    
    // Notification IDs
    private static final int NOTIFICATION_ID_BID_UPDATE = 1000;
    private static final int NOTIFICATION_ID_AUCTION_ENDING = 2000;
    private static final int NOTIFICATION_ID_PAYMENT_SUCCESS = 3000;
    private static final int NOTIFICATION_ID_SYSTEM = 4000;
    
    // Threading
    private final ExecutorService executorService;
    
    // Storage
    private final Map<String, NotificationPreferences> userPreferences;
    private final Map<String, List<NotificationHistory>> notificationHistory;
    
    // Context
    private Context context;
    private android.app.NotificationManager systemNotificationManager;
    
    private BidHubNotificationManager(Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newCachedThreadPool();
        this.userPreferences = new ConcurrentHashMap<>();
        this.notificationHistory = new ConcurrentHashMap<>();
        this.systemNotificationManager = (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        initializeNotificationChannels();
    }
    
    public static synchronized BidHubNotificationManager getInstance(Context context) {
        if (instance == null) {
            instance = new BidHubNotificationManager(context);
        }
        return instance;
    }
    
    // ==================== INITIALIZATION ====================
    
    private void initializeNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(
                CHANNEL_BID_UPDATES,
                "Bid Updates",
                "Notifications for bid-related activities",
                android.app.NotificationManager.IMPORTANCE_HIGH
            );
            
            createNotificationChannel(
                CHANNEL_AUCTION_ALERTS,
                "Auction Alerts",
                "Notifications for auction deadlines and endings",
                android.app.NotificationManager.IMPORTANCE_HIGH
            );
            
            createNotificationChannel(
                CHANNEL_PAYMENT_NOTIFICATIONS,
                "Payment Notifications",
                "Notifications for payment confirmations and failures",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            );
            
            createNotificationChannel(
                CHANNEL_SYSTEM_NOTIFICATIONS,
                "System Notifications",
                "General app notifications and updates",
                android.app.NotificationManager.IMPORTANCE_LOW
            );
        }
    }
    
    private void createNotificationChannel(String channelId, String name, String description, int importance) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, name, importance);
            channel.setDescription(description);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setShowBadge(true);
            
            systemNotificationManager.createNotificationChannel(channel);
        }
    }
    
    // ==================== BID NOTIFICATIONS ====================
    
    /**
     * Send bid update notification
     */
    public void sendBidUpdateNotification(String userId, String itemTitle, double bidAmount, boolean isWinning) {
        if (!isNotificationEnabled(userId, NotificationType.BID_UPDATES)) {
            return;
        }
        
        String title = isWinning ? "You're Winning!" : "You've Been Outbid";
        String message = String.format("%s on %s - ₱%.2f", 
            isWinning ? "Winning bid" : "Outbid", itemTitle, bidAmount);
        
        sendNotification(
            userId,
            NOTIFICATION_ID_BID_UPDATE,
            CHANNEL_BID_UPDATES,
            title,
            message,
            createItemDetailIntent(itemTitle)
        );
    }
    
    /**
     * Send outbid notification
     */
    public void sendOutbidNotification(String userId, String itemTitle, double newBidAmount) {
        if (!isNotificationEnabled(userId, NotificationType.BID_UPDATES)) {
            return;
        }
        
        String title = "You've Been Outbid";
        String message = String.format("Someone bid ₱%.2f on %s", newBidAmount, itemTitle);
        
        sendNotification(
            userId,
            NOTIFICATION_ID_BID_UPDATE,
            CHANNEL_BID_UPDATES,
            title,
            message,
            createItemDetailIntent(itemTitle)
        );
    }
    
    // ==================== AUCTION NOTIFICATIONS ====================
    
    /**
     * Send auction ending soon notification
     */
    public void sendAuctionEndingNotification(String userId, String itemTitle, int minutesLeft) {
        if (!isNotificationEnabled(userId, NotificationType.AUCTION_ALERTS)) {
            return;
        }
        
        String title = "Auction Ending Soon!";
        String message = String.format("%s ends in %d minutes", itemTitle, minutesLeft);
        
        sendNotification(
            userId,
            NOTIFICATION_ID_AUCTION_ENDING,
            CHANNEL_AUCTION_ALERTS,
            title,
            message,
            createItemDetailIntent(itemTitle)
        );
    }
    
    /**
     * Send auction won notification
     */
    public void sendAuctionWonNotification(String userId, String itemTitle, double winningBid) {
        if (!isNotificationEnabled(userId, NotificationType.AUCTION_ALERTS)) {
            return;
        }
        
        String title = "Congratulations! You Won!";
        String message = String.format("You won %s for ₱%.2f", itemTitle, winningBid);
        
        sendNotification(
            userId,
            NOTIFICATION_ID_AUCTION_ENDING,
            CHANNEL_AUCTION_ALERTS,
            title,
            message,
            createItemDetailIntent(itemTitle)
        );
    }
    
    /**
     * Send auction lost notification
     */
    public void sendAuctionLostNotification(String userId, String itemTitle, double winningBid) {
        if (!isNotificationEnabled(userId, NotificationType.AUCTION_ALERTS)) {
            return;
        }
        
        String title = "Auction Ended";
        String message = String.format("%s sold for ₱%.2f", itemTitle, winningBid);
        
        sendNotification(
            userId,
            NOTIFICATION_ID_AUCTION_ENDING,
            CHANNEL_AUCTION_ALERTS,
            title,
            message,
            createItemDetailIntent(itemTitle)
        );
    }
    
    // ==================== PAYMENT NOTIFICATIONS ====================
    
    /**
     * Send payment success notification
     */
    public void sendPaymentSuccessNotification(String userId, double amount, String paymentMethod) {
        if (!isNotificationEnabled(userId, NotificationType.PAYMENT_NOTIFICATIONS)) {
            return;
        }
        
        String title = "Payment Successful";
        String message = String.format("₱%.2f added via %s", amount, paymentMethod);
        
        sendNotification(
            userId,
            NOTIFICATION_ID_PAYMENT_SUCCESS,
            CHANNEL_PAYMENT_NOTIFICATIONS,
            title,
            message,
            createMainActivityIntent()
        );
    }
    
    /**
     * Send payment failure notification
     */
    public void sendPaymentFailureNotification(String userId, double amount, String errorMessage) {
        if (!isNotificationEnabled(userId, NotificationType.PAYMENT_NOTIFICATIONS)) {
            return;
        }
        
        String title = "Payment Failed";
        String message = String.format("Failed to add ₱%.2f: %s", amount, errorMessage);
        
        sendNotification(
            userId,
            NOTIFICATION_ID_PAYMENT_SUCCESS,
            CHANNEL_PAYMENT_NOTIFICATIONS,
            title,
            message,
            createMainActivityIntent()
        );
    }
    
    // ==================== SYSTEM NOTIFICATIONS ====================
    
    /**
     * Send system notification
     */
    public void sendSystemNotification(String userId, String title, String message) {
        if (!isNotificationEnabled(userId, NotificationType.SYSTEM_NOTIFICATIONS)) {
            return;
        }
        
        sendNotification(
            userId,
            NOTIFICATION_ID_SYSTEM,
            CHANNEL_SYSTEM_NOTIFICATIONS,
            title,
            message,
            createMainActivityIntent()
        );
    }
    
    // ==================== CORE NOTIFICATION METHODS ====================
    
    private void sendNotification(String userId, int notificationId, String channelId, 
                                String title, String message, Intent intent) {
        try {
            PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL);
            
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(notificationId, builder.build());
            
            // Log notification
            logNotification(userId, title, message);
            
        } catch (Exception e) {
            Log.e(TAG, "Error sending notification: " + e.getMessage(), e);
        }
    }
    
    private Intent createItemDetailIntent(String itemTitle) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("ACTION", "VIEW_ITEM");
        intent.putExtra("ITEM_TITLE", itemTitle);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return intent;
    }
    
    private Intent createMainActivityIntent() {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return intent;
    }
    
    // ==================== NOTIFICATION PREFERENCES ====================
    
    /**
     * Check if notification type is enabled for user
     */
    private boolean isNotificationEnabled(String userId, NotificationType type) {
        NotificationPreferences prefs = userPreferences.get(userId);
        if (prefs == null) {
            // Default to enabled for all types
            return true;
        }
        
        switch (type) {
            case BID_UPDATES:
                return prefs.isBidUpdatesEnabled();
            case AUCTION_ALERTS:
                return prefs.isAuctionAlertsEnabled();
            case PAYMENT_NOTIFICATIONS:
                return prefs.isPaymentNotificationsEnabled();
            case SYSTEM_NOTIFICATIONS:
                return prefs.isSystemNotificationsEnabled();
            default:
                return true;
        }
    }
    
    /**
     * Set notification preferences for user
     */
    public void setNotificationPreferences(String userId, NotificationPreferences preferences) {
        userPreferences.put(userId, preferences);
    }
    
    /**
     * Get notification preferences for user
     */
    public NotificationPreferences getNotificationPreferences(String userId) {
        return userPreferences.getOrDefault(userId, new NotificationPreferences());
    }
    
    // ==================== NOTIFICATION HISTORY ====================
    
    private void logNotification(String userId, String title, String message) {
        NotificationHistory history = new NotificationHistory(
            System.currentTimeMillis(),
            title,
            message
        );
        
        notificationHistory.computeIfAbsent(userId, k -> new ArrayList<>()).add(history);
        
        // Keep only last 100 notifications per user
        List<NotificationHistory> userHistory = notificationHistory.get(userId);
        if (userHistory.size() > 100) {
            userHistory.remove(0);
        }
    }
    
    /**
     * Get notification history for user
     */
    public List<NotificationHistory> getNotificationHistory(String userId) {
        return notificationHistory.getOrDefault(userId, new ArrayList<>());
    }
    
    /**
     * Clear notification history for user
     */
    public void clearNotificationHistory(String userId) {
        notificationHistory.remove(userId);
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Cancel all notifications
     */
    public void cancelAllNotifications() {
        systemNotificationManager.cancelAll();
    }
    
    /**
     * Cancel specific notification
     */
    public void cancelNotification(int notificationId) {
        systemNotificationManager.cancel(notificationId);
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
    
    // ==================== ENUMS ====================
    
    public enum NotificationType {
        BID_UPDATES,
        AUCTION_ALERTS,
        PAYMENT_NOTIFICATIONS,
        SYSTEM_NOTIFICATIONS
    }
    
    // ==================== DATA CLASSES ====================
    
    public static class NotificationPreferences {
        private boolean bidUpdatesEnabled = true;
        private boolean auctionAlertsEnabled = true;
        private boolean paymentNotificationsEnabled = true;
        private boolean systemNotificationsEnabled = true;
        
        public boolean isBidUpdatesEnabled() { return bidUpdatesEnabled; }
        public void setBidUpdatesEnabled(boolean enabled) { this.bidUpdatesEnabled = enabled; }
        
        public boolean isAuctionAlertsEnabled() { return auctionAlertsEnabled; }
        public void setAuctionAlertsEnabled(boolean enabled) { this.auctionAlertsEnabled = enabled; }
        
        public boolean isPaymentNotificationsEnabled() { return paymentNotificationsEnabled; }
        public void setPaymentNotificationsEnabled(boolean enabled) { this.paymentNotificationsEnabled = enabled; }
        
        public boolean isSystemNotificationsEnabled() { return systemNotificationsEnabled; }
        public void setSystemNotificationsEnabled(boolean enabled) { this.systemNotificationsEnabled = enabled; }
    }
    
    public static class NotificationHistory {
        private final long timestamp;
        private final String title;
        private final String message;
        
        public NotificationHistory(long timestamp, String title, String message) {
            this.timestamp = timestamp;
            this.title = title;
            this.message = message;
        }
        
        public long getTimestamp() { return timestamp; }
        public String getTitle() { return title; }
        public String getMessage() { return message; }
    }
}
