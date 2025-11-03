package com.cc106.bidhub.utils;

import android.util.Log;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps Android's string-based category IDs to backend's integer-based category IDs
 * This should be kept in sync with the backend database categories table
 */
public class CategoryMapping {
    private static final String TAG = "CategoryMapping";
    
    // Mapping: Android categoryId (string) -> Backend category_id (integer)
    // These IDs must match the backend database categories table
    private static final Map<String, Integer> CATEGORY_ID_MAP = new HashMap<>();
    
    static {
        initializeMappings();
    }
    
    private static void initializeMappings() {
        // Main Categories
        // Note: Backend IDs from bidhub_schema_step5_data_final.sql
        // Electronics=1, Fashion=2, Home & Garden=3, Sports & Outdoors=4, Books & Media=5,
        // Automotive=6, Health & Beauty=7, Toys & Games=8, Collectibles=9, Others=10
        
        // Map Android categories to backend IDs
        // Since Android uses different categories, we'll map closest matches
        CATEGORY_ID_MAP.put("electronics", 1);      // Electronics (backend ID 1)
        CATEGORY_ID_MAP.put("fashion", 2);          // Fashion (backend ID 2)
        CATEGORY_ID_MAP.put("home_living", 3);      // Home & Living -> Home & Garden (backend ID 3)
        CATEGORY_ID_MAP.put("hobbies_games", 8);    // Hobbies & Games -> Toys & Games (backend ID 8)
        CATEGORY_ID_MAP.put("babies_kids", 8);      // Babies & Kids -> Toys & Games (backend ID 8)
        CATEGORY_ID_MAP.put("cars", 6);             // Cars -> Automotive (backend ID 6)
        CATEGORY_ID_MAP.put("motorcycles", 6);      // Motorcycles -> Automotive (backend ID 6)
        CATEGORY_ID_MAP.put("property", 10);        // Property -> Others (backend ID 10)
        CATEGORY_ID_MAP.put("services", 10);        // Services -> Others (backend ID 10)
        CATEGORY_ID_MAP.put("jobs", 10);            // Jobs -> Others (backend ID 10)
        CATEGORY_ID_MAP.put("commercial_industrial", 10); // Commercial & Industrial -> Others (backend ID 10)
        CATEGORY_ID_MAP.put("free_items", 10);      // Free Items -> Others (backend ID 10)
        CATEGORY_ID_MAP.put("others", 10);          // Others (backend ID 10)
        
        // Electronics Subcategories (all map to Electronics main category for now)
        CATEGORY_ID_MAP.put("mobile_phones_gadgets", 1);
        CATEGORY_ID_MAP.put("computers_tech", 1);
        CATEGORY_ID_MAP.put("video_gaming", 1);
        CATEGORY_ID_MAP.put("tv_entertainment", 1);
        CATEGORY_ID_MAP.put("photography", 1);
        CATEGORY_ID_MAP.put("audio", 1);
        CATEGORY_ID_MAP.put("other_electronics", 1);
        
        // Mobile Phones & Gadgets Sub-subcategories
        CATEGORY_ID_MAP.put("mobile_phones", 1);
        CATEGORY_ID_MAP.put("tablets", 1);
        CATEGORY_ID_MAP.put("wearables", 1);
        CATEGORY_ID_MAP.put("mobile_accessories", 1);
        CATEGORY_ID_MAP.put("walkie_talkies", 1);
        CATEGORY_ID_MAP.put("other_gadgets", 1);
        
        // Computers & Tech Sub-subcategories
        CATEGORY_ID_MAP.put("laptops_desktops", 1);
        CATEGORY_ID_MAP.put("computer_accessories", 1);
        CATEGORY_ID_MAP.put("computer_parts", 1);
        CATEGORY_ID_MAP.put("network_equipment", 1);
        CATEGORY_ID_MAP.put("servers_storage", 1);
        CATEGORY_ID_MAP.put("software", 1);
        
        // Video Gaming Sub-subcategories
        CATEGORY_ID_MAP.put("gaming_consoles", 1);
        CATEGORY_ID_MAP.put("gaming_accessories", 1);
        CATEGORY_ID_MAP.put("gaming_pc_parts", 1);
        CATEGORY_ID_MAP.put("video_games", 8); // Video games -> Toys & Games
        CATEGORY_ID_MAP.put("gaming_merchandise", 1);
        
        // TV & Entertainment Systems Sub-subcategories
        CATEGORY_ID_MAP.put("tv_screens", 1);
        CATEGORY_ID_MAP.put("streaming_players", 1);
        CATEGORY_ID_MAP.put("home_theater_systems", 1);
        CATEGORY_ID_MAP.put("tv_accessories", 1);
        
        // Photography Sub-subcategories
        CATEGORY_ID_MAP.put("cameras", 1);
        CATEGORY_ID_MAP.put("camera_lenses", 1);
        CATEGORY_ID_MAP.put("camera_accessories", 1);
        CATEGORY_ID_MAP.put("drones", 1);
        
        // Audio Sub-subcategories
        CATEGORY_ID_MAP.put("headphones", 1);
        CATEGORY_ID_MAP.put("speakers", 1);
        CATEGORY_ID_MAP.put("microphones", 1);
        CATEGORY_ID_MAP.put("audio_accessories", 1);
        
        // Fashion Subcategories
        CATEGORY_ID_MAP.put("womens_apparel", 2);
        CATEGORY_ID_MAP.put("mens_apparel", 2);
        CATEGORY_ID_MAP.put("footwear", 2);
        CATEGORY_ID_MAP.put("bags_wallets", 2);
        CATEGORY_ID_MAP.put("luxury", 2);
        CATEGORY_ID_MAP.put("jewelry_accessories", 2);
        CATEGORY_ID_MAP.put("muslimah_fashion", 2);
        CATEGORY_ID_MAP.put("wedding_gowns", 2);
        CATEGORY_ID_MAP.put("traditional_wear", 2);
        CATEGORY_ID_MAP.put("costumes", 2);
        
        // Home & Living Subcategories
        CATEGORY_ID_MAP.put("furniture", 3);
        CATEGORY_ID_MAP.put("home_decor", 3);
        CATEGORY_ID_MAP.put("kitchen_dining", 3);
        CATEGORY_ID_MAP.put("home_improvement", 3);
        CATEGORY_ID_MAP.put("bedding_bath", 3);
        CATEGORY_ID_MAP.put("storage_organization", 3);
        CATEGORY_ID_MAP.put("garden_patio", 3);
        CATEGORY_ID_MAP.put("tools", 3);
        
        Log.i(TAG, "Category ID mappings initialized: " + CATEGORY_ID_MAP.size() + " categories");
    }
    
