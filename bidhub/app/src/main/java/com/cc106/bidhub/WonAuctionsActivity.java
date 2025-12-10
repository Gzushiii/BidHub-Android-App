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

import com.cc106.bidhub.adapters.WonAuctionsAdapter;
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
 * Won Auctions Activity
 * Displays all auctions won by the user with contact and delivery information
 */
public class WonAuctionsActivity extends AppCompatActivity {
    
    // UI Components
    private RecyclerView wonAuctionsRecycler;
    private LinearLayout emptyStateLayout;
    private TextView totalWonText;
    private TextView totalSpentText;
    private TextView pendingContactText;
    private Button filterButton;
    private Button sortButton;
    private Button browseItemsButton;
    
    // Data
    private String userId;
    private List<Bid> wonBids;
    private BiddingEngine biddingEngine;
    private ItemManager itemManager;
    private WonAuctionsAdapter adapter;
    
    // Formatting
    private NumberFormat currencyFormat;
    
    // Filter and Sort states
    private WonFilter currentFilter = WonFilter.ALL;
    private WonSort currentSort = WonSort.DATE_DESC;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_won_auctions);
        
        // Initialize components
        initializeComponents();
        initializeData();
        setupUI();
        loadWonAuctions();
    }
    
    private void initializeComponents() {
        wonAuctionsRecycler = findViewById(R.id.won_auctions_recycler);
        emptyStateLayout = findViewById(R.id.empty_state_layout);
        totalWonText = findViewById(R.id.total_won_text);
        totalSpentText = findViewById(R.id.total_spent_text);
        pendingContactText = findViewById(R.id.pending_contact_text);
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
        
        wonBids = new ArrayList<>();
    }
    
    private void setupUI() {
        // Set up RecyclerView
        adapter = new WonAuctionsAdapter(wonBids, this::onAuctionItemClick, this::onContactSellerClick);
        wonAuctionsRecycler.setLayoutManager(new LinearLayoutManager(this));
        wonAuctionsRecycler.setAdapter(adapter);
        
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
    
    private void loadWonAuctions() {
        // Load all bids for user from cache only (no network call on UI thread)
        List<Bid> allBids = biddingEngine.getUserBidsFromCache(userId);
        wonBids.clear();
        
        // Filter for won bids
        for (Bid bid : allBids) {
            if (bid.getStatus() == BidStatus.WINNING) {
                wonBids.add(bid);
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
        
        for (Bid bid : wonBids) {
            Item item = itemManager.getItemById(bid.getItemId());
            if (item == null) continue;
            
            switch (currentFilter) {
                case ALL:
                    filteredBids.add(bid);
                    break;
                case PENDING_CONTACT:
                    // In a real app, this would check if contact has been initiated
                    if (!hasContactedSeller(bid)) {
                        filteredBids.add(bid);
                    }
                    break;
                case CONTACTED:
                    if (hasContactedSeller(bid)) {
                        filteredBids.add(bid);
                    }
                    break;
                case DELIVERED:
                    if (isDelivered(bid)) {
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
    
    private boolean hasContactedSeller(Bid bid) {
        // In a real app, this would check the contact status from database
        // For now, return false for all bids
        return false;
    }
    
    private boolean isDelivered(Bid bid) {
        // In a real app, this would check the delivery status from database
        // For now, return false for all bids
        return false;
    }
    
    private void updateStats() {
        int totalWon = wonBids.size();
        double totalSpent = 0.0;
        int pendingContact = 0;
        
        for (Bid bid : wonBids) {
            totalSpent += bid.getAmount();
            if (!hasContactedSeller(bid)) {
                pendingContact++;
            }
        }
        
        totalWonText.setText(String.valueOf(totalWon));
        totalSpentText.setText(currencyFormat.format(totalSpent));
        pendingContactText.setText(String.valueOf(pendingContact));
    }
    
    private void updateAuctionList() {
        if (wonBids.isEmpty()) {
            wonAuctionsRecycler.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            wonAuctionsRecycler.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
        }
    }
    
    private void onAuctionItemClick(Bid bid) {
        // Open item detail for the won auction
        Intent intent = new Intent(this, ItemDetailActivity.class);
        intent.putExtra("item_id", bid.getItemId());
        intent.putExtra("user_id", userId);
        startActivity(intent);
    }
    
    private void onContactSellerClick(Bid bid) {
        // Open contact seller dialog or activity
        Item item = itemManager.getItemById(bid.getItemId());
        if (item != null) {
            // In a real app, this would open a contact form or messaging system
            android.widget.Toast.makeText(this, "Contact seller functionality would open here", android.widget.Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showFilterDialog() {
        // Create filter options dialog
        String[] filterOptions = {"All Won Auctions", "Pending Contact", "Contacted", "Delivered"};
        int currentSelection = currentFilter.ordinal();
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Filter Won Auctions")
                .setSingleChoiceItems(filterOptions, currentSelection, (dialog, which) -> {
                    currentFilter = WonFilter.values()[which];
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
                .setTitle("Sort Won Auctions")
                .setSingleChoiceItems(sortOptions, currentSelection, (dialog, which) -> {
                    currentSort = WonSort.values()[which];
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
        loadWonAuctions();
    }
    
    // Enums for filter and sort options
    public enum WonFilter {
        ALL, PENDING_CONTACT, CONTACTED, DELIVERED
    }
    
    public enum WonSort {
        DATE_DESC, DATE_ASC, AMOUNT_DESC, AMOUNT_ASC, ITEM_TITLE
    }
}
