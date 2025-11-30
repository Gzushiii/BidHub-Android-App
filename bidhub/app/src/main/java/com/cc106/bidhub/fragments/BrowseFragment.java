package com.cc106.bidhub.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.cc106.bidhub.toast.ToastHelper;
import com.cc106.bidhub.utils.SharedPreferencesHelper;
import com.cc106.bidhub.adapters.ItemCardAdapter;
import com.cc106.bidhub.items.Item;
import com.cc106.bidhub.items.ItemManager;
import com.cc106.bidhub.items.ItemStatus;
import com.cc106.bidhub.items.FilterCriteria;
import com.cc106.bidhub.items.Category;
import com.cc106.bidhub.ItemDetailActivity;
import com.cc106.bidhub.AdvancedFilterActivity;
import com.cc106.bidhub.CategorySelectionActivity;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.cc106.bidhub.R;

import java.util.ArrayList;
import java.util.List;

public class BrowseFragment extends Fragment implements ItemCardAdapter.OnItemClickListener {

    private String loggedInUserEmail;
    private ItemManager itemManager;
    
    // UI Components
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextInputEditText etSearch;
    private ImageButton btnSort;
    private ImageButton btnViewToggle;
    private RecyclerView rvItems;
    private ProgressBar progressBar;
    private LinearLayout layoutEmptyState;
    private LinearLayout layoutFilterChipsContainer;
    private HorizontalScrollView hsvFilterChips;
    private LinearLayout layoutFilterChips;
    private Button btnClearAllFilters;
    private TextView tvItemCount;
    private ImageView ivEmptyIcon;
    private TextView tvEmptySuggestionsTitle;
    private LinearLayout layoutEmptySuggestions;
    private LinearLayout layoutEmptyActions;
    private Button btnClearSearch;
    private Button btnBrowseCategories;
    
    // Adapter and Data
    private ItemCardAdapter itemAdapter;
    private List<Item> allItems;
    private List<Item> filteredItems;
    private FilterCriteria currentFilter;
    
    // Search handling
    private Handler searchHandler;
    private Runnable searchRunnable;
    private static final int SEARCH_DELAY = 500; // 500ms delay
    
    // Filter chips
    private List<String> activeFilters;
    private List<String> recentSearches;
    private List<String> savedSearches;
    