    /**
     * Convert Android string category ID to backend integer category ID
     * @param androidCategoryId The Android category ID (string)
     * @return Backend category ID (integer) or null if not found
     */
    public static Integer toBackendCategoryId(String androidCategoryId) {
        if (androidCategoryId == null || androidCategoryId.isEmpty()) {
            Log.e(TAG, "Invalid Android category ID: null or empty");
            return null;
        }
        
        Integer backendId = CATEGORY_ID_MAP.get(androidCategoryId);
        
        if (backendId == null) {
            Log.e(TAG, "No backend mapping found for category: " + androidCategoryId);
            Log.w(TAG, "Available categories: " + CATEGORY_ID_MAP.keySet());
            // Fallback to Others category if mapping not found
            Log.w(TAG, "Falling back to 'Others' category (ID: 10)");
            return 10;
        } else {
            Log.d(TAG, "Mapped category: " + androidCategoryId + " -> " + backendId);
        }
        
        return backendId;
    }
    
    /**
     * Convert backend integer category ID to Android string category ID
     * @param backendCategoryId The backend category ID (integer)
     * @return Android category ID (string) or null if not found
     */
    public static String toAndroidCategoryId(Integer backendCategoryId) {
        if (backendCategoryId == null) {
            return null;
        }
        
        for (Map.Entry<String, Integer> entry : CATEGORY_ID_MAP.entrySet()) {
            if (entry.getValue().equals(backendCategoryId)) {
                Log.d(TAG, "Reverse mapped category: " + backendCategoryId + " -> " + entry.getKey());
                return entry.getKey();
            }
        }
        
        Log.e(TAG, "No Android mapping found for backend category: " + backendCategoryId);
        return null;
    }
    
    /**
     * Check if a category ID mapping exists
     */
    public static boolean hasMapping(String androidCategoryId) {
        return CATEGORY_ID_MAP.containsKey(androidCategoryId);
    }
    
    /**
     * Get all available category IDs
     */
    public static java.util.Set<String> getAllCategoryIds() {
        return CATEGORY_ID_MAP.keySet();
    }
}

