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

import com.cc106.bidhub.adapters.LostAuctionsAdapter;
import com.cc106.bidhub.bidding.Bid;
import com.cc106.bidhub.bidding.BidStatus;
import com.cc106.bidhub.bidding.BiddingEngine;
import com.cc106.bidhub.items.Item;
import com.cc106.bidhub.items.ItemManager;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

/**
 * Lost Auctions Activity
 * Displays all auctions lost by the user with analytics and insights
 */
public class LostAuctionsActivity extends AppCompatActivity {
    
    // UI Components
    private RecyclerView lostAuctionsRecycler;
    private LinearLayout emptyStateLayout;
    private TextView totalLostText;
    private TextView totalBidText;
    private TextView winRateText;
    private Button filterButton;
    private Button sortButton;
    private Button browseItemsButton;
    
    // Data
    private String userId;
    private List<Bid> lostBids;
    private List<Bid> allBids;
    private BiddingEngine biddingEngine;
    private ItemManager itemManager;
    private LostAuctionsAdapter adapter;
    
    // Formatting
    private NumberFormat currencyFormat;
    
    // Filter and Sort states
    private LostFilter currentFilter = LostFilter.ALL;
    private LostSort currentSort = LostSort.DATE_DESC;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lost_auctions);
        
        // Initialize components
        initializeComponents();
        initializeData();
        setupUI();
        loadLostAuctions();
    }
    
    private void initializeComponents() {
        lostAuctionsRecycler = findViewById(R.id.lost_auctions_recycler);
        emptyStateLayout = findViewById(R.id.empty_state_layout);
        totalLostText = findViewById(R.id.total_lost_text);
        totalBidText = findViewById(R.id.total_bid_text);
        winRateText = findViewById(R.id.win_rate_text);
        filterButton = findViewById(R.id.filter_button);
        sortButton = findViewById(R.id.sort_button);
        browseItemsButton = findViewById(R.id.browse_items_button);
        
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
        
        lostBids = new ArrayList<>();
        allBids = new ArrayList<>();
    }
    
    private void setupUI() {
        // Set up RecyclerView
        adapter = new LostAuctionsAdapter(lostBids, this::onAuctionItemClick);
        lostAuctionsRecycler.setLayoutManager(new LinearLayoutManager(this));
        lostAuctionsRecycler.setAdapter(adapter);
        
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
    
    private void loadLostAuctions() {
        // Load all bids for user
        allBids = biddingEngine.getUserBids(userId);
        lostBids.clear();
        
        // Filter for lost bids
        for (Bid bid : allBids) {
            if (bid.getStatus() == BidStatus.OUTBID) {
                lostBids.add(bid);
            }
        }
        
        // Apply current filter
        applyFilter();
        
        // Update UI
        updateStats();
        updateAuctionList();
    }
    
    private void applyFilter() {
        List<Bid> filteredBids = new ArrayList<>();
        
        for (Bid bid : lostBids) {
            Item item = itemManager.getItemById(bid.getItemId());
            if (item == null) continue;
            
            switch (currentFilter) {
                case ALL:
                    filteredBids.add(bid);
                    break;
                case RECENT:
                    // In a real app, this would check if the auction ended recently
                    if (isRecentLoss(bid)) {
                        filteredBids.add(bid);
                    }
                    break;
                case HIGH_VALUE:
                    if (bid.getAmount() > 1000) { // High value threshold
                        filteredBids.add(bid);
                    }
                    break;
                case CLOSE_CALLS:
                    if (isCloseCall(bid, item)) {
                        filteredBids.add(bid);
                    }
                    break;
            }
        }
        
        // Apply sorting
        applySorting(filteredBids);
        
        // Update adapter
        adapter.updateBids(filteredBids);
    }
    
    private void applySorting(List<Bid> bids) {
        switch (currentSort) {
            case DATE_DESC:
                bids.sort((b1, b2) -> b2.getPlacedAt().compareTo(b1.getPlacedAt()));
                break;
            case DATE_ASC:
                bids.sort((b1, b2) -> b1.getPlacedAt().compareTo(b2.getPlacedAt()));
                break;
            case AMOUNT_DESC:
                bids.sort((b1, b2) -> Double.compare(b2.getAmount(), b1.getAmount()));
                break;
            case AMOUNT_ASC:
                bids.sort((b1, b2) -> Double.compare(b1.getAmount(), b2.getAmount()));
                break;
            case ITEM_TITLE:
                bids.sort((b1, b2) -> {
                    Item item1 = itemManager.getItemById(b1.getItemId());
                    Item item2 = itemManager.getItemById(b2.getItemId());
                    String title1 = item1 != null ? item1.getTitle() : "";
                    String title2 = item2 != null ? item2.getTitle() : "";
                    return title1.compareTo(title2);
                });
                break;
        }
    }
    
    private boolean isRecentLoss(Bid bid) {
        // In a real app, this would check if the auction ended within the last 7 days
        long timeSincePlaced = System.currentTimeMillis() - bid.getPlacedAt().getTime();
        return timeSincePlaced < 7 * 24 * 60 * 60 * 1000; // 7 days
    }
    
    private boolean isCloseCall(Bid bid, Item item) {
        // Check if the bid was close to the winning bid (within 10%)
        double winningBid = item.getCurrentPrice();
        double bidAmount = bid.getAmount();
        double percentage = (bidAmount / winningBid) * 100;
        return percentage >= 90; // Within 90% of winning bid
    }
    
    private void updateStats() {
        int totalLost = lostBids.size();
        int totalWon = 0;
        double totalBidAmount = 0.0;
        
        // Calculate total bid amount for lost auctions
        for (Bid bid : lostBids) {
            totalBidAmount += bid.getAmount();
        }
        
        // Count won auctions
        for (Bid bid : allBids) {
            if (bid.getStatus() == BidStatus.WINNING) {
                totalWon++;
            }
        }
        
        // Calculate win rate
        int totalAuctions = totalLost + totalWon;
        double winRate = totalAuctions > 0 ? (double) totalWon / totalAuctions * 100 : 0.0;
        
        totalLostText.setText(String.valueOf(totalLost));
        totalBidText.setText(currencyFormat.format(totalBidAmount));
        winRateText.setText(String.format("%.1f%%", winRate));
    }
    
    private void updateAuctionList() {
        if (lostBids.isEmpty()) {
            lostAuctionsRecycler.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            lostAuctionsRecycler.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
        }
    }
    
    private void onAuctionItemClick(Bid bid) {
        // Open item detail for the lost auction
        Intent intent = new Intent(this, ItemDetailActivity.class);
        intent.putExtra("item_id", bid.getItemId());
        intent.putExtra("user_id", userId);
        startActivity(intent);
    }
    
    private void showFilterDialog() {
        // Create filter options dialog
        String[] filterOptions = {"All Lost Auctions", "Recent Losses", "High Value", "Close Calls"};
        int currentSelection = currentFilter.ordinal();
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Filter Lost Auctions")
                .setSingleChoiceItems(filterOptions, currentSelection, (dialog, which) -> {
                    currentFilter = LostFilter.values()[which];
                    applyFilter();
                    updateAuctionList();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void showSortDialog() {
        // Create sort options dialog
        String[] sortOptions = {"Date (Newest)", "Date (Oldest)", "Amount (Highest)", "Amount (Lowest)", "Item Title"};
        int currentSelection = currentSort.ordinal();
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Sort Lost Auctions")
                .setSingleChoiceItems(sortOptions, currentSelection, (dialog, which) -> {
                    currentSort = LostSort.values()[which];
                    applyFilter(); // This will also apply sorting
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to activity
        loadLostAuctions();
    }
    
    // Enums for filter and sort options
    public enum LostFilter {
        ALL, RECENT, HIGH_VALUE, CLOSE_CALLS
    }
    
    public enum LostSort {
        DATE_DESC, DATE_ASC, AMOUNT_DESC, AMOUNT_ASC, ITEM_TITLE
    }
}
