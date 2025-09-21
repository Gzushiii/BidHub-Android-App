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

import com.cc106.bidhub.adapters.ActiveBidsAdapter;
import com.cc106.bidhub.bidding.Bid;
import com.cc106.bidhub.bidding.BiddingEngine;
import com.cc106.bidhub.items.Item;
import com.cc106.bidhub.items.ItemManager;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

/**
 * Active Bids Activity
 * Displays all currently active bids with real-time updates
 */
public class ActiveBidsActivity extends AppCompatActivity {
    
    // UI Components
    private RecyclerView activeBidsRecycler;
    private LinearLayout emptyStateLayout;
    private TextView totalActiveText;
    private TextView winningBidsText;
    private TextView totalValueText;
    private TextView lastUpdatedText;
    private Button refreshButton;
    private Button browseItemsButton;
    
    // Data
    private String userId;
    private List<Bid> activeBids;
    private BiddingEngine biddingEngine;
    private ItemManager itemManager;
    private ActiveBidsAdapter adapter;
    
    // Formatting
    private NumberFormat currencyFormat;
    private SimpleDateFormat timeFormat;
    
    // Auto-refresh
    private Handler refreshHandler;
    private Runnable refreshRunnable;
    private static final long REFRESH_INTERVAL = 30000; // 30 seconds
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_bids);
        
        // Initialize components
        initializeComponents();
        initializeData();
        setupUI();
        loadActiveBids();
        startAutoRefresh();
    }
    
    private void initializeComponents() {
        activeBidsRecycler = findViewById(R.id.active_bids_recycler);
        emptyStateLayout = findViewById(R.id.empty_state_layout);
        totalActiveText = findViewById(R.id.total_active_text);
        winningBidsText = findViewById(R.id.winning_bids_text);
        totalValueText = findViewById(R.id.total_value_text);
        lastUpdatedText = findViewById(R.id.last_updated_text);
        refreshButton = findViewById(R.id.refresh_button);
        browseItemsButton = findViewById(R.id.browse_items_button);
        
        // Initialize managers
        biddingEngine = BiddingEngine.getInstance(this);
        itemManager = ItemManager.getInstance(this);
        
        // Initialize formatters
        currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
        currencyFormat.setCurrency(Currency.getInstance("PHP"));
        timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        
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
        
        activeBids = new ArrayList<>();
    }
    
    private void setupUI() {
        // Set up RecyclerView
        adapter = new ActiveBidsAdapter(activeBids, this::onBidItemClick, this::onCancelBidClick);
        activeBidsRecycler.setLayoutManager(new LinearLayoutManager(this));
        activeBidsRecycler.setAdapter(adapter);
        
        // Set up refresh button
        refreshButton.setOnClickListener(v -> refreshBids());
        
        // Set up browse items button
        browseItemsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, BrowseActivity.class);
            intent.putExtra("user_id", userId);
            startActivity(intent);
        });
    }
    
    private void loadActiveBids() {
        // Load active bids for user
        List<Bid> allBids = biddingEngine.getUserBids(userId);
        activeBids.clear();
        
        for (Bid bid : allBids) {
            if (bid.isActive()) {
                activeBids.add(bid);
            }
        }
        
        // Update UI
        updateStats();
        updateBidList();
        updateLastUpdatedTime();
    }
    
    private void updateStats() {
        int totalActive = activeBids.size();
        int winningBids = 0;
        double totalValue = 0.0;
        
        for (Bid bid : activeBids) {
            totalValue += bid.getAmount();
            
            // Check if bid is currently winning
            Item item = itemManager.getItemById(bid.getItemId());
            if (item != null && bid.getAmount() >= item.getCurrentPrice()) {
                winningBids++;
            }
        }
        
        totalActiveText.setText(String.valueOf(totalActive));
        winningBidsText.setText(String.valueOf(winningBids));
        totalValueText.setText(currencyFormat.format(totalValue));
    }
    
    private void updateBidList() {
        adapter.updateBids(activeBids);
        
        if (activeBids.isEmpty()) {
            activeBidsRecycler.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            activeBidsRecycler.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
        }
    }
    
    private void updateLastUpdatedTime() {
        lastUpdatedText.setText("Last updated: " + timeFormat.format(new java.util.Date()));
    }
    
    private void onBidItemClick(Bid bid) {
        // Open item detail for the bid's item
        Intent intent = new Intent(this, ItemDetailActivity.class);
        intent.putExtra("item_id", bid.getItemId());
        intent.putExtra("user_id", userId);
        startActivity(intent);
    }
    
    private void onCancelBidClick(Bid bid) {
        // Show confirmation dialog
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Cancel Bid")
                .setMessage("Are you sure you want to cancel this bid? This action cannot be undone.")
                .setPositiveButton("Cancel Bid", (dialog, which) -> {
                    if (biddingEngine.cancelBid(bid.getBidId(), userId)) {
                        // Refresh the list
                        loadActiveBids();
                    } else {
                        // Show error message
                        android.widget.Toast.makeText(this, "Failed to cancel bid", android.widget.Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Keep Bid", null)
                .show();
    }
    
    private void refreshBids() {
        refreshButton.setEnabled(false);
        refreshButton.setText("Refreshing...");
        
        // Simulate network delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            loadActiveBids();
            refreshButton.setEnabled(true);
            refreshButton.setText("Refresh");
        }, 1000);
    }
    
    private void startAutoRefresh() {
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                loadActiveBids();
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
        loadActiveBids();
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
}
