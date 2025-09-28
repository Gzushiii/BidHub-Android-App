package com.cc106.bidhub;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.cc106.bidhub.credits.CreditManager;
import com.cc106.bidhub.items.Item;
import com.cc106.bidhub.items.ItemManager;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

public class ItemDetailActivity extends AppCompatActivity {

    private ImageView ivItemImage, ivSellerAvatar;
    private TextView tvItemTitle, tvItemCategory, tvStartingBid, tvCurrentBid, tvTimeLeft, tvDescription, tvSellerName, tvSellerRating;
    private EditText etBidAmount;
    private Button btnPlaceBid, btnBuyNow;
    private ImageButton btnBack, btnShare, btnFavorite;
    private ProgressBar progressTimeLeft;
    private LinearLayout layoutBidHistory;
    private CreditManager creditManager;
    private NumberFormat currencyFormat;
    private Dialog bidConfirmationDialog;
    
    // New fields for enhanced functionality
    private LinearLayout layoutImageIndicators;
    private List<View> imageIndicators;
    private Item currentItem;
    private ItemManager itemManager;
    private String loggedInUserEmail;
    private int currentImageIndex = 0;
    private List<String> itemImages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_item_detail);
            
            // Get item ID and user email from intent
            String itemId = getIntent().getStringExtra("ITEM_ID");
            loggedInUserEmail = getIntent().getStringExtra("USER_EMAIL");
            
            // Initialize views with better error handling
            try {
                initializeViews();
            } catch (Exception e) {
                android.widget.Toast.makeText(this, "Error initializing views: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
                e.printStackTrace();
                finish();
                return;
            }
            
            try {
                initializeComponents();
            } catch (Exception e) {
                android.widget.Toast.makeText(this, "Error initializing components: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
                e.printStackTrace();
                finish();
                return;
            }
            
            try {
                setupClickListeners();
            } catch (Exception e) {
                android.widget.Toast.makeText(this, "Error setting up click listeners: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
                e.printStackTrace();
                finish();
                return;
            }
            
            try {
                loadItemData(itemId);
            } catch (Exception e) {
                android.widget.Toast.makeText(this, "Error loading item data: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
                e.printStackTrace();
                finish();
                return;
            }
            
            try {
                setupBidHistory();
            } catch (Exception e) {
                android.widget.Toast.makeText(this, "Error setting up bid history: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
                e.printStackTrace();
                finish();
                return;
            }
            
            try {
                setupImageIndicators();
            } catch (Exception e) {
                android.widget.Toast.makeText(this, "Error setting up image indicators: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
                e.printStackTrace();
                finish();
                return;
            }
            
        } catch (Exception e) {
            android.widget.Toast.makeText(this, "Error initializing item details: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
            e.printStackTrace();
            finish();
        }
    }

    private void initializeViews() {
        try {
            android.util.Log.d("ItemDetailActivity", "Starting view initialization...");
            
            ivItemImage = findViewById(R.id.iv_item_image);
            android.util.Log.d("ItemDetailActivity", "ivItemImage initialized");
            
            ivSellerAvatar = findViewById(R.id.iv_seller_avatar);
            android.util.Log.d("ItemDetailActivity", "ivSellerAvatar initialized");
            
            tvItemTitle = findViewById(R.id.tv_item_title);
            android.util.Log.d("ItemDetailActivity", "tvItemTitle initialized");
            
            tvItemCategory = findViewById(R.id.tv_item_category);
            android.util.Log.d("ItemDetailActivity", "tvItemCategory initialized");
            
            tvStartingBid = findViewById(R.id.tv_starting_bid);
            android.util.Log.d("ItemDetailActivity", "tvStartingBid initialized");
            
            tvCurrentBid = findViewById(R.id.tv_current_bid);
            android.util.Log.d("ItemDetailActivity", "tvCurrentBid initialized");
            
            tvTimeLeft = findViewById(R.id.tv_time_left);
            android.util.Log.d("ItemDetailActivity", "tvTimeLeft initialized");
            
            tvDescription = findViewById(R.id.tv_description);
            android.util.Log.d("ItemDetailActivity", "tvDescription initialized");
            
            tvSellerName = findViewById(R.id.tv_seller_name);
            android.util.Log.d("ItemDetailActivity", "tvSellerName initialized");
            
            tvSellerRating = findViewById(R.id.tv_seller_rating);
            android.util.Log.d("ItemDetailActivity", "tvSellerRating initialized");
            
            etBidAmount = findViewById(R.id.et_bid_amount);
            android.util.Log.d("ItemDetailActivity", "etBidAmount initialized");
            
            btnPlaceBid = findViewById(R.id.btn_place_bid);
            android.util.Log.d("ItemDetailActivity", "btnPlaceBid initialized");
            
            btnBuyNow = findViewById(R.id.btn_buy_now);
            android.util.Log.d("ItemDetailActivity", "btnBuyNow initialized");
            
            btnBack = findViewById(R.id.btn_back);
            android.util.Log.d("ItemDetailActivity", "btnBack initialized");
            
            btnShare = findViewById(R.id.btn_share);
            android.util.Log.d("ItemDetailActivity", "btnShare initialized");
            
            btnFavorite = findViewById(R.id.btn_favorite);
            android.util.Log.d("ItemDetailActivity", "btnFavorite initialized");
            
            progressTimeLeft = findViewById(R.id.progress_time_left);
            android.util.Log.d("ItemDetailActivity", "progressTimeLeft initialized");
            
            layoutBidHistory = findViewById(R.id.layout_bid_history);
            android.util.Log.d("ItemDetailActivity", "layoutBidHistory initialized");
            
            // Initialize new views
            layoutImageIndicators = findViewById(R.id.layout_image_indicators);
            android.util.Log.d("ItemDetailActivity", "layoutImageIndicators initialized");
            
            imageIndicators = new ArrayList<>();
            android.util.Log.d("ItemDetailActivity", "imageIndicators list created");
            
            // Validate that all required views were found
            if (tvItemTitle == null || btnBack == null) {
                throw new RuntimeException("Required views not found in layout");
            }
            
            android.util.Log.d("ItemDetailActivity", "All views initialized successfully");
            
        } catch (Exception e) {
            android.util.Log.e("ItemDetailActivity", "Error initializing views: " + e.getMessage(), e);
            android.widget.Toast.makeText(this, "Error initializing views: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
            e.printStackTrace();
            throw e;
        }
    }

    private void initializeComponents() {
        creditManager = new CreditManager(this);
        // ItemManager will be initialized when needed
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
                shareItem();
            }
        });
        
        btnFavorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFavorite();
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
        if (layoutBidHistory == null) {
            return;
        }
        
        // Clear any existing views
        layoutBidHistory.removeAllViews();
        
        // Create sample bid history data
        String[] bidderNames = {"Sophia Bennett", "Liam Harper", "Emma Wilson"};
        double[] bidAmounts = {750.0, 700.0, 650.0};
        String[] bidTimes = {"10:30 AM", "10:25 AM", "10:20 AM"};
        
        for (int i = 0; i < bidderNames.length; i++) {
            // Create bid item layout
            LinearLayout bidItemLayout = new LinearLayout(this);
            bidItemLayout.setOrientation(LinearLayout.HORIZONTAL);
            bidItemLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            bidItemLayout.setPadding(0, 16, 0, 16);
            
            // Left side - bidder info
            LinearLayout leftLayout = new LinearLayout(this);
            leftLayout.setOrientation(LinearLayout.VERTICAL);
            leftLayout.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
            ));
            
            TextView bidderName = new TextView(this);
            bidderName.setText(bidderNames[i]);
            bidderName.setTextSize(16);
            bidderName.setTextColor(getResources().getColor(R.color.text_primary));
            bidderName.setTypeface(null, android.graphics.Typeface.BOLD);
            
            TextView bidTime = new TextView(this);
            bidTime.setText(bidTimes[i]);
            bidTime.setTextSize(14);
            bidTime.setTextColor(getResources().getColor(R.color.text_secondary));
            
            leftLayout.addView(bidderName);
            leftLayout.addView(bidTime);
            
            // Right side - bid amount
            TextView bidAmount = new TextView(this);
            bidAmount.setText(currencyFormat.format(bidAmounts[i]));
            bidAmount.setTextSize(16);
            bidAmount.setTextColor(getResources().getColor(R.color.primary));
            bidAmount.setTypeface(null, android.graphics.Typeface.BOLD);
            
            bidItemLayout.addView(leftLayout);
            bidItemLayout.addView(bidAmount);
            
            layoutBidHistory.addView(bidItemLayout);
        }
    }
    
    private void setupImageIndicators() {
        if (layoutImageIndicators != null) {
            // Get all indicator views
            for (int i = 0; i < layoutImageIndicators.getChildCount(); i++) {
                View indicator = layoutImageIndicators.getChildAt(i);
                imageIndicators.add(indicator);
            }
            
            // Set up image carousel functionality
            ivItemImage.setOnClickListener(v -> {
                if (itemImages != null && itemImages.size() > 1) {
                    currentImageIndex = (currentImageIndex + 1) % itemImages.size();
                    updateImageDisplay();
                    // Show toast to indicate image change
                    android.widget.Toast.makeText(ItemDetailActivity.this, 
                        "Image " + (currentImageIndex + 1) + " of " + itemImages.size(), 
                        android.widget.Toast.LENGTH_SHORT).show();
                } else if (itemImages != null && itemImages.size() == 1) {
                    // Only one image, no need to cycle
                    currentImageIndex = 0;
                    updateImageDisplay();
                } else {
                    // No images available - show placeholder
                    android.widget.Toast.makeText(ItemDetailActivity.this, 
                        "No images uploaded for this item", 
                        android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    
    private void updateImageDisplay() {
        if (itemImages != null && !itemImages.isEmpty() && currentImageIndex < itemImages.size()) {
            // Update image (in a real app, you'd load the image from the path)
            // For now, we'll just update the indicators
            updateImageIndicators();
        } else if (itemImages != null && !itemImages.isEmpty() && currentImageIndex >= itemImages.size()) {
            // Reset currentImageIndex if it's out of bounds
            currentImageIndex = 0;
            updateImageIndicators();
        } else {
            // No images available, use placeholder
            if (ivItemImage != null) {
                ivItemImage.setImageResource(R.drawable.placeholder);
            }
            // Hide image indicators when no images
            if (layoutImageIndicators != null) {
                layoutImageIndicators.setVisibility(View.GONE);
            }
        }
    }
    
    private void updateImageIndicators() {
        if (imageIndicators == null || imageIndicators.isEmpty()) {
            return;
        }
        
        for (int i = 0; i < imageIndicators.size(); i++) {
            if (i < imageIndicators.size()) {
                View indicator = imageIndicators.get(i);
                if (indicator != null) {
                    if (i == currentImageIndex) {
                        indicator.setBackgroundResource(R.drawable.indicator_active);
                    } else {
                        indicator.setBackgroundResource(R.drawable.indicator_inactive);
                    }
                }
            }
        }
    }

    private void loadItemData(String itemId) {
        if (itemId != null) {
            // TODO: Load item data from database when ItemManager is properly accessible
            // For now, use sample data
            populateItemData();
            itemImages = new ArrayList<>();
            
            // TODO: Load actual images from item.getImagePaths() when available
            // For now, simulate different scenarios:
            // - Some items have images, others don't
            if (itemId.contains("no_image") || itemId.contains("empty")) {
                // Simulate item with no images - use placeholder
                itemImages.clear();
            } else {
                // Simulate item with images for carousel testing
                itemImages.add("sample_watch_1");
                itemImages.add("sample_watch_2");
                itemImages.add("sample_watch_3");
                itemImages.add("sample_watch_4");
            }
            
            // Ensure currentImageIndex is within bounds
            currentImageIndex = 0;
            updateImageDisplay();
        } else {
            // Use sample data if no item ID provided
            populateItemData();
            itemImages = new ArrayList<>();
            // Add multiple sample images for carousel testing
            itemImages.add("sample_watch_1");
            itemImages.add("sample_watch_2");
            itemImages.add("sample_watch_3");
            itemImages.add("sample_watch_4");
            // Ensure currentImageIndex is within bounds
            currentImageIndex = 0;
            updateImageDisplay();
        }
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
        TextView tvDialogItemName = bidConfirmationDialog.findViewById(R.id.tv_dialog_item_name);
        TextView tvDialogBidAmount = bidConfirmationDialog.findViewById(R.id.tv_dialog_bid_amount);
        TextView tvDialogCreditCost = bidConfirmationDialog.findViewById(R.id.tv_dialog_credit_cost);
        TextView tvDialogRemainingBalance = bidConfirmationDialog.findViewById(R.id.tv_dialog_remaining_balance);
        Button btnCancel = bidConfirmationDialog.findViewById(R.id.btn_dialog_cancel);
        Button btnConfirm = bidConfirmationDialog.findViewById(R.id.btn_dialog_confirm);

        // Set dialog data
        tvDialogItemName.setText(tvItemTitle.getText());
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
    
    private void shareItem() {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            
            String itemTitle = tvItemTitle.getText().toString();
            String itemDescription = tvDescription.getText().toString();
            String currentBid = tvCurrentBid.getText().toString();
            
            String shareText = "Check out this item on BidHub:\n\n" +
                    itemTitle + "\n" +
                    itemDescription + "\n" +
                    "Current Bid: " + currentBid + "\n\n" +
                    "Download BidHub to place your bid!";
            
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "BidHub Item: " + itemTitle);
            
            startActivity(Intent.createChooser(shareIntent, "Share Item"));
        } catch (Exception e) {
            android.widget.Toast.makeText(this, "Error sharing item", android.widget.Toast.LENGTH_SHORT).show();
        }
    }
    
    private void toggleFavorite() {
        try {
            // TODO: Implement actual favorite functionality with database
            // For now, just show a toast message
            android.widget.Toast.makeText(this, "Added to favorites!", android.widget.Toast.LENGTH_SHORT).show();
            
            // Update favorite button icon (in a real app, you'd check the database)
            btnFavorite.setImageResource(R.drawable.ic_favorite);
        } catch (Exception e) {
            android.widget.Toast.makeText(this, "Error updating favorites", android.widget.Toast.LENGTH_SHORT).show();
        }
    }
}