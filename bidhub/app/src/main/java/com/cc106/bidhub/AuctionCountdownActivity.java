package com.cc106.bidhub;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cc106.bidhub.adapters.AuctionCountdownAdapter;
import com.cc106.bidhub.bidding.Bid;
import com.cc106.bidhub.bidding.BiddingEngine;
import com.cc106.bidhub.items.Item;
import com.cc106.bidhub.items.ItemManager;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

/**
 * Auction Countdown Activity
 * Displays all active auctions with countdown timers
 */
public class AuctionCountdownActivity extends AppCompatActivity {
    
    // UI Components
    private RecyclerView countdownRecycler;
    private LinearLayout emptyStateLayout;
    private TextView activeAuctionsText;
    private TextView endingSoonText;
    private TextView totalValueText;
    private Button filterButton;
    private Button sortButton;
    private Button browseItemsButton;
    
    // Data
    private String userId;
    private List<Item> activeItems;
    private BiddingEngine biddingEngine;
    private ItemManager itemManager;
    private AuctionCountdownAdapter adapter;
    
    // Formatting
    private NumberFormat currencyFormat;
    
    // Auto-refresh
    private Handler refreshHandler;
    private Runnable refreshRunnable;
    private static final long REFRESH_INTERVAL = 1000; // 1 second for countdown updates
    
