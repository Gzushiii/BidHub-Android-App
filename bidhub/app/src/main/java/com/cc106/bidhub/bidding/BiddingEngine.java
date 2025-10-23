package com.cc106.bidhub.bidding;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.cc106.bidhub.DatabaseHelper;
import com.cc106.bidhub.api.ApiResponse;
import com.cc106.bidhub.api.BidApiClient;
import com.cc106.bidhub.credits.CreditManager;
import com.cc106.bidhub.items.Item;
import com.cc106.bidhub.items.ItemManager;
import com.cc106.bidhub.items.ItemStatus;
import com.cc106.bidhub.utils.SharedPreferencesHelper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.json.JSONObject;

/**
 * Comprehensive Bidding Engine
 * Handles all bidding operations including bid placement, validation, and management
 */
public class BiddingEngine {
    private static final String TAG = "BiddingEngine";
    private static BiddingEngine instance;
    
    // Configuration constants
    private static final double MIN_BID_INCREMENT = 1.0;
    private static final double MAX_BID_AMOUNT = 1000000.0;
    private static final int MAX_BIDS_PER_ITEM = 1000;
    private static final int MAX_ACTIVE_BIDS_PER_USER = 50;
    
    // Threading
    private final ExecutorService executorService;
    
    // Dependencies
    private Context context;
    private DatabaseHelper dbHelper;
    private CreditManager creditManager;
    private ItemManager itemManager;
    private SharedPreferencesHelper prefsHelper;
    
    // Caches
    private final Map<String, List<Bid>> itemBidsCache;
    private final Map<String, List<Bid>> userBidsCache;
    private final Map<String, Bid> highestBidCache;
    
    private BiddingEngine(Context context) {
        this.context = context.getApplicationContext();
        this.dbHelper = new DatabaseHelper(context);
        this.creditManager = new CreditManager(context);
        this.itemManager = ItemManager.getInstance(context);
        this.prefsHelper = new SharedPreferencesHelper(context);
        this.executorService = Executors.newCachedThreadPool();
        this.itemBidsCache = new ConcurrentHashMap<>();
        this.userBidsCache = new ConcurrentHashMap<>();
        this.highestBidCache = new ConcurrentHashMap<>();
    }
    
    public static synchronized BiddingEngine getInstance(Context context) {
        if (instance == null) {
            instance = new BiddingEngine(context);
        }
        return instance;
    }
    
    // ==================== BID PLACEMENT ====================
    
