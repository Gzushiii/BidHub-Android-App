package com.cc106.bidhub;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.cc106.bidhub.adapters.BidHistoryAdapter;
import com.cc106.bidhub.bidding.Bid;
import com.cc106.bidhub.credits.CreditManager;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

public class ItemDetailActivity extends AppCompatActivity {

    private ImageView ivItemImage, ivSellerAvatar;
    private TextView tvItemTitle, tvItemCategory, tvStartingBid, tvCurrentBid, tvTimeLeft, tvDescription, tvSellerName, tvSellerRating;
    private EditText etBidAmount;
    private Button btnPlaceBid, btnBuyNow, btnBack, btnShare, btnFavorite;
    private ProgressBar progressTimeLeft;
    private RecyclerView rvBidHistory;
    private BidHistoryAdapter bidHistoryAdapter;
    private CreditManager creditManager;
    private NumberFormat currencyFormat;
    private Dialog bidConfirmationDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);
        
        initializeViews();
        initializeComponents();
        setupClickListeners();
        populateItemData();
        setupBidHistory();
    }

    private void initializeViews() {
        ivItemImage = findViewById(R.id.iv_item_image);
        ivSellerAvatar = findViewById(R.id.iv_seller_avatar);
        tvItemTitle = findViewById(R.id.tv_item_title);
        tvItemCategory = findViewById(R.id.tv_item_category);
        tvStartingBid = findViewById(R.id.tv_starting_bid);
        tvCurrentBid = findViewById(R.id.tv_current_bid);
        tvTimeLeft = findViewById(R.id.tv_time_left);
        tvDescription = findViewById(R.id.tv_description);
        tvSellerName = findViewById(R.id.tv_seller_name);
        tvSellerRating = findViewById(R.id.tv_seller_rating);
        etBidAmount = findViewById(R.id.et_bid_amount);
        btnPlaceBid = findViewById(R.id.btn_place_bid);
        btnBuyNow = findViewById(R.id.btn_buy_now);
        btnBack = findViewById(R.id.btn_back);
        btnShare = findViewById(R.id.btn_share);
        btnFavorite = findViewById(R.id.btn_favorite);
        progressTimeLeft = findViewById(R.id.progress_time_left);
        rvBidHistory = findViewById(R.id.rv_bid_history);
    }

    private void initializeComponents() {
        creditManager = new CreditManager(this);
        currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
        currencyFormat.setCurrency(Currency.getInstance("USD"));
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Implement share functionality
                android.widget.Toast.makeText(ItemDetailActivity.this, "Share functionality coming soon!", android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        btnFavorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Implement favorite functionality
                android.widget.Toast.makeText(ItemDetailActivity.this, "Added to favorites!", android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        btnPlaceBid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show bid confirmation dialog
                showBidConfirmationDialog();
            }
        });

        btnBuyNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to buy now flow
                Intent intent = new Intent(ItemDetailActivity.this, PaymentConfirmationActivity.class);
                startActivity(intent);
            }
        });
    }

    private void populateItemData() {
        // Get data from intent
        Intent intent = getIntent();
        String itemId = intent.getStringExtra("ITEM_ID");
        String userEmail = intent.getStringExtra("USER_EMAIL");
        
        if (itemId != null) {
            // TODO: Load actual item data from database using itemId
            // For now, use sample data but with proper item identification
            tvItemTitle.setText("Luxury Watch (ID: " + itemId + ")");
            tvItemCategory.setText("Watches · New");
            tvStartingBid.setText("$500");
            tvCurrentBid.setText("$750");
            tvTimeLeft.setText("2d 12h 30m");
            tvDescription.setText("This luxury watch features a stainless steel case, sapphire crystal, and automatic movement. It's water-resistant up to 100 meters and comes with a certificate of authenticity.");
            tvSellerName.setText("Ethan Carter");
            tvSellerRating.setText("4.8 (125 reviews)");
        } else {
            // Fallback to sample data if no item ID provided
            tvItemTitle.setText("Luxury Watch");
            tvItemCategory.setText("Watches · New");
            tvStartingBid.setText("$500");
            tvCurrentBid.setText("$750");
            tvTimeLeft.setText("2d 12h 30m");
            tvDescription.setText("This luxury watch features a stainless steel case, sapphire crystal, and automatic movement. It's water-resistant up to 100 meters and comes with a certificate of authenticity.");
            tvSellerName.setText("Ethan Carter");
            tvSellerRating.setText("4.8 (125 reviews)");
        }
        
        // Set progress to 50% (example)
        progressTimeLeft.setProgress(50);
    }

    private void setupBidHistory() {
        // Create sample bid history data with realistic timestamps
        List<Bid> bidHistory = new ArrayList<>();
        
        // Create bids with different timestamps
        java.util.Date now = new java.util.Date();
        java.util.Calendar cal = java.util.Calendar.getInstance();
        
        // Most recent bid (5 minutes ago)
        cal.setTime(now);
        cal.add(java.util.Calendar.MINUTE, -5);
        Bid bid1 = new Bid("item1", "user1", "Sophia Bennett", 750.0);
        bid1.setPlacedAt(cal.getTime());
        
        // Second bid (10 minutes ago)
        cal.setTime(now);
        cal.add(java.util.Calendar.MINUTE, -10);
        Bid bid2 = new Bid("item1", "user2", "Liam Harper", 700.0);
        bid2.setPlacedAt(cal.getTime());
        
        // Third bid (15 minutes ago)
        cal.setTime(now);
        cal.add(java.util.Calendar.MINUTE, -15);
        Bid bid3 = new Bid("item1", "user3", "Emma Wilson", 650.0);
        bid3.setPlacedAt(cal.getTime());
        
        bidHistory.add(bid1);
        bidHistory.add(bid2);
        bidHistory.add(bid3);
        
        bidHistoryAdapter = new BidHistoryAdapter(bidHistory, bid -> {
            // Handle bid item click
            android.widget.Toast.makeText(ItemDetailActivity.this, 
                "Bid by " + bid.getBidderAlias() + " for " + currencyFormat.format(bid.getAmount()), 
                android.widget.Toast.LENGTH_SHORT).show();
        });
        rvBidHistory.setLayoutManager(new LinearLayoutManager(this));
        rvBidHistory.setAdapter(bidHistoryAdapter);
    }

    private void showBidConfirmationDialog() {
        String bidAmountText = etBidAmount.getText().toString().trim();
        if (bidAmountText.isEmpty()) {
            etBidAmount.setError("Please enter a bid amount");
            return;
        }

        try {
            double bidAmount = Double.parseDouble(bidAmountText);
            if (bidAmount <= 0) {
                etBidAmount.setError("Bid amount must be greater than 0");
                return;
            }

            // Create and show bid confirmation dialog
            createBidConfirmationDialog(bidAmount);
        } catch (NumberFormatException e) {
            etBidAmount.setError("Please enter a valid number");
        }
    }

    private void createBidConfirmationDialog(double bidAmount) {
        bidConfirmationDialog = new Dialog(this);
        bidConfirmationDialog.setContentView(R.layout.dialog_bid_confirmation);
        bidConfirmationDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // Get dialog views
        TextView tvDialogItemTitle = bidConfirmationDialog.findViewById(R.id.tv_dialog_item_title);
        TextView tvDialogBidAmount = bidConfirmationDialog.findViewById(R.id.tv_dialog_bid_amount);
        TextView tvDialogCreditCost = bidConfirmationDialog.findViewById(R.id.tv_dialog_credit_cost);
        TextView tvDialogRemainingBalance = bidConfirmationDialog.findViewById(R.id.tv_dialog_remaining_balance);
        Button btnCancel = bidConfirmationDialog.findViewById(R.id.btn_dialog_cancel);
        Button btnConfirm = bidConfirmationDialog.findViewById(R.id.btn_dialog_confirm);

        // Set dialog data
        tvDialogItemTitle.setText(tvItemTitle.getText());
        tvDialogBidAmount.setText(currencyFormat.format(bidAmount));
        
        // Calculate credit cost (assuming 1 credit = $1 for simplicity)
        int creditCost = (int) bidAmount;
        tvDialogCreditCost.setText(creditCost + " Credits");
        
        // Calculate remaining balance (assuming user has 100 credits)
        int remainingBalance = 100 - creditCost;
        tvDialogRemainingBalance.setText(remainingBalance + " Credits");

        // Set click listeners
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bidConfirmationDialog.dismiss();
            }
        });

        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Process the bid
                processBid(bidAmount);
                bidConfirmationDialog.dismiss();
            }
        });

        bidConfirmationDialog.show();
    }

    private void processBid(double bidAmount) {
        // TODO: Implement actual bid processing logic
        android.widget.Toast.makeText(this, "Bid of " + currencyFormat.format(bidAmount) + " placed successfully!", android.widget.Toast.LENGTH_LONG).show();
        
        // Update current bid display
        tvCurrentBid.setText(currencyFormat.format(bidAmount));
        
        // Clear bid input
        etBidAmount.setText("");
    }
}