    // Filter and Sort states
    private CountdownFilter currentFilter = CountdownFilter.ALL;
    private CountdownSort currentSort = CountdownSort.TIME_ASC;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auction_countdown);
        
        // Initialize components
        initializeComponents();
        initializeData();
        setupUI();
        loadActiveAuctions();
        startAutoRefresh();
    }
    
    private void initializeComponents() {
        countdownRecycler = findViewById(R.id.countdown_recycler);
        emptyStateLayout = findViewById(R.id.empty_state_layout);
        activeAuctionsText = findViewById(R.id.active_auctions_text);
        endingSoonText = findViewById(R.id.ending_soon_text);
        totalValueText = findViewById(R.id.total_value_text);
        filterButton = findViewById(R.id.filter_button);
        sortButton = findViewById(R.id.sort_button);
        browseItemsButton = findViewById(R.id.browse_items_button);
        
        // Initialize managers
        biddingEngine = BiddingEngine.getInstance(this);
        itemManager = ItemManager.getInstance(this);
        
        // Initialize currency formatter
        currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
        currencyFormat.setCurrency(Currency.getInstance("PHP"));
        
        // Initialize refresh handler
        refreshHandler = new Handler(Looper.getMainLooper());
    }
    
    private void initializeData() {
        // Get user ID from intent or shared preferences
        userId = getIntent().getStringExtra("user_id");
        if (userId == null) {
            // In a real app, this would come from authentication
            userId = "default_user";
        }
        
        activeItems = new ArrayList<>();
    }
    
    private void setupUI() {
        // Set up RecyclerView
        adapter = new AuctionCountdownAdapter(activeItems, this::onAuctionItemClick);
        countdownRecycler.setLayoutManager(new LinearLayoutManager(this));
        countdownRecycler.setAdapter(adapter);
        
        // Set up filter button
        filterButton.setOnClickListener(v -> showFilterDialog());
        
        // Set up sort button
        sortButton.setOnClickListener(v -> showSortDialog());
        
        // Set up browse items button
        browseItemsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, BrowseActivity.class);
            intent.putExtra("user_id", userId);
            startActivity(intent);
        });
    }
    
    private void loadActiveAuctions() {
        // Load all active items
        activeItems = itemManager.getAllActiveItems();
        
        // Apply current filter
        applyFilter();
        
        // Update UI
        updateStats();
        updateAuctionList();
    }
    
    private void applyFilter() {
        List<Item> filteredItems = new ArrayList<>();
        
        for (Item item : activeItems) {
            switch (currentFilter) {
                case ALL:
                    filteredItems.add(item);
                    break;
                case ENDING_SOON:
                    if (isEndingSoon(item)) {
                        filteredItems.add(item);
                    }
                    break;
                case MY_BIDS:
                    if (hasUserBid(item)) {
                        filteredItems.add(item);
                    }
                    break;
                case HIGH_VALUE:
                    if (item.getCurrentPrice() > 1000) { // High value threshold
                        filteredItems.add(item);
                    }
                    break;
            }
        }
        
        // Apply sorting
        applySorting(filteredItems);
        
        // Update adapter
        adapter.updateItems(filteredItems);
    }
    
    private void applySorting(List<Item> items) {
        switch (currentSort) {
            case TIME_ASC:
                items.sort((i1, i2) -> Long.compare(i1.getTimeRemaining(), i2.getTimeRemaining()));
                break;
            case TIME_DESC:
                items.sort((i1, i2) -> Long.compare(i2.getTimeRemaining(), i1.getTimeRemaining()));
                break;
            case PRICE_ASC:
                items.sort((i1, i2) -> Double.compare(i1.getCurrentPrice(), i2.getCurrentPrice()));
                break;
            case PRICE_DESC:
                items.sort((i1, i2) -> Double.compare(i2.getCurrentPrice(), i1.getCurrentPrice()));
                break;
            case TITLE:
                items.sort((i1, i2) -> i1.getTitle().compareTo(i2.getTitle()));
                break;
        }
    }
    
    private boolean isEndingSoon(Item item) {
        long timeRemaining = item.getTimeRemaining();
        return timeRemaining > 0 && timeRemaining < 24 * 60 * 60 * 1000; // Less than 24 hours
    }
    
    private boolean hasUserBid(Item item) {
        // Check if user has bid on this item
        List<Bid> userBids = biddingEngine.getUserBids(userId);
        for (Bid bid : userBids) {
            if (bid.getItemId().equals(item.getItemId()) && bid.isActive()) {
                return true;
            }
        }
        return false;
    }
    
    private void updateStats() {
        int activeCount = activeItems.size();
        int endingSoonCount = 0;
        double totalValue = 0.0;
        
        for (Item item : activeItems) {
            totalValue += item.getCurrentPrice();
            if (isEndingSoon(item)) {
                endingSoonCount++;
            }
        }
        
        activeAuctionsText.setText(String.valueOf(activeCount));
        endingSoonText.setText(String.valueOf(endingSoonCount));
        totalValueText.setText(currencyFormat.format(totalValue));
    }
    
    private void updateAuctionList() {
        if (activeItems.isEmpty()) {
            countdownRecycler.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            countdownRecycler.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
        }
    }
    
    private void onAuctionItemClick(Item item) {
        // Open item detail for the auction
        Intent intent = new Intent(this, ItemDetailActivity.class);
        intent.putExtra("item_id", item.getItemId());
        intent.putExtra("user_id", userId);
        startActivity(intent);
    }
    
    private void showFilterDialog() {
        // Create filter options dialog
        String[] filterOptions = {"All Auctions", "Ending Soon", "My Bids", "High Value"};
        int currentSelection = currentFilter.ordinal();
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Filter Auctions")
                .setSingleChoiceItems(filterOptions, currentSelection, (dialog, which) -> {
                    currentFilter = CountdownFilter.values()[which];
                    applyFilter();
                    updateAuctionList();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void showSortDialog() {
        // Create sort options dialog
        String[] sortOptions = {"Time (Soonest)", "Time (Latest)", "Price (Lowest)", "Price (Highest)", "Title"};
        int currentSelection = currentSort.ordinal();
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Sort Auctions")
                .setSingleChoiceItems(sortOptions, currentSelection, (dialog, which) -> {
                    currentSort = CountdownSort.values()[which];
                    applyFilter(); // This will also apply sorting
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void startAutoRefresh() {
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                loadActiveAuctions();
                refreshHandler.postDelayed(this, REFRESH_INTERVAL);
            }
        };
        refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL);
    }
    
    private void stopAutoRefresh() {
        if (refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to activity
        loadActiveAuctions();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Stop auto-refresh when activity is paused
        stopAutoRefresh();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up handler
        stopAutoRefresh();
    }
    
    // Enums for filter and sort options
    public enum CountdownFilter {
        ALL, ENDING_SOON, MY_BIDS, HIGH_VALUE
    }
    
    public enum CountdownSort {
        TIME_ASC, TIME_DESC, PRICE_ASC, PRICE_DESC, TITLE
    }
}
