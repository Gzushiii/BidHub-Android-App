package com.cc106.bidhub;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cc106.bidhub.bidding.Bid;
import com.cc106.bidhub.bidding.BidResult;
import com.cc106.bidhub.bidding.BiddingEngine;
import com.cc106.bidhub.items.Item;
import com.cc106.bidhub.items.ItemManager;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

/**
 * Bidding Activity
 * Allows users to place bids on items
 */
public class BiddingActivity extends AppCompatActivity {
    
    // UI Components
    private TextView itemTitleText;
    private TextView itemDescriptionText;
    private TextView currentBidText;
    private TextView timeRemainingText;
    private TextView bidderCountText;
    private EditText bidAmountInput;
    private Button placeBidButton;
    private Button cancelButton;
    
    // Data
    private String itemId;
    private String userId;
    private String userAlias;
    private Item item;
    private BiddingEngine biddingEngine;
    private ItemManager itemManager;
    
    // Formatting
    private NumberFormat currencyFormat;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bidding);
        
        // Initialize components
        initializeComponents();
        initializeData();
        setupUI();
        loadItemData();
    }
    
    private void initializeComponents() {
        itemTitleText = findViewById(R.id.item_title_text);
        itemDescriptionText = findViewById(R.id.item_description_text);
        currentBidText = findViewById(R.id.current_bid_text);
        timeRemainingText = findViewById(R.id.time_remaining_text);
        bidderCountText = findViewById(R.id.bidder_count_text);
        bidAmountInput = findViewById(R.id.bid_amount_input);
        placeBidButton = findViewById(R.id.place_bid_button);
        cancelButton = findViewById(R.id.cancel_button);
        
        // Initialize managers
        biddingEngine = BiddingEngine.getInstance(this);
        itemManager = ItemManager.getInstance(this);
        
        // Initialize currency formatter
        currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
        currencyFormat.setCurrency(Currency.getInstance("PHP"));
    }
    
    private void initializeData() {
        // Get data from intent
        Intent intent = getIntent();
        itemId = intent.getStringExtra("item_id");
        userId = intent.getStringExtra("user_id");
        userAlias = intent.getStringExtra("user_alias");
        
        if (itemId == null || userId == null || userAlias == null) {
            Toast.makeText(this, "Missing required data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }
    
    private void setupUI() {
        // Set up place bid button
        placeBidButton.setOnClickListener(v -> placeBid());
        
        // Set up cancel button
        cancelButton.setOnClickListener(v -> finish());
        
        // Set minimum bid amount in input hint
        bidAmountInput.setHint("Minimum bid: " + currencyFormat.format(item.getCurrentPrice() + 1.0));
    }
    
    private void loadItemData() {
        // Get item data
        item = itemManager.getItemById(itemId);
        if (item == null) {
            Toast.makeText(this, "Item not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Update UI with item data
        updateItemDisplay();
        
        // Check if user can bid
        if (!canUserBid()) {
            placeBidButton.setEnabled(false);
            placeBidButton.setText("Cannot Bid");
        }
    }
    
    private void updateItemDisplay() {
        if (item == null) return;
        
        // Set item details
        itemTitleText.setText(item.getTitle());
        itemDescriptionText.setText(item.getDescription());
        
        // Set current bid
        currentBidText.setText(currencyFormat.format(item.getCurrentPrice()));
        
        // Set time remaining
        if (item.getEndDate() != null) {
            long timeRemaining = item.getTimeRemaining();
            if (timeRemaining > 0) {
                int hours = (int) (timeRemaining / (60 * 60 * 1000));
                int minutes = (int) ((timeRemaining % (60 * 60 * 1000)) / (60 * 1000));
                timeRemainingText.setText(String.format("%d hours, %d minutes", hours, minutes));
            } else {
                timeRemainingText.setText("Auction ended");
                placeBidButton.setEnabled(false);
            }
        } else {
            timeRemainingText.setText("No deadline set");
        }
        
        // Set bidder count
        bidderCountText.setText(String.valueOf(item.getBidCount()));
    }
    
    private boolean canUserBid() {
        if (item == null) return false;
        
        // Check if user is the seller
        if (userId.equals(item.getSellerId())) {
            return false;
        }
        
        // Check if auction is active
        if (!item.isAvailableForBidding()) {
            return false;
        }
        
        // Check if user has sufficient credits
        // Note: This would need to be implemented with CreditManager
        return true;
    }
    
    private void placeBid() {
        // Get bid amount
        String bidAmountText = bidAmountInput.getText().toString().trim();
        if (bidAmountText.isEmpty()) {
            Toast.makeText(this, "Please enter a bid amount", Toast.LENGTH_SHORT).show();
            return;
        }
        
        double bidAmount;
        try {
            bidAmount = Double.parseDouble(bidAmountText);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid bid amount", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Validate bid amount
        if (bidAmount <= item.getCurrentPrice()) {
            Toast.makeText(this, "Bid must be higher than current bid", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Place bid
        placeBidButton.setEnabled(false);
        placeBidButton.setText("Placing Bid...");
        
        // Note: In a real implementation, this would be done asynchronously
        BidResult result = biddingEngine.placeBid(itemId, bidAmount);
        
        if (result.isSuccess()) {
            Toast.makeText(this, "Bid placed successfully!", Toast.LENGTH_SHORT).show();
            
            // Update display
            loadItemData();
            
            // Clear input
            bidAmountInput.setText("");
            
        } else {
            Toast.makeText(this, "Failed to place bid: " + result.getMessage(), Toast.LENGTH_LONG).show();
        }
        
        placeBidButton.setEnabled(true);
        placeBidButton.setText("Place Bid");
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh item data when returning to activity
        loadItemData();
    }
}


