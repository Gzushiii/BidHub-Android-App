package com.cc106.bidhub.items;

import android.content.Context;
import android.util.Log;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;

/**
 * Item Management System Usage Examples
 * Demonstrates how to use the comprehensive item management system
 */
public class ItemExample {
    private static final String TAG = "ItemExample";
    
    private ItemManager itemManager;
    private ItemIntegration itemIntegration;
    private Context context;
    
    public ItemExample(Context context) {
        this.context = context;
        this.itemManager = ItemManager.getInstance(context);
        this.itemIntegration = new ItemIntegration(context);
    }
    
    /**
     * Example: Create and manage items
     */
    public void itemManagementExample() {
        Log.i(TAG, "=== Item Management Example ===");
        
        String sellerId = "seller123";
        
        // Create item data
        ItemData itemData = new ItemData();
        itemData.setTitle("Vintage Camera");
        itemData.setDescription("Beautiful vintage camera in excellent condition");
        itemData.setStartingPrice(500.0);
        itemData.setBuyNowPrice(800.0);
        itemData.setCategoryId("electronics");
        itemData.setCondition("Excellent");
        itemData.setLocation("Manila, Philippines");
        itemData.setShippingInfo("Free shipping nationwide");
        itemData.addTag("vintage");
        itemData.addTag("camera");
        itemData.addTag("photography");
        
        // Create item
        boolean created = itemManager.createItem(itemData, sellerId);
        Log.i(TAG, "Item created: " + created);
        
        if (created) {
            // Get seller's items
            List<Item> sellerItems = itemManager.getItemsBySeller(sellerId);
            Log.i(TAG, "Seller has " + sellerItems.size() + " items");
            
            if (!sellerItems.isEmpty()) {
                Item item = sellerItems.get(0);
                Log.i(TAG, "Item: " + item.getTitle() + " - " + item.getCurrentPrice());
                
                // Update item
                itemData.setTitle("Vintage Camera - Updated");
                itemData.setDescription("Beautiful vintage camera in excellent condition. Recently serviced.");
                
                boolean updated = itemManager.updateItem(item.getItemId(), itemData);
                Log.i(TAG, "Item updated: " + updated);
            }
        }
    }
    
    /**
     * Example: Image management
     */
    public void imageManagementExample() {
        Log.i(TAG, "=== Image Management Example ===");
        
        String itemId = "item123";
        List<String> imagePaths = new ArrayList<>();
        imagePaths.add("/path/to/image1.jpg");
        imagePaths.add("/path/to/image2.jpg");
        imagePaths.add("/path/to/image3.jpg");
        
        // Upload images
        boolean uploaded = itemManager.uploadItemImages(itemId, imagePaths);
        Log.i(TAG, "Images uploaded: " + uploaded);
        
        if (uploaded) {
            // Get item images
            List<String> images = itemManager.getItemImages(itemId);
            Log.i(TAG, "Item has " + images.size() + " images");
            
            // Generate thumbnail
            String thumbnail = itemManager.generateThumbnail(images.get(0));
            Log.i(TAG, "Thumbnail generated: " + thumbnail);
            
            // Optimize image
            boolean optimized = itemManager.optimizeImage(images.get(0));
            Log.i(TAG, "Image optimized: " + optimized);
            
            // Delete image
            boolean deleted = itemManager.deleteItemImage(itemId, images.get(0));
            Log.i(TAG, "Image deleted: " + deleted);
        }
    }
    
    /**
     * Example: Category management
     */
    public void categoryManagementExample() {
        Log.i(TAG, "=== Category Management Example ===");
        
        // Get all categories
        List<Category> categories = itemManager.getAllCategories();
        Log.i(TAG, "Available categories: " + categories.size());
        
        for (Category category : categories) {
            Log.i(TAG, "Category: " + category.getName() + " - " + category.getDescription());
        }
        
        // Get category by ID
        Category electronics = itemManager.getCategoryById("electronics");
        if (electronics != null) {
            Log.i(TAG, "Electronics category: " + electronics.getName());
            
            // Get items in category
            List<Item> categoryItems = itemManager.getItemsByCategory("electronics");
            Log.i(TAG, "Electronics items: " + categoryItems.size());
        }
        
        // Assign category to item
        String itemId = "item123";
        String categoryId = "electronics";
        boolean assigned = itemManager.assignCategory(itemId, categoryId);
        Log.i(TAG, "Category assigned: " + assigned);
    }
    
