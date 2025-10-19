package com.cc106.bidhub;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.cc106.bidhub.adapters.CategoryAdapter;
import com.cc106.bidhub.items.Category;
import com.cc106.bidhub.items.CategoryManager;
import com.cc106.bidhub.items.FilterCriteria;
import com.cc106.bidhub.toast.ToastHelper;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdvancedFilterActivity extends BaseActivity implements CategoryAdapter.OnCategoryClickListener {
    
    private String userEmail;
    private FilterCriteria originalFilter;
    private FilterCriteria currentFilter;
    private CategoryManager categoryManager;
    
    // UI Components
    private ImageButton btnBack;
    private Button btnApply;
    private Button btnReset;
    private Button btnClearAll;
    
    // Price Range
    private SeekBar seekBarMinPrice;
    private SeekBar seekBarMaxPrice;
    private TextView tvMinPrice;
    private TextView tvMaxPrice;
    
    // Category Selection
    private RecyclerView rvCategories;
    private CategoryAdapter categoryAdapter;
    private List<Category> categories;
    private String selectedCategoryId;
    
    // Condition
    private RadioGroup radioGroupCondition;
    private RadioButton radioNew;
    private RadioButton radioUsed;
    private RadioButton radioRefurbished;
    
    // Location
    private EditText etLocation;
    
    // Special Filters
    private CheckBox cbFeatured;
    private CheckBox cbTrending;
    private CheckBox cbFreeShipping;
    private CheckBox cbBuyNowAvailable;
    
    // Sort Options
    private RadioGroup radioGroupSort;
    private RadioButton radioNewest;
    private RadioButton radioOldest;
    private RadioButton radioPriceLow;
    private RadioButton radioPriceHigh;
    private RadioButton radioEndingSoon;
    private RadioButton radioMostPopular;
    
    private NumberFormat currencyFormat;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_filter);
        
        // Get data from intent
        userEmail = getIntent().getStringExtra("USER_EMAIL");
        originalFilter = (FilterCriteria) getIntent().getSerializableExtra("FILTER_CRITERIA");
        
        if (originalFilter == null) {
            originalFilter = new FilterCriteria();
        }
        
        currentFilter = new FilterCriteria();
        copyFilterCriteria(originalFilter, currentFilter);
        
        initializeViews();
        setupRecyclerView();
        setupListeners();
        loadCategories();
        populateFilters();
    }
    
    private void initializeViews() {
        btnBack = findViewById(R.id.btn_back);
        btnApply = findViewById(R.id.btn_apply);
        btnReset = findViewById(R.id.btn_reset);
        btnClearAll = findViewById(R.id.btn_clear_all);
        
        // Price Range
        seekBarMinPrice = findViewById(R.id.seek_bar_min_price);
        seekBarMaxPrice = findViewById(R.id.seek_bar_max_price);
        tvMinPrice = findViewById(R.id.tv_min_price);
        tvMaxPrice = findViewById(R.id.tv_max_price);
        
        // Category Selection
        rvCategories = findViewById(R.id.rv_categories);
        
        // Condition
        radioGroupCondition = findViewById(R.id.radio_group_condition);
        radioNew = findViewById(R.id.radio_new);
        radioUsed = findViewById(R.id.radio_used);
        radioRefurbished = findViewById(R.id.radio_refurbished);
        
        // Location
        etLocation = findViewById(R.id.et_location);
        
        // Special Filters
        cbFeatured = findViewById(R.id.cb_featured);
        cbTrending = findViewById(R.id.cb_trending);
        cbFreeShipping = findViewById(R.id.cb_free_shipping);
        cbBuyNowAvailable = findViewById(R.id.cb_buy_now_available);
        
        // Sort Options
        radioGroupSort = findViewById(R.id.radio_group_sort);
        radioNewest = findViewById(R.id.radio_newest);
        radioOldest = findViewById(R.id.radio_oldest);
        radioPriceLow = findViewById(R.id.radio_price_low);
        radioPriceHigh = findViewById(R.id.radio_price_high);
        radioEndingSoon = findViewById(R.id.radio_ending_soon);
        radioMostPopular = findViewById(R.id.radio_most_popular);
        
        categoryManager = CategoryManager.getInstance();
        categories = new ArrayList<>();
        currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
    }
    
    private void setupRecyclerView() {
        categoryAdapter = new CategoryAdapter(categories);
        categoryAdapter.setOnCategoryClickListener(this);
        rvCategories.setLayoutManager(new LinearLayoutManager(this));
        rvCategories.setAdapter(categoryAdapter);
    }
    
    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnApply.setOnClickListener(v -> applyFilters());
        btnReset.setOnClickListener(v -> resetFilters());
        btnClearAll.setOnClickListener(v -> clearAllFilters());
        
        // Price range listeners
        seekBarMinPrice.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    double price = progress * 100.0; // Convert to actual price
                    tvMinPrice.setText(currencyFormat.format(price));
                    currentFilter.setMinPrice(price);
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        seekBarMaxPrice.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    double price = progress * 100.0; // Convert to actual price
                    tvMaxPrice.setText(currencyFormat.format(price));
                    currentFilter.setMaxPrice(price);
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // Condition listeners
        radioGroupCondition.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_new) {
                currentFilter.setCondition("New");
            } else if (checkedId == R.id.radio_used) {
                currentFilter.setCondition("Used");
            } else if (checkedId == R.id.radio_refurbished) {
                currentFilter.setCondition("Refurbished");
            } else {
                currentFilter.setCondition(null);
            }
        });
        
        // Sort listeners
        radioGroupSort.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_newest) {
                currentFilter.setSortBy("createdAt");
                currentFilter.setSortOrder("DESC");
            } else if (checkedId == R.id.radio_oldest) {
                currentFilter.setSortBy("createdAt");
                currentFilter.setSortOrder("ASC");
            } else if (checkedId == R.id.radio_price_low) {
                currentFilter.setSortBy("price");
                currentFilter.setSortOrder("ASC");
            } else if (checkedId == R.id.radio_price_high) {
                currentFilter.setSortBy("price");
                currentFilter.setSortOrder("DESC");
            } else if (checkedId == R.id.radio_ending_soon) {
                currentFilter.setSortBy("endDate");
                currentFilter.setSortOrder("ASC");
            } else if (checkedId == R.id.radio_most_popular) {
                currentFilter.setSortBy("viewCount");
                currentFilter.setSortOrder("DESC");
            }
        });
    }
    
    private void loadCategories() {
        // Load categories on background thread
        new Thread(() -> {
            List<Category> allCategories = categoryManager.getAllCategories();
            
            runOnUiThread(() -> {
                categories.clear();
                categories.addAll(allCategories);
                categoryAdapter.notifyDataSetChanged();
            });
        }).start();
    }
    
    private void populateFilters() {
        // Populate price range
        if (currentFilter.getMinPrice() != null) {
            int progress = (int) (currentFilter.getMinPrice() / 100.0);
            seekBarMinPrice.setProgress(progress);
            tvMinPrice.setText(currencyFormat.format(currentFilter.getMinPrice()));
        }
        
        if (currentFilter.getMaxPrice() != null) {
            int progress = (int) (currentFilter.getMaxPrice() / 100.0);
            seekBarMaxPrice.setProgress(progress);
            tvMaxPrice.setText(currencyFormat.format(currentFilter.getMaxPrice()));
        }
        
        // Populate condition
        String condition = currentFilter.getCondition();
        if ("New".equals(condition)) {
            radioNew.setChecked(true);
        } else if ("Used".equals(condition)) {
            radioUsed.setChecked(true);
        } else if ("Refurbished".equals(condition)) {
            radioRefurbished.setChecked(true);
        }
        
        // Populate location
        
        // Populate special filters
        if (currentFilter.getIsFeatured() != null) {
            cbFeatured.setChecked(currentFilter.getIsFeatured());
        }
        
        if (currentFilter.getIsTrending() != null) {
            cbTrending.setChecked(currentFilter.getIsTrending());
        }
        
        // Populate sort options
        String sortBy = currentFilter.getSortBy();
        String sortOrder = currentFilter.getSortOrder();
        
        if ("createdAt".equals(sortBy) && "DESC".equals(sortOrder)) {
            radioNewest.setChecked(true);
        } else if ("createdAt".equals(sortBy) && "ASC".equals(sortOrder)) {
            radioOldest.setChecked(true);
        } else if ("price".equals(sortBy) && "ASC".equals(sortOrder)) {
            radioPriceLow.setChecked(true);
        } else if ("price".equals(sortBy) && "DESC".equals(sortOrder)) {
            radioPriceHigh.setChecked(true);
        } else if ("endDate".equals(sortBy) && "ASC".equals(sortOrder)) {
            radioEndingSoon.setChecked(true);
        } else if ("viewCount".equals(sortBy) && "DESC".equals(sortOrder)) {
            radioMostPopular.setChecked(true);
        }
    }
    
    @Override
    public void onCategoryClick(Category category) {
        selectedCategoryId = category.getCategoryId();
        currentFilter.setCategoryId(selectedCategoryId);
        ToastHelper.showInfo(this, "Selected: " + category.getName());
    }
    
    private void applyFilters() {
        // Update special filters
        currentFilter.setIsFeatured(cbFeatured.isChecked() ? true : null);
        currentFilter.setIsTrending(cbTrending.isChecked() ? true : null);
        
        // Update location
        String location = etLocation.getText().toString().trim();
        
        // Return result
        Intent resultIntent = new Intent();
        resultIntent.putExtra("FILTER_CRITERIA", currentFilter);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
    
    private void resetFilters() {
        copyFilterCriteria(originalFilter, currentFilter);
        populateFilters();
    }
    
    private void clearAllFilters() {
        currentFilter = new FilterCriteria();
        populateFilters();
    }
    
    private void copyFilterCriteria(FilterCriteria source, FilterCriteria target) {
        target.setQuery(source.getQuery());
        target.setCategoryId(source.getCategoryId());
        target.setMinPrice(source.getMinPrice());
        target.setMaxPrice(source.getMaxPrice());
        target.setCondition(source.getCondition());
        target.setIsFeatured(source.getIsFeatured());
        target.setIsTrending(source.getIsTrending());
        target.setSortBy(source.getSortBy());
        target.setSortOrder(source.getSortOrder());
    }
    
    @Override
    protected boolean isCurrentActivity(int itemId) {
        return false; // This is not a main tab activity
    }
    
    @Override
    protected void setCurrentTabSelected() {
        // No tab selection for filter activity
    }
    
    @Override
    public String getCurrentUserEmail() {
        return userEmail;
    }
}
