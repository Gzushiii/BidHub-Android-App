package com.cc106.bidhub;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
    private TextView tvTitle;
    private LinearLayout tvEmptyState;
    private TextView tvEmptyTitle, tvEmptyMessage;
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
            // Try to show a basic error state instead of finishing
            try {
                if (tvEmptyState != null) {
                    tvEmptyState.setVisibility(View.VISIBLE);
                }
                if (tvEmptyTitle != null) {
                    tvEmptyTitle.setText("Initialization Error");
                }
                if (tvEmptyMessage != null) {
                    tvEmptyMessage.setText("There was an error initializing the My Listings screen.\n\nPlease restart the app and try again.");
                }
            } catch (Exception ex) {
                // If even the error state fails, finish the activity
                finish();
            }
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
            tvEmptyTitle = findViewById(R.id.tv_empty_title);
            tvEmptyMessage = findViewById(R.id.tv_empty_message);
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
                if (itemManager == null) {
                    ToastHelper.showError(this, "ItemManager instance is null");
                }
            } catch (Exception e) {
                ToastHelper.showError(this, "Error initializing item manager: " + e.getMessage());
                e.printStackTrace();
                itemManager = null;
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
        progressBar.setVisibility(View.VISIBLE);
        
        // Try to load from database first, fallback to local
        loadMyListingsFromDatabase();
    }
    
    private void loadMyListingsFromDatabase() {
        // Try to fetch user's listings from backend API first
        new Thread(() -> {
            try {
                com.cc106.bidhub.api.ItemApiClient apiClient = new com.cc106.bidhub.api.ItemApiClient(this);
                // Use seller_email parameter to get user's listings
                com.cc106.bidhub.api.ItemApiClient.ApiResponse response = apiClient.getItems(null, null, null, null, 100, 0);
                
                if (response.isSuccess() && response.getData() != null) {
                    // Parse user's listings from database
                    List<Item> dbListings = parseUserListingsFromResponse(response.getData());
                    android.util.Log.d("MyListingsActivity", "Loaded " + dbListings.size() + " listings from database");
                    
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        if (dbListings != null && !dbListings.isEmpty()) {
                            myListings.clear();
                            myListings.addAll(dbListings);
                            filterListings();
                        } else {
                            showEmptyState();
                        }
                    });
                } else {
                    // Fallback to local items
                    android.util.Log.d("MyListingsActivity", "Database fetch failed, using local items");
                    loadLocalListings();
                }
            } catch (Exception e) {
                // Fallback to local items
                android.util.Log.e("MyListingsActivity", "Error fetching from database", e);
                runOnUiThread(() -> loadLocalListings());
            }
        }).start();
    }
    
    private void loadLocalListings() {
        if (itemManager == null) {
            ToastHelper.showError(this, "Item manager not available. Please restart the app.");
            showEmptyState();
            return;
        }
        
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
                        showEmptyState();
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
    
    private List<Item> parseUserListingsFromResponse(String responseData) {
        List<Item> listings = new ArrayList<>();
        try {
            org.json.JSONObject jsonResponse = new org.json.JSONObject(responseData);
            org.json.JSONArray itemsArray = jsonResponse.getJSONArray("items");
            
            for (int i = 0; i < itemsArray.length(); i++) {
                org.json.JSONObject itemJson = itemsArray.getJSONObject(i);
                
                // Only include items from the current user
                if (loggedInUserEmail.equals(itemJson.optString("seller_email", ""))) {
                    Item item = new Item();
                    
                    item.setItemId(itemJson.getString("id"));
                    item.setTitle(itemJson.getString("title"));
                    item.setDescription(itemJson.getString("description"));
                    item.setStartingPrice(itemJson.getDouble("starting_bid"));
                    item.setCurrentPrice(itemJson.getDouble("current_bid"));
                    item.setCategoryId(itemJson.getString("category_id"));
                    item.setSellerId(itemJson.getString("seller_email"));
                    item.setLocation(itemJson.optString("location", ""));
                    item.setCondition(itemJson.optString("condition", "good"));
                    
                    // Set status based on database status
                    String status = itemJson.optString("status", "draft");
                    switch (status) {
                        case "active":
                            item.setStatus(ItemStatus.ACTIVE);
                            break;
                        case "ended":
                        case "sold":
                            item.setStatus(ItemStatus.ENDED);
                            break;
                        case "cancelled":
                            item.setStatus(ItemStatus.CANCELLED);
                            break;
                        default:
                            item.setStatus(ItemStatus.DRAFT);
                            break;
                    }
                    
                    // Parse images if available
                    if (itemJson.has("images")) {
                        org.json.JSONArray imagesArray = itemJson.getJSONArray("images");
                        List<String> imagePaths = new ArrayList<>();
                        for (int j = 0; j < imagesArray.length(); j++) {
                            imagePaths.add(imagesArray.getString(j));
                        }
                        item.setImagePaths(imagePaths);
                    }
                    
                    listings.add(item);
                }
            }
        } catch (Exception e) {
            android.util.Log.e("MyListingsActivity", "Error parsing user listings from response", e);
        }
        return listings;
    }
    
    private void showEmptyState() {
        // No listings found
        filteredListings.clear();
        listingsAdapter.notifyDataSetChanged();
        tvEmptyState.setVisibility(View.VISIBLE);
        rvMyListings.setVisibility(View.GONE);
        if (tvEmptyTitle != null) {
            tvEmptyTitle.setText("No Listings Yet");
        }
        if (tvEmptyMessage != null) {
            tvEmptyMessage.setText("You haven't posted any items yet.\n\nStart by posting your first item!");
        }
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
                if (tvEmptyTitle != null) {
                    tvEmptyTitle.setText("No " + currentFilter.toLowerCase() + " listings found");
                }
                if (tvEmptyMessage != null) {
                    tvEmptyMessage.setText("Try a different filter or create a new listing!");
                }
            } else {
                if (tvEmptyTitle != null) {
                    tvEmptyTitle.setText("No listings found");
                }
                if (tvEmptyMessage != null) {
                    tvEmptyMessage.setText("No listings found matching \"" + searchQuery + "\".\n\nTry a different search term.");
                }
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
