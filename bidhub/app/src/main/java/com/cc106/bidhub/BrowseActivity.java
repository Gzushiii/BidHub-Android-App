package com.cc106.bidhub;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.cc106.bidhub.adapters.BrowseItemAdapter;
import com.cc106.bidhub.models.BrowseItem;
import com.cc106.bidhub.toast.ToastHelper;

import java.util.ArrayList;
import java.util.List;

public class BrowseActivity extends BaseActivity {

    private String loggedInUserEmail;
    
    // UI Components
    private EditText searchEditText;
    private ImageButton btnSearch, btnFilter;
    private Button btnElectronics, btnFashion, btnHome, btnCollectibles;
    private RecyclerView recyclerViewBrowse;
    private BrowseItemAdapter browseItemAdapter;
    
    // Data
    private List<BrowseItem> allItems;
    private List<BrowseItem> filteredItems;
    private String currentCategory = "Electronics";
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inflate the browse content into the content frame
        getLayoutInflater().inflate(R.layout.activity_browse_content, findViewById(R.id.content_frame));
        
        // Get the logged-in user's email from the Intent
        loggedInUserEmail = getIntent().getStringExtra("USER_EMAIL");
        
        // Initialize UI components
        initializeViews();
        
        // Setup RecyclerView
        setupRecyclerView();
        
        // Load sample data
        loadSampleData();
        
        // Setup click listeners
        setupClickListeners();
        
