package com.cc106.bidhub;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cc106.bidhub.bidding.Bid;
import com.cc106.bidhub.bidding.BidResult;
import com.cc106.bidhub.bidding.BiddingEngine;
import com.cc106.bidhub.credits.CreditManager;
import com.cc106.bidhub.items.Item;
import com.cc106.bidhub.items.ItemManager;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

/**
 * Bid Confirmation Activity
 * Allows users to review and confirm their bid before placing it
 */
public class BidConfirmationActivity extends AppCompatActivity {
    
    // UI Components
    private TextView itemTitleText;
    private TextView currentBidText;
    private TextView timeRemainingText;
    private TextView bidAmountText;
    private TextView bidderAliasText;
    private TextView creditBalanceText;
    private TextView remainingBalanceText;
    private Button cancelButton;
    private Button confirmBidButton;
    
    // Data
    private String itemId;
    private String userId;
    private String userAlias;
    private double bidAmount;
    private Item item;
    private BiddingEngine biddingEngine;
    private ItemManager itemManager;
    private CreditManager creditManager;
    
    // Formatting
    private NumberFormat currencyFormat;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bid_confirmation);
        
        // Initialize components
        initializeComponents();
        initializeData();
        setupUI();
        loadData();
    }
    
    private void initializeComponents() {
        itemTitleText = findViewById(R.id.item_title_text);
        currentBidText = findViewById(R.id.current_bid_text);
        timeRemainingText = findViewById(R.id.time_remaining_text);
        bidAmountText = findViewById(R.id.bid_amount_text);
        bidderAliasText = findViewById(R.id.bidder_alias_text);
        creditBalanceText = findViewById(R.id.credit_balance_text);
        remainingBalanceText = findViewById(R.id.remaining_balance_text);
        cancelButton = findViewById(R.id.cancel_button);
        confirmBidButton = findViewById(R.id.confirm_bid_button);
        
        // Initialize managers
        biddingEngine = BiddingEngine.getInstance(this);
        itemManager = ItemManager.getInstance(this);
        creditManager = new CreditManager(this);
        
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
        bidAmount = intent.getDoubleExtra("bid_amount", 0.0);
        
        if (itemId == null || userId == null || userAlias == null || bidAmount <= 0) {
            Toast.makeText(this, "Missing required data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }
    
    private void setupUI() {
        // Set up cancel button
        cancelButton.setOnClickListener(v -> finish());
        
        // Set up confirm bid button
        confirmBidButton.setOnClickListener(v -> confirmBid());
    }
    
    private void loadData() {
        // Get item data
        item = itemManager.getItemById(itemId);
        if (item == null) {
            Toast.makeText(this, "Item not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Update UI with data
        updateItemDisplay();
        updateBidDisplay();
        updateCreditDisplay();
    }
    
    private void updateItemDisplay() {
        if (item == null) return;
        
        // Set item title
        itemTitleText.setText(item.getTitle());
        
        // Set current bid
        currentBidText.setText(currencyFormat.format(item.getCurrentPrice()));
        
        // Set time remaining
        if (item.getEndDate() != null) {
            long timeRemaining = item.getTimeRemaining();
            if (timeRemaining > 0) {
                int days = (int) (timeRemaining / (24 * 60 * 60 * 1000));
                int hours = (int) ((timeRemaining % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000));
                int minutes = (int) ((timeRemaining % (60 * 60 * 1000)) / (60 * 1000));
                
                if (days > 0) {
                    timeRemainingText.setText(String.format("%d days, %d hours", days, hours));
                } else if (hours > 0) {
                    timeRemainingText.setText(String.format("%d hours, %d minutes", hours, minutes));
                } else {
                    timeRemainingText.setText(String.format("%d minutes", minutes));
                }
            } else {
                timeRemainingText.setText("Auction ended");
                confirmBidButton.setEnabled(false);
            }
        } else {
            timeRemainingText.setText("No deadline set");
        }
    }
    
    private void updateBidDisplay() {
        // Set bid amount
        bidAmountText.setText(currencyFormat.format(bidAmount));
        
        // Set bidder alias
        bidderAliasText.setText(userAlias);
    }
    
    private void updateCreditDisplay() {
        // Get current credit balance
        double currentBalance = creditManager.getCreditBalance(userId);
        creditBalanceText.setText(currencyFormat.format(currentBalance));
        
        // Calculate remaining balance after bid
        double remainingBalance = currentBalance - bidAmount;
        remainingBalanceText.setText(currencyFormat.format(remainingBalance));
        
        // Update button state based on credit availability
        if (remainingBalance < 0) {
            confirmBidButton.setEnabled(false);
            confirmBidButton.setText("Insufficient Credits");
            remainingBalanceText.setTextColor(getResources().getColor(R.color.error_red));
        } else {
            confirmBidButton.setEnabled(true);
            confirmBidButton.setText("Confirm Bid");
            remainingBalanceText.setTextColor(getResources().getColor(R.color.success_green));
        }
    }
    
    private void confirmBid() {
        // Validate bid one more time
        if (!validateBid()) {
            return;
        }
        
        // Disable button to prevent double submission
        confirmBidButton.setEnabled(false);
        confirmBidButton.setText("Placing Bid...");
        
        // Place bid
        BidResult result = biddingEngine.placeBid(itemId, userId, userAlias, bidAmount);
        
        if (result.isSuccess()) {
            // Show success message
            Toast.makeText(this, "Bid placed successfully!", Toast.LENGTH_LONG).show();
            
            // Return to item detail with success result
            Intent resultIntent = new Intent();
            resultIntent.putExtra("bid_success", true);
            resultIntent.putExtra("bid_amount", bidAmount);
            setResult(RESULT_OK, resultIntent);
            finish();
            
        } else {
            // Show error message
            Toast.makeText(this, "Failed to place bid: " + result.getMessage(), Toast.LENGTH_LONG).show();
            
            // Re-enable button
            confirmBidButton.setEnabled(true);
            confirmBidButton.setText("Confirm Bid");
        }
    }
    
    private boolean validateBid() {
        // Check if auction is still active
        if (item == null || !item.isAvailableForBidding()) {
            Toast.makeText(this, "Auction is no longer available for bidding", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        // Check if bid amount is valid
        if (bidAmount <= item.getCurrentPrice()) {
            Toast.makeText(this, "Bid must be higher than current bid", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        // Check if user has sufficient credits
        double currentBalance = creditManager.getCreditBalance(userId);
        if (currentBalance < bidAmount) {
            Toast.makeText(this, "Insufficient credits to place bid", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        // Check if user is not the seller
        if (userId.equals(item.getSellerId())) {
            Toast.makeText(this, "Sellers cannot bid on their own items", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        return true;
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to activity
        loadData();
    }
}
