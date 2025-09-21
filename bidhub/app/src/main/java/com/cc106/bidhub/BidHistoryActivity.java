package com.cc106.bidhub;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cc106.bidhub.adapters.BidHistoryAdapter;
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
 * Bid History Activity
 * Displays all bids made by the user with filtering and sorting options
 */
public class BidHistoryActivity extends AppCompatActivity {
    
    // UI Components
    private RecyclerView bidHistoryRecycler;
    private LinearLayout emptyStateLayout;
    private TextView totalBidsText;
    private TextView activeBidsText;
    private TextView wonAuctionsText;
    private Button filterButton;
    private Button sortButton;
    
    // Data
    private String userId;
    private List<Bid> allBids;
    private List<Bid> filteredBids;
    private BiddingEngine biddingEngine;
    private ItemManager itemManager;
    private BidHistoryAdapter adapter;
    
    // Formatting
    private NumberFormat currencyFormat;
    
    // Filter and Sort states
    private BidFilter currentFilter = BidFilter.ALL;
    private BidSort currentSort = BidSort.DATE_DESC;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bid_history);
        
        // Initialize components
        initializeComponents();
        initializeData();
        setupUI();
        loadBidHistory();
    }
    
    private void initializeComponents() {
        bidHistoryRecycler = findViewById(R.id.bid_history_recycler);
        emptyStateLayout = findViewById(R.id.empty_state_layout);
        totalBidsText = findViewById(R.id.total_bids_text);
        activeBidsText = findViewById(R.id.active_bids_text);
        wonAuctionsText = findViewById(R.id.won_auctions_text);
        filterButton = findViewById(R.id.filter_button);
        sortButton = findViewById(R.id.sort_button);
        
        // Initialize managers
        biddingEngine = BiddingEngine.getInstance(this);
        itemManager = ItemManager.getInstance(this);
        
        // Initialize currency formatter
        currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
        currencyFormat.setCurrency(Currency.getInstance("PHP"));
    }
    
    private void initializeData() {
        // Get user ID from intent or shared preferences
        userId = getIntent().getStringExtra("user_id");
        if (userId == null) {
            // In a real app, this would come from authentication
            userId = "default_user";
        }
        
        allBids = new ArrayList<>();
        filteredBids = new ArrayList<>();
    }
    
    private void setupUI() {
        // Set up RecyclerView
        adapter = new BidHistoryAdapter(filteredBids, this::onBidItemClick);
        bidHistoryRecycler.setLayoutManager(new LinearLayoutManager(this));
        bidHistoryRecycler.setAdapter(adapter);
        
        // Set up filter button
        filterButton.setOnClickListener(v -> showFilterDialog());
        
        // Set up sort button
        sortButton.setOnClickListener(v -> showSortDialog());
    }
    
    private void loadBidHistory() {
        // Load all bids for user
        allBids = biddingEngine.getUserBids(userId);
        
        // Apply current filter
        applyFilter();
        
        // Update UI
        updateStats();
        updateBidList();
    }
    
    private void applyFilter() {
        filteredBids.clear();
        
        switch (currentFilter) {
            case ALL:
                filteredBids.addAll(allBids);
                break;
            case ACTIVE:
                for (Bid bid : allBids) {
                    if (bid.isActive()) {
                        filteredBids.add(bid);
                    }
                }
                break;
            case WON:
                for (Bid bid : allBids) {
                    if (bid.getStatus() == com.cc106.bidhub.bidding.BidStatus.WINNING) {
                        filteredBids.add(bid);
                    }
                }
                break;
            case OUTBID:
                for (Bid bid : allBids) {
                    if (bid.getStatus() == com.cc106.bidhub.bidding.BidStatus.OUTBID) {
                        filteredBids.add(bid);
                    }
                }
                break;
            case CANCELLED:
                for (Bid bid : allBids) {
                    if (bid.getStatus() == com.cc106.bidhub.bidding.BidStatus.CANCELLED) {
                        filteredBids.add(bid);
                    }
                }
                break;
        }
        
        // Apply sorting
        applySorting();
    }
    
    private void applySorting() {
        switch (currentSort) {
            case DATE_DESC:
                filteredBids.sort((b1, b2) -> b2.getPlacedAt().compareTo(b1.getPlacedAt()));
                break;
            case DATE_ASC:
                filteredBids.sort((b1, b2) -> b1.getPlacedAt().compareTo(b2.getPlacedAt()));
                break;
            case AMOUNT_DESC:
                filteredBids.sort((b1, b2) -> Double.compare(b2.getAmount(), b1.getAmount()));
                break;
            case AMOUNT_ASC:
                filteredBids.sort((b1, b2) -> Double.compare(b1.getAmount(), b2.getAmount()));
                break;
            case STATUS:
                filteredBids.sort((b1, b2) -> b1.getStatus().toString().compareTo(b2.getStatus().toString()));
                break;
        }
    }
    
    private void updateStats() {
        int totalBids = allBids.size();
        int activeBids = 0;
        int wonAuctions = 0;
        
        for (Bid bid : allBids) {
            if (bid.isActive()) {
                activeBids++;
            }
            if (bid.getStatus() == com.cc106.bidhub.bidding.BidStatus.WINNING) {
                wonAuctions++;
            }
        }
        
        totalBidsText.setText(String.valueOf(totalBids));
        activeBidsText.setText(String.valueOf(activeBids));
        wonAuctionsText.setText(String.valueOf(wonAuctions));
    }
    
    private void updateBidList() {
        adapter.updateBids(filteredBids);
        
        if (filteredBids.isEmpty()) {
            bidHistoryRecycler.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            bidHistoryRecycler.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
        }
    }
    
    private void onBidItemClick(Bid bid) {
        // Open item detail for the bid's item
        Intent intent = new Intent(this, ItemDetailActivity.class);
        intent.putExtra("item_id", bid.getItemId());
        intent.putExtra("user_id", userId);
        startActivity(intent);
    }
    
    private void showFilterDialog() {
        // Create filter options dialog
        String[] filterOptions = {"All Bids", "Active Bids", "Won Auctions", "Outbid", "Cancelled"};
        int currentSelection = currentFilter.ordinal();
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Filter Bids")
                .setSingleChoiceItems(filterOptions, currentSelection, (dialog, which) -> {
                    currentFilter = BidFilter.values()[which];
                    applyFilter();
                    updateBidList();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void showSortDialog() {
        // Create sort options dialog
        String[] sortOptions = {"Date (Newest)", "Date (Oldest)", "Amount (Highest)", "Amount (Lowest)", "Status"};
        int currentSelection = currentSort.ordinal();
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Sort Bids")
                .setSingleChoiceItems(sortOptions, currentSelection, (dialog, which) -> {
                    currentSort = BidSort.values()[which];
                    applySorting();
                    updateBidList();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to activity
        loadBidHistory();
    }
    
    // Enums for filter and sort options
    public enum BidFilter {
        ALL, ACTIVE, WON, OUTBID, CANCELLED
    }
    
    public enum BidSort {
        DATE_DESC, DATE_ASC, AMOUNT_DESC, AMOUNT_ASC, STATUS
    }
}
