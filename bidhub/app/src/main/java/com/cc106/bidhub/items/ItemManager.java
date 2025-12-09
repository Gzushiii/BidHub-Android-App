package com.cc106.bidhub.items;

import android.content.Context;
import android.util.Log;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.UUID;

/**
 * Comprehensive Item Management System
 * Handles all item operations including CRUD, image management, categories, validation, and search
 */
public class ItemManager {
    private static final String TAG = "ItemManager";
    private static ItemManager instance;
    
    // Configuration constants
    private static final int MAX_IMAGES_PER_ITEM = 10;
    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 1000;
    private static final double MIN_PRICE = 0.01; // Match backend requirement
    private static final double MAX_PRICE = 1000000.0;
    private static final int MAX_TAGS_PER_ITEM = 10;
    private static final int MAX_ITEMS_PER_PAGE = 50;
    
    // Threading
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutor;
    
    // Storage
    private final Map<String, Item> items;
    private final Map<String, Category> categories;
    private final Map<String, List<String>> itemImages;
    private final Map<String, List<String>> userItems;
    private final Map<String, List<String>> categoryItems;
    private final Map<String, Integer> itemViewCounts;
    private final Map<String, Integer> itemBidCounts;
    
    // Context
    private final Context context;
    
    private ItemManager(Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newCachedThreadPool();
        this.scheduledExecutor = Executors.newScheduledThreadPool(2);
        this.items = new ConcurrentHashMap<>();
        this.categories = new ConcurrentHashMap<>();
        this.itemImages = new ConcurrentHashMap<>();
        this.userItems = new ConcurrentHashMap<>();
        this.categoryItems = new ConcurrentHashMap<>();
        this.itemViewCounts = new ConcurrentHashMap<>();
        this.itemBidCounts = new ConcurrentHashMap<>();
        
        initializeDefaultCategories();
        // Do not seed sample data in production; rely on backend as source of truth
        startCleanupTask();
    }
    
    public static synchronized ItemManager getInstance(Context context) {
        if (instance == null) {
            instance = new ItemManager(context);
        }
        return instance;
    }
    
    /**
     * Helper method to replace computeIfAbsent for API 21 compatibility
     * Gets list from map or creates new list if not present
     */
    private List<String> getOrCreateList(Map<String, List<String>> map, String key) {
        List<String> list = map.get(key);
        if (list == null) {
            list = new ArrayList<>();
            map.put(key, list);
        }
        return list;
    }
    
    // ==================== ITEM OPERATIONS ====================
    
    /**
     * Create new item
     */
    public boolean createItem(ItemData itemData, String sellerEmail) {
        Log.i(TAG, "Creating item for seller: " + sellerEmail);
        
        try {
            // Validate item data
            if (!validateItemData(itemData)) {
                Log.e(TAG, "Invalid item data - validation failed");
                Log.e(TAG, "Title: " + (itemData.getTitle() != null ? itemData.getTitle() : "null"));
                Log.e(TAG, "Description: " + (itemData.getDescription() != null ? itemData.getDescription() : "null"));
                Log.e(TAG, "StartingPrice: " + itemData.getStartingPrice());
                Log.e(TAG, "CategoryId: " + (itemData.getCategoryId() != null ? itemData.getCategoryId() : "null"));
                return false;
            }
            
            // Try to create item via backend API first
            try {
                com.cc106.bidhub.api.ItemApiClient apiClient = new com.cc106.bidhub.api.ItemApiClient(context);
                com.cc106.bidhub.api.ItemApiClient.ApiResponse response = apiClient.createItem(itemData, sellerEmail);
                
                if (response.isSuccess()) {
                    Log.i(TAG, "Item created successfully via backend API");
                    Log.d(TAG, "Backend response: " + response.getData());

                    // Parse the backend response to get the UUID assigned by backend and images
                    String backendItemId = null;
                    List<String> backendImageUrls = new ArrayList<>();
                    try {
                        if (response.getData() != null && !response.getData().isEmpty()) {
                            org.json.JSONObject responseJson = new org.json.JSONObject(response.getData());
                            org.json.JSONObject itemJson = null;
                            
                            if (responseJson.has("item")) {
                                itemJson = responseJson.getJSONObject("item");
                            } else {
                                // Response might have item data at root level
                                itemJson = responseJson;
                            }
                            
                            if (itemJson != null) {
                                // Backend returns uuid_id field
                                if (itemJson.has("uuid_id")) {
                                    backendItemId = itemJson.getString("uuid_id");
                                    Log.i(TAG, "Extracted backend item UUID: " + backendItemId);
                                } else if (itemJson.has("id")) {
                                    // Fallback to id field if uuid_id not present
                                    backendItemId = itemJson.getString("id");
                                    Log.i(TAG, "Extracted backend item ID: " + backendItemId);
                                }
                                
                                // Extract images from backend response
                                if (itemJson.has("images")) {
                                    org.json.JSONArray imagesArray = itemJson.getJSONArray("images");
                                    for (int i = 0; i < imagesArray.length(); i++) {
                                        Object imageObj = imagesArray.get(i);
                                        if (imageObj instanceof org.json.JSONObject) {
                                            org.json.JSONObject imageJson = (org.json.JSONObject) imageObj;
                                            if (imageJson.has("image_url")) {
                                                backendImageUrls.add(imageJson.getString("image_url"));
                                            }
                                        } else if (imageObj instanceof String) {
                                            // Images might be returned as array of strings
                                            backendImageUrls.add((String) imageObj);
                                        }
                                    }
                                    Log.i(TAG, "Extracted " + backendImageUrls.size() + " image URLs from backend response");
                                }
                            }
                        }
                    } catch (org.json.JSONException e) {
                        Log.e(TAG, "Failed to parse backend response for UUID and images", e);
                        Log.e(TAG, "Response data: " + response.getData());
                    }

                    // Also store locally for offline access
                    Item item = new Item();
                    // CRITICAL FIX: Use backend's UUID if available, otherwise fallback to local UUID
                    if (backendItemId != null && !backendItemId.isEmpty()) {
                        item.setItemId(backendItemId);
                        Log.i(TAG, "Using backend UUID for local storage: " + backendItemId);
                    } else {
                        item.setItemId(UUID.randomUUID().toString());
                        Log.w(TAG, "Backend did not return UUID, generated local UUID (this may cause 404 errors!)");
                    }
                    item.setTitle(itemData.getTitle());
                    item.setDescription(itemData.getDescription());
                    item.setStartingPrice(itemData.getStartingPrice());
                    // FIX: For new items with no bids, currentPrice should be 0 (will show starting price in UI)
                    item.setCurrentPrice(0.0);
                    item.setBidCount(0); // New item has no bids yet
                    item.setBuyNowPrice(itemData.getBuyNowPrice());
                    item.setCurrency(itemData.getCurrency());
                    item.setSellerId(sellerEmail); // Store email as seller ID for now
                    item.setCategoryId(itemData.getCategoryId());
                    item.setCondition(itemData.getCondition());
                    item.setStartDate(itemData.getStartDate());
                    item.setEndDate(itemData.getEndDate());
                    item.setNotes(itemData.getNotes());
                    item.setMetadata(itemData.getMetadata());
                    item.setStatus(ItemStatus.ACTIVE); // Mark as active since it was posted to backend
                    item.setCreatedAt(new Date());
                    item.setUpdatedAt(new Date());
                    
                    // Add tags
                    if (itemData.getTags() != null) {
                        item.setTags(new ArrayList<>(itemData.getTags()));
                    }
                    
                    // Add image paths - prefer backend response images, fallback to itemData images
                    List<String> finalImagePaths = new ArrayList<>();
                    if (!backendImageUrls.isEmpty()) {
                        // Use images from backend response (these are the actual URLs stored in database)
                        finalImagePaths.addAll(backendImageUrls);
                        Log.i(TAG, "Using " + finalImagePaths.size() + " image URLs from backend response");
                    } else if (itemData.getImagePaths() != null && !itemData.getImagePaths().isEmpty()) {
                        // Fallback to itemData images if backend didn't return them
                        finalImagePaths.addAll(itemData.getImagePaths());
                        Log.i(TAG, "Using " + finalImagePaths.size() + " image URLs from itemData (backend didn't return images)");
                    }
                    
                    if (!finalImagePaths.isEmpty()) {
                        item.setImagePaths(new ArrayList<>(finalImagePaths));
                        itemImages.put(item.getItemId(), new ArrayList<>(finalImagePaths));
                        Log.i(TAG, "Stored " + finalImagePaths.size() + " image URLs for item: " + item.getItemId());
                        for (int i = 0; i < finalImagePaths.size(); i++) {
                            Log.d(TAG, "  Image " + (i + 1) + ": " + finalImagePaths.get(i));
                        }
                    } else {
                        Log.w(TAG, "No images to store for item: " + item.getItemId());
                    }
                    
                    // Store item locally
                    items.put(item.getItemId(), item);
                    Log.i(TAG, "Stored item in local map with ID: " + item.getItemId());
                    Log.d(TAG, "Item details - Title: " + item.getTitle() + ", Images: " + (item.getImagePaths() != null ? item.getImagePaths().size() : 0));
                    
                    // Update user items
                    getOrCreateList(userItems, sellerEmail).add(item.getItemId());
                    Log.d(TAG, "Added item to user items list for: " + sellerEmail);
                    
                    // Update category items
                    if (itemData.getCategoryId() != null) {
                        getOrCreateList(categoryItems, itemData.getCategoryId()).add(item.getItemId());
                        Log.d(TAG, "Added item to category items list for: " + itemData.getCategoryId());
                    }
                    
                    // Initialize counters
                    itemViewCounts.put(item.getItemId(), 0);
                    itemBidCounts.put(item.getItemId(), 0);
                    // FIX: Ensure bidCount is set on the item object itself (already set above, but ensure it's 0)
                    if (item.getBidCount() < 0) {
                        item.setBidCount(0);
                    }
                    
                    Log.i(TAG, "Item created successfully: " + item.getItemId());
                    Log.i(TAG, "Total items in local storage: " + items.size());
                    return true;
                } else {
                    Log.e(TAG, "Backend API error: " + response.getMessage());
                    Log.e(TAG, "Backend API error details: " + response.getData());
                    // Do not fallback to local storage - item must be posted to backend
                    // Return false to indicate failure
                    return false;
                }
            } catch (Exception apiException) {
                Log.e(TAG, "Backend API call failed", apiException);
                Log.i(TAG, "Falling back to local storage due to API exception");
                // Fallback to local storage when API fails
                return createLocalItem(itemData, sellerEmail);
            }
            
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating item", e);
            return false;
        }
    }
    
