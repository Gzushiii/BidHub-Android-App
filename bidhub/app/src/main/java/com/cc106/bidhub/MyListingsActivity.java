package com.cc106.bidhub;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.cc106.bidhub.adapters.MyListingsAdapter;
import com.cc106.bidhub.items.Item;
import com.cc106.bidhub.items.ItemManager;
import com.cc106.bidhub.items.ItemStatus;
import com.cc106.bidhub.toast.ToastHelper;

import java.util.ArrayList;
import java.util.List;

public class MyListingsActivity extends AppCompatActivity implements MyListingsAdapter.OnListingActionListener {

    private ImageButton btnBack, btnAdd;
    private TextView tvTitle, tvEmptyState;
    private EditText etSearch;
    private Button btnFilterActive, btnFilterPending, btnFilterSold, btnFilterDrafts;
    private RecyclerView rvMyListings;
    private ProgressBar progressBar;
    private MyListingsAdapter listingsAdapter;
    private ItemManager itemManager;
    private String loggedInUserEmail;
    private List<Item> myListings;
    private List<Item> filteredListings;
    private String currentFilter = "Active";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_my_listings);
            
            // Get user email from intent
            loggedInUserEmail = getIntent().getStringExtra("USER_EMAIL");
            if (loggedInUserEmail == null) {
                ToastHelper.showError(this, "User session expired. Please login again.");
                finish();
                return;
            }
            
            initializeViews();
            setupClickListeners();
            loadMyListings();
        } catch (Exception e) {
            ToastHelper.showError(this, "Error initializing My Listings: " + e.getMessage());
            e.printStackTrace();
            finish();
        }
    }
    
    private void initializeViews() {
        try {
            btnBack = findViewById(R.id.btn_back);
            btnAdd = findViewById(R.id.btn_add);
            tvTitle = findViewById(R.id.tv_title);
            etSearch = findViewById(R.id.et_search);
            btnFilterActive = findViewById(R.id.btn_filter_active);
            btnFilterPending = findViewById(R.id.btn_filter_pending);
            btnFilterSold = findViewById(R.id.btn_filter_sold);
            btnFilterDrafts = findViewById(R.id.btn_filter_drafts);
            tvEmptyState = findViewById(R.id.tv_empty_state);
            rvMyListings = findViewById(R.id.rv_my_listings);
            progressBar = findViewById(R.id.progress_bar);
            
            // Set title
            if (tvTitle != null) {
                tvTitle.setText("My Listings");
            }
            
            // Initialize RecyclerView
            if (rvMyListings != null) {
                try {
                    rvMyListings.setLayoutManager(new LinearLayoutManager(this));
                    myListings = new ArrayList<>();
                    filteredListings = new ArrayList<>();
                    listingsAdapter = new MyListingsAdapter(filteredListings, this);
                    listingsAdapter.setOnListingActionListener(this);
                    rvMyListings.setAdapter(listingsAdapter);
                } catch (Exception e) {
                    ToastHelper.showError(this, "Error setting up RecyclerView: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                ToastHelper.showError(this, "RecyclerView not found in layout");
            }
            
            // Initialize ItemManager
            try {
                itemManager = ItemManager.getInstance(this);
            } catch (Exception e) {
                ToastHelper.showError(this, "Error initializing item manager: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            ToastHelper.showError(this, "Error initializing views: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setupClickListeners() {
        try {
            if (btnBack != null) {
                btnBack.setOnClickListener(v -> finish());
            }
            
            if (btnAdd != null) {
                btnAdd.setOnClickListener(v -> {
                    // Navigate to post item screen
                    Intent intent = new Intent(this, PostActivity.class);
                    intent.putExtra("USER_EMAIL", loggedInUserEmail);
                    startActivity(intent);
                });
            }
            
            // Filter button listeners
            if (btnFilterActive != null) {
                btnFilterActive.setOnClickListener(v -> setActiveFilter("Active"));
            }
            if (btnFilterPending != null) {
                btnFilterPending.setOnClickListener(v -> setActiveFilter("Pending"));
            }
            if (btnFilterSold != null) {
                btnFilterSold.setOnClickListener(v -> setActiveFilter("Sold"));
            }
            if (btnFilterDrafts != null) {
                btnFilterDrafts.setOnClickListener(v -> setActiveFilter("Draft"));
            }
            
            // Search functionality
            if (etSearch != null) {
                etSearch.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        filterListings();
                    }
                    
                    @Override
                    public void afterTextChanged(Editable s) {}
                });
            }
        } catch (Exception e) {
            ToastHelper.showError(this, "Error setting up click listeners: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void loadMyListings() {
        if (itemManager == null) {
            ToastHelper.showError(this, "Item manager not available");
            return;
        }
        
        progressBar.setVisibility(View.VISIBLE);
        
        // Load user's listings in a background thread
        new Thread(() -> {
            try {
                List<Item> userListings = itemManager.getItemsBySeller(loggedInUserEmail);
                
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    
                    if (userListings != null && !userListings.isEmpty()) {
                        myListings.clear();
                        myListings.addAll(userListings);
                        filterListings(); // Apply current filter and search
                    } else {
                        // No listings found
                        filteredListings.clear();
                        listingsAdapter.notifyDataSetChanged();
                        tvEmptyState.setVisibility(View.VISIBLE);
                        rvMyListings.setVisibility(View.GONE);
                        tvEmptyState.setText("You haven't posted any items yet.\n\nStart by posting your first item!");
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    ToastHelper.showError(this, "Error loading your listings: " + e.getMessage());
                    e.printStackTrace();
                });
            }
        }).start();
    }
    
    private void setActiveFilter(String filter) {
        currentFilter = filter;
        updateFilterButtons();
        filterListings();
    }
    
    private void updateFilterButtons() {
        // Reset all buttons to inactive state
        btnFilterActive.setBackgroundResource(R.drawable.button_filter_inactive);
        btnFilterActive.setTextColor(getResources().getColor(R.color.text_primary));
        btnFilterPending.setBackgroundResource(R.drawable.button_filter_inactive);
        btnFilterPending.setTextColor(getResources().getColor(R.color.text_primary));
        btnFilterSold.setBackgroundResource(R.drawable.button_filter_inactive);
        btnFilterSold.setTextColor(getResources().getColor(R.color.text_primary));
        btnFilterDrafts.setBackgroundResource(R.drawable.button_filter_inactive);
        btnFilterDrafts.setTextColor(getResources().getColor(R.color.text_primary));
        
        // Set active button
        switch (currentFilter) {
            case "Active":
                btnFilterActive.setBackgroundResource(R.drawable.button_filter_active);
                btnFilterActive.setTextColor(getResources().getColor(R.color.white));
                break;
            case "Pending":
                btnFilterPending.setBackgroundResource(R.drawable.button_filter_active);
                btnFilterPending.setTextColor(getResources().getColor(R.color.white));
                break;
            case "Sold":
                btnFilterSold.setBackgroundResource(R.drawable.button_filter_active);
                btnFilterSold.setTextColor(getResources().getColor(R.color.white));
                break;
            case "Draft":
                btnFilterDrafts.setBackgroundResource(R.drawable.button_filter_active);
                btnFilterDrafts.setTextColor(getResources().getColor(R.color.white));
                break;
        }
    }
    
    private void filterListings() {
        filteredListings.clear();
        String searchQuery = etSearch.getText().toString().toLowerCase();
        
        for (Item item : myListings) {
            // Apply status filter
            boolean matchesFilter = false;
            String itemStatus = getStatusString(item.getStatus());
            
            if (currentFilter.equals("Active") && itemStatus.equals("Active")) {
                matchesFilter = true;
            } else if (currentFilter.equals("Pending") && itemStatus.equals("Pending")) {
                matchesFilter = true;
            } else if (currentFilter.equals("Sold") && itemStatus.equals("Sold")) {
                matchesFilter = true;
            } else if (currentFilter.equals("Draft") && itemStatus.equals("Draft")) {
                matchesFilter = true;
            }
            
            // Apply search filter
            if (matchesFilter) {
                if (searchQuery.isEmpty() || 
                    (item.getTitle() != null && item.getTitle().toLowerCase().contains(searchQuery)) ||
                    (item.getDescription() != null && item.getDescription().toLowerCase().contains(searchQuery))) {
                    filteredListings.add(item);
                }
            }
        }
        
        listingsAdapter.notifyDataSetChanged();
        
        // Show/hide empty state
        if (filteredListings.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            rvMyListings.setVisibility(View.GONE);
            if (searchQuery.isEmpty()) {
                tvEmptyState.setText("No " + currentFilter.toLowerCase() + " listings found.\n\nTry a different filter or create a new listing!");
            } else {
                tvEmptyState.setText("No listings found matching \"" + searchQuery + "\".\n\nTry a different search term.");
            }
        } else {
            tvEmptyState.setVisibility(View.GONE);
            rvMyListings.setVisibility(View.VISIBLE);
        }
    }
    
    private String getStatusString(ItemStatus status) {
        if (status == null) return "Active";
        
        switch (status) {
            case ACTIVE:
                return "Active";
            case PAUSED:
                return "Pending";
            case SOLD:
                return "Sold";
            case DRAFT:
                return "Draft";
            case ENDED:
                return "Ended";
            case CANCELLED:
                return "Cancelled";
            default:
                return "Active";
        }
    }
    
    // MyListingsAdapter.OnListingActionListener implementation
    @Override
    public void onItemClick(Item item) {
        // Navigate to item detail view
        Intent intent = new Intent(this, ItemDetailActivity.class);
        intent.putExtra("ITEM_ID", item.getItemId());
        intent.putExtra("USER_EMAIL", loggedInUserEmail);
        startActivity(intent);
    }
    
    @Override
    public void onViewBids(Item item) {
        // TODO: Implement view bids functionality
        ToastHelper.showInfo(this, "View bids functionality coming soon!");
    }
    
    @Override
    public void onEditListing(Item item) {
        // TODO: Implement edit listing functionality
        ToastHelper.showInfo(this, "Edit listing functionality coming soon!");
    }
    
    @Override
    public void onMarkAsSold(Item item) {
        // TODO: Implement mark as sold functionality
        ToastHelper.showInfo(this, "Mark as sold functionality coming soon!");
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh listings when returning to this activity
        loadMyListings();
    }
}
