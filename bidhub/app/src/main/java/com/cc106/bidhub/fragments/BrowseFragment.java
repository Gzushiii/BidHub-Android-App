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
import com.cc106.bidhub.adapters.ItemCardAdapter;
import com.cc106.bidhub.items.Item;
import com.cc106.bidhub.items.ItemManager;
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

import com.cc106.bidhub.R;

import java.util.ArrayList;
import java.util.List;

public class BrowseFragment extends Fragment implements ItemCardAdapter.OnItemClickListener {

    private String loggedInUserEmail;
    private ItemManager itemManager;
    
    // UI Components
    private TextInputEditText etSearch;
    private ImageButton btnFilter;
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
    
    // View state
    private boolean isGridView = true;

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
        etSearch = view.findViewById(R.id.et_search);
        btnFilter = view.findViewById(R.id.btn_filter);
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
            GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
            rvItems.setLayoutManager(layoutManager);
        } else {
            LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
            rvItems.setLayoutManager(layoutManager);
        }
        
        rvItems.setAdapter(itemAdapter);
    }
    
    private void setupSearch() {
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
                    // Schedule new search
                    searchRunnable = () -> performSearch(query);
                    searchHandler.postDelayed(searchRunnable, SEARCH_DELAY);
                }
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
    
    private void setupFilter() {
        btnFilter.setOnClickListener(v -> showFilterDialog());
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
        currentFilter.setQuery(query.isEmpty() ? null : query);
        applyFilters();
    }
    
    private void applyFilters() {
        showLoading(true);
        
        // Perform filtering on background thread
        new Thread(() -> {
            List<Item> results = itemManager.filterItems(currentFilter);
            
            // Update UI on main thread - check if fragment is still attached
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (isAdded()) { // Check if fragment is still added to activity
                        filteredItems.clear();
                        filteredItems.addAll(results);
                        itemAdapter.notifyDataSetChanged();
                        
                        showLoading(false);
                        updateEmptyState();
                        updateItemCount();
                    }
                });
            }
        }).start();
    }
    
    private void loadItems() {
        showLoading(true);
        
        // Load items on background thread
        new Thread(() -> {
            List<Item> items = itemManager.getAllActiveItems();
            
            // Update UI on main thread - check if fragment is still attached
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (isAdded()) { // Check if fragment is still added to activity
                        allItems.clear();
                        allItems.addAll(items);
                        
                        // Apply current filters
                        applyFilters();
                    }
                });
            }
        }).start();
    }
    
    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            rvItems.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            rvItems.setVisibility(View.VISIBLE);
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
        currentFilter = newFilter;
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
        
        if (currentFilter.getLocation() != null) {
            addFilterChip("Location: " + currentFilter.getLocation());
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
            currentFilter.setLocation(null);
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
        
        // Reset filter criteria
        currentFilter = new FilterCriteria();
        
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
    
    @Override
    public void onResume() {
        super.onResume();
        // Refresh items when returning to this fragment
        loadItems();
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
            
            if (filteredCount == totalItems) {
                tvItemCount.setText(totalItems + " items available");
            } else {
                tvItemCount.setText(filteredCount + " of " + totalItems + " items");
            }
        }
    }
}
