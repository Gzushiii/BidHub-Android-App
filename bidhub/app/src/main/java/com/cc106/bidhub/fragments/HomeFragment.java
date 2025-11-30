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
import androidx.recyclerview.widget.GridLayoutManager;

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
import com.cc106.bidhub.adapters.CategoryAdapter;
import com.cc106.bidhub.adapters.ItemCardAdapter;
import com.cc106.bidhub.bidding.Bid;
import com.cc106.bidhub.bidding.BiddingEngine;
import com.cc106.bidhub.items.Category;
import com.cc106.bidhub.items.CategoryManager;
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
    private View searchBar;
    
    // Category chips
    private ViewGroup layoutCategoryChips;
    
    // Quick action buttons
    private View cardBrowse, cardSell, cardMyListings;
    
    // RecyclerViews
    private RecyclerView rvFeaturedItems;
    private RecyclerView rvActiveAuctions;
    private RecyclerView rvActiveBids;
    private RecyclerView rvCategories;
    
    // Adapters
    private ItemCardAdapter featuredItemsAdapter;
    private com.cc106.bidhub.adapters.ActiveAuctionsAdapter activeAuctionsAdapter;
    private ActiveBidsAdapter activeBidsAdapter;
    private CategoryAdapter categoryAdapter;
    
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
    
    // Recent activity
    private TextView textRecentActivity;
    private View layoutRecentActivity;
    
    // Managers
    private ItemManager itemManager;
    private BiddingEngine biddingEngine;
    private CategoryManager categoryManager;
    
    // Data lists
    private List<Item> featuredItems;
    private List<Item> activeAuctions;
    private List<Bid> activeBids;
    private List<Category> categories;
    
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
                categoryManager = CategoryManager.getInstance();
            }
            
            // Initialize data lists
            featuredItems = new ArrayList<>();
            activeAuctions = new ArrayList<>();
            activeBids = new ArrayList<>();
            categories = new ArrayList<>();
            
            // Initialize all UI components
            initializeViews(view);
            
            // Set up RecyclerViews
            setupRecyclerViews();
            
            // Load user data and display it
            loadUserData();
            
            // Load RecyclerView data
            loadFeaturedItems();
            loadActiveAuctions();
            loadActiveBids();
            loadCategories();
            
            // Initialize category chips
            initializeCategoryChips();
            
            // Load quick stats
            loadQuickStats();
            
            // Load recent activity
            loadRecentActivity();
            
            // Set up click listeners
            setupClickListeners();
            
            return view;
        } catch (Exception e) {
            if (getContext() != null) {
                ToastHelper.showError(getContext(), "Error creating home view: " + e.getMessage());
            }
            e.printStackTrace();
            return null;
        }
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
        searchBar = view.findViewById(R.id.search_card);
        
        // Category chips container
        layoutCategoryChips = view.findViewById(R.id.layout_category_chips);
        
        // Quick action buttons (removed - not in new design)
        // cardBrowse = view.findViewById(R.id.card_browse);
        // cardSell = view.findViewById(R.id.card_post);
        // cardMyListings = view.findViewById(R.id.card_my_listings);
        
        // RecyclerViews
        rvFeaturedItems = view.findViewById(R.id.rv_featured_items);
        rvActiveAuctions = view.findViewById(R.id.rv_active_auctions);
        rvActiveBids = view.findViewById(R.id.rv_active_bids);
        rvCategories = view.findViewById(R.id.rv_categories);
        
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
        
        // Categories RecyclerView - grid layout
        if (rvCategories != null) {
            categoryAdapter = new CategoryAdapter(categories);
            categoryAdapter.setOnCategoryClickListener(category -> {
                // Navigate to browse with category filter
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).switchToBrowseTab();
                    // TODO: Pass category filter to BrowseActivity
                }
            });
            GridLayoutManager categoryLayoutManager = new GridLayoutManager(getContext(), 2);
            rvCategories.setLayoutManager(categoryLayoutManager);
            rvCategories.setAdapter(categoryAdapter);
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
        
        // Search bar click listener
        if (searchBar != null) {
            searchBar.setOnClickListener(v -> {
                try {
                    // Navigate to browse tab
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).switchToBrowseTab();
                    }
                } catch (Exception e) {
                    if (getContext() != null) {
                        ToastHelper.showError(getContext(), "Error opening browse: " + e.getMessage());
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
    
    private void loadUserData() {
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
        
        if (loggedInUserEmail == null || loggedInUserEmail.isEmpty()) {
            if (getContext() != null) {
            ToastHelper.showError(getContext(), "Error: User not identified.");
            }
            return;
        }

        if (dbHelper == null) {
            if (getContext() != null) {
                ToastHelper.showError(getContext(), "Error: Database not initialized.");
            }
            return;
        }

        // Load user data from SharedPreferences (synced with backend)
        try {
            SharedPreferencesHelper prefsHelper = new SharedPreferencesHelper(getContext());
            String alias = prefsHelper.getAlias();
            double credits = prefsHelper.getCredits();

            // Update the UI
            if (textViewAlias != null) {
                textViewAlias.setText(alias != null ? alias : "User");
            }
            
            // Update credit balance (header)
            if (tvHeaderCredits != null) {
                tvHeaderCredits.setText(String.format(Locale.getDefault(), "%.0f", credits));
            }
        } catch (Exception e) {
            if (getContext() != null) {
                ToastHelper.showError(getContext(), "Error loading user data: " + e.getMessage());
            }
            e.printStackTrace();
        }
        
        // Load additional data
        loadUserStats();
    }
    
    /**
     * Load featured items from ItemManager
     */
    private void loadFeaturedItems() {
        if (itemManager == null || featuredItemsAdapter == null) {
            hideLoading();
            return;
        }
        
        showLoading();
        
        try {
            List<Item> items = itemManager.getFeaturedItems();
            if (items == null) {
                items = new ArrayList<>();
            }
            
            featuredItems.clear();
            featuredItems.addAll(items);
            featuredItemsAdapter.notifyDataSetChanged();
            
            // Show/hide empty state
            updateEmptyStateVisibility();
            hideLoading();
        } catch (Exception e) {
            android.util.Log.e("HomeFragment", "Error loading featured items: " + e.getMessage(), e);
            if (getContext() != null) {
                ToastHelper.showError(getContext(), "Error loading featured items");
            }
            hideLoading();
        }
    }
    
    /**
     * Load all active auctions (not just user's bids)
     */
    private void loadActiveAuctions() {
        if (itemManager == null || activeAuctionsAdapter == null) {
            hideLoading();
            return;
        }
        
        try {
            List<Item> items = itemManager.getActiveItems();
            if (items == null) {
                items = new ArrayList<>();
            }
            
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
     */
    private void loadActiveBids() {
        if (biddingEngine == null || activeBidsAdapter == null) {
            hideLoading();
            return;
        }
        
        try {
            // Get user ID from SharedPreferences
            SharedPreferencesHelper prefsHelper = new SharedPreferencesHelper(getContext());
            String userId = prefsHelper.getUserId();
            
            if (userId == null || userId.isEmpty()) {
                activeBids.clear();
                activeBidsAdapter.notifyDataSetChanged();
                updateEmptyStateVisibility();
                hideLoading();
                return;
            }
            
            List<Bid> bids = biddingEngine.getUserActiveBids(userId);
            if (bids == null) {
                bids = new ArrayList<>();
            }
            
            activeBids.clear();
            activeBids.addAll(bids);
            activeBidsAdapter.notifyDataSetChanged();
            
            // Show/hide empty state
            updateEmptyStateVisibility();
            hideLoading();
        } catch (Exception e) {
            android.util.Log.e("HomeFragment", "Error loading active bids: " + e.getMessage(), e);
            if (getContext() != null) {
                ToastHelper.showError(getContext(), "Error loading active bids");
            }
            hideLoading();
        }
    }
    
    /**
     * Initialize category chips in HorizontalScrollView
     */
    private void initializeCategoryChips() {
        if (layoutCategoryChips == null || getContext() == null) {
            return;
        }
        
        // Clear existing chips
        layoutCategoryChips.removeAllViews();
        
        // Category names
        String[] categoryNames = {
            getString(R.string.electronics),
            getString(R.string.fashion),
            getString(R.string.collectibles),
            getString(R.string.home_garden),
            getString(R.string.art)
        };
        
        // Create and add chips
        for (String categoryName : categoryNames) {
            TextView chip = (TextView) LayoutInflater.from(getContext())
                    .inflate(R.layout.item_category_chip, layoutCategoryChips, false);
            chip.setText(categoryName);
            chip.setOnClickListener(v -> {
                // Navigate to browse with category filter
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).switchToBrowseTab();
                    // TODO: Pass category filter to BrowseActivity
                }
            });
            layoutCategoryChips.addView(chip);
        }
    }
    
    /**
     * Load categories from CategoryManager
     */
    private void loadCategories() {
        if (categoryManager == null || categoryAdapter == null) {
            hideLoading();
            return;
        }
        
        try {
            List<Category> mainCategories = categoryManager.getAllMainCategories();
            if (mainCategories == null) {
                mainCategories = new ArrayList<>();
            }
            
            categories.clear();
            categories.addAll(mainCategories);
            categoryAdapter.notifyDataSetChanged();
            
            // Show/hide empty state
            updateEmptyStateVisibility();
            hideLoading();
        } catch (Exception e) {
            android.util.Log.e("HomeFragment", "Error loading categories: " + e.getMessage(), e);
            if (getContext() != null) {
                ToastHelper.showError(getContext(), "Error loading categories");
            }
            hideLoading();
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
     */
    private void updateEmptyStateVisibility() {
        if (layoutEmptyState == null) {
            return;
        }
        
        boolean hasData = (featuredItems != null && !featuredItems.isEmpty()) ||
                         (activeBids != null && !activeBids.isEmpty()) ||
                         (categories != null && !categories.isEmpty());
        
        layoutEmptyState.setVisibility(hasData ? View.GONE : View.VISIBLE);
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
            
            // TODO: Display recent activity in layoutRecentActivity
            // This would show recent bids, won items, etc.
            
            recentBidsCursor.close();
            
        } catch (Exception e) {
            android.util.Log.e("HomeFragment", "Error loading recent activity: " + e.getMessage(), e);
        }
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
