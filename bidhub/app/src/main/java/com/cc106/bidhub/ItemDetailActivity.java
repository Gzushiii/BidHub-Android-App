package com.cc106.bidhub;

import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.viewpager2.widget.ViewPager2;
import com.cc106.bidhub.adapters.ImageGalleryAdapter;
import com.cc106.bidhub.credits.CreditManager;
import com.cc106.bidhub.items.Item;
import com.cc106.bidhub.items.ItemManager;
import com.cc106.bidhub.toast.ToastHelper;
import com.cc106.bidhub.utils.ImageLoader;
import com.cc106.bidhub.utils.SharedPreferencesHelper;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

public class ItemDetailActivity extends AppCompatActivity {

    private ImageView ivItemImage, ivSellerAvatar;
    private TextView tvItemTitle, tvItemCategory, tvStartingBid, tvCurrentBid, tvTimeLeft, tvDescription, tvSellerName, tvSellerRating, tvBidCount;
    private EditText etBidAmount;
    private Button btnPlaceBid, btnBuyNow;
    private ImageButton btnBack, btnShare, btnFavorite;
    private ProgressBar progressTimeLeft;
    private LinearLayout layoutBidHistory;
    private CreditManager creditManager;
    private SharedPreferencesHelper prefsHelper;
    private NumberFormat currencyFormat;
    private Dialog bidConfirmationDialog;
    
    // New fields for enhanced functionality
    private ViewPager2 viewPagerImages;
    private ImageGalleryAdapter imageGalleryAdapter;
    private LinearLayout layoutImageIndicators;
    private List<View> imageIndicators;
    private Item currentItem;
    private ItemManager itemManager;
    private String loggedInUserEmail;
    private int currentImageIndex = 0;
    private List<String> itemImages;
    
    // Real-time features
    private android.os.Handler countdownHandler;
    private java.lang.Runnable countdownRunnable;
    private static final int COUNTDOWN_UPDATE_INTERVAL = 1000; // 1 second
    private boolean isAuctionEndingSoon = false;
    private boolean isAuctionEnded = false;
    
