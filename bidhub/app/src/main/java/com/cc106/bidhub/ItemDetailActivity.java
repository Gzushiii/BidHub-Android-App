package com.cc106.bidhub;

import android.content.Intent;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.List;

public class ItemDetailActivity extends AppCompatActivity {

    private ImageView ivItemImage, ivSellerAvatar;
    private TextView tvItemTitle, tvItemCategory, tvStartingBid, tvCurrentBid, tvTimeLeft, tvDescription, tvSellerName, tvSellerRating;
    private EditText etBidAmount;
    private Button btnPlaceBid, btnBuyNow;
    private ProgressBar progressTimeLeft;
    private RecyclerView rvBidHistory;
    private BidHistoryAdapter bidHistoryAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);
        
        initializeViews();
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
        progressTimeLeft = findViewById(R.id.progress_time_left);
        rvBidHistory = findViewById(R.id.rv_bid_history);
    }

    private void setupClickListeners() {
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
        // Set sample data - in real app, this would come from intent or API
        tvItemTitle.setText("Luxury Watch");
        tvItemCategory.setText("Watches · New");
        tvStartingBid.setText("$500");
        tvCurrentBid.setText("$750");
        tvTimeLeft.setText("2d 12h 30m");
        tvDescription.setText("This luxury watch features a stainless steel case, sapphire crystal, and automatic movement. It's water-resistant up to 100 meters and comes with a certificate of authenticity.");
        tvSellerName.setText("Ethan Carter");
        tvSellerRating.setText("4.8 (125 reviews)");
        
        // Set progress to 50% (example)
        progressTimeLeft.setProgress(50);
    }

    private void setupBidHistory() {
        // Create sample bid history data
        List<Bid> bidHistory = new ArrayList<>();
        // Create dummy bids for demonstration
        Bid bid1 = new Bid("item1", "user1", "Sophia Bennett", 750.0);
        Bid bid2 = new Bid("item1", "user2", "Liam Harper", 700.0);
        Bid bid3 = new Bid("item1", "user3", "Emma Wilson", 650.0);
        bidHistory.add(bid1);
        bidHistory.add(bid2);
        bidHistory.add(bid3);
        
        bidHistoryAdapter = new BidHistoryAdapter(bidHistory, bid -> {
            // Handle bid item click
            // For now, just show a toast
        });
        rvBidHistory.setLayoutManager(new LinearLayoutManager(this));
        rvBidHistory.setAdapter(bidHistoryAdapter);
    }

    private void showBidConfirmationDialog() {
        String bidAmount = etBidAmount.getText().toString().trim();
        if (bidAmount.isEmpty()) {
            etBidAmount.setError("Please enter a bid amount");
            return;
        }
        
        // Show confirmation dialog
        // TODO: Implement bid confirmation dialog
        // For now, just show a toast
        android.widget.Toast.makeText(this, "Bid confirmation dialog - Coming Soon!", android.widget.Toast.LENGTH_SHORT).show();
    }
}