    /**
     * Place a bid on an item
     */
    public BidResult placeBid(String itemId, double amount) {
        Log.i(TAG, "Placing bid: " + amount + " on item: " + itemId);
        
        try {
            // Get user info from SharedPreferences
            String bidderId = prefsHelper.getUserId();
            String bidderAlias = prefsHelper.getAlias();
            
            if (bidderId == null || bidderId.isEmpty()) {
                return new BidResult(false, "User not authenticated. Please log in again.", null);
            }
            
            Log.i(TAG, "Bidder info: " + bidderId + " (" + bidderAlias + ")");
            
            // Validate bid parameters
            BidValidationResult validation = validateBid(itemId, bidderId, bidderAlias, amount);
            if (!validation.isValid()) {
                Log.e(TAG, "Bid validation failed: " + validation.getErrorMessage());
                return new BidResult(false, validation.getErrorMessage(), null);
            }
            
            // Get item details
            Item item = itemManager.getItemById(itemId);
            if (item == null) {
                return new BidResult(false, "Item not found", null);
            }
            
            // Check if auction is still active
            if (!item.isAvailableForBidding()) {
                return new BidResult(false, "Auction has ended or is not available for bidding", null);
            }
            
            // Refresh credit balance from backend before placing bid
            double currentBalance = fetchBalanceFromBackendBlocking();
            if (currentBalance < amount) {
                Log.w(TAG, "Local balance insufficient (" + currentBalance + ") vs amount " + amount +
                        ". Proceeding to backend validation to avoid false negatives from stale cache.");
                // Still proceed to backend - let backend be the authoritative source
            } else {
                Log.i(TAG, "Local balance check passed: " + currentBalance + " >= " + amount);
            }
            
            // Get auth token for API call
            String authToken = prefsHelper.getAuthToken();
            if (authToken == null || authToken.isEmpty()) {
                return new BidResult(false, "User not authenticated. Please log in again.", null);
            }
            
            // Call backend API to place bid
            ApiResponse apiResponse = BidApiClient.placeBid(authToken, itemId, amount);
            
            if (apiResponse.isSuccess()) {
                // Create bid object for local caching
                Bid bid = new Bid(itemId, bidderId, bidderAlias, amount);
                bid.setStatus(BidStatus.ACTIVE);
                
                // Update local caches
                updateBidCaches(bid);
                
                // Update item current bid locally
                updateItemCurrentBid(itemId, amount, bidderId);
                
                // Process outbid notifications
                processOutbidNotifications(itemId, bid);
                
                // Refresh credit balance from backend after successful bid
                refreshCreditBalanceFromBackend();
                
                Log.i(TAG, "Bid placed successfully via API: " + bid.getBidId());
                return new BidResult(true, "Bid placed successfully", bid);
            } else {
                Log.e(TAG, "Backend bid placement failed: " + apiResponse.getMessage());
                return new BidResult(false, apiResponse.getMessage(), null);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error placing bid", e);
            return new BidResult(false, "An error occurred while placing bid: " + e.getMessage(), null);
        }
    }
    
    /**
     * Validate bid before placement
     */
    public BidValidationResult validateBid(String itemId, String bidderId, String bidderAlias, double amount) {
        // Validate parameters
        if (itemId == null || itemId.trim().isEmpty()) {
            return new BidValidationResult(false, "Item ID is required");
        }
        
        if (bidderId == null || bidderId.trim().isEmpty()) {
            return new BidValidationResult(false, "Bidder ID is required");
        }
        
        if (bidderAlias == null || bidderAlias.trim().isEmpty()) {
            return new BidValidationResult(false, "Bidder alias is required");
        }
        
        if (amount < MIN_BID_INCREMENT) {
            return new BidValidationResult(false, "Bid amount must be at least " + MIN_BID_INCREMENT);
        }
        
        if (amount > MAX_BID_AMOUNT) {
            return new BidValidationResult(false, "Bid amount exceeds maximum allowed");
        }
        
        // Get item details
        Item item = itemManager.getItemById(itemId);
        if (item == null) {
            return new BidValidationResult(false, "Item not found");
        }
        
        // Check if user is the seller
        if (bidderId.equals(item.getSellerId())) {
            return new BidValidationResult(false, "Sellers cannot bid on their own items");
        }
        
        // Check if auction is active
        if (!item.isAvailableForBidding()) {
            return new BidValidationResult(false, "Auction is not available for bidding");
        }
        
        // Check minimum bid increment
        double currentBid = item.getCurrentPrice();
        if (amount < currentBid + MIN_BID_INCREMENT) {
            return new BidValidationResult(false, "Bid must be at least " + MIN_BID_INCREMENT + " higher than current bid");
        }
        
        // Check user credit balance
        if (!creditManager.validateCreditBalance(bidderId, amount)) {
            return new BidValidationResult(false, "Insufficient credit balance");
        }
        
        // Check user's active bid count
        List<Bid> userBids = getUserActiveBids(bidderId);
        if (userBids.size() >= MAX_ACTIVE_BIDS_PER_USER) {
            return new BidValidationResult(false, "Maximum number of active bids reached");
        }
        
        // Check item bid count
        List<Bid> itemBids = getItemBids(itemId);
        if (itemBids.size() >= MAX_BIDS_PER_ITEM) {
            return new BidValidationResult(false, "Maximum number of bids for this item reached");
        }
        
        return new BidValidationResult(true, "Bid is valid");
    }
    
    // ==================== BID MANAGEMENT ====================
    
    /**
     * Get all bids for an item
     */
    public List<Bid> getItemBids(String itemId) {
        if (itemId == null || itemId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        // Check cache first
        if (itemBidsCache.containsKey(itemId)) {
            return new ArrayList<>(itemBidsCache.get(itemId));
        }
        
        // Load from database
        List<Bid> bids = loadBidsFromDatabase("item_id = ?", new String[]{itemId});
        
        // Sort by amount descending (highest first)
        bids.sort(Comparator.comparing(Bid::getAmount).reversed());
        
        // Update cache
        itemBidsCache.put(itemId, bids);
        
        return bids;
    }
    
    /**
     * Get all bids by a user
     */
    public List<Bid> getUserBids(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        // Check cache first
        if (userBidsCache.containsKey(userId)) {
            return new ArrayList<>(userBidsCache.get(userId));
        }
        
        // Load from database
        List<Bid> bids = loadBidsFromDatabase("bidder_id = ?", new String[]{userId});
        
        // Sort by placed date descending (newest first)
        bids.sort(Comparator.comparing(Bid::getPlacedAt).reversed());
        
        // Update cache
        userBidsCache.put(userId, bids);
        
        return bids;
    }
    
    /**
     * Get active bids by a user
     */
    public List<Bid> getUserActiveBids(String userId) {
        return getUserBids(userId).stream()
                .filter(Bid::isActive)
                .collect(Collectors.toList());
    }
    
    /**
     * Get highest bid for an item
     */
    public Bid getHighestBid(String itemId) {
        if (itemId == null || itemId.trim().isEmpty()) {
            return null;
        }
        
        // Check cache first
        if (highestBidCache.containsKey(itemId)) {
            return highestBidCache.get(itemId);
        }
        
        // Get all bids for item
        List<Bid> bids = getItemBids(itemId);
        
        if (bids.isEmpty()) {
            return null;
        }
        
        // Find highest bid
        Bid highestBid = bids.stream()
                .filter(bid -> bid.getStatus() == BidStatus.ACTIVE || bid.getStatus() == BidStatus.WINNING)
                .max(Comparator.comparing(Bid::getAmount))
                .orElse(null);
        
        // Update cache
        if (highestBid != null) {
            highestBidCache.put(itemId, highestBid);
        }
        
        return highestBid;
    }
    
    /**
     * Cancel a bid
     */
    public boolean cancelBid(String bidId, String userId) {
        Log.i(TAG, "Cancelling bid: " + bidId + " by user: " + userId);
        
        try {
            // Get bid
            Bid bid = getBidById(bidId);
            if (bid == null) {
                Log.e(TAG, "Bid not found: " + bidId);
                return false;
            }
            
            // Check if user owns the bid
            if (!bid.getBidderId().equals(userId)) {
                Log.e(TAG, "User does not own bid: " + bidId);
                return false;
            }
            
            // Check if bid can be cancelled
            if (!bid.getStatus().canBeEdited()) {
                Log.e(TAG, "Bid cannot be cancelled: " + bid.getStatus());
                return false;
            }
            
            // Update bid status
            bid.setStatus(BidStatus.CANCELLED);
            
            // Update in database
            if (!updateBidInDatabase(bid)) {
                Log.e(TAG, "Failed to update bid in database");
                return false;
            }
            
            // Release reserved credits
            creditManager.releaseCredits(userId, bid.getAmount());
            
            // Update caches
            updateBidCaches(bid);
            
            Log.i(TAG, "Bid cancelled successfully: " + bidId);
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling bid", e);
            return false;
        }
    }
    
    /**
     * Get bid by ID
     */
    public Bid getBidById(String bidId) {
        if (bidId == null || bidId.trim().isEmpty()) {
            return null;
        }
        
        // Bids are now managed by backend API - no local database operations
        Log.w(TAG, "getBidById called but bids are now managed by backend API");
        return null;
    }
    
    // ==================== AUCTION MANAGEMENT ====================
    
    /**
     * Process auction end and determine winner
     */
    public AuctionResult processAuctionEnd(String itemId) {
        Log.i(TAG, "Processing auction end for item: " + itemId);
        
        try {
            // Get item
            Item item = itemManager.getItemById(itemId);
            if (item == null) {
                return new AuctionResult(false, "Item not found", null);
            }
            
            // Get highest bid
            Bid highestBid = getHighestBid(itemId);
            if (highestBid == null) {
                // No bids - mark item as expired
                item.setStatus(ItemStatus.ENDED);
                itemManager.updateItem(itemId, item);
                return new AuctionResult(true, "Auction ended with no bids", null);
            }
            
            // Mark highest bid as winning
            highestBid.setStatus(BidStatus.WINNING);
            highestBid.setWinning(true);
            updateBidInDatabase(highestBid);
            
            // Mark other bids as outbid
            List<Bid> allBids = getItemBids(itemId);
            for (Bid bid : allBids) {
                if (!bid.getBidId().equals(highestBid.getBidId()) && 
                    (bid.getStatus() == BidStatus.ACTIVE || bid.getStatus() == BidStatus.WINNING)) {
                    bid.setStatus(BidStatus.OUTBID);
                    updateBidInDatabase(bid);
                    
                    // Credits already deducted when bid was placed, no action needed
                }
            }
            
            // Credits already deducted when bid was placed
            
            // Update item status
            item.setStatus(ItemStatus.ENDED);
            item.setCurrentBidderId(highestBid.getBidderId());
            itemManager.updateItem(itemId, item);
            
            // Update caches
            updateBidCaches(highestBid);
            
            Log.i(TAG, "Auction processed successfully. Winner: " + highestBid.getBidderId());
            return new AuctionResult(true, "Auction completed successfully", highestBid);
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing auction end", e);
            return new AuctionResult(false, "Error processing auction: " + e.getMessage(), null);
        }
    }
    
    /**
     * Check and process expired auctions
     */
    public void processExpiredAuctions() {
        Log.i(TAG, "Processing expired auctions");
        
        try {
            // Get all active items
            List<Item> activeItems = itemManager.getAllActiveItems();
            
            for (Item item : activeItems) {
                if (item.hasEnded()) {
                    processAuctionEnd(item.getItemId());
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing expired auctions", e);
        }
    }
    
    // ==================== PRIVATE HELPER METHODS ====================
    
    private boolean saveBidToDatabase(Bid bid) {
        // Bids are now managed by backend API - no local database operations
        Log.w(TAG, "saveBidToDatabase called but bids are now managed by backend API");
        return true; // Return true to avoid breaking existing code
    }
    
    private boolean updateBidInDatabase(Bid bid) {
        // Bids are now managed by backend API - no local database operations
        Log.w(TAG, "updateBidInDatabase called but bids are now managed by backend API");
        return true; // Return true to avoid breaking existing code
    }
    
    private List<Bid> loadBidsFromDatabase(String whereClause, String[] whereArgs) {
        // Bids are now managed by backend API - no local database operations
        Log.w(TAG, "loadBidsFromDatabase called but bids are now managed by backend API");
        return new ArrayList<>(); // Return empty list to avoid breaking existing code
    }
    
    private Bid createBidFromCursor(Cursor cursor) {
        Bid bid = new Bid();
        bid.setBidId(cursor.getString(0));
        bid.setItemId(cursor.getString(1));
        bid.setBidderId(cursor.getString(2));
        bid.setAmount(cursor.getDouble(3));
        bid.setBidderAlias(cursor.getString(4));
        bid.setPlacedAt(new Date(cursor.getLong(5)));
        bid.setWinning(cursor.getInt(6) == 1);
        
        // Determine status based on winning flag
        if (bid.isWinning()) {
            bid.setStatus(BidStatus.WINNING);
        } else {
            bid.setStatus(BidStatus.ACTIVE);
        }
        
        return bid;
    }
    
    private void updateItemCurrentBid(String itemId, double amount, String bidderId) {
        // This would typically update the item in the database
        // For now, we'll update through the item manager
        Item item = itemManager.getItemById(itemId);
        if (item != null) {
            item.setCurrentPrice(amount);
            item.setHighestBidderId(bidderId);
            item.incrementBidCount();
            // Note: In a real implementation, this would update the database
        }
    }
    
    private void updateBidCaches(Bid bid) {
        // Update item bids cache
        List<Bid> itemBids = itemBidsCache.get(bid.getItemId());
        if (itemBids != null) {
            itemBids.removeIf(b -> b.getBidId().equals(bid.getBidId()));
            itemBids.add(bid);
            itemBids.sort(Comparator.comparing(Bid::getAmount).reversed());
        }
        
        // Update user bids cache
        List<Bid> userBids = userBidsCache.get(bid.getBidderId());
        if (userBids != null) {
            userBids.removeIf(b -> b.getBidId().equals(bid.getBidId()));
            userBids.add(bid);
            userBids.sort(Comparator.comparing(Bid::getPlacedAt).reversed());
        }
        
        // Update highest bid cache
        if (bid.getStatus() == BidStatus.WINNING || bid.getStatus() == BidStatus.ACTIVE) {
            Bid currentHighest = highestBidCache.get(bid.getItemId());
            if (currentHighest == null || bid.getAmount() > currentHighest.getAmount()) {
                highestBidCache.put(bid.getItemId(), bid);
            }
        }
    }
    
    private void processOutbidNotifications(String itemId, Bid newBid) {
        // This would typically send notifications to outbid users
        // For now, we'll just log the action
        Log.i(TAG, "Processing outbid notifications for item: " + itemId);
        
        List<Bid> itemBids = getItemBids(itemId);
        for (Bid bid : itemBids) {
            if (!bid.getBidId().equals(newBid.getBidId()) && 
                bid.getAmount() < newBid.getAmount() && 
                bid.getStatus() == BidStatus.ACTIVE) {
                
                // Mark as outbid
                bid.setStatus(BidStatus.OUTBID);
                updateBidInDatabase(bid);
                
                // Release reserved credits
                creditManager.releaseCredits(bid.getBidderId(), bid.getAmount());
                
                Log.i(TAG, "User " + bid.getBidderId() + " outbid on item " + itemId);
            }
        }
    }
    
    /**
     * Clear caches (call this when needed to free memory)
     */
    public void clearCaches() {
        itemBidsCache.clear();
        userBidsCache.clear();
        highestBidCache.clear();
        Log.i(TAG, "Bidding engine caches cleared");
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Get current user balance from SharedPreferences (synced with backend)
     */
    private double getCurrentUserBalance() {
        return prefsHelper.getCredits();
    }
    
    /**
     * Synchronously fetch the latest balance from backend and update SharedPreferences.
     * Returns the latest known balance (or cached balance if call fails).
     */
    private double fetchBalanceFromBackendBlocking() {
        try {
            String authToken = prefsHelper.getAuthToken();
            if (authToken == null || authToken.isEmpty()) {
                return getCurrentUserBalance();
            }
            java.net.URL url = new java.net.URL("https://bidhub-android-app.onrender.com/api/credits/balance");
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + authToken);
            // Allow for Render free-tier cold starts (~50s)
            connection.setConnectTimeout(60000);
            connection.setReadTimeout(60000);
            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                org.json.JSONObject json = new org.json.JSONObject(response.toString());
                double balance = json.optDouble("credits", json.optDouble("balance", getCurrentUserBalance()));
                prefsHelper.setCredits(balance);
                Log.i(TAG, "Fetched latest balance from backend: " + balance);
                return balance;
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to refresh balance from backend: " + e.getMessage());
        }
        return getCurrentUserBalance();
    }

    /**
     * Refresh credit balance from backend after successful bid
     */
    private void refreshCreditBalanceFromBackend() {
        new Thread(() -> {
            try {
                String authToken = prefsHelper.getAuthToken();
                if (authToken == null || authToken.isEmpty()) {
                    return;
                }
                
                // Call backend to get updated credit balance
                URL url = new URL("https://bidhub-android-app.onrender.com/api/credits/balance");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Authorization", "Bearer " + authToken);
                connection.setConnectTimeout(60000);
                connection.setReadTimeout(60000);
                
                int responseCode = connection.getResponseCode();
                if (responseCode >= 200 && responseCode < 300) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    double newBalance = jsonResponse.optDouble("balance", 0.0);
                    
                    // Update SharedPreferences with new balance
                    prefsHelper.setCredits(newBalance);
                    
                    Log.i(TAG, "Credit balance refreshed from backend: " + newBalance);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error refreshing credit balance from backend", e);
            }
        }).start();
    }
}


