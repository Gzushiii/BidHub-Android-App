package com.cc106.bidhub.bidding;

import android.content.Context;
import android.util.Log;

import com.cc106.bidhub.items.Item;
import com.cc106.bidhub.items.ItemManager;
import com.cc106.bidhub.items.ItemStatus;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Auction Manager
 * Handles auction lifecycle management and automated processes
 */
public class AuctionManager {
    private static final String TAG = "AuctionManager";
    private static AuctionManager instance;
    
    // Threading
    private final ScheduledExecutorService scheduledExecutor;
    
    // Dependencies
    private Context context;
    private ItemManager itemManager;
    private BiddingEngine biddingEngine;
    
    // Configuration
    private static final int AUCTION_CHECK_INTERVAL_MINUTES = 1;
    private static final int AUCTION_EXTENSION_MINUTES = 5;
    
    private AuctionManager(Context context) {
        this.context = context.getApplicationContext();
        this.itemManager = ItemManager.getInstance(context);
        this.biddingEngine = BiddingEngine.getInstance(context);
        this.scheduledExecutor = Executors.newScheduledThreadPool(2);
        
        startAuctionMonitoring();
    }
    
    public static synchronized AuctionManager getInstance(Context context) {
        if (instance == null) {
            instance = new AuctionManager(context);
        }
        return instance;
    }
    
    // ==================== AUCTION LIFECYCLE ====================
    