    /**
     * Example: Item validation
     */
    public void itemValidationExample() {
        Log.i(TAG, "=== Item Validation Example ===");
        
        // Test valid item data
        ItemData validItemData = new ItemData();
        validItemData.setTitle("Test Item");
        validItemData.setDescription("This is a test item");
        validItemData.setStartingPrice(100.0);
        
        boolean isValid = itemManager.validateItemData(validItemData);
        Log.i(TAG, "Valid item data: " + isValid);
        
        // Test invalid item data
        ItemData invalidItemData = new ItemData();
        invalidItemData.setTitle(""); // Empty title
        invalidItemData.setDescription("This is a test item");
        invalidItemData.setStartingPrice(-10.0); // Negative price
        
        boolean isInvalid = itemManager.validateItemData(invalidItemData);
        Log.i(TAG, "Invalid item data: " + isInvalid);
        
        // Test image validation
        List<String> validImages = new ArrayList<>();
        validImages.add("/path/to/image1.jpg");
        validImages.add("/path/to/image2.jpg");
        
        boolean validImagesResult = itemManager.validateItemImages(validImages);
        Log.i(TAG, "Valid images: " + validImagesResult);
        
        // Test permission check
        String itemId = "item123";
        String userId = "user123";
        boolean hasPermission = itemManager.checkItemPermissions(itemId, userId);
        Log.i(TAG, "Has permission: " + hasPermission);
    }
    
    /**
     * Example: Search and filtering
     */
    public void searchFilteringExample() {
        Log.i(TAG, "=== Search and Filtering Example ===");
        
        // Search items
        List<Item> searchResults = itemManager.searchItems("camera");
        Log.i(TAG, "Search results for 'camera': " + searchResults.size());
        
        // Filter items
        FilterCriteria criteria = new FilterCriteria();
        criteria.setQuery("vintage");
        criteria.setCategoryId("electronics");
        criteria.setMinPrice(100.0);
        criteria.setMaxPrice(1000.0);
        criteria.setSortBy("price");
        criteria.setSortOrder("ASC");
        
        List<Item> filteredItems = itemManager.filterItems(criteria);
        Log.i(TAG, "Filtered items: " + filteredItems.size());
        
        // Get featured items
        List<Item> featuredItems = itemManager.getFeaturedItems();
        Log.i(TAG, "Featured items: " + featuredItems.size());
        
        // Get trending items
        List<Item> trendingItems = itemManager.getTrendingItems();
        Log.i(TAG, "Trending items: " + trendingItems.size());
    }
    
    /**
     * Example: Integration features
     */
    public void integrationExample() {
        Log.i(TAG, "=== Integration Example ===");
        
        String sellerId = "seller123";
        String userId = "user123";
        
        // Create item with validation
        ItemData itemData = new ItemData();
        itemData.setTitle("Integration Test Item");
        itemData.setDescription("Testing integration features");
        itemData.setStartingPrice(200.0);
        itemData.setCategoryId("electronics");
        
        String itemId = itemIntegration.createItemWithValidation(itemData, sellerId);
        if (itemId != null) {
            Log.i(TAG, "Item created with validation: " + itemId);
            
            // Upload images with validation
            List<String> imagePaths = new ArrayList<>();
            imagePaths.add("/path/to/test_image.jpg");
            
            boolean imagesUploaded = itemIntegration.uploadImagesWithValidation(itemId, imagePaths, sellerId);
            Log.i(TAG, "Images uploaded with validation: " + imagesUploaded);
            
            // Search with filters
            List<Item> searchResults = itemIntegration.searchItemsWithFilters("test", "electronics", 100.0, 500.0);
            Log.i(TAG, "Search with filters: " + (searchResults != null ? searchResults.size() : 0));
            
            // Get item with view tracking
            Item item = itemIntegration.getItemWithViewTracking(itemId);
            if (item != null) {
                Log.i(TAG, "Item viewed: " + item.getTitle() + " (views: " + item.getViewCount() + ")");
            }
        }
    }
    
