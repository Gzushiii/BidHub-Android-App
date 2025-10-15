package com.cc106.bidhub.items;

import android.content.Context;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Category Manager for handling marketplace categories
 * Based on Carousell Philippines categories with full hierarchical structure
 */
public class CategoryManager {
    private static CategoryManager instance;
    private Map<String, Category> categories;
    private Map<String, List<Category>> subCategories;
    private List<String> allCategoryNames;
    
    private CategoryManager() {
        initializeCategories();
    }
    
    public static synchronized CategoryManager getInstance() {
        if (instance == null) {
            instance = new CategoryManager();
        }
        return instance;
    }
    
    private void initializeCategories() {
        categories = new HashMap<>();
        subCategories = new HashMap<>();
        allCategoryNames = new ArrayList<>();
        
        // Main Categories
        createMainCategory("fashion", "Fashion", "Clothing and accessories");
        createMainCategory("electronics", "Electronics", "Electronic devices and gadgets");
        createMainCategory("home_living", "Home & Living", "Home improvement and living items");
        createMainCategory("hobbies_games", "Hobbies & Games", "Toys, games, and recreational items");
        createMainCategory("babies_kids", "Babies & Kids", "Baby and children's items");
        createMainCategory("cars", "Cars", "Automotive vehicles and parts");
        createMainCategory("motorcycles", "Motorcycles", "Motorcycle vehicles and accessories");
        createMainCategory("property", "Property", "Real estate listings");
        createMainCategory("services", "Services", "Professional and personal services");
        createMainCategory("jobs", "Jobs", "Employment opportunities");
        createMainCategory("commercial_industrial", "Commercial & Industrial", "Business equipment and supplies");
        createMainCategory("free_items", "Free Items", "Items given away for free");
        createMainCategory("others", "Others", "Items that don't fit into specific categories");
        
        // Fashion Subcategories
        addSubCategory("fashion", "womens_apparel", "Women's Apparel");
        addSubCategory("fashion", "mens_apparel", "Men's Apparel");
        addSubCategory("fashion", "footwear", "Footwear");
        addSubCategory("fashion", "bags_wallets", "Bags & Wallets");
        addSubCategory("fashion", "luxury", "Luxury");
        addSubCategory("fashion", "jewelry_accessories", "Jewelry & Accessories");
        addSubCategory("fashion", "muslimah_fashion", "Muslimah Fashion");
        addSubCategory("fashion", "wedding_gowns", "Wedding Gowns & Suits");
        addSubCategory("fashion", "traditional_wear", "Traditional & Cultural Wear");
        addSubCategory("fashion", "costumes", "Costumes");
        
        // Electronics Subcategories
        addSubCategory("electronics", "mobile_phones_gadgets", "Mobile Phones & Gadgets");
        addSubCategory("electronics", "computers_tech", "Computers & Tech");
        addSubCategory("electronics", "video_gaming", "Video Gaming");
        addSubCategory("electronics", "tv_entertainment", "TV & Entertainment Systems");
        addSubCategory("electronics", "photography", "Photography");
        addSubCategory("electronics", "audio", "Audio");
        addSubCategory("electronics", "other_electronics", "Other Electronics");
        
        // Mobile Phones & Gadgets Sub-subcategories
        addSubCategory("mobile_phones_gadgets", "mobile_phones", "Mobile Phones");
        addSubCategory("mobile_phones_gadgets", "tablets", "Tablets");
        addSubCategory("mobile_phones_gadgets", "wearables", "Wearables & Smart Watches");
        addSubCategory("mobile_phones_gadgets", "mobile_accessories", "Mobile & Gadget Accessories");
        addSubCategory("mobile_phones_gadgets", "walkie_talkies", "Walkie Talkies");
        addSubCategory("mobile_phones_gadgets", "other_gadgets", "Other Gadgets");
        
        // Computers & Tech Sub-subcategories
        addSubCategory("computers_tech", "laptops", "Laptops & Notebooks");
        addSubCategory("computers_tech", "desktops", "Desktops");
        addSubCategory("computers_tech", "monitors", "Monitors & Projectors");
        addSubCategory("computers_tech", "computer_parts", "Computer Parts & Accessories");
        addSubCategory("computers_tech", "printers", "Printers, Scanners & Copiers");
        addSubCategory("computers_tech", "networking", "Networking");
        
        // Video Gaming Sub-subcategories
        addSubCategory("video_gaming", "gaming_consoles", "Gaming Consoles");
        addSubCategory("video_gaming", "video_games", "Video Games");
        addSubCategory("video_gaming", "gaming_accessories", "Gaming Accessories");
        
        // TV & Entertainment Systems Sub-subcategories
        addSubCategory("tv_entertainment", "televisions", "Televisions");
        addSubCategory("tv_entertainment", "home_theatre", "Home Theatre & Sound Systems");
        addSubCategory("tv_entertainment", "dvd_players", "DVD & Blu-ray Players");
        
        // Photography Sub-subcategories
        addSubCategory("photography", "cameras", "Cameras");
        addSubCategory("photography", "lenses", "Lenses");
        addSubCategory("photography", "drones", "Drones");
        addSubCategory("photography", "camera_accessories", "Camera Accessories");
        
        // Audio Sub-subcategories
        addSubCategory("audio", "headphones", "Headphones & Headsets");
        addSubCategory("audio", "speakers", "Speakers");
        addSubCategory("audio", "amplifiers", "Amplifiers & Mixers");
        addSubCategory("audio", "microphones", "Microphones");
        
        // Home & Living Subcategories
        addSubCategory("home_living", "furniture", "Furniture");
        addSubCategory("home_living", "home_appliances", "Home Appliances");
        addSubCategory("home_living", "kitchen_dining", "Kitchen & Dining");
        addSubCategory("home_living", "home_decor", "Home Decor");
        addSubCategory("home_living", "gardening_plants", "Gardening & Plants");
        addSubCategory("home_living", "tools_diy", "Tools & DIY");
        addSubCategory("home_living", "household_supplies", "Household Supplies");
        
        // Furniture Sub-subcategories
        addSubCategory("furniture", "sofas_armchairs", "Sofas & Armchairs");
        addSubCategory("furniture", "beds_mattresses", "Beds & Mattresses");
        addSubCategory("furniture", "tables_desks", "Tables & Desks");
        addSubCategory("furniture", "chairs", "Chairs");
        addSubCategory("furniture", "storage_shelving", "Storage & Shelving");
        
        // Home Appliances Sub-subcategories
        addSubCategory("home_appliances", "refrigerators", "Refrigerators & Freezers");
        addSubCategory("home_appliances", "washers_dryers", "Washers & Dryers");
        addSubCategory("home_appliances", "air_conditioners", "Air Conditioners & Fans");
        addSubCategory("home_appliances", "vacuums", "Vacuums & Floor Care");
        addSubCategory("home_appliances", "kitchen_appliances", "Small Kitchen Appliances");
        
        // Kitchen & Dining Sub-subcategories
        addSubCategory("kitchen_dining", "cookware", "Cookware & Bakeware");
        addSubCategory("kitchen_dining", "tableware", "Tableware");
        addSubCategory("kitchen_dining", "kitchen_tools", "Kitchen Tools & Accessories");
        
        // Home Decor Sub-subcategories
        addSubCategory("home_decor", "lighting", "Lighting");
        addSubCategory("home_decor", "rugs_carpets", "Rugs & Carpets");
        addSubCategory("home_decor", "curtains_blinds", "Curtains & Blinds");
        addSubCategory("home_decor", "wall_art", "Wall Art & Decor");
        
        // Hobbies & Games Subcategories
        addSubCategory("hobbies_games", "toys_games", "Toys & Games");
        addSubCategory("hobbies_games", "books_stationery", "Books & Stationery");
        addSubCategory("hobbies_games", "music_media", "Music & Media");
        addSubCategory("hobbies_games", "sports_equipment", "Sports Equipment");
        addSubCategory("hobbies_games", "antiques_collectibles", "Antiques & Collectibles");
        addSubCategory("hobbies_games", "art_craft", "Art & Craft");
        
        // Toys & Games Sub-subcategories
        addSubCategory("toys_games", "action_figures", "Action Figures & Collectibles");
        addSubCategory("toys_games", "board_games", "Board Games & Cards");
        addSubCategory("toys_games", "dolls_stuffed", "Dolls & Stuffed Toys");
        addSubCategory("toys_games", "educational_toys", "Educational Toys");
        addSubCategory("toys_games", "remote_control", "Remote Control Toys");
        
        // Books & Stationery Sub-subcategories
        addSubCategory("books_stationery", "fiction_books", "Fiction Books");
        addSubCategory("books_stationery", "non_fiction_books", "Non-Fiction Books");
        addSubCategory("books_stationery", "textbooks", "Textbooks");
        addSubCategory("books_stationery", "stationery_craft", "Stationery & Craft Supplies");
        
        // Music & Media Sub-subcategories
        addSubCategory("music_media", "musical_instruments", "Musical Instruments");
        addSubCategory("music_media", "vinyl_records", "Vinyl Records, Tapes & CDs");
        addSubCategory("music_media", "movies_tv", "Movies & TV Shows");
        
        // Sports Equipment Sub-subcategories
        addSubCategory("sports_equipment", "bicycles", "Bicycles & Parts");
        addSubCategory("sports_equipment", "gym_fitness", "Gym & Fitness Equipment");
        addSubCategory("sports_equipment", "water_sports", "Water Sports");
        addSubCategory("sports_equipment", "team_sports", "Team Sports");
        
        // Antiques & Collectibles Sub-subcategories
        addSubCategory("antiques_collectibles", "coins_stamps", "Coins & Stamps");
        addSubCategory("antiques_collectibles", "vintage_antiques", "Vintage & Antiques");
        addSubCategory("antiques_collectibles", "figurines", "Figurines & Knick Knacks");
        
        // Babies & Kids Subcategories
        addSubCategory("babies_kids", "baby_kids_fashion", "Baby & Kids' Fashion");
        addSubCategory("babies_kids", "toys_walkers", "Toys & Walkers");
        addSubCategory("babies_kids", "maternity", "Maternity");
        addSubCategory("babies_kids", "nursing_feeding", "Nursing & Feeding");
        addSubCategory("babies_kids", "diapering_potty", "Diapering & Potty");
        addSubCategory("babies_kids", "baby_gear", "Baby Gear");
        addSubCategory("babies_kids", "nursery_furniture", "Nursery & Kids' Furniture");
        
        // Cars Subcategories
        addSubCategory("cars", "cars_for_sale", "Cars for Sale");
        addSubCategory("cars", "car_parts", "Car Parts & Accessories");
        addSubCategory("cars", "car_rentals", "Car Rentals");
        addSubCategory("cars", "other_vehicles", "Other Vehicles");
        
        // Motorcycles Subcategories
        addSubCategory("motorcycles", "motorcycles_for_sale", "Motorcycles for Sale");
        addSubCategory("motorcycles", "motorcycle_parts", "Motorcycle Parts");
        addSubCategory("motorcycles", "motorcycle_accessories", "Motorcycle Accessories");
        addSubCategory("motorcycles", "riding_gear", "Riding Gear");
        
        // Property Subcategories
        addSubCategory("property", "for_sale", "For Sale");
        addSubCategory("property", "for_rent", "For Rent");
        
        // Property For Sale Sub-subcategories
        addSubCategory("for_sale", "apartment_condo_sale", "Apartment & Condo");
        addSubCategory("for_sale", "house_lot_sale", "House & Lot");
        addSubCategory("for_sale", "land", "Land");
        addSubCategory("for_sale", "commercial_sale", "Commercial");
        
        // Property For Rent Sub-subcategories
        addSubCategory("for_rent", "apartment_condo_rent", "Apartment & Condo");
        addSubCategory("for_rent", "house_lot_rent", "House & Lot");
        addSubCategory("for_rent", "room_bedspace", "Room & Bedspace");
        addSubCategory("for_rent", "commercial_rent", "Commercial");
        
        // Services Subcategories
        addSubCategory("services", "home_services", "Home Services");
        addSubCategory("services", "lifestyle_services", "Lifestyle Services");
        addSubCategory("services", "learning_enrichment", "Learning & Enrichment");
        addSubCategory("services", "business_services", "Business Services");
        
        // Home Services Sub-subcategories
        addSubCategory("home_services", "cleaning", "Cleaning");
        addSubCategory("home_services", "plumbing", "Plumbing");
        addSubCategory("home_services", "electrical", "Electrical & Wiring");
        addSubCategory("home_services", "aircon_services", "Aircon Services");
        addSubCategory("home_services", "movers_delivery", "Movers & Delivery");
        addSubCategory("home_services", "renovations", "Renovations & Interior Design");
        addSubCategory("home_services", "home_repairs", "Home Repairs");
        
        // Lifestyle Services Sub-subcategories
        addSubCategory("lifestyle_services", "beauty_health", "Beauty & Health");
        addSubCategory("lifestyle_services", "pet_care", "Pet Care");
        addSubCategory("lifestyle_services", "events_parties", "Events & Parties");
        addSubCategory("lifestyle_services", "photography_videography", "Photography & Videography");
        
        // Learning & Enrichment Sub-subcategories
        addSubCategory("learning_enrichment", "tuition_classes", "Tuition & Classes");
        addSubCategory("learning_enrichment", "music_arts", "Music & Arts");
        addSubCategory("learning_enrichment", "sports_fitness", "Sports & Fitness");
        
        // Business Services Sub-subcategories
        addSubCategory("business_services", "web_tech", "Web & Tech");
        addSubCategory("business_services", "marketing_branding", "Marketing & Branding");
        addSubCategory("business_services", "writing_translation", "Writing & Translation");
        
        // Jobs Subcategories
        addSubCategory("jobs", "full_time", "Full-time");
        addSubCategory("jobs", "part_time", "Part-time");
        addSubCategory("jobs", "contract", "Contract");
        addSubCategory("jobs", "internship", "Internship");
        
        // Commercial & Industrial Subcategories
        addSubCategory("commercial_industrial", "office_furniture", "Office Furniture & Equipment");
        addSubCategory("commercial_industrial", "industrial_machinery", "Industrial Machinery");
        addSubCategory("commercial_industrial", "restaurant_equipment", "Restaurant & Catering Equipment");
        addSubCategory("commercial_industrial", "retail_equipment", "Retail & Store Equipment");
        
        // Build the complete category names list for dropdown
        buildAllCategoryNames();
    }
    