    /**
     * Start an auction
     */
    public boolean startAuction(String itemId) {
        Log.i(TAG, "Starting auction for item: " + itemId);
        
        try {
            Item item = itemManager.getItemById(itemId);
            if (item == null) {
                Log.e(TAG, "Item not found: " + itemId);
                return false;
            }
            
            // Check if item can be started
            if (item.getStatus() != ItemStatus.DRAFT) {
                Log.e(TAG, "Item cannot be started: " + item.getStatus());
                return false;
            }
            
            // Set auction start time
            item.setStartDate(new Date());
            item.setStatus(ItemStatus.ACTIVE);
            
            // Update item
            if (itemManager.updateItem(itemId, item)) {
                Log.i(TAG, "Auction started successfully for item: " + itemId);
                return true;
            } else {
                Log.e(TAG, "Failed to update item status");
                return false;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting auction", e);
            return false;
        }
    }
    
    /**
     * End an auction
     */
    public AuctionResult endAuction(String itemId) {
        Log.i(TAG, "Ending auction for item: " + itemId);
        
        try {
            Item item = itemManager.getItemById(itemId);
            if (item == null) {
                return new AuctionResult(false, "Item not found", null);
            }
            
            // Check if auction can be ended
            if (item.getStatus() != ItemStatus.ACTIVE) {
                return new AuctionResult(false, "Auction is not active", null);
            }
            
            // Process auction end through bidding engine
            AuctionResult result = biddingEngine.processAuctionEnd(itemId);
            
            if (result.isSuccess()) {
                Log.i(TAG, "Auction ended successfully for item: " + itemId);
            } else {
                Log.e(TAG, "Failed to end auction: " + result.getMessage());
            }
            
            return result;
            
        } catch (Exception e) {
            Log.e(TAG, "Error ending auction", e);
            return new AuctionResult(false, "Error ending auction: " + e.getMessage(), null);
        }
    }
    
    /**
     * Extend auction deadline
     */
    public boolean extendAuction(String itemId, int additionalMinutes) {
        Log.i(TAG, "Extending auction for item: " + itemId + " by " + additionalMinutes + " minutes");
        
        try {
            Item item = itemManager.getItemById(itemId);
            if (item == null) {
                Log.e(TAG, "Item not found: " + itemId);
                return false;
            }
            
            // Check if auction can be extended
            if (item.getStatus() != ItemStatus.ACTIVE) {
                Log.e(TAG, "Auction is not active");
                return false;
            }
            
            // Extend deadline
            Date currentDeadline = item.getEndDate();
            if (currentDeadline != null) {
                Date newDeadline = new Date(currentDeadline.getTime() + (additionalMinutes * 60 * 1000));
                item.setEndDate(newDeadline);
                
                if (itemManager.updateItem(itemId, item)) {
                    Log.i(TAG, "Auction extended successfully");
                    return true;
                } else {
                    Log.e(TAG, "Failed to update item deadline");
                    return false;
                }
            } else {
                Log.e(TAG, "Item has no deadline set");
                return false;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error extending auction", e);
            return false;
        }
    }
    
    /**
     * Cancel an auction
     */
    public boolean cancelAuction(String itemId, String reason) {
        Log.i(TAG, "Cancelling auction for item: " + itemId + " - Reason: " + reason);
        
        try {
            Item item = itemManager.getItemById(itemId);
            if (item == null) {
                Log.e(TAG, "Item not found: " + itemId);
                return false;
            }
            
            // Check if auction can be cancelled
            if (item.getStatus() != ItemStatus.ACTIVE) {
                Log.e(TAG, "Auction is not active");
                return false;
            }
            
            // Get all active bids and release credits
            List<Bid> activeBids = biddingEngine.getItemBids(itemId).stream()
                    .filter(Bid::isActive)
                    .collect(Collectors.toList());
            
            for (Bid bid : activeBids) {
                // Release reserved credits
                // Note: This would need to be implemented in CreditManager
                Log.i(TAG, "Releasing credits for bid: " + bid.getBidId());
            }
            
            // Update item status
            item.setStatus(ItemStatus.CANCELLED);
            if (itemManager.updateItem(itemId, item)) {
                Log.i(TAG, "Auction cancelled successfully");
                return true;
            } else {
                Log.e(TAG, "Failed to update item status");
                return false;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling auction", e);
            return false;
        }
    }
    
    // ==================== AUCTION MONITORING ====================
    
    /**
     * Start automated auction monitoring
     */
    private void startAuctionMonitoring() {
        Log.i(TAG, "Starting auction monitoring");
        
        // Check for expired auctions every minute
        scheduledExecutor.scheduleAtFixedRate(() -> {
            try {
                processExpiredAuctions();
            } catch (Exception e) {
                Log.e(TAG, "Error in auction monitoring", e);
            }
        }, 0, AUCTION_CHECK_INTERVAL_MINUTES, TimeUnit.MINUTES);
        
        // Check for auctions ending soon every 5 minutes
        scheduledExecutor.scheduleAtFixedRate(() -> {
            try {
                checkAuctionsEndingSoon();
            } catch (Exception e) {
                Log.e(TAG, "Error checking auctions ending soon", e);
            }
        }, 0, 5, TimeUnit.MINUTES);
    }
    
    /**
     * Process expired auctions
     */
    private void processExpiredAuctions() {
        Log.d(TAG, "Processing expired auctions");
        
        try {
            List<Item> activeItems = itemManager.getAllActiveItems();
            List<Item> expiredItems = activeItems.stream()
                    .filter(Item::hasEnded)
                    .collect(Collectors.toList());
            
            for (Item item : expiredItems) {
                Log.i(TAG, "Processing expired auction: " + item.getItemId());
                endAuction(item.getItemId());
            }
            
            if (!expiredItems.isEmpty()) {
                Log.i(TAG, "Processed " + expiredItems.size() + " expired auctions");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing expired auctions", e);
        }
    }
    
    /**
     * Check for auctions ending soon
     */
    private void checkAuctionsEndingSoon() {
        Log.d(TAG, "Checking auctions ending soon");
        
        try {
            List<Item> activeItems = itemManager.getAllActiveItems();
            Date now = new Date();
            long oneHourMs = 60 * 60 * 1000;
            
            List<Item> endingSoonItems = activeItems.stream()
                    .filter(item -> {
                        if (item.getEndDate() == null) return false;
                        long timeRemaining = item.getEndDate().getTime() - now.getTime();
                        return timeRemaining > 0 && timeRemaining <= oneHourMs;
                    })
                    .collect(Collectors.toList());
            
            for (Item item : endingSoonItems) {
                Log.i(TAG, "Auction ending soon: " + item.getItemId() + 
                      " - Time remaining: " + item.getHoursRemaining() + " hours");
                
                // Send notifications to bidders
                sendAuctionEndingSoonNotifications(item);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking auctions ending soon", e);
        }
    }
    
    /**
     * Send notifications for auctions ending soon
     */
    private void sendAuctionEndingSoonNotifications(Item item) {
        // This would typically send push notifications, emails, or SMS
        // For now, we'll just log the action
        Log.i(TAG, "Sending auction ending soon notifications for item: " + item.getItemId());
        
        List<Bid> bids = biddingEngine.getItemBids(item.getItemId());
        for (Bid bid : bids) {
            if (bid.isActive()) {
                Log.i(TAG, "Notifying bidder " + bid.getBidderId() + 
                      " that auction " + item.getItemId() + " is ending soon");
            }
        }
    }
    
    // ==================== AUCTION STATISTICS ====================
    
    /**
     * Get auction statistics
     */
    public AuctionStatistics getAuctionStatistics() {
        try {
            List<Item> allItems = itemManager.getAllActiveItems();
            
            int totalAuctions = allItems.size();
            int activeAuctions = (int) allItems.stream()
                    .filter(item -> item.getStatus() == ItemStatus.ACTIVE)
                    .count();
            int endedAuctions = (int) allItems.stream()
                    .filter(item -> item.getStatus() == ItemStatus.ENDED)
                    .count();
            int cancelledAuctions = (int) allItems.stream()
                    .filter(item -> item.getStatus() == ItemStatus.CANCELLED)
                    .count();
            
            double totalBidValue = allItems.stream()
                    .mapToDouble(Item::getCurrentPrice)
                    .sum();
            
            int totalBids = allItems.stream()
                    .mapToInt(Item::getBidCount)
                    .sum();
            
            return new AuctionStatistics(
                totalAuctions, activeAuctions, endedAuctions, cancelledAuctions,
                totalBidValue, totalBids
            );
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting auction statistics", e);
            return new AuctionStatistics(0, 0, 0, 0, 0.0, 0);
        }
    }
    
    /**
     * Get auctions by status
     */
    public List<Item> getAuctionsByStatus(ItemStatus status) {
        try {
            return itemManager.getAllActiveItems().stream()
                    .filter(item -> item.getStatus() == status)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            Log.e(TAG, "Error getting auctions by status", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get auctions ending soon
     */
    public List<Item> getAuctionsEndingSoon(int hoursAhead) {
        try {
            Date now = new Date();
            long timeLimit = hoursAhead * 60 * 60 * 1000;
            
            return itemManager.getAllActiveItems().stream()
                    .filter(item -> {
                        if (item.getEndDate() == null) return false;
                        long timeRemaining = item.getEndDate().getTime() - now.getTime();
                        return timeRemaining > 0 && timeRemaining <= timeLimit;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            Log.e(TAG, "Error getting auctions ending soon", e);
            return new ArrayList<>();
        }
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        scheduledExecutor.shutdown();
        try {
            if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduledExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