    /**
     * Example: Statistics and analytics
     */
    public void statisticsExample() {
        Log.i(TAG, "=== Statistics Example ===");
        
        String sellerId = "seller123";
        String categoryId = "electronics";
        
        // Get seller statistics
        ItemIntegration.SellerItemStats sellerStats = itemIntegration.getSellerItemStats(sellerId);
        if (sellerStats != null) {
            Log.i(TAG, "Seller stats: " + sellerStats);
            Log.i(TAG, "Total items: " + sellerStats.getTotalItems());
            Log.i(TAG, "Active items: " + sellerStats.getActiveItems());
            Log.i(TAG, "Total views: " + sellerStats.getTotalViews());
            Log.i(TAG, "Average views: " + sellerStats.getAverageViews());
        }
        
        // Get category statistics
        ItemIntegration.CategoryStats categoryStats = itemIntegration.getCategoryStats(categoryId);
        if (categoryStats != null) {
            Log.i(TAG, "Category stats: " + categoryStats);
            Log.i(TAG, "Category: " + categoryStats.getCategoryName());
            Log.i(TAG, "Total items: " + categoryStats.getTotalItems());
            Log.i(TAG, "Active items: " + categoryStats.getActiveItems());
            Log.i(TAG, "Average price: " + categoryStats.getAveragePrice());
        }
        
        // Get featured and trending items
        ItemIntegration.FeaturedTrendingItems featuredTrending = itemIntegration.getFeaturedTrendingItems();
        if (featuredTrending != null) {
            Log.i(TAG, "Featured and trending: " + featuredTrending);
            Log.i(TAG, "Featured items: " + featuredTrending.getFeaturedItems().size());
            Log.i(TAG, "Trending items: " + featuredTrending.getTrendingItems().size());
        }
    }
    
    /**
     * Example: Item lifecycle
     */
    public void itemLifecycleExample() {
        Log.i(TAG, "=== Item Lifecycle Example ===");
        
        String sellerId = "seller123";
        
        // Create item in draft status
        ItemData itemData = new ItemData();
        itemData.setTitle("Lifecycle Test Item");
        itemData.setDescription("Testing item lifecycle");
        itemData.setStartingPrice(300.0);
        itemData.setCategoryId("electronics");
        
        boolean created = itemManager.createItem(itemData, sellerId);
        Log.i(TAG, "Item created (draft): " + created);
        
        if (created) {
            List<Item> sellerItems = itemManager.getItemsBySeller(sellerId);
            if (!sellerItems.isEmpty()) {
                Item item = sellerItems.get(0);
                Log.i(TAG, "Item status: " + item.getStatus());
                
                // Activate item
                item.setStatus(ItemStatus.ACTIVE);
                item.setStartDate(new Date());
                item.setEndDate(new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000)); // 7 days
                
                Log.i(TAG, "Item activated: " + item.getStatus());
                Log.i(TAG, "Time remaining: " + item.getHoursRemaining() + " hours");
                
                // Simulate item ending
                item.setStatus(ItemStatus.ENDED);
                Log.i(TAG, "Item ended: " + item.getStatus());
                Log.i(TAG, "Item has ended: " + item.hasEnded());
            }
        }
    }
    
    /**
     * Run all examples
     */
    public void runAllExamples() {
        Log.i(TAG, "Starting Item Management System Examples...");
        
        itemManagementExample();
        imageManagementExample();
        categoryManagementExample();
        itemValidationExample();
        searchFilteringExample();
        integrationExample();
        statisticsExample();
        itemLifecycleExample();
        
        Log.i(TAG, "Item Management System Examples completed!");
    }
}