    private void createMainCategory(String id, String name, String description) {
        Category category = new Category(name, description);
        category.setCategoryId(id);
        category.setSortOrder(categories.size() + 1);
        categories.put(id, category);
        subCategories.put(id, new ArrayList<>());
    }
    
    private void addSubCategory(String parentId, String id, String name) {
        Category subCategory = new Category(name, "");
        subCategory.setCategoryId(id);
        subCategory.setParentCategoryId(parentId);
        
        // Ensure the parent category has a subcategories list
        if (!subCategories.containsKey(parentId)) {
            subCategories.put(parentId, new ArrayList<>());
        }
        
        subCategory.setSortOrder(subCategories.get(parentId).size() + 1);
        categories.put(id, subCategory);
        subCategories.get(parentId).add(subCategory);
    }
    
    private void buildAllCategoryNames() {
        if (allCategoryNames == null) {
            allCategoryNames = new ArrayList<>();
        }
        allCategoryNames.clear();
        
        // Add main categories first
        List<Category> mainCategories = getAllMainCategories();
        if (mainCategories != null) {
            for (Category mainCategory : mainCategories) {
                if (mainCategory != null && mainCategory.getName() != null) {
                    allCategoryNames.add(mainCategory.getName());
                    
                    // Add subcategories with indentation
                    List<Category> subCats = getSubCategories(mainCategory.getCategoryId());
                    if (subCats != null) {
                        for (Category subCategory : subCats) {
                            if (subCategory != null && subCategory.getName() != null) {
                                allCategoryNames.add("  " + subCategory.getName());
                                
                                // Add sub-subcategories with more indentation
                                List<Category> subSubCats = getSubCategories(subCategory.getCategoryId());
                                if (subSubCats != null) {
                                    for (Category subSubCategory : subSubCats) {
                                        if (subSubCategory != null && subSubCategory.getName() != null) {
                                            allCategoryNames.add("    " + subSubCategory.getName());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    public List<Category> getAllMainCategories() {
        List<Category> mainCategories = new ArrayList<>();
        if (categories != null) {
            for (Category category : categories.values()) {
                if (category != null && category.getParentCategoryId() == null) {
                    mainCategories.add(category);
                }
            }
        }
        return mainCategories;
    }
    
    public List<Category> getSubCategories(String parentId) {
        if (subCategories == null) {
            return new ArrayList<>();
        }
        return subCategories.getOrDefault(parentId, new ArrayList<>());
    }
    
    public Category getCategoryById(String id) {
        if (categories == null) {
            return null;
        }
        return categories.get(id);
    }
    
    public List<Category> getAllCategories() {
        if (categories == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(categories.values());
    }
    
    public List<String> getCategoryNames() {
        if (allCategoryNames == null) {
            allCategoryNames = new ArrayList<>();
            buildAllCategoryNames();
        }
        return new ArrayList<>(allCategoryNames);
    }
    
    public List<String> getMainCategoryNames() {
        List<String> names = new ArrayList<>();
        for (Category category : getAllMainCategories()) {
            names.add(category.getName());
        }
        return names;
    }
    
    /**
     * Get category ID from display name (handles indented names)
     */
    public String getCategoryIdFromDisplayName(String displayName) {
        String trimmedName = displayName.trim();
        
        // Search through all categories to find matching name
        for (Category category : categories.values()) {
            if (category.getName().equals(trimmedName)) {
                return category.getCategoryId();
            }
        }
        
        return null;
    }
    
    /**
     * Get display name from category ID
     */
    public String getDisplayNameFromCategoryId(String categoryId) {
        Category category = categories.get(categoryId);
        if (category != null) {
            // Check if it's a subcategory and add appropriate indentation
            if (category.getParentCategoryId() != null) {
                Category parent = categories.get(category.getParentCategoryId());
                if (parent != null && parent.getParentCategoryId() != null) {
                    // This is a sub-subcategory
                    return "    " + category.getName();
                } else {
                    // This is a subcategory
                    return "  " + category.getName();
                }
            } else {
                // This is a main category
                return category.getName();
            }
        }
        return null;
    }
}