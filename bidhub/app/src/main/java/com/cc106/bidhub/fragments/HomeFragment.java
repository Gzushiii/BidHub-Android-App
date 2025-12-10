package com.cc106.bidhub.fragments;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.cc106.bidhub.toast.ToastHelper;
import com.cc106.bidhub.utils.SharedPreferencesHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cc106.bidhub.BrowseActivity;
import com.cc106.bidhub.CreditsActivity;
import com.cc106.bidhub.DatabaseHelper;
import com.cc106.bidhub.HelpSupportActivity;
import com.cc106.bidhub.ItemDetailActivity;
import com.cc106.bidhub.MainActivity;
import com.cc106.bidhub.MyListingsActivity;
import com.cc106.bidhub.PostActivity;
import com.cc106.bidhub.ProfileActivity;
import com.cc106.bidhub.R;
import com.cc106.bidhub.adapters.ActiveBidsAdapter;
import com.cc106.bidhub.adapters.ItemCardAdapter;
import com.cc106.bidhub.bidding.Bid;
import com.cc106.bidhub.bidding.BiddingEngine;
import com.cc106.bidhub.items.Item;
import com.cc106.bidhub.items.ItemManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    // Header components
    private ImageView ivProfile;
    private TextView tvAppTitle;
    private TextView tvHeaderCredits;
    
    // Quick action buttons
    private View cardBrowse, cardSell, cardMyListings;
    
    // RecyclerViews
    private RecyclerView rvFeaturedItems;
    private RecyclerView rvActiveAuctions;
    private RecyclerView rvActiveBids;
    
    // Adapters
    private ItemCardAdapter featuredItemsAdapter;
    private com.cc106.bidhub.adapters.ActiveAuctionsAdapter activeAuctionsAdapter;
    private ActiveBidsAdapter activeBidsAdapter;
    
    // Empty states
    private View layoutEmptyState;
    
    // Loading state
    private ProgressBar progressLoading;
    
    // View All buttons
    private Button btnViewAllFeatured;
    private Button btnViewAllBids;
    
    // Credit balance
    private TextView tvCreditBalance;
    
    // Quick stats cards
    private View cardActiveBids, cardWatching, cardWonItems, cardSoldItems;
    private TextView tvActiveBidsCount, tvWatchingCount, tvWonItemsCount, tvSoldItemsCount;
    
    // Recent activity (removed from new design - keeping for potential future use)
    // private TextView textRecentActivity;
    // private View layoutRecentActivity;
    
    // Managers
    private ItemManager itemManager;
    private BiddingEngine biddingEngine;
    
    // Data lists
    private List<Item> featuredItems;
    private List<Item> activeAuctions;
    private List<Bid> activeBids;
    
    private DatabaseHelper dbHelper;
    private String loggedInUserEmail;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        // Get the logged-in user's email from arguments
        if (getArguments() != null) {
            loggedInUserEmail = getArguments().getString("USER_EMAIL");
        }
        
            // Initialize database helper
            if (getContext() != null) {
                try {
        dbHelper = new DatabaseHelper(getContext());
                } catch (Exception e) {
                    ToastHelper.showError(getContext(), "Error initializing database: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            // Initialize managers
            if (getContext() != null) {
                itemManager = ItemManager.getInstance(getContext());
                biddingEngine = BiddingEngine.getInstance(getContext());
            }
            
            // Initialize data lists
            featuredItems = new ArrayList<>();
            activeAuctions = new ArrayList<>();
            activeBids = new ArrayList<>();
            
            // Initialize all UI components
            initializeViews(view);
            
            // Set up RecyclerViews
            setupRecyclerViews();
            
            // FIX: Verify authentication before loading data
            // This prevents empty homepage after login
            verifyAuthenticationAndLoadData();
            
            // Set up click listeners
            setupClickListeners();
            
            // Load quick stats
            loadQuickStats();
            
            // Recent activity loading removed - not part of new dashboard design
            
            return view;
        } catch (Exception e) {
            ToastHelper.showError(getContext(), "Error initializing home fragment: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        android.util.Log.d("HomeFragment", "=== ON RESUME - REFRESHING ALL DATA ===");
        // Reload user data to ensure credits are up to date
        loadUserData();
        // Refresh all sections to show latest data
        refreshAllSections();
    }
    
    /**
     * FIX: Refresh all homepage sections with latest data
     * Syncs items from API and refreshes credits from backend
     * Ensures all sections update properly when data changes
     * Made public so MainActivity can call it after item posting
     */
    public void refreshAllSections() {
        if (getContext() == null) {
            android.util.Log.w("HomeFragment", "Context is null, cannot refresh sections");
            return;
        }
        
        android.util.Log.d("HomeFragment", "=== REFRESHING ALL SECTIONS ===");
        
        // Refresh credits from backend first, then sync items and update sections
        com.cc106.bidhub.repository.UserRepository userRepo = 
            com.cc106.bidhub.repository.UserRepository.getInstance(getContext());
        
        userRepo.refreshCreditsFromBackend(new com.cc106.bidhub.utils.CreditBalanceManager.BalanceUpdateCallback() {
            @Override
            public void onBalanceUpdated(double newBalance) {
                android.util.Log.d("HomeFragment", "Credits refreshed: " + newBalance);
                // Update UI with new balance
                if (getActivity() != null && !getActivity().isFinishing()) {
                    getActivity().runOnUiThread(() -> {
                        if (tvHeaderCredits != null) {
                            tvHeaderCredits.setText(String.format(Locale.getDefault(), "%.0f", newBalance));
                        }
                        // FIX: Reload featured items when credits change
                        // This ensures featured items section updates with new affordability
                        loadFeaturedItems();
                    });
                }
                // Sync items from API, then reload all sections
                syncItemsFromApi();
            }
            
            @Override
            public void onError(String errorMessage) {
                android.util.Log.e("HomeFragment", "Error refreshing credits: " + errorMessage);
                // Still sync items and refresh sections even if credit refresh fails
                // But use current cached credits for featured items
                if (getActivity() != null && !getActivity().isFinishing()) {
                    getActivity().runOnUiThread(() -> {
                        loadFeaturedItems(); // Reload with cached credits
                    });
                }
                syncItemsFromApi();
            }
        });
    }
    
    /**
     * Initialize all UI components
     */
    private void initializeViews(View view) {
        if (view == null) {
            return;
        }
        
        // Header components
        ivProfile = view.findViewById(R.id.iv_profile);
        tvAppTitle = view.findViewById(R.id.tv_app_title);
        tvHeaderCredits = view.findViewById(R.id.tv_header_credits);
        
        // Quick action buttons (removed - not in new design)
        // cardBrowse = view.findViewById(R.id.card_browse);
        // cardSell = view.findViewById(R.id.card_post);
        // cardMyListings = view.findViewById(R.id.card_my_listings);
        
        // RecyclerViews
        rvFeaturedItems = view.findViewById(R.id.rv_featured_items);
        rvActiveAuctions = view.findViewById(R.id.rv_active_auctions);
        rvActiveBids = view.findViewById(R.id.rv_active_bids);
        
        // Empty states
        layoutEmptyState = view.findViewById(R.id.layout_empty_state);
        
        // Loading state
        progressLoading = view.findViewById(R.id.progress_loading);
        
        // View All buttons
        btnViewAllBids = view.findViewById(R.id.btn_view_all_bids);
    }
    
    /**
     * Set up RecyclerViews with adapters and layout managers
     */
    private void setupRecyclerViews() {
        if (getContext() == null) {
            return;
        }
        
        // Featured Items RecyclerView - horizontal scrolling
        if (rvFeaturedItems != null) {
            featuredItemsAdapter = new ItemCardAdapter(featuredItems);
            featuredItemsAdapter.setOnItemClickListener(item -> {
                // Navigate to item detail
                Intent intent = new Intent(getContext(), ItemDetailActivity.class);
                intent.putExtra("ITEM_ID", item.getItemId());
                intent.putExtra("USER_EMAIL", loggedInUserEmail);
                startActivity(intent);
            });
            LinearLayoutManager featuredLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
            rvFeaturedItems.setLayoutManager(featuredLayoutManager);
            rvFeaturedItems.setAdapter(featuredItemsAdapter);
        }
        
        // Active Auctions RecyclerView - vertical scrolling
        if (rvActiveAuctions != null) {
            activeAuctionsAdapter = new com.cc106.bidhub.adapters.ActiveAuctionsAdapter(
                activeAuctions,
                item -> {
                    // Navigate to item detail
                    Intent intent = new Intent(getContext(), ItemDetailActivity.class);
                    intent.putExtra("ITEM_ID", item.getItemId());
                    intent.putExtra("USER_EMAIL", loggedInUserEmail);
                    startActivity(intent);
                }
            );
            LinearLayoutManager activeAuctionsLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
            rvActiveAuctions.setLayoutManager(activeAuctionsLayoutManager);
            rvActiveAuctions.setAdapter(activeAuctionsAdapter);
        }
        
        // Active Bids RecyclerView - vertical scrolling
        if (rvActiveBids != null) {
            activeBidsAdapter = new ActiveBidsAdapter(
                activeBids,
                bid -> {
                    // Navigate to item detail
                    Item item = itemManager.getItemById(bid.getItemId());
                    if (item != null) {
                        Intent intent = new Intent(getContext(), ItemDetailActivity.class);
                        intent.putExtra("ITEM_ID", item.getItemId());
                        intent.putExtra("USER_EMAIL", loggedInUserEmail);
                        startActivity(intent);
                    }
                },
                bid -> {
                    // Cancel bid - show toast for now
                    ToastHelper.showInfo(getContext(), "Cancel bid functionality coming soon");
                }
            );
            LinearLayoutManager activeBidsLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
            rvActiveBids.setLayoutManager(activeBidsLayoutManager);
            rvActiveBids.setAdapter(activeBidsAdapter);
        }
        
    }
    
    /**
     * Set up click listeners for all interactive elements
     */
    private void setupClickListeners() {
        // Profile image click listener
        if (ivProfile != null) {
            ivProfile.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(getContext(), ProfileActivity.class);
                    intent.putExtra("USER_EMAIL", loggedInUserEmail);
                    startActivity(intent);
                } catch (Exception e) {
                    if (getContext() != null) {
                        ToastHelper.showError(getContext(), "Error opening profile: " + e.getMessage());
                    }
                    e.printStackTrace();
                }
            });
        }
        
        // View All buttons
        
        if (btnViewAllBids != null) {
            btnViewAllBids.setOnClickListener(v -> {
                try {
                    // Navigate to active bids activity
                    Intent intent = new Intent(getContext(), com.cc106.bidhub.ActiveBidsActivity.class);
                    intent.putExtra("USER_EMAIL", loggedInUserEmail);
                    startActivity(intent);
                } catch (Exception e) {
                    if (getContext() != null) {
                        ToastHelper.showError(getContext(), "Error opening active bids: " + e.getMessage());
                    }
                    e.printStackTrace();
                }
            });
        }
        
        
        // Logout button - removed from new layout
        // if (buttonLogout != null) {
        //     buttonLogout.setOnClickListener(v -> {
        //         try {
        //             // Navigate back to LoginActivity
        //             if (getActivity() != null) {
        //                 getActivity().finish();
        //             }
        //         } catch (Exception e) {
        //             if (getContext() != null) {
        //                 ToastHelper.showError(getContext(), "Error during logout: " + e.getMessage());
        //             }
        //             e.printStackTrace();
        //         }
        //     });
        // }
    }
    
    /**
     * FIX: Verify authentication and load data in proper sequence
     * This ensures homepage doesn't appear empty after login
     */
    private void verifyAuthenticationAndLoadData() {
        if (getContext() == null) {
            android.util.Log.e("HomeFragment", "Context is null, cannot verify authentication");
            return;
        }
        
        // Run on background thread to avoid blocking UI
        new Thread(() -> {
            try {
                SharedPreferencesHelper prefsHelper = new SharedPreferencesHelper(getContext());
                String token = prefsHelper.getAuthToken();
                String userId = prefsHelper.getUserId();
                
                // Verify authentication is ready
                if (token == null || token.isEmpty() || userId == null || userId.isEmpty()) {
                    android.util.Log.w("HomeFragment", "Authentication not ready, waiting...");
                    // Wait a bit for authentication to complete (max 3 seconds)
                    int retries = 0;
                    while ((token == null || token.isEmpty() || userId == null || userId.isEmpty()) && retries < 6) {
                        Thread.sleep(500); // Wait 500ms
                        token = prefsHelper.getAuthToken();
                        userId = prefsHelper.getUserId();
                        retries++;
                    }
                }
                
                // Load user data first (synchronous on UI thread)
                if (getActivity() != null && !getActivity().isFinishing()) {
                    getActivity().runOnUiThread(() -> {
                        loadUserData();
                    });
                }
                
                // Load initial data from local cache first (for immediate display)
                // This prevents empty homepage on first load
                // Always load data even if authentication is not ready yet
                if (getActivity() != null && !getActivity().isFinishing()) {
                    getActivity().runOnUiThread(() -> {
                        android.util.Log.d("HomeFragment", "Loading initial data from local cache");
                        // Load user data first to get credits for featured items
                        loadUserData();
                        // Then load all sections
                        loadFeaturedItems();
                        loadActiveAuctions();
                        loadActiveBids();
                        // Hide loading indicator
                        hideLoading();
                    });
                }
                
                // Then sync items from API to get latest data
                // This ensures we have latest data from backend and updates the display
                syncItemsFromApi();
                
            } catch (InterruptedException e) {
                android.util.Log.e("HomeFragment", "Authentication verification interrupted", e);
                Thread.currentThread().interrupt();
                // Still try to load data even if interrupted
                if (getActivity() != null && !getActivity().isFinishing()) {
                    getActivity().runOnUiThread(() -> {
                        loadUserData();
                        loadFeaturedItems();
                        loadActiveAuctions();
                        loadActiveBids();
                    });
                }
            } catch (Exception e) {
                android.util.Log.e("HomeFragment", "Error verifying authentication", e);
                // Fallback: still try to load data
                if (getActivity() != null && !getActivity().isFinishing()) {
                    getActivity().runOnUiThread(() -> {
                        loadUserData();
                        loadFeaturedItems();
                        loadActiveAuctions();
                        loadActiveBids();
                    });
                }
            }
        }).start();
    }
    
    public void loadUserData() {
        // Try to get user email from arguments first
        if (loggedInUserEmail == null || loggedInUserEmail.isEmpty()) {
            if (getArguments() != null) {
                loggedInUserEmail = getArguments().getString("USER_EMAIL");
            }
        }
        
        // If still null, try to get it from MainActivity
        if (loggedInUserEmail == null || loggedInUserEmail.isEmpty()) {
            if (getActivity() instanceof MainActivity) {
                loggedInUserEmail = ((MainActivity) getActivity()).getCurrentUserEmail();
            }
        }
        
        // If still null, try to get from SharedPreferences
        if (loggedInUserEmail == null || loggedInUserEmail.isEmpty()) {
            if (getContext() != null) {
                SharedPreferencesHelper prefsHelper = new SharedPreferencesHelper(getContext());
                loggedInUserEmail = prefsHelper.getUserEmail();
            }
        }
        
        if (loggedInUserEmail == null || loggedInUserEmail.isEmpty()) {
            android.util.Log.w("HomeFragment", "User email not found, but continuing with data load");
            // Don't return early - still try to load data
        }

        if (dbHelper == null) {
            if (getContext() != null) {
                android.util.Log.w("HomeFragment", "Database not initialized, but continuing");
                // Don't return early - still try to load data
            }
        }

        // Load user data from UserRepository (single source of truth)
        try {
            if (getContext() != null) {
                com.cc106.bidhub.repository.UserRepository userRepo = 
                    com.cc106.bidhub.repository.UserRepository.getInstance(getContext());
                
                // Reload to ensure latest values
                userRepo.reloadUserData();
                
                String alias = userRepo.getAlias();
                double credits = userRepo.getCredits();

                android.util.Log.d("HomeFragment", String.format("Loading user data - Credits: %.2f", credits));

                // Update credit balance (header)
                if (tvHeaderCredits != null) {
                    tvHeaderCredits.setText(String.format(Locale.getDefault(), "%.0f", credits));
                }
            }
        } catch (Exception e) {
            android.util.Log.e("HomeFragment", "Error loading user data", e);
            if (getContext() != null) {
                ToastHelper.showError(getContext(), "Error loading user data: " + e.getMessage());
            }
            e.printStackTrace();
        }
        
        // Load additional data
        loadUserStats();
    }
    
    /**
     * FIX: Load featured items filtered by user's credit balance
     * Featured auctions should show items where startingPrice OR currentHighestBid ≤ userCredits
     * This method now properly updates when credits change
     */
    private void loadFeaturedItems() {
        if (itemManager == null || featuredItemsAdapter == null) {
            hideLoading();
            return;
        }
        
        showLoading();
        
        try {
            // FIX: Always get fresh credit balance from UserRepository
            // This ensures featured items update when credits change
            double userCredits = 0.0;
            if (getContext() != null) {
                com.cc106.bidhub.repository.UserRepository userRepo = 
                    com.cc106.bidhub.repository.UserRepository.getInstance(getContext());
                // Force reload to get latest credits
                userRepo.reloadUserData();
                userCredits = userRepo.getCredits();
            }
            
            android.util.Log.d("HomeFragment", "Loading featured items for user with credits: " + userCredits);
            
            // Get all active items (we'll filter by affordability)
            List<Item> allActiveItems = itemManager.getAllActiveItems();
            if (allActiveItems == null) {
                allActiveItems = new ArrayList<>();
            }
            
            android.util.Log.d("HomeFragment", "Total active items available: " + allActiveItems.size());
            
            // Filter items by affordability: startingPrice OR currentPrice ≤ userCredits
            List<Item> affordableItems = new ArrayList<>();
            for (Item item : allActiveItems) {
                if (item == null) {
                    continue; // Skip null items
                }
                
                // Only show ACTIVE items that haven't ended
                if (item.getStatus() != com.cc106.bidhub.items.ItemStatus.ACTIVE) {
                    continue;
                }
                
                // Exclude ended auctions
                if (item.hasEnded() || !item.isAvailableForBidding()) {
                    continue;
                }
                
                double startingPrice = item.getStartingPrice();
                double currentPrice = item.getCurrentPrice() > 0 ? item.getCurrentPrice() : startingPrice;
                
                // Item is affordable if starting price or current price is within user's credits
                // If user has 0 credits, show empty state (no affordable items)
                if (userCredits > 0 && (startingPrice <= userCredits || currentPrice <= userCredits)) {
                    affordableItems.add(item);
                }
            }
            
            // Sort by creation date (newest first) and limit to 20
            java.util.Collections.sort(affordableItems, new java.util.Comparator<Item>() {
                @Override
                public int compare(Item i1, Item i2) {
                    java.util.Date d1 = i1.getCreatedAt();
                    java.util.Date d2 = i2.getCreatedAt();
                    if (d1 == null && d2 == null) return 0;
                    if (d1 == null) return 1;
                    if (d2 == null) return -1;
                    return d2.compareTo(d1); // Reversed for newest first
                }
            });
            
            if (affordableItems.size() > 20) {
                affordableItems = affordableItems.subList(0, 20);
            }
            
            android.util.Log.d("HomeFragment", "Found " + affordableItems.size() + " affordable featured items");
            
            // FIX: Always update the list, even if empty, to ensure UI reflects current state
            featuredItems.clear();
            featuredItems.addAll(affordableItems);
            
            // Update adapter on main thread
            if (getActivity() != null && !getActivity().isFinishing()) {
                getActivity().runOnUiThread(() -> {
                    if (featuredItemsAdapter != null) {
                        featuredItemsAdapter.notifyDataSetChanged();
                    }
                    // Show/hide empty state
                    updateEmptyStateVisibility();
                    hideLoading();
                });
            } else {
                hideLoading();
            }
        } catch (Exception e) {
            android.util.Log.e("HomeFragment", "Error loading featured items: " + e.getMessage(), e);
            if (getContext() != null) {
                ToastHelper.showError(getContext(), "Error loading featured items");
            }
            hideLoading();
        }
    }
    
    /**
     * Load all active auctions sorted by creation timestamp (newest first)
     * This shows the most recently added item listings
     */
    private void loadActiveAuctions() {
        if (itemManager == null || activeAuctionsAdapter == null) {
            hideLoading();
            return;
        }
        
        try {
            // Get all active items (already sorted by createdAt descending in ItemManager)
            List<Item> items = itemManager.getAllActiveItems();
            if (items == null) {
                items = new ArrayList<>();
            }
            
            // Additional filter to exclude ended auctions (safety check)
            List<Item> nonEndedItems = new ArrayList<>();
            for (Item item : items) {
                if (item != null && !item.hasEnded() && item.isAvailableForBidding()) {
                    nonEndedItems.add(item);
                }
            }
            items = nonEndedItems;
            
            // Ensure proper sorting by creation date (newest first)
            java.util.Collections.sort(items, new java.util.Comparator<Item>() {
                @Override
                public int compare(Item i1, Item i2) {
                    java.util.Date d1 = i1.getCreatedAt();
                    java.util.Date d2 = i2.getCreatedAt();
                    if (d1 == null && d2 == null) return 0;
                    if (d1 == null) return 1;
                    if (d2 == null) return -1;
                    return d2.compareTo(d1); // Reversed for newest first
                }
            });
            
            // Limit to 20 most recent items for homepage
            if (items.size() > 20) {
                items = items.subList(0, 20);
            }
            
            android.util.Log.d("HomeFragment", "Loaded " + items.size() + " active auctions (newest first, ended auctions excluded)");
            
            activeAuctions.clear();
            activeAuctions.addAll(items);
            activeAuctionsAdapter.updateItems(activeAuctions);
            
            hideLoading();
        } catch (Exception e) {
            android.util.Log.e("HomeFragment", "Error loading active auctions: " + e.getMessage(), e);
            if (getContext() != null) {
                ToastHelper.showError(getContext(), "Error loading active auctions");
            }
            hideLoading();
        }
    }
    
    /**
     * Load active bids for current user
     * Fetches all listings where the user has submitted at least one bid
     * Shows item details, latest bid amount, auction end time, and bid status
     * First syncs bids from API, then loads from local cache
     */
    private void loadActiveBids() {
        if (biddingEngine == null || activeBidsAdapter == null || itemManager == null) {
            hideLoading();
            return;
        }
        
        // Sync bids from API first, then load from local cache
        syncBidsFromApi();
    }
    
    /**
     * FIX: Sync user bids from backend API
     * Since there's no dedicated endpoint, we extract bid info from items the user has bid on
     * by checking items that have bids from this user
     * This method now properly handles authentication and API responses
     */
    private void syncBidsFromApi() {
        if (getContext() == null) {
            loadActiveBidsFromLocal();
            return;
        }
        
        // Run on background thread
        new Thread(() -> {
            try {
                SharedPreferencesHelper prefsHelper = new SharedPreferencesHelper(getContext());
                String userId = prefsHelper.getUserId();
                String token = prefsHelper.getAuthToken();
                
                // FIX: Wait for authentication if not ready
                if ((userId == null || userId.isEmpty() || token == null || token.isEmpty())) {
                    android.util.Log.w("HomeFragment", "No user ID or token, waiting for authentication...");
                    int retries = 0;
                    while ((userId == null || userId.isEmpty() || token == null || token.isEmpty()) && retries < 6) {
                        Thread.sleep(500);
                        userId = prefsHelper.getUserId();
                        token = prefsHelper.getAuthToken();
                        retries++;
                    }
                }
                
                if (userId == null || userId.isEmpty() || token == null || token.isEmpty()) {
                    android.util.Log.w("HomeFragment", "No user ID or token after waiting, loading bids from local only");
                    if (getActivity() != null && !getActivity().isFinishing()) {
                        getActivity().runOnUiThread(() -> loadActiveBidsFromLocal());
                    }
                    return;
                }
                
                // Fetch all items from API - items with bids will have bid_count > 0
                // We'll filter items where the user has placed bids
                com.cc106.bidhub.api.ItemApiClient apiClient = new com.cc106.bidhub.api.ItemApiClient(getContext());
                com.cc106.bidhub.api.ItemApiClient.ApiResponse response = apiClient.getItems(null, null, null, null, null, 100, 0);
                
                if (response.isSuccess() && response.getData() != null) {
                    // Parse items and extract bid information
                    String responseData = response.getData();
                    if (responseData != null && !responseData.isEmpty()) {
                        List<Item> allItems = parseItemsFromResponse(responseData);
                        
                        android.util.Log.d("HomeFragment", "Synced " + allItems.size() + " items from API for bid loading");
                        
                        // Store items in ItemManager so they're available for bid loading
                        for (Item item : allItems) {
                            if (item != null && item.getItemId() != null && !item.getItemId().isEmpty()) {
                                itemManager.storeItem(item);
                            }
                        }
                    }
                    
                    // Populate bid cache from API on background thread (to avoid NetworkOnMainThreadException)
                    // This ensures cache is available when loadActiveBidsFromLocal() is called on UI thread
                    android.util.Log.d("HomeFragment", "Items synced, now fetching user bids from API to populate cache");
                    biddingEngine.getUserBids(userId); // This populates the cache on background thread
                    
                    android.util.Log.d("HomeFragment", "Bid cache populated, now loading bids from cache");
                    
                    if (getActivity() != null && !getActivity().isFinishing()) {
                        getActivity().runOnUiThread(() -> loadActiveBidsFromLocal());
                    }
                } else {
                    android.util.Log.w("HomeFragment", "API sync failed: " + (response.getMessage() != null ? response.getMessage() : "Unknown error") + ", loading bids from local");
                    if (getActivity() != null && !getActivity().isFinishing()) {
                        getActivity().runOnUiThread(() -> loadActiveBidsFromLocal());
                    }
                }
            } catch (InterruptedException e) {
                android.util.Log.e("HomeFragment", "Bid sync interrupted", e);
                Thread.currentThread().interrupt();
                if (getActivity() != null && !getActivity().isFinishing()) {
                    getActivity().runOnUiThread(() -> loadActiveBidsFromLocal());
                }
            } catch (Exception e) {
                android.util.Log.e("HomeFragment", "Error syncing bids from API", e);
                if (getActivity() != null && !getActivity().isFinishing()) {
                    getActivity().runOnUiThread(() -> loadActiveBidsFromLocal());
                }
            }
        }).start();
    }
    
    /**
     * FIX: Load active bids from local database/cache
     * Properly handles cases where items might not be in ItemManager yet
     * Ensures active bids section updates dynamically
     */
    private void loadActiveBidsFromLocal() {
        if (biddingEngine == null || activeBidsAdapter == null || itemManager == null) {
            hideLoading();
            return;
        }
        
        try {
            // Get user ID from SharedPreferences
            SharedPreferencesHelper prefsHelper = new SharedPreferencesHelper(getContext());
            String userId = prefsHelper.getUserId();
            
            if (userId == null || userId.isEmpty()) {
                android.util.Log.w("HomeFragment", "No user ID found, clearing active bids");
                activeBids.clear();
                if (activeBidsAdapter != null) {
                    activeBidsAdapter.notifyDataSetChanged();
                }
                updateEmptyStateVisibility();
                hideLoading();
                return;
            }
            
            // Get all active bids for the user from cache only (no network call on UI thread)
            // Network calls are handled in syncBidsFromApi() on background thread
            List<Bid> allUserBids = biddingEngine.getUserBidsFromCache(userId);
            if (allUserBids == null) {
                allUserBids = new ArrayList<>();
            }
            
            android.util.Log.d("HomeFragment", "Found " + allUserBids.size() + " total bids for user");
            
            // Filter to only active bids (bids on items that are still active)
            List<Bid> activeBidsList = new ArrayList<>();
            java.util.Set<String> itemIdsWithBids = new java.util.HashSet<>();
            
            for (Bid bid : allUserBids) {
                if (bid == null || bid.getItemId() == null || bid.getItemId().isEmpty()) {
                    continue; // Skip invalid bids
                }
                
                Item item = itemManager.getItemById(bid.getItemId());
                
                // FIX: If item not found in ItemManager, try to get from active items list
                if (item == null) {
                    // Try to find in active auctions or featured items
                    for (Item activeItem : itemManager.getAllActiveItems()) {
                        if (activeItem != null && activeItem.getItemId() != null && 
                            activeItem.getItemId().equals(bid.getItemId())) {
                            item = activeItem;
                            break;
                        }
                    }
                }
                
                // Only include bids on active items
                if (item != null && item.getStatus() == com.cc106.bidhub.items.ItemStatus.ACTIVE) {
                    // Check if item is still active (hasn't ended)
                    boolean isActive = true;
                    if (item.getEndDate() != null) {
                        long timeRemaining = item.getTimeRemaining();
                        isActive = timeRemaining > 0;
                    }
                    
                    if (isActive) {
                        // Only add one bid per item (the highest one)
                        if (!itemIdsWithBids.contains(bid.getItemId())) {
                            activeBidsList.add(bid);
                            itemIdsWithBids.add(bid.getItemId());
                        } else {
                            // Replace with higher bid if this one is higher
                            for (int i = 0; i < activeBidsList.size(); i++) {
                                Bid existingBid = activeBidsList.get(i);
                                if (existingBid.getItemId().equals(bid.getItemId()) && 
                                    bid.getAmount() > existingBid.getAmount()) {
                                    activeBidsList.set(i, bid);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            
            // Sort by bid amount descending (highest bids first)
            java.util.Collections.sort(activeBidsList, new java.util.Comparator<Bid>() {
                @Override
                public int compare(Bid b1, Bid b2) {
                    return Double.compare(b2.getAmount(), b1.getAmount());
                }
            });
            
            android.util.Log.d("HomeFragment", "Loaded " + activeBidsList.size() + " active bids for user");
            
            // FIX: Always update the list, even if empty, to ensure UI reflects current state
            activeBids.clear();
            activeBids.addAll(activeBidsList);
            
            // Update adapter on main thread
            if (getActivity() != null && !getActivity().isFinishing()) {
                getActivity().runOnUiThread(() -> {
                    if (activeBidsAdapter != null) {
                        activeBidsAdapter.notifyDataSetChanged();
                    }
                    // Show/hide empty state
                    updateEmptyStateVisibility();
                    hideLoading();
                });
            } else {
                hideLoading();
            }
        } catch (Exception e) {
            android.util.Log.e("HomeFragment", "Error loading active bids: " + e.getMessage(), e);
            if (getContext() != null) {
                ToastHelper.showError(getContext(), "Error loading active bids");
            }
            hideLoading();
        }
    }
    
    /**
     * FIX: Public method to refresh featured items when credits change
     * This can be called from MainActivity or other fragments when credits are updated
     */
    public void refreshFeaturedItems() {
        android.util.Log.d("HomeFragment", "Refreshing featured items due to credit change");
        if (getActivity() != null && !getActivity().isFinishing()) {
            getActivity().runOnUiThread(() -> {
                loadFeaturedItems();
            });
        }
    }
    
    /**
     * FIX: Public method to refresh active bids when a new bid is placed
     * This ensures the active bids section updates immediately after bidding
     */
    public void refreshActiveBids() {
        android.util.Log.d("HomeFragment", "Refreshing active bids after new bid placed");
        if (getActivity() != null && !getActivity().isFinishing()) {
            getActivity().runOnUiThread(() -> {
                // Sync from API first to get latest bid data, then load from local
                syncBidsFromApi();
            });
        }
    }
    
    /**
     * Show loading indicator
     */
    private void showLoading() {
        if (progressLoading != null) {
            progressLoading.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * Hide loading indicator
     */
    private void hideLoading() {
        if (progressLoading != null) {
            progressLoading.setVisibility(View.GONE);
        }
    }
    
    /**
     * Update empty state visibility based on data availability
     * Also handles empty states for individual sections
     */
    private void updateEmptyStateVisibility() {
        if (layoutEmptyState == null) {
            return;
        }
        
        boolean hasData = (featuredItems != null && !featuredItems.isEmpty()) ||
                         (activeBids != null && !activeBids.isEmpty()) ||
                         (activeAuctions != null && !activeAuctions.isEmpty());
        
        layoutEmptyState.setVisibility(hasData ? View.GONE : View.VISIBLE);
        
        // Log empty states for debugging
        if (featuredItems != null && featuredItems.isEmpty()) {
            android.util.Log.d("HomeFragment", "Featured items section is empty");
        }
        if (activeBids != null && activeBids.isEmpty()) {
            android.util.Log.d("HomeFragment", "Active bids section is empty");
        }
        if (activeAuctions != null && activeAuctions.isEmpty()) {
            android.util.Log.d("HomeFragment", "Active auctions section is empty");
        }
    }
    
    /**
     * Sync items from backend API before displaying
     * This ensures we have the latest data from the server
     */
    private void syncItemsFromApi() {
        if (getContext() == null) {
            return;
        }
        
        // Sync items from API on background thread
        new Thread(() -> {
            try {
                SharedPreferencesHelper prefsHelper = new SharedPreferencesHelper(getContext());
                String token = prefsHelper.getAuthToken();
                
                // FIX: Wait for authentication if not ready
                if (token == null || token.isEmpty()) {
                    android.util.Log.w("HomeFragment", "No auth token available, waiting for authentication...");
                    int retries = 0;
                    while ((token == null || token.isEmpty()) && retries < 6) {
                        Thread.sleep(500);
                        token = prefsHelper.getAuthToken();
                        retries++;
                    }
                }
                
                if (token == null || token.isEmpty()) {
                    android.util.Log.w("HomeFragment", "No auth token available after waiting, using local items");
                    // Still try to load from local data even without token
                    if (getActivity() != null && !getActivity().isFinishing()) {
                        getActivity().runOnUiThread(() -> {
                            // Load from local ItemManager cache
                            loadFeaturedItems();
                            loadActiveAuctions();
                            loadActiveBids();
                        });
                    }
                    return;
                }
                
                // Fetch items from API
                com.cc106.bidhub.api.ItemApiClient apiClient = new com.cc106.bidhub.api.ItemApiClient(getContext());
                com.cc106.bidhub.api.ItemApiClient.ApiResponse response = apiClient.getItems(null, null, null, null, null, 100, 0);
                
                if (response.isSuccess() && response.getData() != null) {
                    // Parse items from response using same logic as BrowseFragment
                    try {
                        // getData() returns String, so we can use it directly
                        String responseData = response.getData();
                        if (responseData == null || responseData.isEmpty()) {
                            android.util.Log.w("HomeFragment", "API response data is null or empty");
                            // Fallback to local data
                            if (getActivity() != null && !getActivity().isFinishing()) {
                                getActivity().runOnUiThread(() -> {
                                    loadFeaturedItems();
                                    loadActiveAuctions();
                                    loadActiveBids();
                                });
                            }
                            return;
                        }
                        
                        List<Item> apiItems = parseItemsFromResponse(responseData);
                        
                        // Store items in ItemManager using storeItem() which is designed for API-synced items
                        for (Item item : apiItems) {
                            // Always store items from API to ensure we have the latest data
                            // storeItem() handles all the necessary mappings (user items, category items, etc.)
                            if (item != null && item.getItemId() != null && !item.getItemId().isEmpty()) {
                                itemManager.storeItem(item);
                            }
                        }
                        
                        android.util.Log.d("HomeFragment", "Synced " + apiItems.size() + " items from API and stored in ItemManager");
                        
                        // FIX: Now load UI with synced data
                        // Ensure all sections are refreshed with latest data
                        if (getActivity() != null && !getActivity().isFinishing()) {
                            getActivity().runOnUiThread(() -> {
                                android.util.Log.d("HomeFragment", "=== REFRESHING ALL SECTIONS AFTER API SYNC ===");
                                // Reload user data to ensure credits are up to date
                                loadUserData();
                                // Reload all sections with synced data
                                // Order matters: load items first, then bids (bids depend on items)
                                loadFeaturedItems();
                                loadActiveAuctions();
                                // Load active bids last since they depend on items being in ItemManager
                                loadActiveBids();
                                android.util.Log.d("HomeFragment", "All sections refreshed after API sync");
                            });
                        }
                    } catch (Exception e) {
                        android.util.Log.e("HomeFragment", "Error parsing items from API", e);
                        // Fallback to loading from local data
                        if (getActivity() != null && !getActivity().isFinishing()) {
                            getActivity().runOnUiThread(() -> {
                                loadFeaturedItems();
                                loadActiveAuctions();
                                loadActiveBids();
                            });
                        }
                    }
                } else {
                    android.util.Log.w("HomeFragment", "API sync failed, using local items");
                    // Fallback to loading from local data
                    if (getActivity() != null && !getActivity().isFinishing()) {
                        getActivity().runOnUiThread(() -> {
                            loadFeaturedItems();
                            loadActiveAuctions();
                            loadActiveBids();
                        });
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("HomeFragment", "Error syncing items from API", e);
                // Fallback to loading from local data
                if (getActivity() != null && !getActivity().isFinishing()) {
                    getActivity().runOnUiThread(() -> {
                        loadFeaturedItems();
                        loadActiveAuctions();
                        loadActiveBids();
                    });
                }
            }
        }).start();
    }
    
    /**
     * Load user statistics (active bids, items posted)
     */
    private void loadUserStats() {
        if (loggedInUserEmail == null || loggedInUserEmail.isEmpty()) {
            return;
        }
        
        if (dbHelper == null) {
            return;
        }
        
        SQLiteDatabase db = null;
        Cursor bidsCursor = null;
        Cursor itemsCursor = null;
        
        try {
            db = dbHelper.getReadableDatabase();
            
            // Count active bids
            int activeBids = 0;
            bidsCursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_BIDS + 
                " WHERE " + DatabaseHelper.COLUMN_BID_BIDDER_EMAIL + " = ? AND " + 
                DatabaseHelper.COLUMN_BID_STATUS + " = 'ACTIVE'",
                new String[]{loggedInUserEmail}
            );
            if (bidsCursor != null && bidsCursor.moveToFirst()) {
                activeBids = bidsCursor.getInt(0);
            }
            
            // Count items posted
            int itemsPosted = 0;
            itemsCursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_ITEMS + 
                " WHERE " + DatabaseHelper.COLUMN_ITEM_SELLER_EMAIL + " = ?",
                new String[]{loggedInUserEmail}
            );
            if (itemsCursor != null && itemsCursor.moveToFirst()) {
                itemsPosted = itemsCursor.getInt(0);
            }
            
            // Update UI - textActiveBids removed, using RecyclerView now
            // if (textActiveBids != null) {
            //     textActiveBids.setText(String.valueOf(activeBids));
            // }
            
        } catch (Exception e) {
            if (getContext() != null) {
                ToastHelper.showError(getContext(), "Error loading user stats: " + e.getMessage());
            }
            e.printStackTrace();
        } finally {
            if (bidsCursor != null) {
                bidsCursor.close();
            }
            if (itemsCursor != null) {
                itemsCursor.close();
            }
            if (db != null) {
                db.close();
            }
        }
    }
    
    
    public void updateUserEmail(String email) {
        this.loggedInUserEmail = email;
        loadUserData();
    }
    
    /**
     * Load quick stats for dashboard cards
     */
    private void loadQuickStats() {
        if (dbHelper == null || loggedInUserEmail == null) {
            return;
        }
        
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            
            // Get user ID
            String userId = getCurrentUserId();
            if (userId == null) {
                return;
            }
            
            // Count active bids
            int activeBids = 0;
            Cursor bidsCursor = db.rawQuery(
                "SELECT COUNT(*) FROM bids WHERE bidder_id = ? AND status = 'ACTIVE'",
                new String[]{userId}
            );
            if (bidsCursor.moveToFirst()) {
                activeBids = bidsCursor.getInt(0);
            }
            bidsCursor.close();
            
            // Count watching items (placeholder - would need watchlist table)
            int watchingItems = 0;
            
            // Count won items
            int wonItems = 0;
            Cursor wonCursor = db.rawQuery(
                "SELECT COUNT(*) FROM bids WHERE bidder_id = ? AND status = 'WINNING'",
                new String[]{userId}
            );
            if (wonCursor.moveToFirst()) {
                wonItems = wonCursor.getInt(0);
            }
            wonCursor.close();
            
            // Count sold items
            int soldItems = 0;
            Cursor soldCursor = db.rawQuery(
                "SELECT COUNT(*) FROM items WHERE seller_id = ? AND status = 'ENDED'",
                new String[]{userId}
            );
            if (soldCursor.moveToFirst()) {
                soldItems = soldCursor.getInt(0);
            }
            soldCursor.close();
            
            // Update UI if views exist
            if (tvActiveBidsCount != null) {
                tvActiveBidsCount.setText(String.valueOf(activeBids));
            }
            if (tvWatchingCount != null) {
                tvWatchingCount.setText(String.valueOf(watchingItems));
            }
            if (tvWonItemsCount != null) {
                tvWonItemsCount.setText(String.valueOf(wonItems));
            }
            if (tvSoldItemsCount != null) {
                tvSoldItemsCount.setText(String.valueOf(soldItems));
            }
            
        } catch (Exception e) {
            android.util.Log.e("HomeFragment", "Error loading quick stats: " + e.getMessage(), e);
        }
    }
    
    /**
     * Load recent activity for dashboard
     */
    private void loadRecentActivity() {
        if (dbHelper == null || loggedInUserEmail == null) {
            return;
        }
        
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            String userId = getCurrentUserId();
            if (userId == null) {
                return;
            }
            
            // Get recent bids
            Cursor recentBidsCursor = db.rawQuery(
                "SELECT b.amount, i.title, b.placed_at FROM bids b " +
                "JOIN items i ON b.item_id = i.id " +
                "WHERE b.bidder_id = ? " +
                "ORDER BY b.placed_at DESC LIMIT 5",
                new String[]{userId}
            );
            
            // Recent activity display removed from new dashboard design
            // If needed in future, can be re-implemented as a separate section
            // This would show recent bids, won items, etc.
            
            recentBidsCursor.close();
            
        } catch (Exception e) {
            android.util.Log.e("HomeFragment", "Error loading recent activity: " + e.getMessage(), e);
        }
    }
    
    /**
     * Parse items from API response JSON (same logic as BrowseFragment)
     * Enhanced with better error handling and null safety
     */
    private List<Item> parseItemsFromResponse(String responseData) {
        List<Item> items = new ArrayList<>();
        try {
            if (responseData == null || responseData.isEmpty()) {
                android.util.Log.w("HomeFragment", "parseItemsFromResponse: responseData is null or empty");
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
                android.util.Log.w("HomeFragment", "parseItemsFromResponse: No items array found in response");
                return items;
            }
            
            if (itemsArray == null) {
                android.util.Log.w("HomeFragment", "parseItemsFromResponse: itemsArray is null");
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
                        android.util.Log.w("HomeFragment", "Item at index " + i + " has no valid ID, skipping");
                        continue;
                    }
                    item.setItemId(itemId);
                    
                    String title = itemJson.optString("title", "");
                    if (title.isEmpty()) {
                        android.util.Log.w("HomeFragment", "Skipping item with empty title at index " + i);
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
                    
                    // Set seller username
                    String sellerUsername = itemJson.optString("seller_username", null);
                    if (sellerUsername != null && !sellerUsername.isEmpty()) {
                        item.setSellerName(sellerUsername);
                    } else {
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
                    
                    // Set bid count (already retrieved above for price logic)
                    item.setBidCount(bidCount);
                    
                    // Set end date
                    if (itemJson.has("end_date") && !itemJson.isNull("end_date")) {
                        try {
                            String endDateStr = itemJson.getString("end_date");
                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault());
                            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                            item.setEndDate(sdf.parse(endDateStr));
                        } catch (Exception e) {
                            android.util.Log.w("HomeFragment", "Error parsing end_date: " + e.getMessage());
                        }
                    } else if (itemJson.has("bid_deadline") && !itemJson.isNull("bid_deadline")) {
                        try {
                            String deadlineStr = itemJson.getString("bid_deadline");
                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault());
                            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                            item.setEndDate(sdf.parse(deadlineStr));
                        } catch (Exception e) {
                            android.util.Log.w("HomeFragment", "Error parsing bid_deadline: " + e.getMessage());
                        }
                    }
                    
                    // Set created date
                    if (itemJson.has("created_at") && !itemJson.isNull("created_at")) {
                        try {
                            String createdAtStr = itemJson.getString("created_at");
                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault());
                            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                            item.setCreatedAt(sdf.parse(createdAtStr));
                        } catch (Exception e) {
                            android.util.Log.w("HomeFragment", "Error parsing created_at: " + e.getMessage());
                        }
                    }
                    
                    item.setCondition(itemJson.optString("item_condition", itemJson.optString("condition", "good")));
                    item.setStatus(com.cc106.bidhub.items.ItemStatus.ACTIVE);
                    
                    // Parse images - handle multiple formats
                    if (itemJson.has("images")) {
                        try {
                            Object imagesObj = itemJson.get("images");
                            List<String> imagePaths = new ArrayList<>();
                            
                            if (imagesObj instanceof org.json.JSONArray) {
                                org.json.JSONArray imagesArray = (org.json.JSONArray) imagesObj;
                                for (int j = 0; j < imagesArray.length(); j++) {
                                    Object imgObj = imagesArray.get(j);
                                    String imageUrl = null;
                                    
                                    if (imgObj instanceof String) {
                                        imageUrl = (String) imgObj;
                                    } else if (imgObj instanceof org.json.JSONObject) {
                                        org.json.JSONObject imgJson = (org.json.JSONObject) imgObj;
                                        imageUrl = imgJson.optString("image_url", imgJson.optString("url", null));
                                    }
                                    
                                    // FIX: Convert relative URLs to absolute URLs
                                    if (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.equals("null")) {
                                        if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
                                            String baseUrl = "https://bidhub-android-app.onrender.com";
                                            if (imageUrl.startsWith("/")) {
                                                imageUrl = baseUrl + imageUrl;
                                            } else {
                                                imageUrl = baseUrl + "/" + imageUrl;
                                            }
                                            android.util.Log.d("HomeFragment", "Converted relative URL to absolute: " + imageUrl);
                                        }
                                        imagePaths.add(imageUrl);
                                    }
                                }
                            } else if (imagesObj instanceof String) {
                                String imagesString = (String) imagesObj;
                                if (!imagesString.isEmpty() && !imagesString.equals("null")) {
                                    try {
                                        org.json.JSONArray imagesArray = new org.json.JSONArray(imagesString);
                                        for (int j = 0; j < imagesArray.length(); j++) {
                                            String imageUrl = imagesArray.optString(j, null);
                                            // FIX: Convert relative URLs to absolute URLs
                                            if (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.equals("null")) {
                                                if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
                                                    String baseUrl = "https://bidhub-android-app.onrender.com";
                                                    if (imageUrl.startsWith("/")) {
                                                        imageUrl = baseUrl + imageUrl;
                                                    } else {
                                                        imageUrl = baseUrl + "/" + imageUrl;
                                                    }
                                                    android.util.Log.d("HomeFragment", "Converted relative URL to absolute: " + imageUrl);
                                                }
                                                imagePaths.add(imageUrl);
                                            }
                                        }
                                    } catch (org.json.JSONException e) {
                                        android.util.Log.w("HomeFragment", "Failed to parse images string as JSON array: " + imagesString);
                                    }
                                }
                            }
                            
                            item.setImagePaths(imagePaths);
                        } catch (Exception e) {
                            android.util.Log.w("HomeFragment", "Error parsing images", e);
                            item.setImagePaths(new ArrayList<>());
                        }
                    } else {
                        item.setImagePaths(new ArrayList<>());
                    }
                    
                    items.add(item);
                } catch (Exception e) {
                    // Log error for individual item but continue parsing others
                    android.util.Log.e("HomeFragment", "Error parsing item at index " + i + ": " + e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            android.util.Log.e("HomeFragment", "Error parsing items from response", e);
        }
        return items;
    }
    
    /**
     * Get current user ID from email
     */
    private String getCurrentUserId() {
        if (dbHelper == null || loggedInUserEmail == null) {
            return null;
        }
        
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery(
                "SELECT id FROM users WHERE email = ?",
                new String[]{loggedInUserEmail}
            );
            
            String userId = null;
            if (cursor.moveToFirst()) {
                userId = cursor.getString(0);
            }
            cursor.close();
            return userId;
            
        } catch (Exception e) {
            android.util.Log.e("HomeFragment", "Error getting user ID: " + e.getMessage(), e);
            return null;
        }
    }
}
