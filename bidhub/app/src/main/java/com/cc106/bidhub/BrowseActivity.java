package com.cc106.bidhub;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.cc106.bidhub.adapters.BrowseItemAdapter;
import com.cc106.bidhub.api.ItemApiClient;
import com.cc106.bidhub.items.Item;
import com.cc106.bidhub.items.ItemStatus;
import com.cc106.bidhub.models.BrowseItem;
import com.cc106.bidhub.toast.ToastHelper;
import com.cc106.bidhub.utils.SharedPreferencesHelper;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BrowseActivity extends BaseActivity {

    private String loggedInUserEmail;
    
    // UI Components
    private EditText searchEditText;
    private ImageButton btnSearch, btnFilter;
    private Button btnElectronics, btnFashion, btnHome, btnCollectibles;
    private RecyclerView recyclerViewBrowse;
    private BrowseItemAdapter browseItemAdapter;
    
    // Data
    private List<BrowseItem> allItems;
    private List<BrowseItem> filteredItems;
    private String currentCategory = "All"; // Start with "All" to show all items initially
    private String currentSearchQuery = "";
    private boolean isLoading = false;
    
    // UI Components for loading state
    private ProgressBar progressBar;
    private TextView emptyStateText;
    
    // Intent extra key for category filter
    public static final String EXTRA_CATEGORY_FILTER = "category_filter";
    
    private static final String TAG = "BrowseActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inflate the browse content into the content frame
        getLayoutInflater().inflate(R.layout.activity_browse_content, findViewById(R.id.content_frame));
        
        // Get the logged-in user's email from the Intent
        loggedInUserEmail = getIntent().getStringExtra("USER_EMAIL");
        
        // Get category filter from Intent if provided
        String categoryFilter = getIntent().getStringExtra(EXTRA_CATEGORY_FILTER);
        if (categoryFilter != null && !categoryFilter.isEmpty()) {
            currentCategory = categoryFilter;
        }
        
        // Initialize UI components
        initializeViews();
        
        // Setup RecyclerView
        setupRecyclerView();
        
        // Setup click listeners
        setupClickListeners();
        
        // Load items from API
        loadItemsFromApi();
        
        // Apply category filter if provided
        if (categoryFilter != null && !categoryFilter.isEmpty()) {
            selectCategory(categoryFilter);
        }
        
        // Animate content in after inflation
        animateContentIn();
    }

    private void initializeViews() {
        searchEditText = findViewById(R.id.searchEditText);
        btnSearch = findViewById(R.id.btnSearch);
        btnFilter = findViewById(R.id.btnFilter);
        btnElectronics = findViewById(R.id.btnElectronics);
        btnFashion = findViewById(R.id.btnFashion);
        btnHome = findViewById(R.id.btnHome);
        btnCollectibles = findViewById(R.id.btnCollectibles);
        recyclerViewBrowse = findViewById(R.id.recyclerViewBrowse);
        
        // Try to find progress bar and empty state (may not exist in layout)
        try {
            int progressBarId = getResources().getIdentifier("progressBar", "id", getPackageName());
            if (progressBarId != 0) {
                View progressBarView = findViewById(progressBarId);
                if (progressBarView instanceof ProgressBar) {
                    progressBar = (ProgressBar) progressBarView;
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Progress bar not found in layout");
        }
        try {
            int emptyStateId = getResources().getIdentifier("emptyStateText", "id", getPackageName());
            if (emptyStateId != 0) {
                View emptyStateView = findViewById(emptyStateId);
                if (emptyStateView instanceof TextView) {
                    emptyStateText = (TextView) emptyStateView;
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Empty state text not found in layout");
        }
    }

    private void setupRecyclerView() {
        allItems = new ArrayList<>();
        filteredItems = new ArrayList<>();
        
        browseItemAdapter = new BrowseItemAdapter(filteredItems, this::onItemClick);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        recyclerViewBrowse.setLayoutManager(layoutManager);
        recyclerViewBrowse.setAdapter(browseItemAdapter);
        
        // Accessibility improvements
        recyclerViewBrowse.setContentDescription("Browse items grid");
        if (searchEditText != null) {
            searchEditText.setContentDescription("Search items");
            searchEditText.setHint("Search for items...");
        }
        if (btnSearch != null) {
            btnSearch.setContentDescription("Search button");
        }
        if (btnFilter != null) {
            btnFilter.setContentDescription("Filter items");
        }
        
        // Add smooth item animations
        recyclerViewBrowse.setItemAnimator(new DefaultItemAnimator() {
            @Override
            public boolean animateAdd(RecyclerView.ViewHolder holder) {
                holder.itemView.setAlpha(0f);
                holder.itemView.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start();
                return super.animateAdd(holder);
            }
        });
    }

    private void loadItemsFromApi() {
        if (isLoading) {
            Log.d(TAG, "Already loading, skipping API call.");
            return; // Prevent multiple simultaneous loads
        }
        
        isLoading = true;
        showLoading(true);
        // Don't clear items here - wait until we have new data
        Log.d(TAG, "Starting to load items from API. Current items count: " + allItems.size());
        
        // Load items on background thread
        new Thread(() -> {
            try {
                // Verify authentication token before making request
                SharedPreferencesHelper prefsHelper = new SharedPreferencesHelper(this);
                String token = prefsHelper.getAuthToken();
                if (token == null || token.isEmpty()) {
                    Log.w(TAG, "No auth token available, showing empty state");
                    runOnUiThread(() -> {
                        isLoading = false;
                        showLoading(false);
                        showEmptyState("Please log in to browse items");
                        ToastHelper.showInfo(this, "Please log in to view items");
                    });
                    return;
                }
                
                // Always load ALL items from API (no category filter)
                // Category filtering will be done locally to avoid items disappearing on refresh
                // Only apply search query to API if it's not empty
                String searchQuery = null;
                if (currentSearchQuery != null && !currentSearchQuery.isEmpty() && currentSearchQuery.length() >= 3) {
                    // Only use search if it's at least 3 characters to avoid too many API calls
                    searchQuery = currentSearchQuery;
                }
                
                // Fetch items from API - always get all items, filter locally
                // Use a high limit to ensure we get all items (adjust if needed)
                ItemApiClient apiClient = new ItemApiClient(this);
                ItemApiClient.ApiResponse response = apiClient.getItems(
                    null, // categoryId - always null, load all items
                    searchQuery, // search query if provided
                    null, // minPrice
                    null, // maxPrice
                    null, // sellerEmail
                    1000, // limit - increased to get all items
                    0     // offset
                );
                
                if (response.isSuccess() && response.getData() != null) {
                    // Parse items from response
                    List<Item> apiItems = parseItemsFromResponse(response.getData());
                    
                    // Filter to only show ACTIVE items
                    List<Item> activeItems = new ArrayList<>();
                    for (Item item : apiItems) {
                        if (item.getStatus() == ItemStatus.ACTIVE) {
                            activeItems.add(item);
                        }
                    }
                    
                    Log.d(TAG, "Loaded " + activeItems.size() + " active items from API");
                    
                    // Convert Item objects to BrowseItem objects
                    List<BrowseItem> browseItems = convertItemsToBrowseItems(activeItems);
                    
                    runOnUiThread(() -> {
                        // Only update items if we got new data
                        if (browseItems != null && !browseItems.isEmpty()) {
                            // Store old count for logging
                            int oldCount = allItems.size();
                            
                            // CRITICAL FIX: Atomic update - create new list first, then replace
                            // This prevents items from vanishing if something goes wrong during update
                            List<BrowseItem> newItemsList = new ArrayList<>(browseItems);
                            
                            // Only replace allItems if we successfully created the new list
                            if (!newItemsList.isEmpty()) {
                                // Log warning if significant drop in item count
                                if (oldCount > 0 && newItemsList.size() < oldCount * 0.5) {
                                    Log.w(TAG, "WARNING: API returned only " + newItemsList.size() + " items (had " + oldCount + "), possible data loss");
                                }
                                
                                // Atomic replacement - this is the only place we clear allItems
                                allItems.clear();
                                allItems.addAll(newItemsList);
                                
                                if (oldCount == 0) {
                                    Log.d(TAG, "First load: Added " + newItemsList.size() + " items");
                                } else {
                                    Log.d(TAG, "Refresh: Replaced " + oldCount + " -> " + newItemsList.size() + " items from API");
                                }
                            } else {
                                Log.w(TAG, "New items list is empty after creation, keeping existing " + oldCount + " items");
                            }
                        } else if (browseItems != null && browseItems.isEmpty()) {
                            // API returned empty list - but don't clear existing items on refresh
                            // This might be a temporary API issue or the user might have filtered items
                            // Only clear if we have no existing items (first load)
                            if (allItems.isEmpty()) {
                                Log.w(TAG, "API returned empty list on first load");
        allItems.clear();
                            } else {
                                Log.w(TAG, "API returned empty list on refresh, keeping existing " + allItems.size() + " items");
                            }
                        } else {
                            // browseItems is null - API error, keep existing items
                            Log.w(TAG, "No items returned from API (null response), keeping existing " + allItems.size() + " items");
                        }
                        isLoading = false;
                        showLoading(false);
                        // Apply filters to show items based on current category/search
                        applyFilters();
                        Log.d(TAG, "Showing " + filteredItems.size() + " items after filtering (out of " + allItems.size() + " total)");
                    });
                } else {
                    // API call failed - preserve existing items instead of clearing them
                    Log.w(TAG, "Failed to load items: " + (response.getMessage() != null ? response.getMessage() : "Unknown error"));
                    runOnUiThread(() -> {
                        isLoading = false;
                        showLoading(false);
                        // Don't clear items on error - keep existing items visible
                        if (allItems.isEmpty()) {
                            showEmptyState("Unable to load items. Please check your connection and try again.");
                            ToastHelper.showError(this, "Failed to load items. Tap to retry.");
                        } else {
                            // Reapply filters to show existing items
                            Log.d(TAG, "API call failed, but keeping existing " + allItems.size() + " items visible");
                            applyFilters();
                            ToastHelper.showWarning(this, "Showing cached items. Pull down to refresh.");
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading items from API", e);
                runOnUiThread(() -> {
                    isLoading = false;
                    showLoading(false);
                    // Don't clear items on error - keep existing items visible
                    if (allItems.isEmpty()) {
                        showEmptyState("Something went wrong. Please check your connection and try again.");
                        ToastHelper.showError(this, "Error loading items. Tap to retry.");
                    } else {
                        // Reapply filters to show existing items
                        Log.d(TAG, "Error loading items, but keeping existing " + allItems.size() + " items visible");
                        applyFilters();
                        ToastHelper.showWarning(this, "Showing cached items. Pull down to refresh.");
                    }
                });
            }
        }).start();
    }
    
    private List<Item> parseItemsFromResponse(String responseData) {
        List<Item> items = new ArrayList<>();
        try {
            org.json.JSONObject jsonResponse = new org.json.JSONObject(responseData);
            org.json.JSONArray itemsArray = jsonResponse.getJSONArray("items");
            
            for (int i = 0; i < itemsArray.length(); i++) {
                org.json.JSONObject itemJson = itemsArray.getJSONObject(i);
                Item item = new Item();
                
                // CRITICAL: Prioritize uuid_id (backend primary ID), fallback to id
                String itemId = itemJson.optString("uuid_id", itemJson.optString("id", ""));
                if (itemId == null || itemId.isEmpty()) {
                    continue; // Skip items without valid IDs
                }
                item.setItemId(itemId);
                item.setTitle(itemJson.getString("title"));
                item.setDescription(itemJson.optString("description", ""));
                
                // Handle price fields - try different possible field names
                double startingPrice = itemJson.optDouble("starting_price", itemJson.optDouble("starting_bid", 0.0));
                double currentPrice = itemJson.optDouble("current_bid", itemJson.optDouble("current_price", startingPrice));
                item.setStartingPrice(startingPrice);
                item.setCurrentPrice(currentPrice);
                
                item.setCategoryId(itemJson.optString("category_id", ""));
                item.setSellerId(itemJson.optString("seller_email", itemJson.optString("seller_id", "")));
                item.setCondition(itemJson.optString("item_condition", itemJson.optString("condition", "good")));
                item.setStatus(ItemStatus.ACTIVE);
                
                // Parse images
                if (itemJson.has("images")) {
                    try {
                        Object imagesObj = itemJson.get("images");
                        List<String> imagePaths = new ArrayList<>();
                        
                        if (imagesObj instanceof org.json.JSONArray) {
                            org.json.JSONArray imagesArray = (org.json.JSONArray) imagesObj;
                            for (int j = 0; j < imagesArray.length(); j++) {
                                imagePaths.add(imagesArray.getString(j));
                            }
                        } else if (imagesObj instanceof String) {
                            String imagesString = (String) imagesObj;
                            if (!imagesString.isEmpty() && !imagesString.equals("null")) {
                                org.json.JSONArray imagesArray = new org.json.JSONArray(imagesString);
                                for (int j = 0; j < imagesArray.length(); j++) {
                                    imagePaths.add(imagesArray.getString(j));
                                }
                            }
                        }
                        
                        item.setImagePaths(imagePaths);
                    } catch (Exception e) {
                        Log.w(TAG, "Error parsing images for item: " + item.getTitle(), e);
                        item.setImagePaths(new ArrayList<>());
                    }
                }
                
                // Parse dates if available
                try {
                    if (itemJson.has("bid_deadline") || itemJson.has("end_date")) {
                        String dateStr = itemJson.optString("bid_deadline", itemJson.optString("end_date", ""));
                        if (!dateStr.isEmpty()) {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                            item.setEndDate(sdf.parse(dateStr));
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error parsing date for item: " + item.getTitle(), e);
                }
                
                items.add(item);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing items from response", e);
        }
        return items;
    }
    
    private List<BrowseItem> convertItemsToBrowseItems(List<Item> items) {
        List<BrowseItem> browseItems = new ArrayList<>();
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        currencyFormat.setCurrency(java.util.Currency.getInstance("PHP"));
        
        for (Item item : items) {
            // Format current bid/price
            String currentBidText;
            boolean isBuyNow = item.getBuyNowPrice() > 0;
            
            if (isBuyNow && item.getCurrentPrice() == item.getStartingPrice()) {
                currentBidText = "Price: " + currencyFormat.format(item.getBuyNowPrice());
            } else {
                currentBidText = "Current Bid: " + currencyFormat.format(item.getCurrentPrice());
            }
            
            // Format time left
            String timeLeftText = formatTimeLeft(item);
            
            // Get category name
            String categoryName = getCategoryName(item.getCategoryId());
            
            // Get first image URL
            String imageUrl = "";
            if (item.getImagePaths() != null && !item.getImagePaths().isEmpty()) {
                imageUrl = item.getImagePaths().get(0);
            }
            
            // Determine status
            String status = determineItemStatus(item);
            
            BrowseItem browseItem = new BrowseItem(
                item.getItemId(),
                item.getTitle(),
                currentBidText,
                timeLeftText,
                imageUrl,
                categoryName,
                isBuyNow,
                status
            );
            
            browseItems.add(browseItem);
        }
        
        return browseItems;
    }
    
    private String formatTimeLeft(Item item) {
        if (item.getEndDate() == null) {
            return "No deadline";
        }
        
        long timeRemaining = item.getTimeRemaining();
        if (timeRemaining <= 0) {
            return "Ending soon";
        }
        
        long hours = timeRemaining / (60 * 60 * 1000);
        long days = hours / 24;
        hours = hours % 24;
        
        if (days > 0) {
            return days + "d " + hours + "h left";
        } else if (hours > 0) {
            return hours + "h left";
        } else {
            long minutes = timeRemaining / (60 * 1000);
            if (minutes > 0) {
                return minutes + "m left";
            } else {
                return "Ending soon";
            }
        }
    }
    
    private String getCategoryName(String categoryId) {
        // Map category IDs to names - you may need to adjust this based on your category system
        if (categoryId == null || categoryId.isEmpty()) {
            return "All";
        }
        
        // Try to get category name from category ID
        // For now, return a default based on common category IDs
        try {
            int id = Integer.parseInt(categoryId);
            switch (id) {
                case 1: return "Electronics";
                case 2: return "Fashion";
                case 3: return "Home";
                case 4: return "Collectibles";
                default: return "All";
            }
        } catch (NumberFormatException e) {
            return "All";
        }
    }
    
    private String getCategoryId(String categoryName) {
        // Map category names to IDs
        switch (categoryName) {
            case "Electronics": return "1";
            case "Fashion": return "2";
            case "Home": return "3";
            case "Collectibles": return "4";
            default: return null;
        }
    }
    
    private String determineItemStatus(Item item) {
        if (item.getBuyNowPrice() > 0) {
            return "buy_now";
        }
        
        long timeRemaining = item.getTimeRemaining();
        if (timeRemaining > 0 && timeRemaining < 24 * 60 * 60 * 1000) { // Less than 24 hours
            return "ending_soon";
        }
        
        return "active";
    }
    
    private void showLoading(boolean show) {
        if (progressBar != null) {
            if (show) {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setAlpha(0f);
                progressBar.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start();
            } else {
                progressBar.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> progressBar.setVisibility(View.GONE))
                    .start();
            }
        }
        // Don't hide recyclerView during loading - keep items visible
        // Only hide if we have no items to show
        if (!show) {
            recyclerViewBrowse.setVisibility(filteredItems.isEmpty() ? View.GONE : View.VISIBLE);
            // Smooth fade-in for RecyclerView
            if (!filteredItems.isEmpty() && recyclerViewBrowse.getVisibility() == View.VISIBLE) {
                recyclerViewBrowse.setAlpha(0f);
                recyclerViewBrowse.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start();
            }
        }
        // Progress bar overlay is enough to indicate loading
        if (!show && emptyStateText != null) {
            boolean shouldShowEmpty = filteredItems.isEmpty();
            if (shouldShowEmpty) {
                emptyStateText.setAlpha(0f);
                emptyStateText.setVisibility(View.VISIBLE);
                emptyStateText.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start();
            } else {
                emptyStateText.setVisibility(View.GONE);
            }
        }
    }
    
    private void showEmptyState(String message) {
        if (emptyStateText != null) {
            emptyStateText.setText(message);
            emptyStateText.setAlpha(0f);
            emptyStateText.setVisibility(View.VISIBLE);
            emptyStateText.animate()
                .alpha(1f)
                .setDuration(300)
                .start();
        }
        if (recyclerViewBrowse.getVisibility() == View.VISIBLE) {
            recyclerViewBrowse.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> recyclerViewBrowse.setVisibility(View.GONE))
                .start();
        } else {
            recyclerViewBrowse.setVisibility(View.GONE);
        }
    }

    private void setupClickListeners() {
        // Search functionality
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString();
                // Apply local filters for immediate feedback
                // Only reload from API if search query is substantial (3+ chars) and we don't have items
                if (allItems.isEmpty() && currentSearchQuery.length() >= 3) {
                    loadItemsFromApi();
                } else {
                    // Just filter locally for instant feedback
                applyFilters();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnSearch.setOnClickListener(v -> {
            // Focus on search field
            searchEditText.requestFocus();
        });

        btnFilter.setOnClickListener(v -> showFilterDialog());

        // Category buttons
        btnElectronics.setOnClickListener(v -> selectCategory("Electronics"));
        btnFashion.setOnClickListener(v -> selectCategory("Fashion"));
        btnHome.setOnClickListener(v -> selectCategory("Home"));
        btnCollectibles.setOnClickListener(v -> selectCategory("Collectibles"));
        
        // Make sure "All" category is available - if no category button is selected, show all
        // This is handled by default currentCategory = "All"
    }

    private void selectCategory(String category) {
        currentCategory = category;
        
        // Update button states
        btnElectronics.setBackgroundResource(category.equals("Electronics") ? R.drawable.chip_background_primary : R.drawable.chip_background);
        btnElectronics.setTextColor(getResources().getColor(category.equals("Electronics") ? R.color.primary : R.color.text_secondary));
        
        btnFashion.setBackgroundResource(category.equals("Fashion") ? R.drawable.chip_background_primary : R.drawable.chip_background);
        btnFashion.setTextColor(getResources().getColor(category.equals("Fashion") ? R.color.primary : R.color.text_secondary));
        
        btnHome.setBackgroundResource(category.equals("Home") ? R.drawable.chip_background_primary : R.drawable.chip_background);
        btnHome.setTextColor(getResources().getColor(category.equals("Home") ? R.color.primary : R.color.text_secondary));
        
        btnCollectibles.setBackgroundResource(category.equals("Collectibles") ? R.drawable.chip_background_primary : R.drawable.chip_background);
        btnCollectibles.setTextColor(getResources().getColor(category.equals("Collectibles") ? R.color.primary : R.color.text_secondary));
        
        // Always apply local filters - never reload from API with category filter
        // This ensures all items stay loaded and filtering is done locally
        if (allItems.isEmpty()) {
            // Only reload if we don't have items yet
            loadItemsFromApi();
        } else {
            // Just apply filters to existing items - this preserves all items
        applyFilters();
        }
    }

    private void applyFilters() {
        filteredItems.clear();
        
        Log.d(TAG, "Applying filters - Category: '" + currentCategory + "', Search: '" + currentSearchQuery + "', Total items: " + allItems.size());
        
        // If no items available, don't filter
        if (allItems.isEmpty()) {
            Log.w(TAG, "No items to filter");
            browseItemAdapter.updateItems(filteredItems);
            if (emptyStateText != null) {
                emptyStateText.setVisibility(View.VISIBLE);
            }
            recyclerViewBrowse.setVisibility(View.GONE);
            return;
        }
        
        for (BrowseItem item : allItems) {
            // Category matching - case insensitive and handle "All"
            boolean matchesCategory = true;
            if (!currentCategory.equals("All") && currentCategory != null && !currentCategory.isEmpty()) {
                String itemCategory = item.getCategory();
                if (itemCategory == null || itemCategory.isEmpty()) {
                    // If item has no category, show it in "All" view only
                    matchesCategory = false;
                } else {
                    // Case-insensitive category matching with flexible matching
                    String normalizedItemCategory = itemCategory.trim().toLowerCase();
                    String normalizedCurrentCategory = currentCategory.trim().toLowerCase();
                    matchesCategory = normalizedItemCategory.equals(normalizedCurrentCategory);
                    
                    // Also check if category name contains the filter (for partial matches)
                    if (!matchesCategory) {
                        matchesCategory = normalizedItemCategory.contains(normalizedCurrentCategory) || 
                                        normalizedCurrentCategory.contains(normalizedItemCategory);
                    }
                }
            }
            
            // Search matching
            boolean matchesSearch = true;
            if (currentSearchQuery != null && !currentSearchQuery.isEmpty()) {
                String itemTitle = item.getTitle();
                if (itemTitle == null) {
                    matchesSearch = false;
                } else {
                    matchesSearch = itemTitle.toLowerCase().contains(currentSearchQuery.toLowerCase());
                }
            }
            
            if (matchesCategory && matchesSearch) {
                filteredItems.add(item);
            }
        }
        
        Log.d(TAG, "Filter applied - Showing " + filteredItems.size() + " items out of " + allItems.size() + " total");
        
        // Safety check: If filtering resulted in zero items but we have items in allItems,
        // and category is "All" with no search, show all items (might be a filtering bug)
        if (filteredItems.isEmpty() && !allItems.isEmpty() && 
            (currentCategory == null || currentCategory.equals("All")) && 
            (currentSearchQuery == null || currentSearchQuery.isEmpty())) {
            Log.w(TAG, "Filter resulted in zero items but category is 'All' - showing all items as fallback");
            filteredItems.addAll(allItems);
        }
        
        browseItemAdapter.updateItems(filteredItems);
        
        // Update empty state with helpful message
        if (filteredItems.isEmpty()) {
            String emptyMessage;
            if (!allItems.isEmpty()) {
                // We have items but they don't match the filter
                if (currentSearchQuery != null && !currentSearchQuery.isEmpty()) {
                    emptyMessage = String.format("No items found for \"%s\". Try a different search term.", currentSearchQuery);
                } else if (currentCategory != null && !currentCategory.equals("All")) {
                    emptyMessage = String.format("No items in %s category. Try browsing all categories.", currentCategory);
                } else {
                    emptyMessage = "No items available. Check back later!";
                }
            } else {
                emptyMessage = "No items available. Pull down to refresh.";
            }
            showEmptyState(emptyMessage);
        } else {
            if (emptyStateText != null) {
                emptyStateText.setVisibility(View.GONE);
            }
            recyclerViewBrowse.setVisibility(View.VISIBLE);
        }
    }

    private void showFilterDialog() {
        Dialog filterDialog = new Dialog(this);
        filterDialog.setContentView(R.layout.filter_modal);
        filterDialog.getWindow().setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
        );
        
        // Initialize filter dialog components
        CheckBox checkboxElectronics = filterDialog.findViewById(R.id.checkboxElectronics);
        CheckBox checkboxFashion = filterDialog.findViewById(R.id.checkboxFashion);
        CheckBox checkboxHome = filterDialog.findViewById(R.id.checkboxHome);
        CheckBox checkboxCollectibles = filterDialog.findViewById(R.id.checkboxCollectibles);
        
        SeekBar priceRangeSeekBar = filterDialog.findViewById(R.id.priceRangeSeekBar);
        TextView priceRangeText = filterDialog.findViewById(R.id.priceRangeText);
        
        RadioGroup conditionRadioGroup = filterDialog.findViewById(R.id.conditionRadioGroup);
        RadioGroup statusRadioGroup = filterDialog.findViewById(R.id.statusRadioGroup);
        
        Button btnClearFilters = filterDialog.findViewById(R.id.btnClearFilters);
        Button btnApplyFilters = filterDialog.findViewById(R.id.btnApplyFilters);
        
        // Set up price range seekbar
        priceRangeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (priceRangeText != null) {
                    priceRangeText.setText("$" + progress);
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // Clear filters
        btnClearFilters.setOnClickListener(v -> {
            checkboxElectronics.setChecked(true);
            checkboxFashion.setChecked(false);
            checkboxHome.setChecked(false);
            checkboxCollectibles.setChecked(false);
            priceRangeSeekBar.setProgress(500);
            conditionRadioGroup.check(R.id.radioNew);
            statusRadioGroup.check(R.id.radioAll);
        });
        
        // Apply filters
        btnApplyFilters.setOnClickListener(v -> {
            // Apply filter logic here
            filterDialog.dismiss();
            ToastHelper.showInfo(this, "Filters applied!");
        });
        
        filterDialog.show();
    }

    private void onItemClick(BrowseItem item) {
        // Navigate to item detail
        Intent intent = new Intent(this, ItemDetailActivity.class);
        intent.putExtra("item_id", item.getItemId());
        intent.putExtra("item_title", item.getTitle());
        intent.putExtra("item_bid", item.getCurrentBid());
        intent.putExtra("item_time_left", item.getTimeLeft());
        intent.putExtra("item_category", item.getCategory());
        intent.putExtra("item_is_buy_now", item.isBuyNow());
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // When returning from ItemDetailActivity, make sure items are still visible
        // Only reload if we don't have items - otherwise just reapply filters
        if (allItems.isEmpty() && !isLoading) {
            Log.d(TAG, "No items loaded, refreshing on resume");
            loadItemsFromApi();
        } else if (!allItems.isEmpty()) {
            // Just reapply filters to ensure items are visible - don't reload
            Log.d(TAG, "Reapplying filters on resume - " + allItems.size() + " items available");
            applyFilters();
        }
        // If isLoading is true, don't do anything - let the current load finish
    }

    @Override
    protected boolean isCurrentActivity(int itemId) {
        return itemId == R.id.nav_browse;
    }

    @Override
    protected void setCurrentTabSelected() {
        bottomNavigationView.setSelectedItemId(R.id.nav_browse);
    }

    @Override
    public String getCurrentUserEmail() {
        return loggedInUserEmail;
    }
}