    /**
     * Create new item (asynchronous version to prevent NetworkOnMainThreadException)
     */
    public void createItemAsync(ItemData itemData, String sellerEmail, ItemCreationCallback callback) {
        Log.i(TAG, "Creating item async for seller: " + sellerEmail);
        
        // Run on background thread to prevent NetworkOnMainThreadException
        new Thread(() -> {
            boolean success = createItem(itemData, sellerEmail);
            
            // Call callback on main thread
            if (callback != null) {
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> callback.onResult(success));
            }
        }).start();
    }
    
    /**
     * Callback interface for async item creation
     */
    public interface ItemCreationCallback {
        void onResult(boolean success);
    }
    
    /**
     * Create item locally (fallback when API fails)
     */
    private boolean createLocalItem(ItemData itemData, String sellerEmail) {
        try {
            Log.i(TAG, "Creating item locally for seller: " + sellerEmail);
            
            // Create local item
            Item item = new Item();
            item.setItemId(UUID.randomUUID().toString());
            item.setTitle(itemData.getTitle());
            item.setDescription(itemData.getDescription());
            item.setStartingPrice(itemData.getStartingPrice());
            // FIX: For new items with no bids, currentPrice should be 0 (will show starting price in UI)
            item.setCurrentPrice(0.0);
            item.setBidCount(0); // New local item has no bids yet
            item.setBuyNowPrice(itemData.getBuyNowPrice());
            item.setCurrency(itemData.getCurrency());
            item.setSellerId(sellerEmail);
            item.setCategoryId(itemData.getCategoryId());
            item.setCondition(itemData.getCondition());
            item.setStartDate(itemData.getStartDate());
            item.setEndDate(itemData.getEndDate());
            item.setNotes(itemData.getNotes());
            item.setMetadata(itemData.getMetadata());
            item.setStatus(ItemStatus.ACTIVE);
            item.setCreatedAt(new Date());
            item.setUpdatedAt(new Date());
            
            // Add tags
            if (itemData.getTags() != null) {
                item.setTags(new ArrayList<>(itemData.getTags()));
            }
            
            // Add image paths
            if (itemData.getImagePaths() != null && !itemData.getImagePaths().isEmpty()) {
                item.setImagePaths(new ArrayList<>(itemData.getImagePaths()));
                itemImages.put(item.getItemId(), new ArrayList<>(itemData.getImagePaths()));
            }
            
            // Store item locally
            items.put(item.getItemId(), item);
            
            // Update user items
            userItems.computeIfAbsent(sellerEmail, k -> new ArrayList<>()).add(item.getItemId());
            
            // Update category items
            if (itemData.getCategoryId() != null) {
                categoryItems.computeIfAbsent(itemData.getCategoryId(), k -> new ArrayList<>()).add(item.getItemId());
            }
            
            // Initialize counters
            itemViewCounts.put(item.getItemId(), 0);
            itemBidCounts.put(item.getItemId(), 0);
            
            Log.i(TAG, "Item created locally successfully: " + item.getItemId());
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating local item", e);
            return false;
        }
    }
    
    /**
     * Save item as draft (persisted to backend)
     */
    public boolean saveDraftItem(ItemData itemData, String sellerEmail) {
        Log.i(TAG, "Saving item as draft for seller: " + sellerEmail);
        
        try {
            // Validate item data
            if (!validateItemData(itemData)) {
                Log.e(TAG, "Invalid item data");
                return false;
            }
            
            // Try to create draft via backend API first
            try {
                com.cc106.bidhub.api.ItemApiClient apiClient = new com.cc106.bidhub.api.ItemApiClient(context);
                com.cc106.bidhub.api.ItemApiClient.ApiResponse response = apiClient.createDraftItem(itemData, sellerEmail);
                
                if (response.isSuccess()) {
                    Log.i(TAG, "Draft item created successfully via backend API");
                    
                    // Also store locally for offline access
                    Item item = new Item();
                    item.setItemId(UUID.randomUUID().toString());
                    item.setTitle(itemData.getTitle());
                    item.setDescription(itemData.getDescription());
                    item.setStartingPrice(itemData.getStartingPrice());
                    item.setCurrentPrice(itemData.getStartingPrice());
                    item.setBuyNowPrice(itemData.getBuyNowPrice());
                    item.setCurrency(itemData.getCurrency());
                    item.setSellerId(sellerEmail);
                    item.setCategoryId(itemData.getCategoryId());
                    item.setCondition(itemData.getCondition());
                    item.setShippingInfo(itemData.getShippingInfo());
                    item.setStartDate(itemData.getStartDate());
                    item.setEndDate(itemData.getEndDate());
                    item.setNotes(itemData.getNotes());
                    item.setMetadata(itemData.getMetadata());
                    item.setStatus(ItemStatus.DRAFT); // Explicitly set as DRAFT
                    item.setCreatedAt(new Date());
                    item.setUpdatedAt(new Date());
                    
                    // Add tags
                    if (itemData.getTags() != null) {
                        item.setTags(new ArrayList<>(itemData.getTags()));
                    }
                    
                    // Add image paths
                    if (itemData.getImagePaths() != null && !itemData.getImagePaths().isEmpty()) {
                        item.setImagePaths(new ArrayList<>(itemData.getImagePaths()));
                        itemImages.put(item.getItemId(), new ArrayList<>(itemData.getImagePaths()));
                    }
                    
                    // Store item locally
                    items.put(item.getItemId(), item);
                    
                    // Update user items
                    getOrCreateList(userItems, sellerEmail).add(item.getItemId());
                    
                    // Update category items
                    if (itemData.getCategoryId() != null) {
                        getOrCreateList(categoryItems, itemData.getCategoryId()).add(item.getItemId());
                    }
                    
                    // Initialize counters
                    itemViewCounts.put(item.getItemId(), 0);
                    itemBidCounts.put(item.getItemId(), 0);
                    
                    Log.i(TAG, "Draft item saved successfully: " + item.getItemId());
                    return true;
                } else {
                    Log.e(TAG, "Failed to create draft via API: " + response.getMessage());
                    return false;
                }
            } catch (Exception apiError) {
                Log.e(TAG, "Error creating draft via API, falling back to local storage", apiError);
                // Fallback to local storage if API fails
            }
            
            // Fallback: Create item locally only with DRAFT status
            Item item = new Item();
            item.setItemId(UUID.randomUUID().toString());
            item.setTitle(itemData.getTitle());
            item.setDescription(itemData.getDescription());
            item.setStartingPrice(itemData.getStartingPrice());
            item.setCurrentPrice(itemData.getStartingPrice());
            item.setBuyNowPrice(itemData.getBuyNowPrice());
            item.setCurrency(itemData.getCurrency());
            item.setSellerId(sellerEmail);
            item.setCategoryId(itemData.getCategoryId());
            item.setCondition(itemData.getCondition());
            item.setShippingInfo(itemData.getShippingInfo());
            item.setStartDate(itemData.getStartDate());
            item.setEndDate(itemData.getEndDate());
            item.setNotes(itemData.getNotes());
            item.setMetadata(itemData.getMetadata());
            item.setStatus(ItemStatus.DRAFT); // Explicitly set as DRAFT
            item.setCreatedAt(new Date());
            item.setUpdatedAt(new Date());
            
            // Add tags
            if (itemData.getTags() != null) {
                item.setTags(new ArrayList<>(itemData.getTags()));
            }
            
            // Add image paths
            if (itemData.getImagePaths() != null && !itemData.getImagePaths().isEmpty()) {
                item.setImagePaths(new ArrayList<>(itemData.getImagePaths()));
                itemImages.put(item.getItemId(), new ArrayList<>(itemData.getImagePaths()));
            }
            
            // Store item
            items.put(item.getItemId(), item);
            
            // Update user items
            userItems.computeIfAbsent(sellerEmail, k -> new ArrayList<>()).add(item.getItemId());
            
            // Update category items
            if (itemData.getCategoryId() != null) {
                categoryItems.computeIfAbsent(itemData.getCategoryId(), k -> new ArrayList<>()).add(item.getItemId());
            }
            
            // Initialize counters
            itemViewCounts.put(item.getItemId(), 0);
            itemBidCounts.put(item.getItemId(), 0);
            
            Log.i(TAG, "Item saved as draft locally: " + item.getItemId());
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error saving draft item", e);
            return false;
        }
    }
    
    /**
     * Save item as draft (asynchronous version to prevent NetworkOnMainThreadException)
     */
    public void saveDraftItemAsync(ItemData itemData, String sellerEmail, ItemCreationCallback callback) {
        Log.i(TAG, "Saving item as draft async for seller: " + sellerEmail);
        
        // Run on background thread to prevent NetworkOnMainThreadException
        new Thread(() -> {
            boolean success = saveDraftItem(itemData, sellerEmail);
            
            // Call callback on main thread
            if (callback != null) {
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> callback.onResult(success));
            }
        }).start();
    }
    
    /**
     * Update item with Item object
     */
    public boolean updateItem(String itemId, Item item) {
        Log.i(TAG, "Updating item: " + itemId);
        
        try {
            if (item == null) {
                Log.e(TAG, "Item is null");
                return false;
            }
            
            // Check if item can be edited
            if (!item.getStatus().canBeEdited()) {
                Log.e(TAG, "Item cannot be edited: " + item.getStatus());
                return false;
            }
            
            // Update item
            item.setUpdatedAt(new Date());
            items.put(itemId, item);
            
            Log.i(TAG, "Item updated successfully: " + itemId);
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating item", e);
            return false;
        }
    }
    
    /**
     * Update item with ItemData
     */
    public boolean updateItem(String itemId, ItemData itemData) {
        Log.i(TAG, "Updating item: " + itemId);
        
        try {
            Item item = items.get(itemId);
            if (item == null) {
                Log.e(TAG, "Item not found: " + itemId);
                return false;
            }
            
            // Check if item can be edited
            if (!item.getStatus().canBeEdited()) {
                Log.e(TAG, "Item cannot be edited: " + item.getStatus());
                return false;
            }
            
            // Validate item data
            if (!validateItemData(itemData)) {
                Log.e(TAG, "Invalid item data");
                return false;
            }
            
            // Update item data
            item.setTitle(itemData.getTitle());
            item.setDescription(itemData.getDescription());
            item.setStartingPrice(itemData.getStartingPrice());
            item.setBuyNowPrice(itemData.getBuyNowPrice());
            item.setCurrency(itemData.getCurrency());
            item.setCategoryId(itemData.getCategoryId());
            item.setCondition(itemData.getCondition());
            item.setShippingInfo(itemData.getShippingInfo());
            item.setStartDate(itemData.getStartDate());
            item.setEndDate(itemData.getEndDate());
            item.setNotes(itemData.getNotes());
            item.setMetadata(itemData.getMetadata());
            item.setUpdatedAt(new Date());
            
            // Update tags
            if (itemData.getTags() != null) {
                item.setTags(new ArrayList<>(itemData.getTags()));
            }
            
            // Update storage
            items.put(itemId, item);
            
            Log.i(TAG, "Item updated successfully: " + itemId);
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating item", e);
            return false;
        }
    }
    
    /**
     * Delete item
     */
    public boolean deleteItem(String itemId) {
        Log.i(TAG, "Deleting item: " + itemId);
        
        try {
            Item item = items.get(itemId);
            if (item == null) {
                Log.e(TAG, "Item not found: " + itemId);
                return false;
            }
            
            // Check if item can be deleted
            if (item.getStatus() == ItemStatus.ACTIVE && item.getBidCount() > 0) {
                Log.e(TAG, "Cannot delete item with active bids");
                return false;
            }
            
            // Remove from storage
            items.remove(itemId);
            itemImages.remove(itemId);
            itemViewCounts.remove(itemId);
            itemBidCounts.remove(itemId);
            
            // Remove from user items
            List<String> userItemList = userItems.get(item.getSellerId());
            if (userItemList != null) {
                userItemList.remove(itemId);
            }
            
            // Remove from category items
            if (item.getCategoryId() != null) {
                List<String> categoryItemList = categoryItems.get(item.getCategoryId());
                if (categoryItemList != null) {
                    categoryItemList.remove(itemId);
                }
            }
            
            Log.i(TAG, "Item deleted successfully: " + itemId);
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error deleting item", e);
            return false;
        }
    }
    
    /**
     * Get item by ID
     */
    public Item getItemById(String itemId) {
        Log.d(TAG, "getItemById called with ID: " + itemId);
        Log.d(TAG, "Total items in local storage: " + items.size());
        Log.d(TAG, "Available item IDs: " + items.keySet());
        
        Item item = items.get(itemId);
        if (item != null) {
            Log.i(TAG, "Item found: " + itemId + " - Title: " + item.getTitle());
            Log.d(TAG, "Item images count: " + (item.getImagePaths() != null ? item.getImagePaths().size() : 0));
            // Increment view count
            item.incrementViewCount();
            itemViewCounts.put(itemId, item.getViewCount());
        } else {
            Log.w(TAG, "Item NOT found in local storage: " + itemId);
            Log.w(TAG, "This might mean the item was created but ItemManager was recreated, or the ID doesn't match");
        }
        return item;
    }
    
    /**
     * Store an item in the local cache (useful when fetching from API)
     * @param item The item to store
     */
    public void storeItem(Item item) {
        if (item != null && item.getItemId() != null) {
            items.put(item.getItemId(), item);
            Log.i(TAG, "Stored item in local cache: " + item.getItemId() + " - " + item.getTitle());
            
            // Also store images if available
            if (item.getImagePaths() != null && !item.getImagePaths().isEmpty()) {
                itemImages.put(item.getItemId(), new ArrayList<>(item.getImagePaths()));
            }
            
            // Update user items mapping
            if (item.getSellerId() != null) {
                getOrCreateList(userItems, item.getSellerId()).add(item.getItemId());
            }
            
            // Update category items mapping
            if (item.getCategoryId() != null) {
                getOrCreateList(categoryItems, item.getCategoryId()).add(item.getItemId());
            }
        } else {
            Log.w(TAG, "Attempted to store null item or item with null ID");
        }
    }
    
    /**
     * Get items by seller
     */
    public List<Item> getItemsBySeller(String sellerId) {
        List<String> itemIds = userItems.get(sellerId);
        if (itemIds == null) {
            return new ArrayList<>();
        }
        
        List<Item> result = new ArrayList<>();
        for (String itemId : itemIds) {
            Item item = items.get(itemId);
            if (item != null) {
                result.add(item);
            }
        }
        return result;
    }
    
    // ==================== IMAGE MANAGEMENT ====================
    
    /**
     * Upload item images
     */
    public boolean uploadItemImages(String itemId, List<String> imagePaths) {
        Log.i(TAG, "Uploading images for item: " + itemId);
        
        try {
            Item item = items.get(itemId);
            if (item == null) {
                Log.e(TAG, "Item not found: " + itemId);
                return false;
            }
            
            // Validate images
            if (!validateItemImages(imagePaths)) {
                Log.e(TAG, "Invalid images");
                return false;
            }
            
            // Store image paths
            itemImages.put(itemId, new ArrayList<>(imagePaths));
            item.setImagePaths(new ArrayList<>(imagePaths));
            
            // Update item
            items.put(itemId, item);
            
            Log.i(TAG, "Images uploaded successfully for item: " + itemId);
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error uploading images", e);
            return false;
        }
    }
    
    /**
     * Get item images
     */
    public List<String> getItemImages(String itemId) {
        return itemImages.getOrDefault(itemId, new ArrayList<>());
    }
    
    /**
     * Delete item image
     */
    public boolean deleteItemImage(String itemId, String imagePath) {
        Log.i(TAG, "Deleting image: " + imagePath + " for item: " + itemId);
        
        try {
            List<String> images = itemImages.get(itemId);
            if (images == null) {
                Log.e(TAG, "No images found for item: " + itemId);
                return false;
            }
            
            boolean removed = images.remove(imagePath);
            if (removed) {
                // Update item
                Item item = items.get(itemId);
                if (item != null) {
                    item.setImagePaths(new ArrayList<>(images));
                    items.put(itemId, item);
                }
                
                Log.i(TAG, "Image deleted successfully");
                return true;
            } else {
                Log.w(TAG, "Image not found: " + imagePath);
                return false;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error deleting image", e);
            return false;
        }
    }
    
    /**
     * Generate thumbnail
     */
    public String generateThumbnail(String imagePath) {
        Log.i(TAG, "Generating thumbnail for: " + imagePath);
        
        try {
            // Simulate thumbnail generation
            String thumbnailPath = imagePath.replace(".jpg", "_thumb.jpg");
            
            // In production, this would use an image processing library
            // For now, just return a modified path
            Log.i(TAG, "Thumbnail generated: " + thumbnailPath);
            return thumbnailPath;
            
        } catch (Exception e) {
            Log.e(TAG, "Error generating thumbnail", e);
            return null;
        }
    }
    
    /**
     * Optimize image
     */
    public boolean optimizeImage(String imagePath) {
        Log.i(TAG, "Optimizing image: " + imagePath);
        
        try {
            // Simulate image optimization
            // In production, this would compress and resize the image
            
            Log.i(TAG, "Image optimized: " + imagePath);
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error optimizing image", e);
            return false;
        }
    }
    
    // ==================== CATEGORY MANAGEMENT ====================
    
    /**
     * Get all categories
     */
    public List<Category> getAllCategories() {
        List<Category> result = new ArrayList<>();
        for (Category category : categories.values()) {
            if (category.isActive()) {
                result.add(category);
            }
        }
        // Sort by sort order
        java.util.Collections.sort(result, new Comparator<Category>() {
            @Override
            public int compare(Category c1, Category c2) {
                return Integer.compare(c1.getSortOrder(), c2.getSortOrder());
            }
        });
        return result;
    }
    
    /**
     * Get category by ID
     */
    public Category getCategoryById(String categoryId) {
        return categories.get(categoryId);
    }
    
    /**
     * Assign category to item
     */
    public boolean assignCategory(String itemId, String categoryId) {
        Log.i(TAG, "Assigning category: " + categoryId + " to item: " + itemId);
        
        try {
            Item item = items.get(itemId);
            if (item == null) {
                Log.e(TAG, "Item not found: " + itemId);
                return false;
            }
            
            Category category = categories.get(categoryId);
            if (category == null) {
                Log.e(TAG, "Category not found: " + categoryId);
                return false;
            }
            
            // Remove from old category
            if (item.getCategoryId() != null) {
                List<String> oldCategoryItems = categoryItems.get(item.getCategoryId());
                if (oldCategoryItems != null) {
                    oldCategoryItems.remove(itemId);
                }
            }
            
            // Add to new category
            item.setCategoryId(categoryId);
            item.setCategoryName(category.getName());
            items.put(itemId, item);
            
            getOrCreateList(categoryItems, categoryId).add(itemId);
            
            Log.i(TAG, "Category assigned successfully");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error assigning category", e);
            return false;
        }
    }
    
    /**
     * Get items by category
     */
    public List<Item> getItemsByCategory(String categoryId) {
        List<String> itemIds = categoryItems.get(categoryId);
        if (itemIds == null) {
            return new ArrayList<>();
        }
        
        List<Item> result = new ArrayList<>();
        for (String itemId : itemIds) {
            Item item = items.get(itemId);
            if (item != null) {
                result.add(item);
            }
        }
        return result;
    }
    
    // ==================== ITEM VALIDATION ====================
    
    /**
     * Validate item data
     */
    public boolean validateItemData(ItemData itemData) {
        if (itemData == null) {
            Log.e(TAG, "Item data is null");
            return false;
        }
        
        // Validate title
        if (itemData.getTitle() == null || itemData.getTitle().trim().isEmpty()) {
            Log.e(TAG, "Title is required");
            return false;
        }
        
        if (itemData.getTitle().length() > MAX_TITLE_LENGTH) {
            Log.e(TAG, "Title too long: " + itemData.getTitle().length());
            return false;
        }
        
        // Validate description (optional - but if provided, must meet requirements)
        if (itemData.getDescription() != null && !itemData.getDescription().trim().isEmpty()) {
            String description = itemData.getDescription().trim();
            if (description.length() < 10) {
                Log.e(TAG, "Description too short: " + description.length() + " (must be at least 10 characters if provided)");
            return false;
        }
            if (description.length() > MAX_DESCRIPTION_LENGTH) {
                Log.e(TAG, "Description too long: " + description.length());
            return false;
        }
        }
        // Description is optional, so no error if null or empty
        
        // Validate price (must be at least 0.01 to match backend requirement)
        if (itemData.getStartingPrice() < MIN_PRICE || itemData.getStartingPrice() > MAX_PRICE) {
            Log.e(TAG, "Invalid starting price: " + itemData.getStartingPrice() + " (must be between " + MIN_PRICE + " and " + MAX_PRICE + ")");
            return false;
        }
        
        // Special case: allow 0.01 for donation items (backend requires minimum 0.01)
        // This is handled in PostFragment where donation items are set to 0.01
        
        if (itemData.getBuyNowPrice() != 0 && 
            (itemData.getBuyNowPrice() < itemData.getStartingPrice() || 
             itemData.getBuyNowPrice() > MAX_PRICE)) {
            Log.e(TAG, "Invalid buy now price: " + itemData.getBuyNowPrice());
            return false;
        }
        
        // Validate tags
        if (itemData.getTags() != null && itemData.getTags().size() > MAX_TAGS_PER_ITEM) {
            Log.e(TAG, "Too many tags: " + itemData.getTags().size());
            return false;
        }
        
        return true;
    }
    
    /**
     * Validate item images
     */
    public boolean validateItemImages(List<String> imagePaths) {
        if (imagePaths == null || imagePaths.isEmpty()) {
            Log.e(TAG, "No images provided");
            return false;
        }
        
        if (imagePaths.size() > MAX_IMAGES_PER_ITEM) {
            Log.e(TAG, "Too many images: " + imagePaths.size());
            return false;
        }
        
        // Validate each image path
        for (String imagePath : imagePaths) {
            if (imagePath == null || imagePath.trim().isEmpty()) {
                Log.e(TAG, "Invalid image path");
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Check item permissions
     */
    public boolean checkItemPermissions(String itemId, String userId) {
        Item item = items.get(itemId);
        if (item == null) {
            return false;
        }
        
        return userId.equals(item.getSellerId());
    }
    
    /**
     * Validate item status
     */
    public boolean validateItemStatus(String itemId) {
        Item item = items.get(itemId);
        if (item == null) {
            return false;
        }
        
        // Check if item has ended
        if (item.hasEnded() && item.getStatus() != ItemStatus.ENDED) {
            item.setStatus(ItemStatus.ENDED);
            items.put(itemId, item);
        }
        
        return true;
    }
    
    // ==================== SEARCH & FILTERING ====================
    
    /**
     * Search items
     */
    public List<Item> searchItems(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        String searchQuery = query.toLowerCase().trim();
        
        List<Item> result = new ArrayList<>();
        for (Item item : items.values()) {
            if (item.getStatus() == ItemStatus.ACTIVE) {
                boolean matches = item.getTitle().toLowerCase().contains(searchQuery) ||
                    (item.getDescription() != null && item.getDescription().toLowerCase().contains(searchQuery));
                
                if (!matches && item.getTags() != null) {
                    for (String tag : item.getTags()) {
                        if (tag.toLowerCase().contains(searchQuery)) {
                            matches = true;
                            break;
                        }
                    }
                }
                
                if (matches) {
                    result.add(item);
                }
            }
        }
        // Sort by created date (newest first)
        java.util.Collections.sort(result, new Comparator<Item>() {
            @Override
            public int compare(Item i1, Item i2) {
                Date d1 = i1.getCreatedAt();
                Date d2 = i2.getCreatedAt();
                if (d1 == null && d2 == null) return 0;
                if (d1 == null) return 1;
                if (d2 == null) return -1;
                return d2.compareTo(d1); // Reversed for newest first
            }
        });
        return result;
    }
    
    /**
     * Filter items with comprehensive logging and proper no-op defaults
     */
    public List<Item> filterItems(FilterCriteria criteria) {
        if (criteria == null) {
            android.util.Log.w("ItemManager", "FilterCriteria is null, returning empty list");
            return new ArrayList<>();
        }
        
        // Normalize criteria to convert "null" strings to actual null
        FilterCriteria fc = FilterCriteria.normalize(criteria);
        
        // Log normalized filter criteria for debugging
        android.util.Log.d("ItemManager", "Filter criteria (normalized): " + fc.toString());
        
        // Start with all active/draft items
        List<Item> filteredItems = new ArrayList<>();
        for (Item item : items.values()) {
            if (item.getStatus() == ItemStatus.ACTIVE || item.getStatus() == ItemStatus.DRAFT) {
                filteredItems.add(item);
            }
        }
        
        android.util.Log.d("ItemManager", "Starting with " + filteredItems.size() + " active/draft items");
        
        // Apply filters with proper no-op defaults
        List<Item> filteredResult = new ArrayList<>();
        for (Item item : filteredItems) {
                    // Search filter (no-op if null)
            boolean matchesSearch = true;
            if (fc.getQuery() != null) {
                String queryLower = fc.getQuery().toLowerCase();
                matchesSearch = item.getTitle().toLowerCase().contains(queryLower) ||
                    (item.getDescription() != null && item.getDescription().toLowerCase().contains(queryLower));
                
                if (!matchesSearch && item.getTags() != null) {
                    for (String tag : item.getTags()) {
                        if (tag.toLowerCase().contains(queryLower)) {
                            matchesSearch = true;
                            break;
                        }
                    }
                }
            }
                    
                    // Category filter (no-op if null)
                    boolean matchesCategory = (fc.getCategoryId() == null) || 
                        fc.getCategoryId().equals(item.getCategoryId());
                    
                    // Condition filter (no-op if null)
                    boolean matchesCondition = (fc.getCondition() == null) || 
                        fc.getCondition().equals(item.getCondition());
                    
                    // Price filters (no-op if null, use current price or starting bid)
                    double price = item.getCurrentPrice() > 0 ? item.getCurrentPrice() : 
                                  (item.getStartingPrice() > 0 ? item.getStartingPrice() : 0.0);
                    boolean matchesMin = (fc.getMinPrice() == null) || price >= fc.getMinPrice();
                    boolean matchesMax = (fc.getMaxPrice() == null) || price <= fc.getMaxPrice();
                    
                    // Status filter (no-op if null)
                    boolean matchesStatus = (fc.getStatus() == null) || 
                        fc.getStatus().equals(item.getStatus());
                    
                    // Featured filter (no-op if null)
                    boolean matchesFeatured = (fc.getIsFeatured() == null) || 
                        fc.getIsFeatured() == item.isFeatured();
                    
                    // Trending filter (no-op if null)
                    boolean matchesTrending = (fc.getIsTrending() == null) || 
                        fc.getIsTrending() == item.isTrending();
                    
            if (matchesSearch && matchesCategory && matchesCondition && 
                           matchesMin && matchesMax && matchesStatus && 
                matchesFeatured && matchesTrending) {
                filteredResult.add(item);
            }
        }
        filteredItems = filteredResult;
        
        android.util.Log.d("ItemManager", "After all filters: " + filteredItems.size() + " items");
        
        // Sort items
        Comparator<Item> comparator = getComparator(fc.getSortBy(), fc.getSortOrder());
        java.util.Collections.sort(filteredItems, comparator);
        android.util.Log.d("ItemManager", "After sorting by " + fc.getSortBy() + " " + fc.getSortOrder() + ": " + filteredItems.size() + " items");
        
        // Apply pagination with safe bounds AFTER filtering and sorting
        List<Item> paginatedItems;
        int totalFiltered = filteredItems.size();
        int from = Math.max(0, fc.getOffset());
        int maxLimit = (fc.getLimit() <= 0) ? totalFiltered : Math.min(fc.getLimit(), MAX_ITEMS_PER_PAGE);
        int to = Math.min(totalFiltered, from + maxLimit);
        
        // Ensure from doesn't exceed bounds
        if (from >= totalFiltered) {
            from = totalFiltered;
            to = totalFiltered;
        }
        
        if (from == to) {
            // No items in range
            paginatedItems = new ArrayList<>();
            android.util.Log.d("ItemManager", "Paging: total=" + totalFiltered + " from=" + from + " to=" + to + " page=0 (empty range)");
        } else {
            paginatedItems = filteredItems.subList(from, to);
            android.util.Log.d("ItemManager", "Paging: total=" + totalFiltered + " from=" + from + " to=" + to + " page=" + paginatedItems.size());
        }
        
        // Defensive fallback: if all criteria are null and result is empty, use unfiltered list
        if (fc.isAllCriteriaNull() && paginatedItems.isEmpty() && !items.isEmpty()) {
            android.util.Log.w("ItemManager", "WARNING: Empty result with default filters - using unfiltered list as fallback");
            paginatedItems = new ArrayList<>(items.values());
        }
        
        android.util.Log.d("ItemManager", "Final result: " + paginatedItems.size() + " items");
        
        return paginatedItems;
    }
    
    /**
     * Get featured items
     */
    public List<Item> getFeaturedItems() {
        List<Item> result = new ArrayList<>();
        for (Item item : items.values()) {
            if (item.getStatus() == ItemStatus.ACTIVE && item.isFeatured()) {
                result.add(item);
            }
        }
        // Sort by created date (newest first) and limit to 20
        java.util.Collections.sort(result, new Comparator<Item>() {
            @Override
            public int compare(Item i1, Item i2) {
                Date d1 = i1.getCreatedAt();
                Date d2 = i2.getCreatedAt();
                if (d1 == null && d2 == null) return 0;
                if (d1 == null) return 1;
                if (d2 == null) return -1;
                return d2.compareTo(d1); // Reversed for newest first
            }
        });
        if (result.size() > 20) {
            result = result.subList(0, 20);
        }
        return result;
    }
    
    /**
     * Get trending items
     */
    public List<Item> getTrendingItems() {
        List<Item> result = new ArrayList<>();
        for (Item item : items.values()) {
            if (item.getStatus() == ItemStatus.ACTIVE && item.isTrending()) {
                result.add(item);
            }
        }
        // Sort by view count (highest first) and limit to 20
        java.util.Collections.sort(result, new Comparator<Item>() {
            @Override
            public int compare(Item i1, Item i2) {
                return Integer.compare(i2.getViewCount(), i1.getViewCount()); // Reversed for highest first
            }
        });
        if (result.size() > 20) {
            result = result.subList(0, 20);
        }
        return result;
    }
    
    /**
     * Get all active items
     */
    public List<Item> getAllActiveItems() {
        List<Item> result = new ArrayList<>();
        for (Item item : items.values()) {
            // Only include items that are ACTIVE and not ended
            if (item.getStatus() == ItemStatus.ACTIVE && !item.hasEnded()) {
                result.add(item);
            }
        }
        // Sort by created date (newest first)
        java.util.Collections.sort(result, new Comparator<Item>() {
            @Override
            public int compare(Item i1, Item i2) {
                Date d1 = i1.getCreatedAt();
                Date d2 = i2.getCreatedAt();
                if (d1 == null && d2 == null) return 0;
                if (d1 == null) return 1;
                if (d2 == null) return -1;
                return d2.compareTo(d1); // Reversed for newest first
            }
        });
        return result;
    }
    
    /**
     * Get all browsable items (both ACTIVE and DRAFT, excluding ended auctions)
     */
    public List<Item> getAllBrowsableItems() {
        List<Item> result = new ArrayList<>();
        for (Item item : items.values()) {
            // Include ACTIVE items that haven't ended, or DRAFT items
            if ((item.getStatus() == ItemStatus.ACTIVE && !item.hasEnded()) || 
                item.getStatus() == ItemStatus.DRAFT) {
                result.add(item);
            }
        }
        // Sort by created date (newest first)
        java.util.Collections.sort(result, new Comparator<Item>() {
            @Override
            public int compare(Item i1, Item i2) {
                Date d1 = i1.getCreatedAt();
                Date d2 = i2.getCreatedAt();
                if (d1 == null && d2 == null) return 0;
                if (d1 == null) return 1;
                if (d2 == null) return -1;
                return d2.compareTo(d1); // Reversed for newest first
            }
        });
        return result;
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Initialize default categories
     */
    private void initializeDefaultCategories() {
        // Electronics
        Category electronics = new Category("Electronics", "Electronic devices and gadgets");
        electronics.setCategoryId("electronics");
        electronics.setSortOrder(1);
        categories.put("electronics", electronics);
        
        // Fashion
        Category fashion = new Category("Fashion", "Clothing and accessories");
        fashion.setCategoryId("fashion");
        fashion.setSortOrder(2);
        categories.put("fashion", fashion);
        
        // Home & Garden
        Category homeGarden = new Category("Home & Garden", "Home improvement and garden items");
        homeGarden.setCategoryId("home_garden");
        homeGarden.setSortOrder(3);
        categories.put("home_garden", homeGarden);
        
        // Sports
        Category sports = new Category("Sports", "Sports equipment and gear");
        sports.setCategoryId("sports");
        sports.setSortOrder(4);
        categories.put("sports", sports);
        
        // Books
        Category books = new Category("Books", "Books and educational materials");
        books.setCategoryId("books");
        books.setSortOrder(5);
        categories.put("books", books);
        
        // Collectibles
        Category collectibles = new Category("Collectibles", "Collectible items and antiques");
        collectibles.setCategoryId("collectibles");
        collectibles.setSortOrder(6);
        categories.put("collectibles", collectibles);
        
        Log.i(TAG, "Default categories initialized");
    }
    
    /**
     * Get comparator for sorting
     */
    private Comparator<Item> getComparator(String sortBy, String sortOrder) {
        final String sortField = sortBy != null ? sortBy : "createdAt";
        final boolean ascending = "ASC".equalsIgnoreCase(sortOrder);
        
        Comparator<Item> comparator = new Comparator<Item>() {
            @Override
            public int compare(Item i1, Item i2) {
                int result = 0;
                
                switch (sortField) {
            case "title":
                        String t1 = i1.getTitle();
                        String t2 = i2.getTitle();
                        if (t1 == null && t2 == null) result = 0;
                        else if (t1 == null) result = 1;
                        else if (t2 == null) result = -1;
                        else result = t1.compareTo(t2);
                break;
            case "price":
                        result = Double.compare(i1.getCurrentPrice(), i2.getCurrentPrice());
                break;
            case "viewCount":
                        result = Integer.compare(i1.getViewCount(), i2.getViewCount());
                break;
            case "bidCount":
                        result = Integer.compare(i1.getBidCount(), i2.getBidCount());
                break;
            case "endDate":
                        Date d1 = i1.getEndDate();
                        Date d2 = i2.getEndDate();
                        if (d1 == null && d2 == null) result = 0;
                        else if (d1 == null) result = 1;
                        else if (d2 == null) result = -1;
                        else result = d1.compareTo(d2);
                break;
                    default: // createdAt
                        Date c1 = i1.getCreatedAt();
                        Date c2 = i2.getCreatedAt();
                        if (c1 == null && c2 == null) result = 0;
                        else if (c1 == null) result = 1;
                        else if (c2 == null) result = -1;
                        else result = c1.compareTo(c2);
                break;
        }
        
                return ascending ? result : -result; // Reverse for descending
            }
        };
        
            return comparator;
    }
    
    /**
     * Start cleanup task
     */
    private void startCleanupTask() {
        scheduledExecutor.scheduleWithFixedDelay(() -> {
            try {
                cleanupExpiredItems();
                updateTrendingItems();
            } catch (Exception e) {
                Log.e(TAG, "Cleanup task failed", e);
            }
        }, 1, 1, TimeUnit.HOURS);
    }
    
    /**
     * Cleanup expired items
     */
    private void cleanupExpiredItems() {
        int cleaned = 0;
        
        for (Item item : items.values()) {
            if (item.hasEnded() && item.getStatus() == ItemStatus.ACTIVE) {
                item.setStatus(ItemStatus.ENDED);
                cleaned++;
            }
        }
        
        if (cleaned > 0) {
            Log.i(TAG, "Cleaned up " + cleaned + " expired items");
        }
    }
    
    /**
     * Update trending items
     */
    private void updateTrendingItems() {
        // Reset all trending flags
        for (Item item : items.values()) {
            item.setTrending(false);
        }
        
        // Set top 10 most viewed items as trending
        List<Item> trendingItems = new ArrayList<>();
        for (Item item : items.values()) {
            if (item.getStatus() == ItemStatus.ACTIVE) {
                trendingItems.add(item);
            }
        }
        // Sort by view count (highest first) and limit to 10
        java.util.Collections.sort(trendingItems, new Comparator<Item>() {
            @Override
            public int compare(Item i1, Item i2) {
                return Integer.compare(i2.getViewCount(), i1.getViewCount()); // Reversed for highest first
            }
        });
        if (trendingItems.size() > 10) {
            trendingItems = trendingItems.subList(0, 10);
        }
        
        for (Item item : trendingItems) {
            item.setTrending(true);
        }
        
        Log.d(TAG, "Updated trending items");
    }
    
    /**
     * Initialize sample data for testing
     */
    private void initializeSampleData() {
        // Sample Electronics Items
        Item laptop = new Item("MacBook Pro 13-inch", "Excellent condition MacBook Pro with M1 chip", 25000.0, "seller1");
        laptop.setCategoryId("electronics");
        laptop.setCategoryName("Electronics");
        laptop.setCondition("Like New");
        laptop.setSellerName("TechGuru_Manila");
        laptop.setStatus(ItemStatus.ACTIVE);
        laptop.setCurrentPrice(25000.0);
        laptop.setBidCount(5);
        laptop.setViewCount(120);
        laptop.setFeatured(true);
        laptop.setEndDate(new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000)); // 7 days from now
        items.put(laptop.getItemId(), laptop);
        
        // Sample items for My Listings testing
        // Add items for test user (you can change this to any email for testing)
        String testUserEmail = "test@example.com";
        
        Item vintageJacket = new Item("Vintage Leather Jacket", "Classic vintage leather jacket in excellent condition", 120.0, testUserEmail);
        vintageJacket.setCategoryId("clothing");
        vintageJacket.setCategoryName("Clothing");
        vintageJacket.setCondition("Good");
        vintageJacket.setSellerName("Test User");
        vintageJacket.setStatus(ItemStatus.ACTIVE);
        vintageJacket.setCurrentPrice(120.0);
        vintageJacket.setBidCount(3);
        vintageJacket.setViewCount(45);
        vintageJacket.setEndDate(new Date(System.currentTimeMillis() + 12 * 60 * 60 * 1000)); // 12 hours from now
        items.put(vintageJacket.getItemId(), vintageJacket);
        getOrCreateList(userItems, testUserEmail).add(vintageJacket.getItemId());
        
        Item antiqueWatch = new Item("Antique Pocket Watch", "Beautiful antique pocket watch from the 1800s", 85.0, testUserEmail);
        antiqueWatch.setCategoryId("collectibles");
        antiqueWatch.setCategoryName("Collectibles");
        antiqueWatch.setCondition("Fair");
        antiqueWatch.setSellerName("Test User");
        antiqueWatch.setStatus(ItemStatus.PAUSED);
        antiqueWatch.setCurrentPrice(85.0);
        antiqueWatch.setBidCount(1);
        antiqueWatch.setViewCount(23);
        items.put(antiqueWatch.getItemId(), antiqueWatch);
        userItems.computeIfAbsent(testUserEmail, k -> new ArrayList<>()).add(antiqueWatch.getItemId());
        
        Item signedBaseball = new Item("Signed Baseball", "Baseball signed by famous player", 250.0, testUserEmail);
        signedBaseball.setCategoryId("sports");
        signedBaseball.setCategoryName("Sports");
        signedBaseball.setCondition("Good");
        signedBaseball.setSellerName("Test User");
        signedBaseball.setStatus(ItemStatus.SOLD);
        signedBaseball.setCurrentPrice(250.0);
        signedBaseball.setBidCount(7);
        signedBaseball.setViewCount(89);
        items.put(signedBaseball.getItemId(), signedBaseball);
        userItems.computeIfAbsent(testUserEmail, k -> new ArrayList<>()).add(signedBaseball.getItemId());
        
        Item rareCoins = new Item("Rare Coin Collection", "Collection of rare coins from different countries", 500.0, testUserEmail);
        rareCoins.setCategoryId("collectibles");
        rareCoins.setCategoryName("Collectibles");
        rareCoins.setCondition("Excellent");
        rareCoins.setSellerName("Test User");
        rareCoins.setStatus(ItemStatus.DRAFT);
        rareCoins.setCurrentPrice(500.0);
        rareCoins.setBidCount(0);
        rareCoins.setViewCount(0);
        items.put(rareCoins.getItemId(), rareCoins);
        userItems.computeIfAbsent(testUserEmail, k -> new ArrayList<>()).add(rareCoins.getItemId());
        
        Item phone = new Item("iPhone 14 Pro", "Brand new iPhone 14 Pro 128GB", 45000.0, "seller2");
        phone.setCategoryId("electronics");
        phone.setCategoryName("Electronics");
        phone.setCondition("New");
        phone.setSellerName("MobileDeals_QC");
        phone.setStatus(ItemStatus.ACTIVE);
        phone.setCurrentPrice(45000.0);
        phone.setBidCount(12);
        phone.setViewCount(200);
        phone.setTrending(true);
        phone.setEndDate(new Date(System.currentTimeMillis() + 5 * 24 * 60 * 60 * 1000)); // 5 days from now
        items.put(phone.getItemId(), phone);
        
        Item camera = new Item("Canon EOS R5", "Professional mirrorless camera", 180000.0, "seller3");
        camera.setCategoryId("electronics");
        camera.setCategoryName("Electronics");
        camera.setCondition("Excellent");
        camera.setSellerName("PhotoPro_Makati");
        camera.setStatus(ItemStatus.ACTIVE);
        camera.setCurrentPrice(180000.0);
        camera.setBidCount(3);
        camera.setViewCount(85);
        camera.setEndDate(new Date(System.currentTimeMillis() + 10 * 24 * 60 * 60 * 1000)); // 10 days from now
        items.put(camera.getItemId(), camera);
        
        // Sample Fashion Items
        Item watch = new Item("Rolex Submariner", "Vintage Rolex Submariner watch", 80000.0, "seller4");
        watch.setCategoryId("fashion");
        watch.setCategoryName("Fashion");
        watch.setCondition("Good");
        watch.setSellerName("LuxuryWatches_Taguig");
        watch.setStatus(ItemStatus.ACTIVE);
        watch.setCurrentPrice(80000.0);
        watch.setBidCount(8);
        watch.setViewCount(150);
        watch.setFeatured(true);
        watch.setEndDate(new Date(System.currentTimeMillis() + 3 * 24 * 60 * 60 * 1000)); // 3 days from now
        items.put(watch.getItemId(), watch);
        
        Item shoes = new Item("Nike Air Jordan 1", "Classic red and white colorway", 8000.0, "seller5");
        shoes.setCategoryId("fashion");
        shoes.setCategoryName("Fashion");
        shoes.setCondition("Like New");
        shoes.setSellerName("SneakerHead_Pasig");
        shoes.setStatus(ItemStatus.ACTIVE);
        shoes.setCurrentPrice(8000.0);
        shoes.setBidCount(15);
        shoes.setViewCount(300);
        shoes.setTrending(true);
        shoes.setEndDate(new Date(System.currentTimeMillis() + 2 * 24 * 60 * 60 * 1000)); // 2 days from now
        items.put(shoes.getItemId(), shoes);
        
        // Sample Home & Garden Items
        Item furniture = new Item("Vintage Wooden Dining Table", "Beautiful antique dining table", 15000.0, "seller6");
        furniture.setCategoryId("home_garden");
        furniture.setCategoryName("Home & Garden");
        furniture.setCondition("Good");
        furniture.setSellerName("AntiqueFinds_Marikina");
        furniture.setStatus(ItemStatus.ACTIVE);
        furniture.setCurrentPrice(15000.0);
        furniture.setBidCount(4);
        furniture.setViewCount(90);
        furniture.setEndDate(new Date(System.currentTimeMillis() + 6 * 24 * 60 * 60 * 1000)); // 6 days from now
        items.put(furniture.getItemId(), furniture);
        
        // Sample Sports Items
        Item bike = new Item("Mountain Bike", "Trek mountain bike in excellent condition", 25000.0, "seller7");
        bike.setCategoryId("sports");
        bike.setCategoryName("Sports");
        bike.setCondition("Very Good");
        bike.setSellerName("SportsGear_Mandaluyong");
        bike.setStatus(ItemStatus.ACTIVE);
        bike.setCurrentPrice(25000.0);
        bike.setBidCount(6);
        bike.setViewCount(110);
        bike.setEndDate(new Date(System.currentTimeMillis() + 4 * 24 * 60 * 60 * 1000)); // 4 days from now
        items.put(bike.getItemId(), bike);
        
        // Sample Books
        Item book = new Item("Programming Book Collection", "Set of 5 programming books", 2000.0, "seller8");
        book.setCategoryId("books");
        book.setCategoryName("Books");
        book.setCondition("Good");
        book.setSellerName("BookLover_SanJuan");
        book.setStatus(ItemStatus.ACTIVE);
        book.setCurrentPrice(2000.0);
        book.setBidCount(2);
        book.setViewCount(45);
        book.setEndDate(new Date(System.currentTimeMillis() + 8 * 24 * 60 * 60 * 1000)); // 8 days from now
        items.put(book.getItemId(), book);
        
        Log.d(TAG, "Initialized " + items.size() + " sample items");
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        executorService.shutdown();
        scheduledExecutor.shutdown();
        
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            scheduledExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
