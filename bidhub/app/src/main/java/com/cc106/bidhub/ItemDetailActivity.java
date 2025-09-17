package com.cc106.bidhub;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.cc106.bidhub.adapters.ImageGalleryAdapter;
import com.cc106.bidhub.items.Item;
import com.cc106.bidhub.items.ItemManager;
import com.cc106.bidhub.toast.ToastHelper;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ItemDetailActivity extends BaseActivity {
    
    private String itemId;
    private String userEmail;
    private Item item;
    private ItemManager itemManager;
    
    // UI Components
    private RecyclerView rvImageGallery;
    private TextView tvTitle;
    private TextView tvDescription;
    private TextView tvCurrentPrice;
    private TextView tvStartingPrice;
    private TextView tvSeller;
    private TextView tvCategory;
    private TextView tvCondition;
    private TextView tvLocation;
    private TextView tvTimeRemaining;
    private TextView tvBidCount;
    private TextView tvViewCount;
    private Button btnPlaceBid;
    private Button btnBuyNow;
    private ProgressBar progressBar;
    
    private ImageGalleryAdapter imageAdapter;
    private NumberFormat currencyFormat;
    private SimpleDateFormat dateFormat;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);
        
        // Get data from intent
        itemId = getIntent().getStringExtra("ITEM_ID");
        userEmail = getIntent().getStringExtra("USER_EMAIL");
        
        if (itemId == null) {
            ToastHelper.showError(this, "Item not found");
            finish();
            return;
        }
        
        initializeViews();
        setupRecyclerView();
        loadItemDetails();
    }
    
    private void initializeViews() {
        rvImageGallery = findViewById(R.id.rv_image_gallery);
        tvTitle = findViewById(R.id.tv_item_title);
        tvDescription = findViewById(R.id.tv_item_description);
        tvCurrentPrice = findViewById(R.id.tv_current_price);
        tvStartingPrice = findViewById(R.id.tv_starting_price);
        tvSeller = findViewById(R.id.tv_seller);
        tvCategory = findViewById(R.id.tv_category);
        tvCondition = findViewById(R.id.tv_condition);
        tvLocation = findViewById(R.id.tv_location);
        tvTimeRemaining = findViewById(R.id.tv_time_remaining);
        tvBidCount = findViewById(R.id.tv_bid_count);
        tvViewCount = findViewById(R.id.tv_view_count);
        btnPlaceBid = findViewById(R.id.btn_place_bid);
        btnBuyNow = findViewById(R.id.btn_buy_now);
        progressBar = findViewById(R.id.progress_bar);
        
        itemManager = ItemManager.getInstance(this);
        currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        dateFormat = new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault());
        
        // Setup click listeners
        btnPlaceBid.setOnClickListener(v -> showBidDialog());
        btnBuyNow.setOnClickListener(v -> showBuyNowDialog());
    }
    
    private void setupRecyclerView() {
        imageAdapter = new ImageGalleryAdapter();
        rvImageGallery.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvImageGallery.setAdapter(imageAdapter);
    }
    
    private void loadItemDetails() {
        showLoading(true);
        
        // Load item on background thread
        new Thread(() -> {
            item = itemManager.getItemById(itemId);
            
            // Update UI on main thread
            runOnUiThread(() -> {
                if (item != null) {
                    populateItemDetails();
                    loadItemImages();
                } else {
                    ToastHelper.showError(this, "Item not found");
                    finish();
                }
                showLoading(false);
            });
        }).start();
    }
    
    private void populateItemDetails() {
        tvTitle.setText(item.getTitle());
        tvDescription.setText(item.getDescription());
        tvCurrentPrice.setText(currencyFormat.format(item.getCurrentPrice()));
        tvStartingPrice.setText("Starting: " + currencyFormat.format(item.getStartingPrice()));
        tvSeller.setText("Sold by: " + item.getSellerId());
        tvCategory.setText("Category: " + item.getCategoryName());
        tvCondition.setText("Condition: " + item.getCondition());
        tvLocation.setText("Location: " + item.getLocation());
        tvBidCount.setText(item.getBidCount() + " bids");
        tvViewCount.setText(item.getViewCount() + " views");
        
        // Calculate time remaining
        if (item.getEndDate() != null) {
            long timeRemaining = item.getEndDate().getTime() - System.currentTimeMillis();
            if (timeRemaining > 0) {
                long days = timeRemaining / (1000 * 60 * 60 * 24);
                long hours = (timeRemaining % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
                tvTimeRemaining.setText("Ends in: " + days + "d " + hours + "h");
            } else {
                tvTimeRemaining.setText("Auction ended");
                btnPlaceBid.setEnabled(false);
                btnBuyNow.setEnabled(false);
            }
        }
        
        // Show/hide Buy Now button
        if (item.getBuyNowPrice() > 0) {
            btnBuyNow.setText("Buy Now: " + currencyFormat.format(item.getBuyNowPrice()));
            btnBuyNow.setVisibility(View.VISIBLE);
        } else {
            btnBuyNow.setVisibility(View.GONE);
        }
    }
    
    private void loadItemImages() {
        // Load images from item
        if (item.getImagePaths() != null && !item.getImagePaths().isEmpty()) {
            imageAdapter.setImages(item.getImagePaths());
        } else {
            // Use placeholder images
            imageAdapter.setImages(java.util.Arrays.asList("placeholder1", "placeholder2"));
        }
    }
    
    private void showBidDialog() {
        // TODO: Implement bid dialog
        ToastHelper.showInfo(this, "Bidding functionality coming soon!");
    }
    
    private void showBuyNowDialog() {
        // TODO: Implement buy now dialog
        ToastHelper.showInfo(this, "Buy Now functionality coming soon!");
    }
    
    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.GONE);
        }
    }
    
    @Override
    protected boolean isCurrentActivity(int itemId) {
        return false; // This is not a main tab activity
    }
    
    @Override
    protected void setCurrentTabSelected() {
        // No tab selection for detail activity
    }
    
    @Override
    public String getCurrentUserEmail() {
        return userEmail;
    }
}