    // View state
    private boolean isGridView = true;
    private boolean isLoadingMore = false;
    private boolean isLoading = false; // Prevent duplicate loading
    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 20;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_browse, container, false);
        
        // Get the logged-in user's email from arguments
        if (getArguments() != null) {
            loggedInUserEmail = getArguments().getString("USER_EMAIL");
        }
        
        initializeViews(view);
        setupRecyclerView();
        setupSearch();
        setupFilter();
        loadItems();
        
        return view;
    }
    
    private void initializeViews(View view) {
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        etSearch = view.findViewById(R.id.et_search);
        btnSort = view.findViewById(R.id.btn_sort);
        btnViewToggle = view.findViewById(R.id.btn_view_toggle);
        rvItems = view.findViewById(R.id.rv_items);
        progressBar = view.findViewById(R.id.progress_bar);
        layoutEmptyState = view.findViewById(R.id.layout_empty_state);
        layoutFilterChipsContainer = view.findViewById(R.id.layout_filter_chips_container);
        hsvFilterChips = view.findViewById(R.id.hsv_filter_chips);
        layoutFilterChips = view.findViewById(R.id.layout_filter_chips);
        btnClearAllFilters = view.findViewById(R.id.btn_clear_all_filters);
        tvItemCount = view.findViewById(R.id.tv_item_count);
        ivEmptyIcon = view.findViewById(R.id.iv_empty_icon);
        tvEmptySuggestionsTitle = view.findViewById(R.id.tv_empty_suggestions_title);
        layoutEmptySuggestions = view.findViewById(R.id.layout_empty_suggestions);
        layoutEmptyActions = view.findViewById(R.id.layout_empty_actions);
        btnClearSearch = view.findViewById(R.id.btn_clear_search);
        btnBrowseCategories = view.findViewById(R.id.btn_browse_categories);
        
        itemManager = ItemManager.getInstance(getContext());
        allItems = new ArrayList<>();
        filteredItems = new ArrayList<>();
        currentFilter = new FilterCriteria();
        activeFilters = new ArrayList<>();
        searchHandler = new Handler(Looper.getMainLooper());
        
        // Setup SwipeRefreshLayout
        setupSwipeRefresh();
    }
    
    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(
            R.color.primary_blue,
            R.color.accent_orange,
            R.color.success_green
        );
        
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // Refresh items when user pulls down
            // Prevent duplicate refresh calls
            if (!isLoading) {
                android.util.Log.d("BrowseFragment", "Swipe refresh triggered");
                loadItems();
            } else {
                android.util.Log.d("BrowseFragment", "Swipe refresh ignored - already loading");
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }
    
    private void setupRecyclerView() {
        if (itemAdapter == null) {
            itemAdapter = new ItemCardAdapter(filteredItems);
            itemAdapter.setOnItemClickListener(this);
        } else {
            itemAdapter.updateItems(filteredItems);
        }
        
        // Set layout manager based on view type
        if (isGridView) {
            // Use GridLayoutManager with 2 columns and proper spacing
            GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
            // Add spacing between items
            androidx.recyclerview.widget.RecyclerView.ItemDecoration spacingDecoration = 
                new androidx.recyclerview.widget.RecyclerView.ItemDecoration() {
                    @Override
                    public void getItemOffsets(android.graphics.Rect outRect, View view, 
                                             androidx.recyclerview.widget.RecyclerView parent, 
                                             androidx.recyclerview.widget.RecyclerView.State state) {
                        int spacing = (int) (8 * view.getContext().getResources().getDisplayMetrics().density);
                        outRect.left = spacing / 2;
                        outRect.right = spacing / 2;
                        outRect.top = spacing / 2;
                        outRect.bottom = spacing / 2;
                    }
                };
            // Remove existing decorations to avoid duplicates
            if (rvItems.getItemDecorationCount() > 0) {
                for (int i = rvItems.getItemDecorationCount() - 1; i >= 0; i--) {
                    rvItems.removeItemDecorationAt(i);
                }
            }
            rvItems.addItemDecoration(spacingDecoration);
            rvItems.setLayoutManager(layoutManager);
        } else {
            LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
            rvItems.setLayoutManager(layoutManager);
        }
        
        rvItems.setAdapter(itemAdapter);
        // Ensure RecyclerView doesn't clip children
        rvItems.setClipToPadding(false);
        rvItems.setClipChildren(false);
    }
    
    private void setupSearch() {
        searchHandler = new Handler(Looper.getMainLooper());
        recentSearches = new ArrayList<>();
        savedSearches = new ArrayList<>();
        
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Cancel previous search
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                
                // Only search if there's actual text or if we're clearing the search
                String query = s.toString().trim();
                if (!query.isEmpty() || before > 0) {
                    // Show search suggestions if query is not empty
                    if (query.length() > 0) {
                        showSearchSuggestions(query);
                    } else {
                        hideSearchSuggestions();
                    }
                    
                    // Schedule new search
                    searchRunnable = () -> performSearch(query);
                    searchHandler.postDelayed(searchRunnable, SEARCH_DELAY);
                }
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // Add search focus listener
        etSearch.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && etSearch.getText().toString().isEmpty()) {
                showRecentSearches();
            } else if (!hasFocus) {
                hideSearchSuggestions();
            }
        });
    }
    
    private void showSearchSuggestions(String query) {
        // TODO: Implement search suggestions dropdown
        // This would show a dropdown with matching search terms
        android.util.Log.d("BrowseFragment", "Showing search suggestions for: " + query);
    }
    
    private void hideSearchSuggestions() {
        // TODO: Hide search suggestions dropdown
        android.util.Log.d("BrowseFragment", "Hiding search suggestions");
    }
    
    private void showRecentSearches() {
        // TODO: Show recent searches dropdown
        android.util.Log.d("BrowseFragment", "Showing recent searches");
    }
    
    private void addToRecentSearches(String query) {
        if (query != null && !query.trim().isEmpty()) {
            recentSearches.remove(query); // Remove if already exists
            recentSearches.add(0, query); // Add to beginning
            
            // Keep only last 10 searches
            if (recentSearches.size() > 10) {
                recentSearches = recentSearches.subList(0, 10);
            }
        }
    }
    
    private void setupFilter() {
        btnSort.setOnClickListener(v -> showSortDialog());
        btnClearAllFilters.setOnClickListener(v -> clearAllFilters());
        btnViewToggle.setOnClickListener(v -> toggleView());
        btnClearSearch.setOnClickListener(v -> clearSearch());
        btnBrowseCategories.setOnClickListener(v -> showCategorySelection());
    }
    
    
    private void showSortDialog() {
        String[] sortOptions = {
            "Newest First",
            "Oldest First", 
            "Price: Low to High",
            "Price: High to Low",
            "Ending Soon",
            "Most Popular",
            "Most Bids"
        };
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Sort Items")
               .setItems(sortOptions, (dialog, which) -> {
                   applySorting(which);
               });
        builder.show();
    }
    
    private void applySorting(int sortIndex) {
        switch (sortIndex) {
            case 0: // Newest First
                currentFilter.setSortBy("createdAt");
                currentFilter.setSortOrder("DESC");
                break;
            case 1: // Oldest First
                currentFilter.setSortBy("createdAt");
                currentFilter.setSortOrder("ASC");
                break;
            case 2: // Price: Low to High
                currentFilter.setSortBy("price");
                currentFilter.setSortOrder("ASC");
                break;
            case 3: // Price: High to Low
                currentFilter.setSortBy("price");
                currentFilter.setSortOrder("DESC");
                break;
            case 4: // Ending Soon
                currentFilter.setSortBy("endDate");
                currentFilter.setSortOrder("ASC");
                break;
            case 5: // Most Popular
                currentFilter.setSortBy("viewCount");
                currentFilter.setSortOrder("DESC");
                break;
            case 6: // Most Bids
                currentFilter.setSortBy("bidCount");
                currentFilter.setSortOrder("DESC");
                break;
        }
        applyFilters();
    }
    
    private void performSearch(String query) {
        if (!query.isEmpty()) {
            addToRecentSearches(query);
        }
        currentFilter.setQuery(query.isEmpty() ? null : query);
        applyFilters();
    }
    
    private void applyFilters() {
        showLoading(true);

        // Debug: Log before filtering with comprehensive filter info
        android.util.Log.d("BrowseFragment", "Applying filters to " + allItems.size() + " items");
        android.util.Log.d("BrowseFragment", "Current filter criteria (raw): " + (currentFilter != null ? currentFilter.toString() : "null"));

        // Normalize filter criteria to convert "null" strings to actual null
        FilterCriteria normalizedFilter = FilterCriteria.normalize(currentFilter);

        // Update currentFilter to use normalized version to prevent future issues
        currentFilter = normalizedFilter;

        // Enhanced logging to verify normalization worked
        android.util.Log.d("BrowseFragment", "Current filter criteria (normalized): " + normalizedFilter.toString());
        android.util.Log.d("BrowseFragment", "  - query: " + (normalizedFilter.getQuery() == null ? "null" : "'" + normalizedFilter.getQuery() + "'"));
        android.util.Log.d("BrowseFragment", "  - categoryId: " + (normalizedFilter.getCategoryId() == null ? "null" : "'" + normalizedFilter.getCategoryId() + "'"));
        
        // Verify normalization worked by checking actual field values
        if (normalizedFilter != null) {
            boolean hasActualNulls = (normalizedFilter.getQuery() == null) && 
                                     (normalizedFilter.getCategoryId() == null) && 
                                     (normalizedFilter.getCondition() == null);
            
            if (hasActualNulls) {
                android.util.Log.d("BrowseFragment", "Normalization successful - all fields are actual nulls");
            }
        }
        android.util.Log.d("BrowseFragment", "  - condition: " + (normalizedFilter.getCondition() == null ? "null" : "'" + normalizedFilter.getCondition() + "'"));
        android.util.Log.d("BrowseFragment", "  - limit: " + normalizedFilter.getLimit() + ", offset: " + normalizedFilter.getOffset());

        // Perform filtering on background thread
        new Thread(() -> {
            List<Item> results = itemManager.filterItems(normalizedFilter);

            // Debug: Log filter results
            android.util.Log.d("BrowseFragment", "Filter results: " + results.size() + " items");

            // Defensive fallback: if filtering resulted in 0 items but we have items, use unfiltered list
            final List<Item> finalResults;
            if (results.isEmpty() && !allItems.isEmpty() && normalizedFilter.isAllCriteriaNull()) {
                android.util.Log.w("BrowseFragment", "WARNING: Filtering resulted in 0 items from " + allItems.size() + " total items with default filters. Using unfiltered list as fallback.");
                finalResults = new ArrayList<>(allItems);
            } else {
                finalResults = results;
            }

            // Update UI on main thread - check if fragment is still attached
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (isAdded()) { // Check if fragment is still added to activity
                        filteredItems.clear();
                        filteredItems.addAll(finalResults);
                        itemAdapter.notifyDataSetChanged();

                        // Debug: Log after updating adapter
                        android.util.Log.d("BrowseFragment", "Adapter updated with " + filteredItems.size() + " filtered items");

                        showLoading(false);
                        updateEmptyState();
                        updateItemCount();
                    }
                });
            }
        }).start();
    }
    
    public void loadItems() {
        // Prevent duplicate loading
        if (isLoading) {
            android.util.Log.d("BrowseFragment", "Already loading items, skipping duplicate load");
            return;
        }
        
        isLoading = true;
        showLoading(true);
        
        // Debug: Log before loading
        android.util.Log.d("BrowseFragment", "Loading items...");
        
        // Try to load from database first, fallback to local
        loadItemsFromDatabase();
    }
    
    private void loadItemsFromDatabase() {
        // Try to fetch from backend API first
        new Thread(() -> {
            try {
                // Verify authentication token before making request
                SharedPreferencesHelper prefsHelper = new SharedPreferencesHelper(getContext());
                String token = prefsHelper.getAuthToken();
                if (token == null || token.isEmpty()) {
                    android.util.Log.w("BrowseFragment", "No auth token available, using local items");
                    if (getActivity() != null && !getActivity().isFinishing()) {
                        getActivity().runOnUiThread(() -> {
                            isLoading = false;
                            loadLocalItems();
                        });
                    } else {
                        isLoading = false;
                    }
                    return;
                }
                
                com.cc106.bidhub.api.ItemApiClient apiClient = new com.cc106.bidhub.api.ItemApiClient(getContext());
                com.cc106.bidhub.api.ItemApiClient.ApiResponse response = apiClient.getItems(null, null, null, null, null, 100, 0);
                
                if (response.isSuccess() && response.getData() != null) {
                    // Parse and display items from database
                    List<Item> dbItems = parseItemsFromResponse(response.getData());
                    // Filter to only show ACTIVE items
                    List<Item> activeItems = dbItems.stream()
                        .filter(item -> item.getStatus() == com.cc106.bidhub.items.ItemStatus.ACTIVE)
                        .collect(java.util.stream.Collectors.toList());
                    android.util.Log.d("BrowseFragment", "Loaded " + activeItems.size() + " active items from database (filtered from " + dbItems.size() + " total)");
                    
                    // CRITICAL: Only update items if we have valid data
                    if (!activeItems.isEmpty() || dbItems.isEmpty()) {
                        // Only clear if we have new data OR if API explicitly returned empty (not an error)
                        if (getActivity() != null && !getActivity().isFinishing()) {
                            getActivity().runOnUiThread(() -> {
                                if (isAdded() && !isDetached()) {
                                    // Preserve existing items if new data is empty (might be temporary)
                                    if (!activeItems.isEmpty()) {
                                        allItems.clear();
                                        allItems.addAll(activeItems);
                                        android.util.Log.d("BrowseFragment", "Updated allItems with " + activeItems.size() + " items from API");
                                    } else {
                                        android.util.Log.w("BrowseFragment", "API returned empty items list - preserving existing " + allItems.size() + " items");
                                    }
                                    isLoading = false;
                                    swipeRefreshLayout.setRefreshing(false);
                                    applyFilters();
                                } else {
                                    isLoading = false;
                                    swipeRefreshLayout.setRefreshing(false);
                                }
                            });
                        } else {
                            isLoading = false;
                        }
                    } else {
                        // API returned data but all items were filtered out - preserve existing
                        android.util.Log.w("BrowseFragment", "All items filtered out, preserving existing items");
                        if (getActivity() != null && !getActivity().isFinishing()) {
                            getActivity().runOnUiThread(() -> {
                                isLoading = false;
                                swipeRefreshLayout.setRefreshing(false);
                                // Don't clear items, just refresh filters
                                applyFilters();
                            });
                        } else {
                            isLoading = false;
                        }
                    }
                } else {
                    // API call failed - preserve existing items, don't clear
                    android.util.Log.w("BrowseFragment", "Database fetch failed: " + (response.getMessage() != null ? response.getMessage() : "Unknown error") + ", preserving existing items");
                    if (getActivity() != null && !getActivity().isFinishing()) {
                        getActivity().runOnUiThread(() -> {
                            isLoading = false;
                            swipeRefreshLayout.setRefreshing(false);
                            // Don't clear items on error - preserve what we have
                            android.util.Log.d("BrowseFragment", "Preserving " + allItems.size() + " existing items after API failure");
                            // Try local fallback only if we have no items
                            if (allItems.isEmpty()) {
                                loadLocalItems();
                            } else {
                                // Refresh display with existing items
                                applyFilters();
                            }
                        });
                    } else {
                        isLoading = false;
                    }
                }
            } catch (Exception e) {
                // Fallback to local items
                android.util.Log.e("BrowseFragment", "Error fetching from database", e);
                if (getActivity() != null && !getActivity().isFinishing()) {
                    getActivity().runOnUiThread(() -> {
                        isLoading = false; // Reset loading flag
                        loadLocalItems();
                    });
                } else {
                    isLoading = false;
                }
            }
        }).start();
    }
    
    private void loadLocalItems() {
        // Load items on background thread
        new Thread(() -> {
            try {
                List<Item> items = itemManager.getAllBrowsableItems();
                // Filter to only show ACTIVE items
                List<Item> activeItems = items.stream()
                    .filter(item -> item.getStatus() == com.cc106.bidhub.items.ItemStatus.ACTIVE)
                    .collect(java.util.stream.Collectors.toList());
                
                // Debug: Log item count
                android.util.Log.d("BrowseFragment", "Loaded " + activeItems.size() + " active local items (filtered from " + items.size() + " total)");
                
                // Update UI on main thread - check if fragment is still attached
                if (getActivity() != null && !getActivity().isFinishing()) {
                    getActivity().runOnUiThread(() -> {
                        if (isAdded() && !isDetached()) { // Check if fragment is still added to activity
                            // Only update if we have items, otherwise preserve existing
                            if (!activeItems.isEmpty()) {
                                allItems.clear();
                                allItems.addAll(activeItems);
                                android.util.Log.d("BrowseFragment", "Updated allItems with " + allItems.size() + " active local items");
                            } else {
                                android.util.Log.w("BrowseFragment", "Local items list is empty - preserving existing " + allItems.size() + " items");
                            }
                            isLoading = false; // Reset loading flag
                            swipeRefreshLayout.setRefreshing(false);
                            
                            // Apply current filters
                            applyFilters();
                        } else {
                            isLoading = false;
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    });
                } else {
                    isLoading = false;
                }
            } catch (Exception e) {
                android.util.Log.e("BrowseFragment", "Error loading local items", e);
                if (getActivity() != null && !getActivity().isFinishing()) {
                    getActivity().runOnUiThread(() -> {
                        isLoading = false;
                        showLoading(false);
                        updateEmptyState();
                    });
                } else {
                    isLoading = false;
                }
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
                
                item.setItemId(itemJson.getString("id"));
                item.setTitle(itemJson.getString("title"));
                item.setDescription(itemJson.getString("description"));
                item.setStartingPrice(itemJson.getDouble("starting_bid"));
                item.setCurrentPrice(itemJson.getDouble("current_bid"));
                item.setCategoryId(itemJson.getString("category_id"));
                item.setSellerId(itemJson.optString("seller_email", itemJson.optString("seller_id", "")));
                
                // Set seller username from API response
                String sellerUsername = itemJson.optString("seller_username", null);
                if (sellerUsername != null && !sellerUsername.isEmpty()) {
                    item.setSellerName(sellerUsername);
                } else {
                    // Fallback: extract from email if username not available
                    String sellerEmail = itemJson.optString("seller_email", "");
                    if (!sellerEmail.isEmpty()) {
                        int atIndex = sellerEmail.indexOf('@');
                        if (atIndex > 0) {
                            item.setSellerName(sellerEmail.substring(0, atIndex));
                        } else {
                            item.setSellerName("Unknown");
                        }
                    } else {
                        item.setSellerName("Unknown");
                    }
                }
                
                // Set bid count from API response
                int bidCount = itemJson.optInt("bid_count", 0);
                item.setBidCount(bidCount);
                
                // Set end date for countdown
                if (itemJson.has("end_date") && !itemJson.isNull("end_date")) {
                    try {
                        String endDateStr = itemJson.getString("end_date");
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault());
                        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                        item.setEndDate(sdf.parse(endDateStr));
                    } catch (Exception e) {
                        android.util.Log.w("BrowseFragment", "Error parsing end_date: " + e.getMessage());
                    }
                } else if (itemJson.has("bid_deadline") && !itemJson.isNull("bid_deadline")) {
                    try {
                        String deadlineStr = itemJson.getString("bid_deadline");
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault());
                        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                        item.setEndDate(sdf.parse(deadlineStr));
                    } catch (Exception e) {
                        android.util.Log.w("BrowseFragment", "Error parsing bid_deadline: " + e.getMessage());
                    }
                }
                
                item.setCondition(itemJson.optString("item_condition", itemJson.optString("condition", "good")));
                item.setStatus(ItemStatus.ACTIVE);
                
                // Parse images if available - handle both JSON array and JSON string
                if (itemJson.has("images")) {
                    try {
                        Object imagesObj = itemJson.get("images");
                        List<String> imagePaths = new ArrayList<>();
                        
                        if (imagesObj instanceof org.json.JSONArray) {
                            // Images is already a JSON array
                            org.json.JSONArray imagesArray = (org.json.JSONArray) imagesObj;
                            for (int j = 0; j < imagesArray.length(); j++) {
                                imagePaths.add(imagesArray.getString(j));
                            }
                        } else if (imagesObj instanceof String) {
                            // Images is a JSON string, parse it
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
                        android.util.Log.w("BrowseFragment", "Error parsing images for item: " + item.getTitle(), e);
                        // Set empty list as fallback
                        item.setImagePaths(new ArrayList<>());
                    }
                }
                
                items.add(item);
            }
        } catch (Exception e) {
            android.util.Log.e("BrowseFragment", "Error parsing items from response", e);
        }
        return items;
    }
    
    private void showLoading(boolean show) {
        if (show) {
            // Don't show progress bar if SwipeRefreshLayout is already showing
            if (!swipeRefreshLayout.isRefreshing()) {
                progressBar.setVisibility(View.VISIBLE);
            }
            rvItems.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            rvItems.setVisibility(View.VISIBLE);
            // Stop SwipeRefreshLayout if it's refreshing
            if (swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
        }
    }
    
    private void updateEmptyState() {
        if (filteredItems.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            rvItems.setVisibility(View.GONE);
            updateEmptyStateMessage();
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            rvItems.setVisibility(View.VISIBLE);
        }
    }
    
    private void updateEmptyStateMessage() {
        // Find the empty state text views
        TextView tvEmptyTitle = layoutEmptyState.findViewById(R.id.tv_empty_title);
        TextView tvEmptySubtitle = layoutEmptyState.findViewById(R.id.tv_empty_subtitle);
        
        if (tvEmptyTitle == null || tvEmptySubtitle == null) {
            return; // Views not found, use default layout
        }
        
        // Determine the type of empty state based on current filters
        boolean hasSearchQuery = currentFilter.getQuery() != null && !currentFilter.getQuery().trim().isEmpty();
        boolean hasActiveFilters = !activeFilters.isEmpty();
        
        if (hasSearchQuery && hasActiveFilters) {
            tvEmptyTitle.setText("No items found");
            tvEmptySubtitle.setText("No items match your search and filter criteria");
            ivEmptyIcon.setImageResource(R.drawable.ic_search);
            tvEmptySuggestionsTitle.setVisibility(View.VISIBLE);
            layoutEmptySuggestions.setVisibility(View.VISIBLE);
            layoutEmptyActions.setVisibility(View.VISIBLE);
        } else if (hasSearchQuery) {
            tvEmptyTitle.setText("No search results");
            tvEmptySubtitle.setText("No items match your search terms");
            ivEmptyIcon.setImageResource(R.drawable.ic_search);
            tvEmptySuggestionsTitle.setVisibility(View.VISIBLE);
            layoutEmptySuggestions.setVisibility(View.VISIBLE);
            layoutEmptyActions.setVisibility(View.VISIBLE);
        } else if (hasActiveFilters) {
            tvEmptyTitle.setText("No filtered results");
            tvEmptySubtitle.setText("No items match your filter criteria");
            ivEmptyIcon.setImageResource(R.drawable.ic_filter);
            tvEmptySuggestionsTitle.setVisibility(View.VISIBLE);
            layoutEmptySuggestions.setVisibility(View.VISIBLE);
            layoutEmptyActions.setVisibility(View.VISIBLE);
        } else {
            tvEmptyTitle.setText("No items available");
            tvEmptySubtitle.setText("There are currently no items to browse");
            ivEmptyIcon.setImageResource(R.drawable.ic_shopping_bag);
            tvEmptySuggestionsTitle.setVisibility(View.GONE);
            layoutEmptySuggestions.setVisibility(View.GONE);
            layoutEmptyActions.setVisibility(View.GONE);
        }
    }
    
    private void showFilterDialog() {
        Intent intent = new Intent(getContext(), AdvancedFilterActivity.class);
        intent.putExtra("USER_EMAIL", loggedInUserEmail);
        intent.putExtra("FILTER_CRITERIA", currentFilter);
        startActivityForResult(intent, 1001);
    }
    
    private void onFilterApplied(FilterCriteria newFilter) {
        // Normalize the filter before applying it
        currentFilter = FilterCriteria.normalize(newFilter);
        applyFilters();
        updateFilterChips();
    }
    
    private void updateFilterChips() {
        // Clear existing chips
        layoutFilterChips.removeAllViews();
        activeFilters.clear();
        
        // Add chips based on active filters
        if (currentFilter.getCategoryId() != null) {
            Category category = itemManager.getCategoryById(currentFilter.getCategoryId());
            if (category != null) {
                addFilterChip("Category: " + category.getName());
            }
        }
        
        if (currentFilter.hasPriceRange()) {
            String priceText = "Price: ";
            if (currentFilter.getMinPrice() != null) {
                priceText += "₱" + String.format("%.0f", currentFilter.getMinPrice());
            }
            if (currentFilter.getMaxPrice() != null) {
                priceText += " - ₱" + String.format("%.0f", currentFilter.getMaxPrice());
            }
            addFilterChip(priceText);
        }
        
        if (currentFilter.getCondition() != null) {
            addFilterChip("Condition: " + currentFilter.getCondition());
        }
        
        
        if (currentFilter.getIsFeatured() != null && currentFilter.getIsFeatured()) {
            addFilterChip("Featured");
        }
        
        if (currentFilter.getIsTrending() != null && currentFilter.getIsTrending()) {
            addFilterChip("Trending");
        }
    }
    
    private void addFilterChip(String filterText) {
        if (activeFilters.contains(filterText)) {
            return;
        }
        
        activeFilters.add(filterText);
        
        Chip chip = new Chip(getContext());
        chip.setText(filterText);
        chip.setCloseIconVisible(true);
        chip.setCloseIconTint(getResources().getColorStateList(android.R.color.white));
        chip.setChipBackgroundColorResource(R.color.primary_blue);
        chip.setTextColor(getResources().getColor(android.R.color.white));
        chip.setChipStrokeWidth(0);
        chip.setChipCornerRadius(16);
        chip.setPadding(16, 8, 16, 8);
        chip.setOnCloseIconClickListener(v -> removeFilterChip(chip, filterText));
        
        // Add margin to the chip
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 8, 0);
        chip.setLayoutParams(params);
        
        layoutFilterChips.addView(chip);
        layoutFilterChipsContainer.setVisibility(View.VISIBLE);
    }
    
    private void removeFilterChip(Chip chip, String filterText) {
        activeFilters.remove(filterText);
        layoutFilterChips.removeView(chip);
        
        // Reset the corresponding filter criteria
        resetFilterCriteria(filterText);
        
        if (activeFilters.isEmpty()) {
            layoutFilterChipsContainer.setVisibility(View.GONE);
        }
        
        // Reapply filters
        applyFilters();
    }
    
    private void resetFilterCriteria(String filterText) {
        if (filterText.startsWith("Category:")) {
            currentFilter.setCategoryId(null);
        } else if (filterText.startsWith("Price:")) {
            currentFilter.setMinPrice(null);
            currentFilter.setMaxPrice(null);
        } else if (filterText.startsWith("Condition:")) {
            currentFilter.setCondition(null);
        } else if (filterText.startsWith("Location:")) {
        } else if (filterText.equals("Featured")) {
            currentFilter.setIsFeatured(null);
        } else if (filterText.equals("Trending")) {
            currentFilter.setIsTrending(null);
        }
    }
    
    /**
     * Clear all active filters and search query
     */
    public void clearAllFilters() {
        // Clear search
        etSearch.setText("");
        
        // Reset filter criteria and normalize it immediately
        currentFilter = new FilterCriteria();
        currentFilter = FilterCriteria.normalize(currentFilter);
        
        // Clear filter chips
        layoutFilterChips.removeAllViews();
        activeFilters.clear();
        layoutFilterChipsContainer.setVisibility(View.GONE);
        
        // Reload items
        loadItems();
    }
    
    @Override
    public void onItemClick(Item item) {
        try {
            // Validate item data
            if (item == null) {
                ToastHelper.showError(getContext(), "Item data is invalid");
                return;
            }
            
            if (item.getItemId() == null || item.getItemId().isEmpty()) {
                ToastHelper.showError(getContext(), "Item ID is missing");
                return;
            }
            
            if (loggedInUserEmail == null || loggedInUserEmail.isEmpty()) {
                ToastHelper.showError(getContext(), "User session expired. Please login again.");
                return;
            }
            
            // Check if fragment is still attached
            if (!isAdded() || getContext() == null) {
                return;
            }
            
            // Navigate to item detail activity
            Intent intent = new Intent(getContext(), ItemDetailActivity.class);
            intent.putExtra("ITEM_ID", item.getItemId());
            intent.putExtra("USER_EMAIL", loggedInUserEmail);
            startActivity(intent);
            
        } catch (Exception e) {
            if (getContext() != null) {
                ToastHelper.showError(getContext(), "Error opening item details: " + e.getMessage());
            }
            e.printStackTrace();
        }
    }
    
    public void updateUserEmail(String email) {
        this.loggedInUserEmail = email;
    }
    
    /**
     * Set category filter by category name
     * Called when navigating from HomeFragment category chips
     */
    public void setCategoryFilter(String categoryName) {
        if (categoryName == null || categoryName.isEmpty()) {
            return;
        }
        
        // Find category by name
        com.cc106.bidhub.items.CategoryManager categoryManager = com.cc106.bidhub.items.CategoryManager.getInstance();
        List<Category> allCategories = categoryManager.getAllMainCategories();
        
        for (Category category : allCategories) {
            if (category.getName() != null && category.getName().equalsIgnoreCase(categoryName)) {
                // Set category filter
                if (currentFilter == null) {
                    currentFilter = new FilterCriteria();
                }
                currentFilter.setCategoryId(category.getCategoryId());
                
                // Apply filters to show filtered items
                applyFilters();
                return;
            }
        }
        
        // If category not found by name, try to match by common names
        // Map common display names to category names
        String normalizedName = categoryName.toLowerCase();
        String categoryId = null;
        
        if (normalizedName.contains("electronics") || normalizedName.contains("electronic")) {
            categoryId = findCategoryIdByName("Electronics", allCategories);
        } else if (normalizedName.contains("fashion") || normalizedName.contains("clothing")) {
            categoryId = findCategoryIdByName("Fashion", allCategories);
        } else if (normalizedName.contains("collectible")) {
            categoryId = findCategoryIdByName("Collectibles", allCategories);
        } else if (normalizedName.contains("home") || normalizedName.contains("garden")) {
            categoryId = findCategoryIdByName("Home & Garden", allCategories);
        } else if (normalizedName.contains("art")) {
            categoryId = findCategoryIdByName("Art", allCategories);
        }
        
        if (categoryId != null) {
            if (currentFilter == null) {
                currentFilter = new FilterCriteria();
            }
            currentFilter.setCategoryId(categoryId);
            applyFilters();
        }
    }
    
    private String findCategoryIdByName(String name, List<Category> categories) {
        for (Category category : categories) {
            if (category.getName() != null && category.getName().equalsIgnoreCase(name)) {
                return category.getCategoryId();
            }
        }
        return null;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Only refresh if items are empty or if explicitly needed
        // Don't refresh on every resume to prevent items disappearing
        if (allItems.isEmpty() && !isLoading) {
            android.util.Log.d("BrowseFragment", "onResume: Items empty, loading...");
            loadItems();
        } else {
            android.util.Log.d("BrowseFragment", "Skipping refresh on resume - items already loaded");
        }
        // Only refresh if items list is empty or if explicitly needed
        // This prevents duplicate loads when fragment is already visible
        if (allItems.isEmpty() && !isLoading) {
            android.util.Log.d("BrowseFragment", "Items list is empty, refreshing on resume");
            loadItems();
        } else {
            android.util.Log.d("BrowseFragment", "Skipping refresh on resume - items already loaded");
        }
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case 1001: // AdvancedFilterActivity
                    if (data != null && data.getSerializableExtra("FILTER_CRITERIA") != null) {
                        FilterCriteria newFilter = (FilterCriteria) data.getSerializableExtra("FILTER_CRITERIA");
                        onFilterApplied(newFilter);
                    }
                    break;
                case 1002: // CategorySelectionActivity
                    if (data != null && data.getSerializableExtra("SELECTED_CATEGORY") != null) {
                        Category selectedCategory = (Category) data.getSerializableExtra("SELECTED_CATEGORY");
                        currentFilter.setCategoryId(selectedCategory.getCategoryId());
                        applyFilters();
                        updateFilterChips();
                    }
                    break;
            }
        }
    }
    
    /**
     * Toggle between grid and list view
     */
    private void toggleView() {
        isGridView = !isGridView;
        
        // Update button icon
        if (isGridView) {
            btnViewToggle.setImageResource(R.drawable.ic_grid_view);
        } else {
            btnViewToggle.setImageResource(R.drawable.ic_list_view);
        }
        
        // Update RecyclerView layout
        setupRecyclerView();
    }
    
    /**
     * Clear search query and refresh
     */
    private void clearSearch() {
        etSearch.setText("");
        currentFilter.setQuery(null);
        applyFilters();
    }
    
    /**
     * Show category selection dialog
     */
    private void showCategorySelection() {
        Intent intent = new Intent(getContext(), CategorySelectionActivity.class);
        intent.putExtra("USER_EMAIL", loggedInUserEmail);
        startActivityForResult(intent, 1002);
    }
    
    /**
     * Update item count display
     */
    private void updateItemCount() {
        if (tvItemCount != null) {
            int totalItems = allItems.size();
            int filteredCount = filteredItems.size();
            
            // Debug logging
            android.util.Log.d("BrowseFragment", "updateItemCount: totalItems=" + totalItems + ", filteredCount=" + filteredCount);
            
            if (totalItems == 0) {
                tvItemCount.setText("No items available");
            } else if (filteredCount == totalItems) {
                tvItemCount.setText(totalItems + " items available");
            } else {
                tvItemCount.setText(filteredCount + " of " + totalItems + " items");
            }
        }
    }
}
