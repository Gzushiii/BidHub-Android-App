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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private static final double MIN_PRICE = 1.0;
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
    private Context context;
    
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
        initializeSampleData();
        startCleanupTask();
    }
    
    public static synchronized ItemManager getInstance(Context context) {
        if (instance == null) {
            instance = new ItemManager(context);
        }
        return instance;
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
                    
                    // Also store locally for offline access
                    Item item = new Item();
                    item.setItemId(UUID.randomUUID().toString());
                    item.setTitle(itemData.getTitle());
                    item.setDescription(itemData.getDescription());
                    item.setStartingPrice(itemData.getStartingPrice());
                    item.setCurrentPrice(itemData.getStartingPrice());
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
                    
                    Log.i(TAG, "Item created successfully: " + item.getItemId());
                    return true;
                } else {
                    Log.e(TAG, "Backend API error: " + response.getMessage());
                    // Don't fallback to local storage - return false to show error
                    return false;
                }
            } catch (Exception apiException) {
                Log.e(TAG, "Backend API call failed", apiException);
                // Don't fallback to local storage - return false to show error
                return false;
            }
            
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating item", e);
            return false;
        }
    }
    
    /**
     * Save item as draft (local only, not posted to backend)
     */
    public boolean saveDraftItem(ItemData itemData, String sellerEmail) {
        Log.i(TAG, "Saving item as draft for seller: " + sellerEmail);
        
        try {
            // Validate item data
            if (!validateItemData(itemData)) {
                Log.e(TAG, "Invalid item data");
                return false;
            }
            
            // Create item locally only with DRAFT status
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
            
            Log.i(TAG, "Item saved as draft successfully: " + item.getItemId());
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error saving draft item", e);
            return false;
        }
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
        Item item = items.get(itemId);
        if (item != null) {
            // Increment view count
            item.incrementViewCount();
            itemViewCounts.put(itemId, item.getViewCount());
        }
        return item;
    }
    
    /**
     * Get items by seller
     */
    public List<Item> getItemsBySeller(String sellerId) {
        List<String> itemIds = userItems.get(sellerId);
        if (itemIds == null) {
            return new ArrayList<>();
        }
        
        return itemIds.stream()
                .map(items::get)
                .filter(item -> item != null)
                .collect(Collectors.toList());
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
        return categories.values().stream()
                .filter(Category::isActive)
                .sorted(Comparator.comparing(Category::getSortOrder))
                .collect(Collectors.toList());
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
            
            categoryItems.computeIfAbsent(categoryId, k -> new ArrayList<>()).add(itemId);
            
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
        
        return itemIds.stream()
                .map(items::get)
                .filter(item -> item != null)
                .collect(Collectors.toList());
    }
    
    // ==================== ITEM VALIDATION ====================
    
    /**
     * Validate item data
     */
    public boolean validateItemData(ItemData itemData) {
        Log.d(TAG, "=== VALIDATING ITEM DATA ===");
        
        if (itemData == null) {
            Log.e(TAG, "Item data is null");
            return false;
        }
        
        // Validate title
        Log.d(TAG, "Validating title: '" + itemData.getTitle() + "' (length: " + (itemData.getTitle() != null ? itemData.getTitle().length() : 0) + ")");
        if (itemData.getTitle() == null || itemData.getTitle().trim().isEmpty()) {
            Log.e(TAG, "Title is required");
            return false;
        }
        
        if (itemData.getTitle().length() > MAX_TITLE_LENGTH) {
            Log.e(TAG, "Title too long: " + itemData.getTitle().length() + " > " + MAX_TITLE_LENGTH);
            return false;
        }
        
        // Validate description
        Log.d(TAG, "Validating description: '" + itemData.getDescription() + "' (length: " + (itemData.getDescription() != null ? itemData.getDescription().length() : 0) + ")");
        if (itemData.getDescription() == null || itemData.getDescription().trim().isEmpty()) {
            Log.e(TAG, "Description is required");
            return false;
        }
        
        if (itemData.getDescription().length() > MAX_DESCRIPTION_LENGTH) {
            Log.e(TAG, "Description too long: " + itemData.getDescription().length() + " > " + MAX_DESCRIPTION_LENGTH);
            return false;
        }
        
        // Validate price
        Log.d(TAG, "Validating starting price: " + itemData.getStartingPrice() + " (MIN: " + MIN_PRICE + ", MAX: " + MAX_PRICE + ")");
        if (itemData.getStartingPrice() < MIN_PRICE || itemData.getStartingPrice() > MAX_PRICE) {
            Log.e(TAG, "Invalid starting price: " + itemData.getStartingPrice());
            return false;
        }
        
        Log.d(TAG, "Validating buy now price: " + itemData.getBuyNowPrice());
        if (itemData.getBuyNowPrice() != 0 && 
            (itemData.getBuyNowPrice() < itemData.getStartingPrice() || 
             itemData.getBuyNowPrice() > MAX_PRICE)) {
            Log.e(TAG, "Invalid buy now price: " + itemData.getBuyNowPrice());
            return false;
        }
        
        // Validate category ID
        Log.d(TAG, "Validating category ID: '" + itemData.getCategoryId() + "'");
        if (itemData.getCategoryId() == null || itemData.getCategoryId().trim().isEmpty()) {
            Log.e(TAG, "Category ID is required");
            return false;
        }
        
        // Validate tags
        Log.d(TAG, "Validating tags: " + (itemData.getTags() != null ? itemData.getTags().size() : 0) + " (MAX: " + MAX_TAGS_PER_ITEM + ")");
        if (itemData.getTags() != null && itemData.getTags().size() > MAX_TAGS_PER_ITEM) {
            Log.e(TAG, "Too many tags: " + itemData.getTags().size());
            return false;
        }
        
        Log.d(TAG, "=== ITEM DATA VALIDATION PASSED ===");
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
        
        return items.values().stream()
                .filter(item -> item.getStatus() == ItemStatus.ACTIVE)
                .filter(item -> 
                    item.getTitle().toLowerCase().contains(searchQuery) ||
                    item.getDescription().toLowerCase().contains(searchQuery) ||
                    (item.getTags() != null && item.getTags().stream()
                        .anyMatch(tag -> tag.toLowerCase().contains(searchQuery)))
                )
                .sorted(Comparator.comparing(Item::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }
    
    /**
     * Filter items
     */
    public List<Item> filterItems(FilterCriteria criteria) {
        if (criteria == null) {
            return new ArrayList<>();
        }
        
        List<Item> filteredItems = items.values().stream()
                .filter(item -> item.getStatus() == ItemStatus.ACTIVE || item.getStatus() == ItemStatus.DRAFT)
                .collect(Collectors.toList());
        
        // Apply filters
        if (criteria.getQuery() != null && !criteria.getQuery().trim().isEmpty()) {
            String searchQuery = criteria.getQuery().toLowerCase().trim();
            filteredItems = filteredItems.stream()
                    .filter(item -> 
                        item.getTitle().toLowerCase().contains(searchQuery) ||
                        item.getDescription().toLowerCase().contains(searchQuery) ||
                        (item.getTags() != null && item.getTags().stream()
                            .anyMatch(tag -> tag.toLowerCase().contains(searchQuery)))
                    )
                    .collect(Collectors.toList());
        }
        
        if (criteria.getCategoryId() != null) {
            filteredItems = filteredItems.stream()
                    .filter(item -> criteria.getCategoryId().equals(item.getCategoryId()))
                    .collect(Collectors.toList());
        }
        
        if (criteria.getMinPrice() != null) {
            filteredItems = filteredItems.stream()
                    .filter(item -> item.getCurrentPrice() >= criteria.getMinPrice())
                    .collect(Collectors.toList());
        }
        
        if (criteria.getMaxPrice() != null) {
            filteredItems = filteredItems.stream()
                    .filter(item -> item.getCurrentPrice() <= criteria.getMaxPrice())
                    .collect(Collectors.toList());
        }
        
        if (criteria.getCondition() != null) {
            filteredItems = filteredItems.stream()
                    .filter(item -> criteria.getCondition().equals(item.getCondition()))
                    .collect(Collectors.toList());
        }
        
        
        if (criteria.getIsFeatured() != null) {
            filteredItems = filteredItems.stream()
                    .filter(item -> criteria.getIsFeatured() == item.isFeatured())
                    .collect(Collectors.toList());
        }
        
        if (criteria.getIsTrending() != null) {
            filteredItems = filteredItems.stream()
                    .filter(item -> criteria.getIsTrending() == item.isTrending())
                    .collect(Collectors.toList());
        }
        
        // Sort items
        Comparator<Item> comparator = getComparator(criteria.getSortBy(), criteria.getSortOrder());
        filteredItems.sort(comparator);
        
        // Apply pagination
        int offset = criteria.getOffset();
        int limit = Math.min(criteria.getLimit(), MAX_ITEMS_PER_PAGE);
        
        if (offset >= filteredItems.size()) {
            return new ArrayList<>();
        }
        
        int endIndex = Math.min(offset + limit, filteredItems.size());
        return filteredItems.subList(offset, endIndex);
    }
    
    /**
     * Get featured items
     */
    public List<Item> getFeaturedItems() {
        return items.values().stream()
                .filter(item -> item.getStatus() == ItemStatus.ACTIVE)
                .filter(Item::isFeatured)
                .sorted(Comparator.comparing(Item::getCreatedAt).reversed())
                .limit(20)
                .collect(Collectors.toList());
    }
    
    /**
     * Get trending items
     */
    public List<Item> getTrendingItems() {
        return items.values().stream()
                .filter(item -> item.getStatus() == ItemStatus.ACTIVE)
                .filter(Item::isTrending)
                .sorted(Comparator.comparing(Item::getViewCount).reversed())
                .limit(20)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all active items
     */
    public List<Item> getAllActiveItems() {
        return items.values().stream()
                .filter(item -> item.getStatus() == ItemStatus.ACTIVE)
                .sorted(Comparator.comparing(Item::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }
    
    /**
     * Get all browsable items (both ACTIVE and DRAFT)
     */
    public List<Item> getAllBrowsableItems() {
        return items.values().stream()
                .filter(item -> item.getStatus() == ItemStatus.ACTIVE || 
                               item.getStatus() == ItemStatus.DRAFT)
                .sorted(Comparator.comparing(Item::getCreatedAt).reversed())
                .collect(Collectors.toList());
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
        Comparator<Item> comparator;
        
        switch (sortBy != null ? sortBy : "createdAt") {
            case "title":
                comparator = Comparator.comparing(Item::getTitle);
                break;
            case "price":
                comparator = Comparator.comparing(Item::getCurrentPrice);
                break;
            case "viewCount":
                comparator = Comparator.comparing(Item::getViewCount);
                break;
            case "bidCount":
                comparator = Comparator.comparing(Item::getBidCount);
                break;
            case "endDate":
                comparator = Comparator.comparing(Item::getEndDate, Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            default:
                comparator = Comparator.comparing(Item::getCreatedAt);
                break;
        }
        
        if ("ASC".equalsIgnoreCase(sortOrder)) {
            return comparator;
        } else {
            return comparator.reversed();
        }
    }
    
    /**
     * Start cleanup task
     */
    private void startCleanupTask() {
        scheduledExecutor.scheduleAtFixedRate(() -> {
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
        List<Item> trendingItems = items.values().stream()
                .filter(item -> item.getStatus() == ItemStatus.ACTIVE)
                .sorted(Comparator.comparing(Item::getViewCount).reversed())
                .limit(10)
                .collect(Collectors.toList());
        
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
        userItems.computeIfAbsent(testUserEmail, k -> new ArrayList<>()).add(vintageJacket.getItemId());
        
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