    // UI Enhancement features
    private LinearLayout layoutBidIncrements;
    private Button btnIncrement10, btnIncrement50, btnIncrement100;
    private TextView tvWinningStatus;
    private LinearLayout layoutSellerInfo;
    private TextView tvSellerReviews;

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
                com.cc106.bidhub.utils.ErrorHandler.handleInitError(
                    this,
                    "initialize views",
                    e,
                    String.format("ItemID: %s, UserEmail: %s", 
                        getIntent().getStringExtra("ITEM_ID"),
                        getIntent().getStringExtra("USER_EMAIL"))
                );
                finish();
                return;
            }
            
            try {
                initializeComponents();
            } catch (Exception e) {
                com.cc106.bidhub.utils.ErrorHandler.handleInitError(
                    this,
                    "initialize components",
                    e,
                    String.format("ItemID: %s, UserEmail: %s", 
                        getIntent().getStringExtra("ITEM_ID"),
                        getIntent().getStringExtra("USER_EMAIL"))
                );
                finish();
                return;
            }
            
            try {
                setupClickListeners();
            } catch (Exception e) {
                com.cc106.bidhub.utils.ErrorHandler.handleInitError(
                    this,
                    "setup click listeners",
                    e,
                    String.format("ItemID: %s, UserEmail: %s", 
                        getIntent().getStringExtra("ITEM_ID"),
                        getIntent().getStringExtra("USER_EMAIL"))
                );
                finish();
                return;
            }
            
            try {
                loadItemData(itemId);
            } catch (Exception e) {
                com.cc106.bidhub.utils.ErrorHandler.handleInitError(
                    this,
                    "load item data",
                    e,
                    String.format("ItemID: %s, UserEmail: %s", itemId, loggedInUserEmail)
                );
                finish();
                return;
            }
            
            try {
                setupBidHistory();
            } catch (Exception e) {
                com.cc106.bidhub.utils.ErrorHandler.handleInitError(
                    this,
                    "setup bid history",
                    e,
                    String.format("ItemID: %s, UserEmail: %s", itemId, loggedInUserEmail)
                );
                finish();
                return;
            }
            
            try {
                setupImageIndicators();
            } catch (Exception e) {
                com.cc106.bidhub.utils.ErrorHandler.handleInitError(
                    this,
                    "setup image indicators",
                    e,
                    String.format("ItemID: %s, UserEmail: %s", itemId, loggedInUserEmail)
                );
                finish();
                return;
            }
            
            // Setup back button handling
            setupBackButtonHandling();
            
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
            
            tvBidCount = findViewById(R.id.tv_bid_count);
            android.util.Log.d("ItemDetailActivity", "tvBidCount initialized");
            
            
            etBidAmount = findViewById(R.id.et_bid_amount);
            android.util.Log.d("ItemDetailActivity", "etBidAmount initialized");
            
            btnPlaceBid = findViewById(R.id.btn_place_bid);
            if (btnPlaceBid != null) {
                android.util.Log.d("ItemDetailActivity", "btnPlaceBid initialized successfully");
                // Ensure button is visible by default
                btnPlaceBid.setVisibility(View.VISIBLE);
                btnPlaceBid.setEnabled(true);
                android.util.Log.d("ItemDetailActivity", "btnPlaceBid visibility set to VISIBLE, enabled=true");
            } else {
                android.util.Log.e("ItemDetailActivity", "ERROR: btnPlaceBid is NULL after findViewById!");
            }
            
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
            
            // Initialize ViewPager2 for image gallery
            viewPagerImages = findViewById(R.id.viewpager_images);
            android.util.Log.d("ItemDetailActivity", "viewPagerImages initialized");
            
            // Initialize image gallery adapter
            imageGalleryAdapter = new ImageGalleryAdapter();
            if (viewPagerImages != null) {
                viewPagerImages.setAdapter(imageGalleryAdapter);
            }
            
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
        prefsHelper = new SharedPreferencesHelper(this);
        itemManager = ItemManager.getInstance(this);
        currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
        currencyFormat.setCurrency(Currency.getInstance("PHP"));
        
        // Initialize countdown handler
        countdownHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    }
    
    /**
     * Setup back button handling for consistent navigation
     */
    private void setupBackButtonHandling() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
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
                // Check if item has buy now price
                if (currentItem == null || currentItem.getBuyNowPrice() <= 0) {
                    ToastHelper.showError(ItemDetailActivity.this, "Buy Now option not available for this item");
                    return;
                }
                
                // Check if user is logged in
                String userId = getCurrentUserId();
                if (userId == null) {
                    ToastHelper.showError(ItemDetailActivity.this, "Please log in to purchase this item");
                    return;
                }
                
                // Check if user is trying to buy their own item
                if (userId.equals(currentItem.getSellerId())) {
                    ToastHelper.showError(ItemDetailActivity.this, "You cannot buy your own item");
                    return;
                }
                
                // Check if item is still available
                if (!currentItem.isAvailableForBidding()) {
                    ToastHelper.showError(ItemDetailActivity.this, "This item is no longer available for purchase");
                    return;
                }
                
                // Show buy now confirmation dialog
                showBuyNowConfirmationDialog();
            }
        });
    }

    private void populateItemDataFromDatabase() {
        if (currentItem == null) {
            populateItemData(); // Fallback to sample data
            return;
        }
        
        try {
            // Set basic item information
            tvItemTitle.setText(currentItem.getTitle());
            tvItemCategory.setText(getCategoryDisplayName(currentItem.getCategoryId()) + " · " + currentItem.getCondition());
            tvStartingBid.setText(currencyFormat.format(currentItem.getStartingPrice()));
            tvCurrentBid.setText(currencyFormat.format(currentItem.getCurrentPrice()));
            tvDescription.setText(currentItem.getDescription());
            
            // Display bid count
            if (tvBidCount != null) {
                int bidCount = currentItem.getBidCount();
                if (bidCount > 0) {
                    tvBidCount.setText(bidCount + (bidCount == 1 ? " bid" : " bids"));
                    tvBidCount.setVisibility(View.VISIBLE);
                } else {
                    tvBidCount.setText("No bids yet");
                    tvBidCount.setVisibility(View.VISIBLE);
                }
            }
            
            // Calculate and display time left
            String timeLeft = calculateTimeLeft(currentItem.getEndDate());
            tvTimeLeft.setText(timeLeft);
            
            // Set progress based on time remaining
            int progress = calculateAuctionProgress(currentItem.getEndDate());
            progressTimeLeft.setProgress(progress);
            
            // Load seller information
            loadSellerInformation(currentItem.getSellerId());
            
            // Check if user is the seller and hide bid button
            updateBidButton();
            
            // Update bid input with minimum bid amount
            // Minimum bid is either: current highest bid + 1, or starting price (whichever is higher)
            double currentHighestBid = currentItem.getCurrentPrice();
            double startingPrice = currentItem.getStartingPrice();
            double minBid = Math.max(currentHighestBid + 1.0, startingPrice);
            if (etBidAmount != null) {
                etBidAmount.setHint("Min: " + currencyFormat.format(minBid));
            }
            
            // Start countdown timer
            startCountdownTimer();
            
        } catch (Exception e) {
            android.util.Log.e("ItemDetailActivity", "Error populating item data: " + e.getMessage(), e);
            populateItemData(); // Fallback to sample data
        }
    }
    
    private void updateBidButton() {
        if (btnPlaceBid == null) {
            android.util.Log.e("ItemDetailActivity", "btnPlaceBid is null in updateBidButton()");
            return;
        }
        
        // Default: Show button and enable it
        btnPlaceBid.setVisibility(View.VISIBLE);
        btnPlaceBid.setEnabled(true);
        btnPlaceBid.setText(getString(R.string.place_bid));
        
        // Check if user is the seller
        // TEMPORARY: For testing, allow sellers to see the button (backend will reject the bid anyway)
        // Only hide button if we're absolutely certain the user is the seller AND we want to hide it
        boolean hideForSeller = false; // Set to true to hide button for sellers
        
        if (currentItem != null && loggedInUserEmail != null && hideForSeller) {
            String sellerId = currentItem.getSellerId();
            
            // Log for debugging
            android.util.Log.d("ItemDetailActivity", "Checking seller match - loggedInUserEmail: " + loggedInUserEmail + ", sellerId: " + sellerId);
            
            // Only hide if sellerId matches loggedInUserEmail exactly (case-insensitive)
            // This prevents false positives from partial matches
            if (sellerId != null && sellerId.trim().length() > 0 && 
                loggedInUserEmail.trim().equalsIgnoreCase(sellerId.trim())) {
                btnPlaceBid.setVisibility(View.GONE);
                // Show "Your Item" indicator instead
                if (tvSellerName != null) {
                    tvSellerName.setText("Your Listing");
                    tvSellerName.setTextColor(getResources().getColor(R.color.primary_color));
                }
                android.util.Log.d("ItemDetailActivity", "User is seller (exact match), hiding Place Bid button");
                return;
            } else {
                android.util.Log.d("ItemDetailActivity", "User is NOT seller - showing Place Bid button. Match check: " + 
                    (sellerId != null ? sellerId : "null") + " vs " + loggedInUserEmail);
            }
        } else {
            if (currentItem != null && loggedInUserEmail != null) {
                String sellerId = currentItem.getSellerId();
                android.util.Log.d("ItemDetailActivity", "Seller check disabled for testing - loggedInUserEmail: " + loggedInUserEmail + ", sellerId: " + sellerId + 
                    " (Button will be shown, backend will validate)");
            }
        }
        
        // Check if auction is active
        if (currentItem != null) {
            if (currentItem.isAvailableForBidding()) {
                btnPlaceBid.setVisibility(View.VISIBLE);
                btnPlaceBid.setEnabled(true);
                android.util.Log.d("ItemDetailActivity", "Auction is active, showing Place Bid button");
            } else {
                // Auction ended or not available
                btnPlaceBid.setVisibility(View.VISIBLE);
                btnPlaceBid.setEnabled(false);
                btnPlaceBid.setText("Auction Ended");
                btnPlaceBid.setBackgroundColor(getResources().getColor(R.color.state_disabled));
                android.util.Log.d("ItemDetailActivity", "Auction not available, disabling Place Bid button");
            }
        } else {
            // No item data yet, show button anyway (will be validated on click)
            btnPlaceBid.setVisibility(View.VISIBLE);
            btnPlaceBid.setEnabled(true);
            android.util.Log.d("ItemDetailActivity", "No item data, showing Place Bid button (will validate on click)");
        }
    }
    
    private void populateItemData() {
        // Get data from intent
        Intent intent = getIntent();
        String itemId = intent.getStringExtra("ITEM_ID");
        String userEmail = intent.getStringExtra("USER_EMAIL");
        
        if (itemId != null) {
            // Use sample data but with proper item identification
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
        
        try {
            if (currentItem != null) {
                // Load real bid history from BiddingEngine
                com.cc106.bidhub.bidding.BiddingEngine biddingEngine = com.cc106.bidhub.bidding.BiddingEngine.getInstance(this);
                List<com.cc106.bidhub.bidding.Bid> bids = biddingEngine.getItemBids(currentItem.getItemId());
                
                if (bids != null && !bids.isEmpty()) {
                    // Sort bids by amount (highest first)
                    Collections.sort(bids, (b1, b2) -> Double.compare(b2.getAmount(), b1.getAmount()));
                    
                    for (com.cc106.bidhub.bidding.Bid bid : bids) {
                        createBidHistoryItem(bid);
                    }
                } else {
                    // No bids yet - show empty state
                    createEmptyBidHistory();
                }
            } else {
                // Fallback to sample data
                createSampleBidHistory();
            }
        } catch (Exception e) {
            android.util.Log.e("ItemDetailActivity", "Error loading bid history: " + e.getMessage(), e);
            createSampleBidHistory();
        }
    }
    
    private void createBidHistoryItem(com.cc106.bidhub.bidding.Bid bid) {
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
        bidderName.setText(bid.getBidderAlias());
        bidderName.setTextSize(16);
        bidderName.setTextColor(getResources().getColor(R.color.text_primary));
        bidderName.setTypeface(null, android.graphics.Typeface.BOLD);
        
        TextView bidTime = new TextView(this);
        bidTime.setText(formatBidTime(bid.getPlacedAt()));
        bidTime.setTextSize(14);
        bidTime.setTextColor(getResources().getColor(R.color.text_secondary));
        
        leftLayout.addView(bidderName);
        leftLayout.addView(bidTime);
        
        // Right side - bid amount
        TextView bidAmount = new TextView(this);
        bidAmount.setText(currencyFormat.format(bid.getAmount()));
        bidAmount.setTextSize(16);
        bidAmount.setTextColor(getResources().getColor(R.color.primary));
        bidAmount.setTypeface(null, android.graphics.Typeface.BOLD);
        
        // Add status indicator if winning
        if (bid.isWinning()) {
            bidAmount.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            bidAmount.setText(bidAmount.getText() + " (Winning)");
        }
        
        bidItemLayout.addView(leftLayout);
        bidItemLayout.addView(bidAmount);
        
        layoutBidHistory.addView(bidItemLayout);
    }
    
    private void createEmptyBidHistory() {
        TextView emptyText = new TextView(this);
        emptyText.setText("No bids yet. Be the first to bid!");
        emptyText.setTextSize(16);
        emptyText.setTextColor(getResources().getColor(R.color.text_secondary));
        emptyText.setGravity(android.view.Gravity.CENTER);
        emptyText.setPadding(0, 32, 0, 32);
        
        layoutBidHistory.addView(emptyText);
    }
    
    private void createSampleBidHistory() {
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
    
    private String formatBidTime(java.util.Date createdAt) {
        if (createdAt == null) {
            return "Unknown time";
        }
        
        java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault());
        return timeFormat.format(createdAt);
    }
    
    private void setupImageIndicators() {
        if (layoutImageIndicators == null || itemImages == null) {
            return;
        }
        
        // Clear existing indicators
        layoutImageIndicators.removeAllViews();
        imageIndicators.clear();
        
        // Hide indicators if no images or only one image
        if (itemImages.isEmpty() || itemImages.size() <= 1) {
            layoutImageIndicators.setVisibility(View.GONE);
            return;
        }
        
        // Show indicators for multiple images
        layoutImageIndicators.setVisibility(View.VISIBLE);
        
        // Create indicator views dynamically
        for (int i = 0; i < itemImages.size(); i++) {
            View indicator = new View(this);
            int size = (int) (8 * getResources().getDisplayMetrics().density); // 8dp
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins((int) (4 * getResources().getDisplayMetrics().density), 0, 
                             (int) (4 * getResources().getDisplayMetrics().density), 0);
            indicator.setLayoutParams(params);
            indicator.setBackgroundResource(R.drawable.indicator_inactive);
            layoutImageIndicators.addView(indicator);
            imageIndicators.add(indicator);
        }
        
        // Update initial indicator state
        updateImageIndicators(0);
        
        // Connect ViewPager2 page change listener
        if (viewPagerImages != null) {
            viewPagerImages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    currentImageIndex = position;
                    updateImageIndicators(position);
                }
            });
        }
    }
    
    private void updateImageDisplay() {
        if (itemImages != null && !itemImages.isEmpty() && currentImageIndex < itemImages.size()) {
            // Load actual image from file path using Glide
            String imagePath = itemImages.get(currentImageIndex);
            com.cc106.bidhub.utils.ImageLoader.loadImageWithErrorCallback(
                this,
                imagePath,
                ivItemImage,
                new com.cc106.bidhub.utils.ImageLoader.ImageLoadErrorCallback() {
                    @Override
                    public void onError(String errorMessage) {
                        com.cc106.bidhub.utils.ErrorHandler.handleImageError(
                            ItemDetailActivity.this,
                            imagePath,
                            null,
                            "ItemDetailActivity image display"
                        );
                    }
                }
            );
            updateImageIndicators(currentImageIndex);
        } else if (itemImages != null && !itemImages.isEmpty() && currentImageIndex >= itemImages.size()) {
            // Reset currentImageIndex if it's out of bounds
            currentImageIndex = 0;
            updateImageDisplay(); // Recursive call to load first image
        } else {
            // No images available, use placeholder
            com.cc106.bidhub.utils.ImageLoader.loadPlaceholder(this, ivItemImage);
            // Hide image indicators when no images
            if (layoutImageIndicators != null) {
                layoutImageIndicators.setVisibility(View.GONE);
            }
        }
    }
    
    private void updateImageIndicators(int position) {
        if (imageIndicators == null || imageIndicators.isEmpty()) {
            return;
        }
        
        for (int i = 0; i < imageIndicators.size(); i++) {
            View indicator = imageIndicators.get(i);
            if (indicator != null) {
                if (i == position) {
                    indicator.setBackgroundResource(R.drawable.indicator_active);
                } else {
                    indicator.setBackgroundResource(R.drawable.indicator_inactive);
                }
            }
        }
    }

    private void loadItemData(String itemId) {
        android.util.Log.i("ItemDetailActivity", "loadItemData called with itemId: " + itemId);
        if (itemId != null) {
            try {
                // Load real item data from database
                android.util.Log.d("ItemDetailActivity", "Calling itemManager.getItemById(" + itemId + ")");
                currentItem = itemManager.getItemById(itemId);
                if (currentItem != null) {
                    android.util.Log.i("ItemDetailActivity", "Item found! Title: " + currentItem.getTitle());
                    android.util.Log.d("ItemDetailActivity", "Item images: " + (currentItem.getImagePaths() != null ? currentItem.getImagePaths().size() : 0));
                    populateItemDataFromDatabase();
                    loadItemImages();
                    com.cc106.bidhub.utils.ErrorHandler.logInfo(
                        "ItemDetailActivity", 
                        "Successfully loaded item data from database",
                        String.format("ItemID: %s, Title: %s", itemId, currentItem.getTitle())
                    );
                } else {
                    // Item not found in ItemManager, try fetching from API
                    android.util.Log.w("ItemDetailActivity", "Item NOT found in ItemManager for ID: " + itemId);
                    android.util.Log.i("ItemDetailActivity", "Attempting to fetch item from API...");
                    fetchItemFromApi(itemId);
                }
            } catch (Exception e) {
                com.cc106.bidhub.utils.ErrorHandler.handleDatabaseError(
                    this,
                    "load item data",
                    e,
                    String.format("ItemID: %s, UserEmail: %s", itemId, loggedInUserEmail)
                );
                android.widget.Toast.makeText(this, "Error loading item. Showing sample data.", android.widget.Toast.LENGTH_SHORT).show();
                populateItemData();
                loadSampleImages();
            }
        } else {
            // Use sample data if no item ID provided
            com.cc106.bidhub.utils.ErrorHandler.logWarning(
                "ItemDetailActivity", 
                "No item ID provided, showing sample data",
                "No context data available"
            );
            android.widget.Toast.makeText(this, "No item ID provided. Showing sample data.", android.widget.Toast.LENGTH_SHORT).show();
            populateItemData();
            loadSampleImages();
        }
    }
    
    /**
     * Fetch item from backend API when not found in ItemManager
     */
    private void fetchItemFromApi(String itemId) {
        // Run on background thread to avoid NetworkOnMainThreadException
        new Thread(() -> {
            try {
                com.cc106.bidhub.api.ItemApiClient apiClient = new com.cc106.bidhub.api.ItemApiClient(this);
                com.cc106.bidhub.api.ItemApiClient.ApiResponse response = apiClient.getItemById(itemId);
                
                if (response.isSuccess() && response.getData() != null) {
                    // Parse the item from response
                    Item item = parseItemFromApiResponse(response.getData());
                    if (item != null) {
                        // Store in ItemManager for future lookups
                        itemManager.storeItem(item);
                        android.util.Log.i("ItemDetailActivity", "Item fetched from API and stored in ItemManager: " + item.getTitle());
                        
                        // Update UI on main thread
                        runOnUiThread(() -> {
                            currentItem = item;
                            populateItemDataFromDatabase();
                            loadItemImages();
                            com.cc106.bidhub.utils.ErrorHandler.logInfo(
                                "ItemDetailActivity", 
                                "Successfully loaded item data from API",
                                String.format("ItemID: %s, Title: %s", itemId, item.getTitle())
                            );
                        });
                    } else {
                        android.util.Log.e("ItemDetailActivity", "Failed to parse item from API response");
                        runOnUiThread(() -> {
                            showItemNotFoundError(itemId);
                        });
                    }
                } else {
                    android.util.Log.w("ItemDetailActivity", "API returned error: " + response.getMessage());
                    runOnUiThread(() -> {
                        showItemNotFoundError(itemId);
                    });
                }
            } catch (Exception e) {
                android.util.Log.e("ItemDetailActivity", "Error fetching item from API", e);
                runOnUiThread(() -> {
                    showItemNotFoundError(itemId);
                });
            }
        }).start();
    }
    
    /**
     * Parse Item object from API response JSON
     */
    private Item parseItemFromApiResponse(String responseData) {
        try {
            org.json.JSONObject jsonResponse = new org.json.JSONObject(responseData);
            org.json.JSONObject itemJson = jsonResponse.getJSONObject("item");
            
            Item item = new Item();
            
            // Parse basic fields
            item.setItemId(itemJson.optString("id", itemJson.optString("uuid_id", "")));
            item.setTitle(itemJson.getString("title"));
            item.setDescription(itemJson.optString("description", ""));
            item.setStartingPrice(itemJson.optDouble("starting_bid", itemJson.optDouble("starting_price", 0.0)));
            item.setCurrentPrice(itemJson.optDouble("current_bid", itemJson.optDouble("current_price", 0.0)));
            item.setCategoryId(itemJson.optString("category_id", ""));
            item.setCategoryName(itemJson.optString("category_name", ""));
            item.setSellerId(itemJson.optString("seller_email", itemJson.optString("seller_id", "")));
            
            // Parse seller username
            String sellerUsername = itemJson.optString("seller_username", null);
            if (sellerUsername != null && !sellerUsername.isEmpty()) {
                item.setSellerName(sellerUsername);
            } else {
                // Fallback: extract from email
                String sellerEmail = itemJson.optString("seller_email", "");
                if (!sellerEmail.isEmpty() && sellerEmail.contains("@")) {
                    int atIndex = sellerEmail.indexOf('@');
                    if (atIndex > 0) {
                        item.setSellerName(sellerEmail.substring(0, atIndex));
                    } else {
                        item.setSellerName("Unknown Seller");
                    }
                } else {
                    item.setSellerName("Unknown Seller");
                }
            }
            
            // Parse bid count
            int bidCount = itemJson.optInt("bid_count", 0);
            item.setBidCount(bidCount);
            
            // Parse end date
            if (itemJson.has("end_date") && !itemJson.isNull("end_date")) {
                try {
                    String endDateStr = itemJson.getString("end_date");
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault());
                    sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                    item.setEndDate(sdf.parse(endDateStr));
                } catch (Exception e) {
                    android.util.Log.w("ItemDetailActivity", "Error parsing end_date: " + e.getMessage());
                }
            } else if (itemJson.has("bid_deadline") && !itemJson.isNull("bid_deadline")) {
                try {
                    String deadlineStr = itemJson.getString("bid_deadline");
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault());
                    sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                    item.setEndDate(sdf.parse(deadlineStr));
                } catch (Exception e) {
                    android.util.Log.w("ItemDetailActivity", "Error parsing bid_deadline: " + e.getMessage());
                }
            }
            
            item.setCondition(itemJson.optString("item_condition", itemJson.optString("condition", "good")));
            
            // Parse status
            String status = itemJson.optString("status", "active");
            switch (status) {
                case "active":
                    item.setStatus(com.cc106.bidhub.items.ItemStatus.ACTIVE);
                    break;
                case "ended":
                case "sold":
                    item.setStatus(com.cc106.bidhub.items.ItemStatus.ENDED);
                    break;
                case "cancelled":
                    item.setStatus(com.cc106.bidhub.items.ItemStatus.CANCELLED);
                    break;
                default:
                    item.setStatus(com.cc106.bidhub.items.ItemStatus.DRAFT);
                    break;
            }
            
            // Parse images if available
            if (itemJson.has("images")) {
                try {
                    Object imagesObj = itemJson.get("images");
                    List<String> imagePaths = new ArrayList<>();
                    
                    if (imagesObj instanceof org.json.JSONArray) {
                        // Images is a JSON array
                        org.json.JSONArray imagesArray = (org.json.JSONArray) imagesObj;
                        for (int j = 0; j < imagesArray.length(); j++) {
                            Object imageObj = imagesArray.get(j);
                            if (imageObj instanceof String) {
                                // Direct URL string
                                String url = (String) imageObj;
                                if (url != null && !url.isEmpty() && !url.equals("null")) {
                                    imagePaths.add(url);
                                }
                            } else if (imageObj instanceof org.json.JSONObject) {
                                // Image object with image_url field
                                org.json.JSONObject imageJson = (org.json.JSONObject) imageObj;
                                String imageUrl = imageJson.optString("image_url", imageJson.optString("url", ""));
                                if (!imageUrl.isEmpty() && !imageUrl.equals("null")) {
                                    imagePaths.add(imageUrl);
                                }
                            }
                        }
                    } else if (imagesObj instanceof String) {
                        // Images is a JSON string, parse it
                        String imagesString = (String) imagesObj;
                        if (!imagesString.isEmpty() && !imagesString.equals("null")) {
                            try {
                                org.json.JSONArray imagesArray = new org.json.JSONArray(imagesString);
                                for (int j = 0; j < imagesArray.length(); j++) {
                                    Object imageObj = imagesArray.get(j);
                                    if (imageObj instanceof String) {
                                        imagePaths.add((String) imageObj);
                                    } else if (imageObj instanceof org.json.JSONObject) {
                                        org.json.JSONObject imageJson = (org.json.JSONObject) imageObj;
                                        String imageUrl = imageJson.optString("image_url", imageJson.optString("url", ""));
                                        if (!imageUrl.isEmpty()) {
                                            imagePaths.add(imageUrl);
                                        }
                                    }
                                }
                            } catch (org.json.JSONException e) {
                                android.util.Log.w("ItemDetailActivity", "Error parsing images string: " + imagesString, e);
                            }
                        }
                    }
                    
                    if (!imagePaths.isEmpty()) {
                        item.setImagePaths(imagePaths);
                        android.util.Log.d("ItemDetailActivity", "Parsed " + imagePaths.size() + " images for item: " + item.getTitle());
                    } else {
                        android.util.Log.d("ItemDetailActivity", "No images found for item: " + item.getTitle());
                        item.setImagePaths(new ArrayList<>());
                    }
                } catch (Exception e) {
                    android.util.Log.w("ItemDetailActivity", "Error parsing images for item", e);
                    item.setImagePaths(new ArrayList<>());
                }
            }
            
            return item;
        } catch (Exception e) {
            android.util.Log.e("ItemDetailActivity", "Error parsing item from API response", e);
            return null;
        }
    }
    
    /**
     * Show error when item is not found
     */
    private void showItemNotFoundError(String itemId) {
        com.cc106.bidhub.utils.ErrorHandler.logWarning(
            "ItemDetailActivity", 
            "Item not found in database or API, showing sample data",
            String.format("ItemID: %s", itemId)
        );
        android.widget.Toast.makeText(this, "Item not found. Showing sample data.", android.widget.Toast.LENGTH_SHORT).show();
        populateItemData();
        loadSampleImages();
    }
    
    private void loadItemImages() {
        try {
            if (currentItem != null && currentItem.getImagePaths() != null) {
                itemImages = new ArrayList<>(currentItem.getImagePaths());
                com.cc106.bidhub.utils.ErrorHandler.logInfo(
                    "ItemDetailActivity", 
                    "Loaded item images from database",
                    String.format("ItemID: %s, ImageCount: %d", 
                        currentItem.getItemId(), 
                        itemImages.size())
                );
            } else {
                itemImages = new ArrayList<>();
                com.cc106.bidhub.utils.ErrorHandler.logWarning(
                    "ItemDetailActivity", 
                    "No images found for item",
                    String.format("ItemID: %s, CurrentItem: %s", 
                        currentItem != null ? currentItem.getItemId() : "null",
                        currentItem != null ? "not null" : "null")
                );
            }
            
            // Load images into ViewPager2 adapter
            if (imageGalleryAdapter != null) {
                imageGalleryAdapter.setImages(itemImages);
            }
            
            // Setup image indicators
            setupImageIndicators();
            
            currentImageIndex = 0;
        } catch (Exception e) {
            com.cc106.bidhub.utils.ErrorHandler.handleImageError(
                this,
                currentItem != null ? currentItem.getItemId() : "unknown",
                e,
                "load item images"
            );
            itemImages = new ArrayList<>();
            if (imageGalleryAdapter != null) {
                imageGalleryAdapter.setImages(itemImages);
            }
            setupImageIndicators();
            currentImageIndex = 0;
        }
    }
    
    private void loadSampleImages() {
        itemImages = new ArrayList<>();
        // Add multiple sample images for carousel testing
        itemImages.add("sample_watch_1");
        itemImages.add("sample_watch_2");
        itemImages.add("sample_watch_3");
        itemImages.add("sample_watch_4");
        
        // Load images into ViewPager2 adapter
        if (imageGalleryAdapter != null) {
            imageGalleryAdapter.setImages(itemImages);
        }
        
        // Setup image indicators
        setupImageIndicators();
        
        currentImageIndex = 0;
    }
    
    private String getCategoryDisplayName(String categoryId) {
        try {
            if (itemManager != null) {
                com.cc106.bidhub.items.Category category = itemManager.getCategoryById(categoryId);
                if (category != null) {
                    return category.getName();
                }
            }
        } catch (Exception e) {
            android.util.Log.e("ItemDetailActivity", "Error getting category name: " + e.getMessage(), e);
        }
        return "General"; // Default fallback
    }
    
    private String calculateTimeLeft(java.util.Date deadline) {
        if (deadline == null) {
            return "No deadline set";
        }
        
        long now = System.currentTimeMillis();
        long deadlineTime = deadline.getTime();
        long diff = deadlineTime - now;
        
        if (diff <= 0) {
            return "Auction ended";
        }
        
        long days = diff / (24 * 60 * 60 * 1000);
        long hours = (diff % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);
        long minutes = (diff % (60 * 60 * 1000)) / (60 * 1000);
        
        if (days > 0) {
            return days + "d " + hours + "h " + minutes + "m";
        } else if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return minutes + "m";
        }
    }
    
    private int calculateAuctionProgress(java.util.Date deadline) {
        if (deadline == null) {
            return 0;
        }
        
        long now = System.currentTimeMillis();
        long deadlineTime = deadline.getTime();
        long diff = deadlineTime - now;
        
        if (diff <= 0) {
            return 100; // Auction ended
        }
        
        // Assuming 7-day auction duration for progress calculation
        long totalDuration = 7 * 24 * 60 * 60 * 1000L; // 7 days in milliseconds
        long elapsed = totalDuration - diff;
        
        if (elapsed <= 0) {
            return 0;
        }
        
        int progress = (int) ((elapsed * 100) / totalDuration);
        return Math.min(progress, 100);
    }
    
    private void loadSellerInformation(String sellerId) {
        try {
            // Use seller name from item if available (from API)
            if (currentItem != null && currentItem.getSellerName() != null && !currentItem.getSellerName().isEmpty()) {
                tvSellerName.setText(currentItem.getSellerName());
                tvSellerRating.setText("4.5 (50 reviews)");
                com.cc106.bidhub.utils.ErrorHandler.logInfo(
                    "ItemDetailActivity", 
                    "Successfully loaded seller username from item",
                    String.format("SellerID: %s, Username: %s", sellerId, currentItem.getSellerName())
                );
                return;
            }
            
            // Try to load seller information from database
            String username = getUsernameFromDatabase(sellerId);
            if (username != null && !username.isEmpty()) {
                // Display just the username without "Seller" prefix
                tvSellerName.setText(username);
                tvSellerRating.setText("4.5 (50 reviews)");
                com.cc106.bidhub.utils.ErrorHandler.logInfo(
                    "ItemDetailActivity", 
                    "Successfully loaded seller username from database",
                    String.format("SellerID: %s, Username: %s", sellerId, username)
                );
            } else {
                // Fallback: extract username from email
                String extractedUsername = extractUsernameFromEmail(sellerId);
                tvSellerName.setText(extractedUsername);
                tvSellerRating.setText("4.5 (50 reviews)");
                com.cc106.bidhub.utils.ErrorHandler.logWarning(
                    "ItemDetailActivity", 
                    "Could not find username in database, using email extraction",
                    String.format("SellerID: %s, Extracted: %s", sellerId, extractedUsername)
                );
            }
        } catch (Exception e) {
            com.cc106.bidhub.utils.ErrorHandler.handleDatabaseError(
                this,
                "load seller information",
                e,
                String.format("SellerID: %s", sellerId)
            );
            tvSellerName.setText("Unknown Seller");
            tvSellerRating.setText("No rating");
        }
    }
    
    /**
     * Get username from database by seller ID (email)
     */
    private String getUsernameFromDatabase(String sellerId) {
        try {
            // TODO: Implement actual database query
            // For now, return null to trigger fallback
            return null;
        } catch (Exception e) {
            com.cc106.bidhub.utils.ErrorHandler.logError(
                "ItemDetailActivity", 
                "Error querying database for username",
                e,
                String.format("SellerID: %s", sellerId)
            );
            return null;
        }
    }
    
    /**
     * Extract username from email address
     */
    private String extractUsernameFromEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "Unknown Seller";
        }
        
        try {
            int atIndex = email.indexOf('@');
            if (atIndex > 0) {
                return email.substring(0, atIndex);
            } else {
                return email.length() > 8 ? email.substring(0, 8) : email;
            }
        } catch (Exception e) {
            com.cc106.bidhub.utils.ErrorHandler.logError(
                "ItemDetailActivity", 
                "Error extracting username from email",
                e,
                String.format("Email: %s", email)
            );
            return "Unknown Seller";
        }
    }
    
    private void showBidConfirmationDialog() {
        if (currentItem == null) {
            ToastHelper.showError(this, "Item information not available. Please try again.");
            return;
        }

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

            // Calculate minimum bid (current highest bid + 1, or starting price if no bids)
            double currentHighestBid = currentItem.getCurrentPrice();
            double startingPrice = currentItem.getStartingPrice();
            double minimumBid = Math.max(currentHighestBid + 1.0, startingPrice);
            
            // Validate minimum bid
            if (bidAmount < minimumBid) {
                String errorMsg = String.format("Bid must be at least %s (current highest: %s)", 
                    currencyFormat.format(minimumBid), 
                    currencyFormat.format(currentHighestBid > 0 ? currentHighestBid : startingPrice));
                etBidAmount.setError(errorMsg);
                ToastHelper.showError(this, errorMsg);
                return;
            }

            // Check user's credit balance
            double userBalance = prefsHelper.getCredits();
            if (userBalance < bidAmount) {
                String errorMsg = String.format("Insufficient credits. Required: %s, Available: %s", 
                    currencyFormat.format(bidAmount), 
                    currencyFormat.format(userBalance));
                etBidAmount.setError(errorMsg);
                ToastHelper.showError(this, errorMsg);
                return;
            }

            // All validations passed - create and show bid confirmation dialog
            createBidConfirmationDialog(bidAmount);
        } catch (NumberFormatException e) {
            etBidAmount.setError("Please enter a valid number");
            ToastHelper.showError(this, "Please enter a valid bid amount");
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
        
        // Show loading state while fetching fresh balance
        tvDialogRemainingBalance.setText("Loading balance...");
        tvDialogRemainingBalance.setTextColor(getResources().getColor(R.color.text_secondary));
        
        // Fetch fresh credit balance from backend before showing dialog
        fetchFreshCreditBalanceAndUpdateDialog(bidAmount, tvDialogRemainingBalance, btnConfirm);

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

    private void fetchFreshCreditBalanceAndUpdateDialog(double bidAmount, TextView tvDialogRemainingBalance, Button btnConfirm) {
        new Thread(() -> {
            try {
                String authToken = prefsHelper.getAuthToken();
                if (authToken == null || authToken.isEmpty()) {
                    runOnUiThread(() -> {
                        tvDialogRemainingBalance.setText("Authentication error");
                        tvDialogRemainingBalance.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                        btnConfirm.setEnabled(false);
                    });
                    return;
                }

                // Call backend to get fresh credit balance
                URL url = new URL("https://bidhub-android-app.onrender.com/api/credits/balance");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Authorization", "Bearer " + authToken);
                connection.setConnectTimeout(60000); // 60 seconds
                connection.setReadTimeout(60000);    // 60 seconds

                int responseCode = connection.getResponseCode();
                if (responseCode >= 200 && responseCode < 300) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    double freshBalance = jsonResponse.optDouble("credits", jsonResponse.optDouble("balance", 0.0));
                    
                    // Update SharedPreferences with fresh balance
                    prefsHelper.setCredits(freshBalance);
                    
                    // Calculate remaining balance
                    int creditCost = (int) bidAmount;
                    int remainingBalance = (int) (freshBalance - creditCost);
                    
                    // Update UI on main thread
                    runOnUiThread(() -> {
                        tvDialogRemainingBalance.setText(remainingBalance + " Credits");
                        
                        if (remainingBalance < 0) {
                            tvDialogRemainingBalance.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                            tvDialogRemainingBalance.setText("Insufficient Credits! (" + remainingBalance + ")");
                            btnConfirm.setEnabled(false);
                            btnConfirm.setText("Insufficient Credits");
                        } else {
                            tvDialogRemainingBalance.setTextColor(getResources().getColor(R.color.text_primary));
                            btnConfirm.setEnabled(true);
                            btnConfirm.setText("Confirm Bid");
                        }
                    });
                    
                    Log.i("ItemDetailActivity", "Fresh credit balance fetched: " + freshBalance + ", remaining after bid: " + remainingBalance);
                } else {
                    runOnUiThread(() -> {
                        tvDialogRemainingBalance.setText("Failed to load balance");
                        tvDialogRemainingBalance.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                        btnConfirm.setEnabled(false);
                    });
                }
            } catch (Exception e) {
                Log.e("ItemDetailActivity", "Error fetching fresh credit balance", e);
                runOnUiThread(() -> {
                    tvDialogRemainingBalance.setText("Error loading balance");
                    tvDialogRemainingBalance.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    btnConfirm.setEnabled(false);
                });
            }
        }).start();
    }

    private void processBid(double bidAmount) {
        // Move bid placement to background thread to avoid NetworkOnMainThreadException
        new Thread(() -> {
            try {
                // First check if item exists on server
                if (!checkItemExistsOnServer()) {
                    return; // Error already handled in checkItemExistsOnServer
                }
                
                // Use BiddingEngine to place the bid
                com.cc106.bidhub.bidding.BiddingEngine biddingEngine = com.cc106.bidhub.bidding.BiddingEngine.getInstance(this);
                com.cc106.bidhub.bidding.BidResult result = biddingEngine.placeBid(
                    currentItem.getItemId(),
                    bidAmount
                );
                
                if (result.isSuccess()) {
                    // Refresh credit balance after successful bid
                    com.cc106.bidhub.utils.CreditBalanceManager.refreshBalance(
                        ItemDetailActivity.this,
                        new com.cc106.bidhub.utils.CreditBalanceManager.BalanceUpdateCallback() {
                            @Override
                            public void onBalanceUpdated(double newBalance) {
                                // Balance updated, continue with UI updates
                            }
                            
                            @Override
                            public void onError(String errorMessage) {
                                // Silent fail
                            }
                        }
                    );
                    
                    // Update UI on main thread
                    runOnUiThread(() -> {
                        android.widget.Toast.makeText(this, "Bid of " + currencyFormat.format(bidAmount) + " placed successfully!", android.widget.Toast.LENGTH_LONG).show();
                        
                        // Update current bid display
                        tvCurrentBid.setText(currencyFormat.format(bidAmount));
                        
                        // Update bid count
                        if (currentItem != null) {
                            currentItem.incrementBidCount();
                            if (tvBidCount != null) {
                                int bidCount = currentItem.getBidCount();
                                tvBidCount.setText(bidCount + (bidCount == 1 ? " bid" : " bids"));
                            }
                        }
                        
                        // Clear bid input
                        etBidAmount.setText("");
                        
                        // Refresh bid history
                        setupBidHistory();
                    });
                } else {
                    String errorMessage = result.getMessage();
                    
                    // Check if it's an insufficient credits error
                    if (errorMessage != null && (errorMessage.toLowerCase().contains("insufficient credits") || 
                        errorMessage.toLowerCase().contains("insufficient credit"))) {
                        
                        // Redirect to credits screen for insufficient credits
                        runOnUiThread(() -> {
                            ToastHelper.showError(this, "Insufficient credits. Please purchase more credits to continue.");
                            Intent intent = new Intent(this, com.cc106.bidhub.CreditsActivity.class);
                            intent.putExtra("USER_EMAIL", getCurrentUserEmail());
                            startActivity(intent);
                        });
                    } else {
                        final String finalErrorMessage = errorMessage;
                        runOnUiThread(() -> android.widget.Toast.makeText(this, "Failed to place bid: " + finalErrorMessage, android.widget.Toast.LENGTH_LONG).show());
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("ItemDetailActivity", "Error placing bid: " + e.getMessage(), e);
                final String errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown error occurred";
                runOnUiThread(() -> android.widget.Toast.makeText(this, "Error placing bid: " + errorMessage, android.widget.Toast.LENGTH_LONG).show());
            }
        }).start();
    }
    
    private String getCurrentUserId() {
        try {
            // Get user ID from SharedPreferences (synced with backend)
            SharedPreferencesHelper prefsHelper = new SharedPreferencesHelper(this);
            String userId = prefsHelper.getUserId();
            
            if (userId != null && !userId.isEmpty()) {
                return userId;
            }
        } catch (Exception e) {
            android.util.Log.e("ItemDetailActivity", "Error getting user ID: " + e.getMessage(), e);
        }
        
        // Fallback to test user ID
        return "test_user_123";
    }
    
    private String getCurrentUserEmail() {
        if (loggedInUserEmail != null && !loggedInUserEmail.isEmpty()) {
            return loggedInUserEmail;
        }
        
        // Fallback to test email
        return "test@example.com";
    }
    
    private String getCurrentUserAlias() {
        try {
            // Get user alias from SharedPreferences (synced with backend)
            SharedPreferencesHelper prefsHelper = new SharedPreferencesHelper(this);
            String alias = prefsHelper.getAlias();
            
            if (alias != null && !alias.isEmpty()) {
                return alias;
            }
        } catch (Exception e) {
            android.util.Log.e("ItemDetailActivity", "Error getting user alias: " + e.getMessage(), e);
        }
        
        // Fallback to test alias
        return "TestUser";
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
    
    // ==================== REAL-TIME FEATURES ====================
    
    /**
     * Start countdown timer for auction
     */
    private void startCountdownTimer() {
        if (currentItem == null || currentItem.getEndDate() == null) {
            return;
        }
        
        // Stop existing timer if running
        stopCountdownTimer();
        
        countdownRunnable = new java.lang.Runnable() {
            @Override
            public void run() {
                updateCountdownDisplay();
                if (!isAuctionEnded) {
                    countdownHandler.postDelayed(this, COUNTDOWN_UPDATE_INTERVAL);
                }
            }
        };
        
        countdownHandler.post(countdownRunnable);
    }
    
    /**
     * Stop countdown timer
     */
    private void stopCountdownTimer() {
        if (countdownRunnable != null) {
            countdownHandler.removeCallbacks(countdownRunnable);
            countdownRunnable = null;
        }
    }
    
    /**
     * Update countdown display
     */
    private void updateCountdownDisplay() {
        if (currentItem == null || currentItem.getEndDate() == null) {
            return;
        }
        
        try {
            long now = System.currentTimeMillis();
            long deadlineTime = currentItem.getEndDate().getTime();
            long diff = deadlineTime - now;
            
            if (diff <= 0) {
                // Auction ended
                isAuctionEnded = true;
                tvTimeLeft.setText("Auction Ended");
                tvTimeLeft.setTextColor(getResources().getColor(R.color.semantic_error));
                progressTimeLeft.setProgress(100);
                stopCountdownTimer();
                
                // Disable bidding
                btnPlaceBid.setEnabled(false);
                btnPlaceBid.setText("Auction Ended");
                btnPlaceBid.setBackgroundColor(getResources().getColor(R.color.state_disabled));
                
                // Send notification if user was bidding
                sendAuctionEndedNotification();
                
            } else {
                // Update countdown display
                String timeLeft = calculateTimeLeft(currentItem.getEndDate());
                tvTimeLeft.setText(timeLeft);
                
                // Update progress
                int progress = calculateAuctionProgress(currentItem.getEndDate());
                progressTimeLeft.setProgress(progress);
                
                // Check if ending soon (less than 5 minutes)
                long minutesLeft = diff / (60 * 1000);
                if (minutesLeft <= 5 && minutesLeft > 0 && !isAuctionEndingSoon) {
                    isAuctionEndingSoon = true;
                    // Visual indicator for ending soon
                    tvTimeLeft.setTextColor(getResources().getColor(R.color.ending_orange));
                    tvTimeLeft.setText("⚠️ " + timeLeft + " (Ending Soon!)");
                    progressTimeLeft.setProgressTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.ending_orange)));
                    
                    // Send ending soon notification
                    sendAuctionEndingSoonNotification();
                } else if (minutesLeft > 5) {
                    // Reset to normal state
                    isAuctionEndingSoon = false;
                    tvTimeLeft.setTextColor(getResources().getColor(R.color.text_primary));
                    progressTimeLeft.setProgressTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.primary)));
                }
            }
        } catch (Exception e) {
            android.util.Log.e("ItemDetailActivity", "Error updating countdown: " + e.getMessage(), e);
        }
    }
    
    /**
     * Send auction ending soon notification
     */
    private void sendAuctionEndingSoonNotification() {
        try {
            if (loggedInUserEmail != null) {
                com.cc106.bidhub.notifications.BidHubNotificationManager notificationManager = 
                    com.cc106.bidhub.notifications.BidHubNotificationManager.getInstance(this);
                
                long now = System.currentTimeMillis();
                long deadlineTime = currentItem.getEndDate().getTime();
                long diff = deadlineTime - now;
                int minutesLeft = (int) (diff / (60 * 1000));
                
                notificationManager.sendAuctionEndingNotification(
                    loggedInUserEmail,
                    currentItem.getTitle(),
                    minutesLeft
                );
            }
        } catch (Exception e) {
            android.util.Log.e("ItemDetailActivity", "Error sending ending soon notification: " + e.getMessage(), e);
        }
    }
    
    /**
     * Send auction ended notification
     */
    private void sendAuctionEndedNotification() {
        try {
            if (loggedInUserEmail != null) {
                com.cc106.bidhub.notifications.BidHubNotificationManager notificationManager = 
                    com.cc106.bidhub.notifications.BidHubNotificationManager.getInstance(this);
                
                notificationManager.sendSystemNotification(
                    loggedInUserEmail,
                    "Auction Ended",
                    currentItem.getTitle() + " auction has ended"
                );
            }
        } catch (Exception e) {
            android.util.Log.e("ItemDetailActivity", "Error sending ended notification: " + e.getMessage(), e);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopCountdownTimer();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        stopCountdownTimer();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (currentItem != null && currentItem.getEndDate() != null) {
            startCountdownTimer();
        }
    }
    
    private void showBuyNowConfirmationDialog() {
        if (currentItem == null) {
            ToastHelper.showError(this, "Item information not available");
            return;
        }

        double buyNowPrice = currentItem.getBuyNowPrice();

        // Show loading message while fetching balance
        runOnUiThread(() -> ToastHelper.showInfo(this, "Checking your balance..."));

        // Show buy now confirmation dialog immediately
        // Let the backend handle credit validation during actual purchase
        runOnUiThread(() -> {
            // Get current balance for display purposes only
            double currentBalance = prefsHelper.getCredits();

            // Create and show buy now confirmation dialog
                Dialog buyNowDialog = new Dialog(this);
                buyNowDialog.setContentView(R.layout.dialog_buy_now_confirmation);
                buyNowDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

                // Get dialog views
                TextView tvItemName = buyNowDialog.findViewById(R.id.tv_item_name);
                TextView tvBuyNowPrice = buyNowDialog.findViewById(R.id.tv_buy_now_price);
                TextView tvCreditCost = buyNowDialog.findViewById(R.id.tv_credit_cost);
                TextView tvRemainingBalance = buyNowDialog.findViewById(R.id.tv_remaining_balance);
                Button btnConfirmPurchase = buyNowDialog.findViewById(R.id.btn_confirm_purchase);
                Button btnCancelPurchase = buyNowDialog.findViewById(R.id.btn_cancel_purchase);

                // Set dialog content with current balance
                tvItemName.setText(currentItem.getTitle());
                tvBuyNowPrice.setText(currencyFormat.format(buyNowPrice));
                tvCreditCost.setText(currencyFormat.format(buyNowPrice));
                tvRemainingBalance.setText(currencyFormat.format(currentBalance - buyNowPrice));

                // Set click listeners
                btnConfirmPurchase.setOnClickListener(v -> {
                    buyNowDialog.dismiss();
                    processBuyNow();
                });

                btnCancelPurchase.setOnClickListener(v -> buyNowDialog.dismiss());

                buyNowDialog.show();
        });
    }
    
    /**
     * Check if the current item exists on the server
     * @return true if item exists, false if not (error already handled)
     */
    private boolean checkItemExistsOnServer() {
        if (currentItem == null || currentItem.getItemId() == null) {
            runOnUiThread(() -> ToastHelper.showError(this, "Item information not available"));
            return false;
        }
        
        try {
            com.cc106.bidhub.api.ItemApiClient itemApiClient = new com.cc106.bidhub.api.ItemApiClient(this);
            com.cc106.bidhub.api.ItemApiClient.ApiResponse response = itemApiClient.checkItemExists(currentItem.getItemId());
            
            if (!response.isSuccess()) {
                android.util.Log.w("ItemDetailActivity", "Item not found on server: " + currentItem.getItemId());
                runOnUiThread(() -> {
                    android.widget.Toast.makeText(this, 
                        "This item is not available on the server. It may not have been synced yet. Please try refreshing or recreating the item.", 
                        android.widget.Toast.LENGTH_LONG).show();
                });
                return false;
            }
            
            android.util.Log.i("ItemDetailActivity", "Item confirmed to exist on server: " + currentItem.getItemId());
            return true;
            
        } catch (Exception e) {
            android.util.Log.e("ItemDetailActivity", "Error checking item existence", e);
            runOnUiThread(() -> ToastHelper.showError(this, "Error verifying item availability"));
            return false;
        }
    }
    
    private void processBuyNow() {
        if (currentItem == null) {
            ToastHelper.showError(this, "Item information not available");
            return;
        }
        
        // Get user balance from backend
        double userBalance = prefsHelper.getCredits();
        double buyNowPrice = currentItem.getBuyNowPrice();
        
        // Move network operation to background thread to avoid NetworkOnMainThreadException
        new Thread(() -> {
            try {
                // First check if item exists on server
                if (!checkItemExistsOnServer()) {
                    return; // Error already handled in checkItemExistsOnServer
                }
                
            // Complete purchase via backend
            String token = prefsHelper.getAuthToken();
            android.util.Log.i("ItemDetailActivity", "=== BUY NOW DEBUG ===");
            android.util.Log.i("ItemDetailActivity", "Auth token: " + (token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "NULL"));
            android.util.Log.i("ItemDetailActivity", "Item ID: " + currentItem.getItemId());
            android.util.Log.i("ItemDetailActivity", "Buy now price: " + buyNowPrice);
            
                if (token == null || token.isEmpty()) {
                    android.util.Log.e("ItemDetailActivity", "No auth token available");
                    runOnUiThread(() -> ToastHelper.showError(this, "User not authenticated. Please log in."));
                    return;
                }

            java.net.URL url = new java.net.URL("https://bidhub-android-app.onrender.com/api/items/" + currentItem.getItemId() + "/buy-now");
            android.util.Log.i("ItemDetailActivity", "Buy now URL: " + url.toString());
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(60000); // 60 seconds for Render cold starts
            conn.setReadTimeout(60000);    // 60 seconds for Render cold starts
            
            // Add connection properties for better reliability
            conn.setUseCaches(false);
            conn.setDoInput(true);
            
            org.json.JSONObject payload = new org.json.JSONObject();
            payload.put("amount", buyNowPrice);
            
            String requestBody = payload.toString();
            android.util.Log.i("ItemDetailActivity", "Buy now request body: " + requestBody);
            
            try (java.io.OutputStream os = conn.getOutputStream()) {
                os.write(requestBody.getBytes("UTF-8"));
                os.flush();
            }
            android.util.Log.i("ItemDetailActivity", "Buy now request sent successfully");
            
            int code = conn.getResponseCode();
            android.util.Log.i("ItemDetailActivity", "Buy now response code: " + code);
            if (code < 200 || code >= 300) {
                java.io.BufferedReader er = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getErrorStream()));
                StringBuilder sb = new StringBuilder();
                String ln; while ((ln = er.readLine()) != null) sb.append(ln); er.close();
                
                // Parse error response for better error message
                String errorMessage = "Purchase failed";
                try {
                    org.json.JSONObject errorJson = new org.json.JSONObject(sb.toString());
                    String parsedError = errorJson.optString("error", "Purchase failed");
                    
                    // Check if it's an insufficient credits error
                    if (parsedError.toLowerCase().contains("insufficient credits") || 
                        parsedError.toLowerCase().contains("insufficient credit")) {
                        
                        // Redirect to credits screen for insufficient credits
                        runOnUiThread(() -> {
                            ToastHelper.showError(this, "Insufficient credits. Please purchase more credits to continue.");
                            Intent intent = new Intent(this, com.cc106.bidhub.CreditsActivity.class);
                            intent.putExtra("USER_EMAIL", getCurrentUserEmail());
                            startActivity(intent);
                        });
                        return;
                    }
                    errorMessage = parsedError;
                } catch (Exception parseError) {
                    errorMessage = "Purchase failed: " + sb.toString();
                }
                
                final String finalErrorMessage = errorMessage;
                runOnUiThread(() -> ToastHelper.showError(this, finalErrorMessage));
                return;
            }

            // Refresh credits from backend using CreditBalanceManager
            com.cc106.bidhub.utils.CreditBalanceManager.refreshBalance(
                this,
                new com.cc106.bidhub.utils.CreditBalanceManager.BalanceUpdateCallback() {
                    @Override
                    public void onBalanceUpdated(double newBalance) {
                        // Balance updated in SharedPreferences, UI will reflect on next refresh
                        Log.i("ItemDetailActivity", "Balance refreshed after buy now: " + newBalance);
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        // Silent fail
                        Log.w("ItemDetailActivity", "Failed to refresh balance: " + errorMessage);
                    }
                }
            );

            {
                // Update item status to sold
                currentItem.setStatus(com.cc106.bidhub.items.ItemStatus.SOLD);
                currentItem.setCurrentPrice(buyNowPrice);
                currentItem.setUpdatedAt(new java.util.Date());
                
                // Update UI on main thread
                runOnUiThread(() -> {
                    // Update UI - disable buttons
                    btnPlaceBid.setEnabled(false);
                    btnBuyNow.setEnabled(false);
                    
                    // Show success message
                    ToastHelper.showSuccess(this, "Purchase successful! You have bought this item.");
                    
                    // Navigate to payment confirmation
                    Intent intent = new Intent(this, com.cc106.bidhub.PaymentConfirmationActivity.class);
                    intent.putExtra("ITEM_TITLE", currentItem.getTitle());
                    intent.putExtra("PURCHASE_PRICE", buyNowPrice);
                    intent.putExtra("USER_EMAIL", getCurrentUserEmail());
                    startActivity(intent);
                });
            }
            
            } catch (Exception e) {
                android.util.Log.e("ItemDetailActivity", "Error processing buy now: " + e.getMessage(), e);
                String errorMessage = e.getMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "Network error or server unavailable";
                }
                final String finalErrorMessage = "Error processing purchase: " + errorMessage;
                runOnUiThread(() -> ToastHelper.showError(this, finalErrorMessage));
            }
        }).start();
    }

    /**
     * Fetches the latest credit balance from the backend server.
     * This method blocks the calling thread, so it should be called from a background thread.
     * If the fetch fails, it returns the cached balance from SharedPreferences.
     *
     * @return The user's current credit balance
     */
    private double fetchFreshBalanceFromBackend() {
        try {
            String authToken = prefsHelper.getAuthToken();
            if (authToken == null || authToken.isEmpty()) {
                Log.w("ItemDetailActivity", "No auth token available, using cached balance");
                return prefsHelper.getCredits();
            }

            // Call backend API to get fresh balance
            URL url = new URL("https://bidhub-android-app.onrender.com/api/credits/balance");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + authToken);
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(60000); // 60 seconds for cold starts
            connection.setReadTimeout(60000);

            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                // Read response
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Parse JSON response
                JSONObject jsonResponse = new JSONObject(response.toString());
                double freshBalance = jsonResponse.optDouble("credits", jsonResponse.optDouble("balance", prefsHelper.getCredits()));

                // Update cached balance
                prefsHelper.setCredits(freshBalance);

                Log.i("ItemDetailActivity", "Fresh balance fetched from backend: " + freshBalance);
                return freshBalance;
            } else {
                Log.w("ItemDetailActivity", "Failed to fetch balance from backend, status: " + responseCode);
                return prefsHelper.getCredits();
            }
        } catch (Exception e) {
            Log.e("ItemDetailActivity", "Error fetching balance from backend, using cached value", e);
            return prefsHelper.getCredits();
        }
    }

}