        // Animate content in after inflation
        animateContentIn();
    }

    private void initializeViews() {
        searchEditText = findViewById(R.id.searchEditText);
        btnSearch = findViewById(R.id.btnSearch);
        btnFilter = findViewById(R.id.btnFilter);
        btnElectronics = findViewById(R.id.btnElectronics);
        btnFashion = findViewById(R.id.btnFashion);
        btnHome = findViewById(R.id.btnHome);
        btnCollectibles = findViewById(R.id.btnCollectibles);
        recyclerViewBrowse = findViewById(R.id.recyclerViewBrowse);
    }

    private void setupRecyclerView() {
        allItems = new ArrayList<>();
        filteredItems = new ArrayList<>();
        
        browseItemAdapter = new BrowseItemAdapter(filteredItems, this::onItemClick);
        recyclerViewBrowse.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerViewBrowse.setAdapter(browseItemAdapter);
    }

    private void loadSampleData() {
        allItems.clear();
        
        // Sample items matching the HTML design
        allItems.add(new BrowseItem("1", "Vintage Camera", "Current Bid: $150", "2h 15m left", "", "Electronics", false, "ending_soon"));
        allItems.add(new BrowseItem("2", "Designer Handbag", "Current Bid: $800", "1d 4h left", "", "Fashion", false, "active"));
        allItems.add(new BrowseItem("3", "Modern Sofa", "Price: $1200", "Buy It Now", "", "Home", true, "buy_now"));
        allItems.add(new BrowseItem("4", "Rare Coin", "Current Bid: $50", "Ending soon", "", "Collectibles", false, "ending_soon"));
        
        // Add more sample items
        allItems.add(new BrowseItem("5", "Smartphone", "Current Bid: $300", "5h 30m left", "", "Electronics", false, "active"));
        allItems.add(new BrowseItem("6", "Vintage Watch", "Current Bid: $250", "1d 2h left", "", "Fashion", false, "active"));
        allItems.add(new BrowseItem("7", "Dining Table", "Price: $800", "Buy It Now", "", "Home", true, "buy_now"));
        allItems.add(new BrowseItem("8", "Art Painting", "Current Bid: $400", "3h 45m left", "", "Collectibles", false, "ending_soon"));
        
        applyFilters();
    }

    private void setupClickListeners() {
        // Search functionality
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString();
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnSearch.setOnClickListener(v -> {
            // Focus on search field
            searchEditText.requestFocus();
        });

        btnFilter.setOnClickListener(v -> showFilterDialog());

        // Category buttons
        btnElectronics.setOnClickListener(v -> selectCategory("Electronics"));
        btnFashion.setOnClickListener(v -> selectCategory("Fashion"));
        btnHome.setOnClickListener(v -> selectCategory("Home"));
        btnCollectibles.setOnClickListener(v -> selectCategory("Collectibles"));
    }

    private void selectCategory(String category) {
        currentCategory = category;
        
        // Update button states
        btnElectronics.setBackgroundResource(category.equals("Electronics") ? R.drawable.chip_background_primary : R.drawable.chip_background);
        btnElectronics.setTextColor(getResources().getColor(category.equals("Electronics") ? R.color.primary : R.color.text_secondary));
        
        btnFashion.setBackgroundResource(category.equals("Fashion") ? R.drawable.chip_background_primary : R.drawable.chip_background);
        btnFashion.setTextColor(getResources().getColor(category.equals("Fashion") ? R.color.primary : R.color.text_secondary));
        
        btnHome.setBackgroundResource(category.equals("Home") ? R.drawable.chip_background_primary : R.drawable.chip_background);
        btnHome.setTextColor(getResources().getColor(category.equals("Home") ? R.color.primary : R.color.text_secondary));
        
        btnCollectibles.setBackgroundResource(category.equals("Collectibles") ? R.drawable.chip_background_primary : R.drawable.chip_background);
        btnCollectibles.setTextColor(getResources().getColor(category.equals("Collectibles") ? R.color.primary : R.color.text_secondary));
        
        applyFilters();
    }

    private void applyFilters() {
        filteredItems.clear();
        
        for (BrowseItem item : allItems) {
            boolean matchesCategory = currentCategory.equals("All") || item.getCategory().equals(currentCategory);
            boolean matchesSearch = currentSearchQuery.isEmpty() || 
                    item.getTitle().toLowerCase().contains(currentSearchQuery.toLowerCase());
            
            if (matchesCategory && matchesSearch) {
                filteredItems.add(item);
            }
        }
        
        browseItemAdapter.updateItems(filteredItems);
    }

    private void showFilterDialog() {
        Dialog filterDialog = new Dialog(this);
        filterDialog.setContentView(R.layout.filter_modal);
        filterDialog.getWindow().setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
        );
        
        // Initialize filter dialog components
        CheckBox checkboxElectronics = filterDialog.findViewById(R.id.checkboxElectronics);
        CheckBox checkboxFashion = filterDialog.findViewById(R.id.checkboxFashion);
        CheckBox checkboxHome = filterDialog.findViewById(R.id.checkboxHome);
        CheckBox checkboxCollectibles = filterDialog.findViewById(R.id.checkboxCollectibles);
        
        SeekBar priceRangeSeekBar = filterDialog.findViewById(R.id.priceRangeSeekBar);
        TextView priceRangeText = filterDialog.findViewById(R.id.priceRangeText);
        
        RadioGroup conditionRadioGroup = filterDialog.findViewById(R.id.conditionRadioGroup);
        RadioGroup statusRadioGroup = filterDialog.findViewById(R.id.statusRadioGroup);
        
        Button btnClearFilters = filterDialog.findViewById(R.id.btnClearFilters);
        Button btnApplyFilters = filterDialog.findViewById(R.id.btnApplyFilters);
        
        // Set up price range seekbar
        priceRangeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (priceRangeText != null) {
                    priceRangeText.setText("$" + progress);
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // Clear filters
        btnClearFilters.setOnClickListener(v -> {
            checkboxElectronics.setChecked(true);
            checkboxFashion.setChecked(false);
            checkboxHome.setChecked(false);
            checkboxCollectibles.setChecked(false);
            priceRangeSeekBar.setProgress(500);
            conditionRadioGroup.check(R.id.radioNew);
            statusRadioGroup.check(R.id.radioAll);
        });
        
        // Apply filters
        btnApplyFilters.setOnClickListener(v -> {
            // Apply filter logic here
            filterDialog.dismiss();
            ToastHelper.showInfo(this, "Filters applied!");
        });
        
        filterDialog.show();
    }

    private void onItemClick(BrowseItem item) {
        // Navigate to item detail
        Intent intent = new Intent(this, ItemDetailActivity.class);
        intent.putExtra("item_id", item.getItemId());
        intent.putExtra("item_title", item.getTitle());
        intent.putExtra("item_bid", item.getCurrentBid());
        intent.putExtra("item_time_left", item.getTimeLeft());
        intent.putExtra("item_category", item.getCategory());
        intent.putExtra("item_is_buy_now", item.isBuyNow());
        startActivity(intent);
    }

    @Override
    protected boolean isCurrentActivity(int itemId) {
        return itemId == R.id.nav_browse;
    }

    @Override
    protected void setCurrentTabSelected() {
        bottomNavigationView.setSelectedItemId(R.id.nav_browse);
    }

    @Override
    public String getCurrentUserEmail() {
        return loggedInUserEmail;
    }
}
