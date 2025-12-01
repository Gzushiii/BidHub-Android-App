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
        
        // FIX: Only load items if we don't have any cached, or always refresh from API
        // This ensures items persist across configuration changes
        if (savedInstanceState == null || allItems.isEmpty()) {
            loadItems();
        } else {
            // Restore existing items and apply filters
            applyFilters();
        }
        
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
            // FIX: Use GridLayoutManager with 2 columns and proper spacing
            // Calculate column count based on screen size for better responsiveness
            int columnCount = 2;
            android.content.res.Resources resources = getContext().getResources();
            android.util.DisplayMetrics metrics = resources.getDisplayMetrics();
            float screenWidthDp = metrics.widthPixels / metrics.density;
            
            // Adjust columns for larger screens
            if (screenWidthDp >= 600) {
                columnCount = 3; // Tablets and larger screens
            }
            
            GridLayoutManager layoutManager = new GridLayoutManager(getContext(), columnCount);
            
            // FIX: Add proper spacing between items with consistent margins
            // Remove existing decorations first to avoid duplicates
            if (rvItems.getItemDecorationCount() > 0) {
                for (int i = rvItems.getItemDecorationCount() - 1; i >= 0; i--) {
                    rvItems.removeItemDecorationAt(i);
                }
            }
            
            // Add spacing decoration
            androidx.recyclerview.widget.RecyclerView.ItemDecoration spacingDecoration = 
                new androidx.recyclerview.widget.RecyclerView.ItemDecoration() {
                    @Override
                    public void getItemOffsets(android.graphics.Rect outRect, View view, 
                                             androidx.recyclerview.widget.RecyclerView parent, 
                                             androidx.recyclerview.widget.RecyclerView.State state) {
                        // Use consistent 8dp spacing (matches card margin)
                        int spacing = (int) (8 * view.getContext().getResources().getDisplayMetrics().density);
                        int position = parent.getChildAdapterPosition(view);
                        
                        if (position == androidx.recyclerview.widget.RecyclerView.NO_POSITION) {
                            return;
                        }
                        
                        GridLayoutManager gridLayoutManager = (GridLayoutManager) parent.getLayoutManager();
                        if (gridLayoutManager == null) {
                            return;
                        }
                        
                        int spanCount = gridLayoutManager.getSpanCount();
                        
                        // Calculate column index
                        int column = position % spanCount;
                        
                        // Apply spacing evenly - left and right spacing
                        outRect.left = spacing - column * spacing / spanCount;
                        outRect.right = (column + 1) * spacing / spanCount;
                        
                        // Vertical spacing - top spacing for first row
                        if (position < spanCount) {
                            outRect.top = spacing; // Top row
                        } else {
                            outRect.top = 0; // No top spacing for other rows
                        }
                        outRect.bottom = spacing; // Bottom spacing for all rows
                    }
                };
            rvItems.addItemDecoration(spacingDecoration);
            rvItems.setLayoutManager(layoutManager);
        } else {
            LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
            rvItems.setLayoutManager(layoutManager);
        }
        
        rvItems.setAdapter(itemAdapter);
        // FIX: Ensure RecyclerView doesn't clip children and has proper padding
        rvItems.setClipToPadding(false);
        rvItems.setClipChildren(false);
        // Add padding to RecyclerView to match card margins
        int padding = (int) (8 * getContext().getResources().getDisplayMetrics().density);
        rvItems.setPadding(padding, padding, padding, padding);
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

        // FIX: Ensure we have items to filter - if allItems is empty, try loading from ItemManager
        if (allItems.isEmpty() && itemManager != null) {
            android.util.Log.w("BrowseFragment", "allItems is empty, attempting to load from ItemManager");
            List<Item> managerItems = itemManager.getAllActiveItems();
            if (!managerItems.isEmpty()) {
                allItems.clear();
                allItems.addAll(managerItems);
                android.util.Log.d("BrowseFragment", "Loaded " + allItems.size() + " items from ItemManager");
            }
        }

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
            // FIX: Filter directly from allItems instead of ItemManager cache
            // This ensures we're filtering from the items we just loaded from API
            List<Item> results = filterItemsLocally(allItems, normalizedFilter);

            // Debug: Log filter results
            android.util.Log.d("BrowseFragment", "Filter results: " + results.size() + " items from " + allItems.size() + " total items");

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
                            if (isAdded() && !isDetached()) { // Check if fragment is still added to activity
                                // FIX: Update filtered items list
                                filteredItems.clear();
                                filteredItems.addAll(finalResults);
                                
                                // FIX: Ensure adapter is properly updated with new data
                                if (itemAdapter != null) {
                                    itemAdapter.updateItems(filteredItems);
                                    android.util.Log.d("BrowseFragment", "Adapter updated with " + filteredItems.size() + " filtered items");
                                } else {
                                    android.util.Log.w("BrowseFragment", "Adapter is null, reinitializing");
                                    setupRecyclerView();
                                    if (itemAdapter != null) {
                                        itemAdapter.updateItems(filteredItems);
                                    }
                                }

                                showLoading(false);
                                updateEmptyState();
                                updateItemCount();
                            } else {
                                android.util.Log.w("BrowseFragment", "Fragment not attached, skipping UI update");
                            }
                        });
                    }
        }).start();
    }
    
    /**
     * Filter items locally from allItems list
     * This ensures we filter from the items we loaded, not from ItemManager cache
     */
    private List<Item> filterItemsLocally(List<Item> itemsToFilter, FilterCriteria criteria) {
        if (itemsToFilter == null || itemsToFilter.isEmpty()) {
            android.util.Log.d("BrowseFragment", "filterItemsLocally: No items to filter");
            return new ArrayList<>();
        }
        
        if (criteria == null) {
            android.util.Log.d("BrowseFragment", "filterItemsLocally: No filter criteria, returning all items");
            return new ArrayList<>(itemsToFilter);
        }
        
        List<Item> filtered = new ArrayList<>();
        
        for (Item item : itemsToFilter) {
            if (item == null) continue;
            
            // Only show ACTIVE items
            if (item.getStatus() != com.cc106.bidhub.items.ItemStatus.ACTIVE) {
                continue;
            }
            
            // Search filter
            boolean matchesSearch = true;
            if (criteria.getQuery() != null && !criteria.getQuery().trim().isEmpty()) {
                String queryLower = criteria.getQuery().toLowerCase().trim();
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
            
            // Category filter
            boolean matchesCategory = (criteria.getCategoryId() == null || criteria.getCategoryId().isEmpty()) ||
                criteria.getCategoryId().equals(item.getCategoryId());
            
            // Condition filter
            boolean matchesCondition = (criteria.getCondition() == null || criteria.getCondition().isEmpty()) ||
                criteria.getCondition().equals(item.getCondition());
            
            // Price filters
            double price = item.getCurrentPrice() > 0 ? item.getCurrentPrice() : 
                          (item.getStartingPrice() > 0 ? item.getStartingPrice() : 0.0);
            boolean matchesMin = (criteria.getMinPrice() == null) || price >= criteria.getMinPrice();
            boolean matchesMax = (criteria.getMaxPrice() == null) || price <= criteria.getMaxPrice();
            
            // Featured filter
            boolean matchesFeatured = (criteria.getIsFeatured() == null) ||
                criteria.getIsFeatured() == item.isFeatured();
            
            // Trending filter
            boolean matchesTrending = (criteria.getIsTrending() == null) ||
                criteria.getIsTrending() == item.isTrending();
            
            if (matchesSearch && matchesCategory && matchesCondition && 
                matchesMin && matchesMax && matchesFeatured && matchesTrending) {
                filtered.add(item);
            }
        }
        
        // Sort items
        if (criteria.getSortBy() != null) {
            java.util.Comparator<Item> comparator = getComparator(criteria.getSortBy(), criteria.getSortOrder());
            java.util.Collections.sort(filtered, comparator);
        } else {
            // Default: sort by creation date (newest first)
            java.util.Collections.sort(filtered, new java.util.Comparator<Item>() {
                @Override
                public int compare(Item i1, Item i2) {
                    java.util.Date d1 = i1.getCreatedAt();
                    java.util.Date d2 = i2.getCreatedAt();
                    if (d1 == null && d2 == null) return 0;
                    if (d1 == null) return 1;
                    if (d2 == null) return -1;
                    return d2.compareTo(d1); // Newest first
                }
            });
        }
        
        android.util.Log.d("BrowseFragment", "filterItemsLocally: Filtered " + filtered.size() + " items from " + itemsToFilter.size() + " total");
        return filtered;
    }
    
    /**
     * Get comparator for sorting items
     */
    private java.util.Comparator<Item> getComparator(String sortBy, String sortOrder) {
        boolean ascending = "asc".equalsIgnoreCase(sortOrder);
        
        return new java.util.Comparator<Item>() {
            @Override
            public int compare(Item i1, Item i2) {
                int result = 0;
                
                if ("price".equalsIgnoreCase(sortBy)) {
                    double price1 = i1.getCurrentPrice() > 0 ? i1.getCurrentPrice() : i1.getStartingPrice();
                    double price2 = i2.getCurrentPrice() > 0 ? i2.getCurrentPrice() : i2.getStartingPrice();
                    result = Double.compare(price1, price2);
                } else if ("title".equalsIgnoreCase(sortBy)) {
                    result = i1.getTitle().compareToIgnoreCase(i2.getTitle());
                } else if ("date".equalsIgnoreCase(sortBy) || "created".equalsIgnoreCase(sortBy)) {
                    java.util.Date d1 = i1.getCreatedAt();
                    java.util.Date d2 = i2.getCreatedAt();
                    if (d1 == null && d2 == null) result = 0;
                    else if (d1 == null) result = 1;
                    else if (d2 == null) result = -1;
                    else result = d1.compareTo(d2);
                } else {
                    // Default: sort by creation date
                    java.util.Date d1 = i1.getCreatedAt();
                    java.util.Date d2 = i2.getCreatedAt();
                    if (d1 == null && d2 == null) result = 0;
                    else if (d1 == null) result = 1;
                    else if (d2 == null) result = -1;
                    else result = d2.compareTo(d1); // Newest first by default
                }
                
                return ascending ? result : -result;
            }
        };
    }
    
    public void loadItems() {
        loadItems(false);
    }
    
    /**
     * Load items from API with optional force refresh
     * @param forceRefresh If true, bypasses loading check and forces fresh data from API
     */
    public void loadItems(boolean forceRefresh) {
        // Prevent duplicate loading, but allow refresh if explicitly requested
        if (!forceRefresh && isLoading) {
            android.util.Log.d("BrowseFragment", "Already loading items, skipping duplicate load");
            // If swipe refresh is active, ensure it completes
            if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                // Don't return - allow the existing load to complete
                return;
            }
            return;
        }
        
        isLoading = true;
        // Only show loading indicator if not already refreshing via swipe
        if (swipeRefreshLayout == null || !swipeRefreshLayout.isRefreshing()) {
            showLoading(true);
        }
        
        // Debug: Log before loading
        android.util.Log.d("BrowseFragment", "Loading items from API..." + (forceRefresh ? " (FORCE REFRESH)" : ""));
        
        // Always load from API to get latest data
        loadItemsFromDatabase(forceRefresh);
    }
    
    private void loadItemsFromDatabase() {
        loadItemsFromDatabase(false);
    }
    
    private void loadItemsFromDatabase(boolean forceRefresh) {
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
                // FIX: Pass forceRefresh flag to ensure fresh data from API
                com.cc106.bidhub.api.ItemApiClient.ApiResponse response = apiClient.getItems(null, null, null, null, null, 100, 0, forceRefresh);
                
                if (response.isSuccess() && response.getData() != null) {
                    // Parse and display items from database
                    List<Item> dbItems = parseItemsFromResponse(response.getData());
                    // Filter to only show ACTIVE items
                    List<Item> activeItems = dbItems.stream()
                        .filter(item -> item.getStatus() == com.cc106.bidhub.items.ItemStatus.ACTIVE)
                        .collect(java.util.stream.Collectors.toList());
                    android.util.Log.d("BrowseFragment", "Loaded " + activeItems.size() + " active items from database (filtered from " + dbItems.size() + " total)");
                    
                    // CRITICAL FIX: Always update items when API returns successfully, even if empty
                    // This ensures consistency - if API says there are no items, we should show that
                    // But only clear if we got a successful response with data
                    if (getActivity() != null && !getActivity().isFinishing()) {
                        getActivity().runOnUiThread(() -> {
                            if (isAdded() && !isDetached()) {
                                // Always update with API response - this ensures items persist correctly
                                allItems.clear();
                                allItems.addAll(activeItems);
                                android.util.Log.d("BrowseFragment", "Updated allItems with " + activeItems.size() + " items from API");
                                
                                // Also update ItemManager cache for consistency
                                // Use storeItem() which is designed for API-synced items
                                for (Item item : activeItems) {
                                    if (item != null && item.getItemId() != null && !item.getItemId().isEmpty()) {
                                        itemManager.storeItem(item);
                                    }
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
            if (responseData == null || responseData.isEmpty()) {
                android.util.Log.w("BrowseFragment", "parseItemsFromResponse: responseData is null or empty");
                return items;
            }
            
            org.json.JSONObject jsonResponse = new org.json.JSONObject(responseData);
            
            // Handle both array and object responses
            org.json.JSONArray itemsArray = null;
            if (jsonResponse.has("items")) {
                itemsArray = jsonResponse.getJSONArray("items");
            } else if (jsonResponse.has("data") && jsonResponse.get("data") instanceof org.json.JSONArray) {
                itemsArray = jsonResponse.getJSONArray("data");
            } else {
                android.util.Log.w("BrowseFragment", "parseItemsFromResponse: No items array found in response");
                return items;
            }
            
            if (itemsArray == null) {
                android.util.Log.w("BrowseFragment", "parseItemsFromResponse: itemsArray is null");
                return items;
            }
            
            for (int i = 0; i < itemsArray.length(); i++) {
                try {
                    org.json.JSONObject itemJson = itemsArray.getJSONObject(i);
                    Item item = new Item();
                    
                    // Use optString/optDouble for safer parsing
                    // CRITICAL: Prioritize uuid_id (backend primary ID), fallback to id
                    String itemId = itemJson.optString("uuid_id", itemJson.optString("id", ""));
                    if (itemId == null || itemId.isEmpty()) {
                        android.util.Log.w("BrowseFragment", "Item at index " + i + " has no valid ID, skipping");
                        continue;
                    }
                    item.setItemId(itemId);
                    
                    String title = itemJson.optString("title", "");
                    if (title.isEmpty()) {
                        android.util.Log.w("BrowseFragment", "Skipping item with empty title at index " + i);
                        continue; // Skip items without titles
                    }
                    item.setTitle(title);
                    item.setDescription(itemJson.optString("description", ""));
                    double startingPrice = itemJson.optDouble("starting_bid", itemJson.optDouble("starting_price", 0.0));
                    item.setStartingPrice(startingPrice);
                    
                    // FIX: Set current price correctly - if no bids, use starting price
                    int bidCount = itemJson.optInt("bid_count", 0);
                    double currentPrice;
                    if (bidCount > 0) {
                        // Has bids - use current_bid/current_price from API
                        currentPrice = itemJson.optDouble("current_bid", itemJson.optDouble("current_price", startingPrice));
                    } else {
                        // No bids yet - current price should be 0 or starting price
                        currentPrice = 0.0; // Will show starting price in UI
                    }
                    item.setCurrentPrice(currentPrice);
                    
                    item.setCategoryId(itemJson.optString("category_id", ""));
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
                
                // Set bid count from API response (already retrieved above for price logic)
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
                
                // Parse images if available - handle multiple formats
                // FIX: Enhanced image parsing to handle all possible formats from API
                if (itemJson.has("images")) {
                    try {
                        Object imagesObj = itemJson.get("images");
                        List<String> imagePaths = new ArrayList<>();
                        
                        if (imagesObj instanceof org.json.JSONArray) {
                            // Images is already a JSON array
                            org.json.JSONArray imagesArray = (org.json.JSONArray) imagesObj;
                            for (int j = 0; j < imagesArray.length(); j++) {
                                Object imgObj = imagesArray.get(j);
                                String imageUrl = null;
                                
                                // Handle different image object formats
                                if (imgObj instanceof String) {
                                    // Direct URL string
                                    imageUrl = (String) imgObj;
                                } else if (imgObj instanceof org.json.JSONObject) {
                                    // Object with image_url field
                                    org.json.JSONObject imgJson = (org.json.JSONObject) imgObj;
                                    imageUrl = imgJson.optString("image_url", imgJson.optString("url", null));
                                }
                                
                                // Validate and add image URL
                                if (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.equals("null")) {
                                    // FIX: Convert relative URLs to absolute URLs
                                    if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
                                        // If it's a relative path, prepend base URL
                                        String baseUrl = "https://bidhub-android-app.onrender.com";
                                        if (imageUrl.startsWith("/")) {
                                            imageUrl = baseUrl + imageUrl;
                                        } else {
                                            imageUrl = baseUrl + "/" + imageUrl;
                                        }
                                        android.util.Log.d("BrowseFragment", "Converted relative URL to absolute: " + imageUrl);
                                    }
                                    imagePaths.add(imageUrl);
                                }
                            }
                        } else if (imagesObj instanceof String) {
                            // Images is a JSON string, parse it
                            String imagesString = (String) imagesObj;
                            if (!imagesString.isEmpty() && !imagesString.equals("null")) {
                                try {
                                    org.json.JSONArray imagesArray = new org.json.JSONArray(imagesString);
                                    for (int j = 0; j < imagesArray.length(); j++) {
                                    String imageUrl = imagesArray.optString(j, null);
                                    if (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.equals("null")) {
                                        // FIX: Convert relative URLs to absolute URLs
                                        if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
                                            String baseUrl = "https://bidhub-android-app.onrender.com";
                                            if (imageUrl.startsWith("/")) {
                                                imageUrl = baseUrl + imageUrl;
                                            } else {
                                                imageUrl = baseUrl + "/" + imageUrl;
                                            }
                                            android.util.Log.d("BrowseFragment", "Converted relative URL to absolute: " + imageUrl);
                                        }
                                        imagePaths.add(imageUrl);
                                    }
                                    }
                                } catch (org.json.JSONException e) {
                                    android.util.Log.w("BrowseFragment", "Failed to parse images string as JSON array: " + imagesString);
                                }
                            }
                        }
                        
                        // Log image parsing result for debugging
                        if (!imagePaths.isEmpty()) {
                            android.util.Log.d("BrowseFragment", "Parsed " + imagePaths.size() + " images for item: " + item.getTitle());
                        } else {
                            android.util.Log.w("BrowseFragment", "No valid images found for item: " + item.getTitle());
                        }
                        
                        item.setImagePaths(imagePaths);
                    } catch (Exception e) {
                        android.util.Log.e("BrowseFragment", "Error parsing images for item: " + item.getTitle(), e);
                        // Set empty list as fallback
                        item.setImagePaths(new ArrayList<>());
                    }
                } else {
                    // No images field - set empty list
                    item.setImagePaths(new ArrayList<>());
                    android.util.Log.d("BrowseFragment", "No images field for item: " + item.getTitle());
                }
                
                    items.add(item);
                } catch (Exception e) {
                    // Log error for individual item but continue parsing others
                    android.util.Log.e("BrowseFragment", "Error parsing item at index " + i + ": " + e.getMessage(), e);
                }
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
        // FIX: Always refresh items on resume to ensure latest data
        // Use force refresh to ensure we get the most up-to-date items from the server
        // This ensures new items posted by other users appear immediately when the tab becomes visible
        if (!isLoading) {
            android.util.Log.d("BrowseFragment", "onResume: Refreshing items to ensure latest data (force refresh)");
            // Force refresh from API to get latest items (bypasses loading check)
            loadItems(true); // true = force refresh
        } else {
            android.util.Log.d("BrowseFragment", "Skipping refresh on resume - currently loading");
            // Even if loading, schedule a refresh after a short delay to ensure we get latest data
            android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
            handler.postDelayed(() -> {
                if (isAdded() && !isDetached()) {
                    android.util.Log.d("BrowseFragment", "Delayed refresh after resume");
                    loadItems(true); // Force refresh after delay
                }
            }, 1000); // 1 second delay
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
