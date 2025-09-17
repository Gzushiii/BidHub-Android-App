package com.cc106.bidhub.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
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
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
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
    private RecyclerView rvItems;
    private ProgressBar progressBar;
    private LinearLayout layoutEmptyState;
    private HorizontalScrollView hsvFilterChips;
    private LinearLayout layoutFilterChips;
    
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
        rvItems = view.findViewById(R.id.rv_items);
        progressBar = view.findViewById(R.id.progress_bar);
        layoutEmptyState = view.findViewById(R.id.layout_empty_state);
        hsvFilterChips = view.findViewById(R.id.hsv_filter_chips);
        layoutFilterChips = view.findViewById(R.id.layout_filter_chips);
        
        itemManager = ItemManager.getInstance(getContext());
        allItems = new ArrayList<>();
        filteredItems = new ArrayList<>();
        currentFilter = new FilterCriteria();
        activeFilters = new ArrayList<>();
        searchHandler = new Handler(Looper.getMainLooper());
    }
    
    private void setupRecyclerView() {
        itemAdapter = new ItemCardAdapter(filteredItems);
        itemAdapter.setOnItemClickListener(this);
        
        // Use GridLayoutManager for 2 columns
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        rvItems.setLayoutManager(layoutManager);
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
                
                // Schedule new search
                searchRunnable = () -> performSearch(s.toString());
                searchHandler.postDelayed(searchRunnable, SEARCH_DELAY);
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
    
    private void setupFilter() {
        btnFilter.setOnClickListener(v -> showFilterDialog());
        btnSort.setOnClickListener(v -> showSortDialog());
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
        currentFilter.setQuery(query);
        applyFilters();
    }
    
    private void applyFilters() {
        showLoading(true);
        
        // Perform filtering on background thread
        new Thread(() -> {
            List<Item> results = itemManager.filterItems(currentFilter);
            
            // Update UI on main thread
            getActivity().runOnUiThread(() -> {
                filteredItems.clear();
                filteredItems.addAll(results);
                itemAdapter.notifyDataSetChanged();
                
                showLoading(false);
                updateEmptyState();
            });
        }).start();
    }
    
    private void loadItems() {
        showLoading(true);
        
        // Load items on background thread
        new Thread(() -> {
            List<Item> items = itemManager.getAllActiveItems();
            
            // Update UI on main thread
            getActivity().runOnUiThread(() -> {
                allItems.clear();
                allItems.addAll(items);
                
                // Apply current filters
                applyFilters();
            });
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
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            rvItems.setVisibility(View.VISIBLE);
        }
    }
    
    private void showFilterDialog() {
        FilterDialogFragment dialog = new FilterDialogFragment();
        dialog.setFilterCriteria(currentFilter);
        dialog.setOnFilterAppliedListener(this::onFilterApplied);
        dialog.show(getParentFragmentManager(), "FilterDialog");
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
        chip.setOnCloseIconClickListener(v -> removeFilterChip(chip, filterText));
        
        layoutFilterChips.addView(chip);
        hsvFilterChips.setVisibility(View.VISIBLE);
    }
    
    private void removeFilterChip(Chip chip, String filterText) {
        activeFilters.remove(filterText);
        layoutFilterChips.removeView(chip);
        
        if (activeFilters.isEmpty()) {
            hsvFilterChips.setVisibility(View.GONE);
        }
        
        // Reapply filters
        applyFilters();
    }
    
    @Override
    public void onItemClick(Item item) {
        // Navigate to item detail activity
        Intent intent = new Intent(getContext(), ItemDetailActivity.class);
        intent.putExtra("ITEM_ID", item.getItemId());
        intent.putExtra("USER_EMAIL", loggedInUserEmail);
        startActivity(intent);
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
}
