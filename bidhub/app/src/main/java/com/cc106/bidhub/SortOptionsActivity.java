package com.cc106.bidhub;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SortOptionsActivity extends BaseActivity {
    
    private String userEmail;
    private String currentSortBy;
    private String currentSortOrder;
    
    // UI Components
    private ImageButton btnBack;
    private Button btnApply;
    private RadioGroup radioGroupSort;
    private RadioButton radioNewest;
    private RadioButton radioOldest;
    private RadioButton radioPriceLow;
    private RadioButton radioPriceHigh;
    private RadioButton radioEndingSoon;
    private RadioButton radioMostPopular;
    private RadioButton radioMostBids;
    private RadioButton radioAlphabetical;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sort_options);
        
        // Get data from intent
        userEmail = getIntent().getStringExtra("USER_EMAIL");
        currentSortBy = getIntent().getStringExtra("SORT_BY");
        currentSortOrder = getIntent().getStringExtra("SORT_ORDER");
        
        if (currentSortBy == null) {
            currentSortBy = "createdAt";
        }
        if (currentSortOrder == null) {
            currentSortOrder = "DESC";
        }
        
        initializeViews();
        setupListeners();
        populateCurrentSelection();
    }
    
    private void initializeViews() {
        btnBack = findViewById(R.id.btn_back);
        btnApply = findViewById(R.id.btn_apply);
        radioGroupSort = findViewById(R.id.radio_group_sort);
        radioNewest = findViewById(R.id.radio_newest);
        radioOldest = findViewById(R.id.radio_oldest);
        radioPriceLow = findViewById(R.id.radio_price_low);
        radioPriceHigh = findViewById(R.id.radio_price_high);
        radioEndingSoon = findViewById(R.id.radio_ending_soon);
        radioMostPopular = findViewById(R.id.radio_most_popular);
        radioMostBids = findViewById(R.id.radio_most_bids);
        radioAlphabetical = findViewById(R.id.radio_alphabetical);
    }
    
    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnApply.setOnClickListener(v -> applySorting());
        
        radioGroupSort.setOnCheckedChangeListener((group, checkedId) -> {
            // Update selection immediately for better UX
            updateSelection(checkedId);
        });
    }
    
    private void populateCurrentSelection() {
        // Set current selection based on sort criteria
        if ("createdAt".equals(currentSortBy) && "DESC".equals(currentSortOrder)) {
            radioNewest.setChecked(true);
        } else if ("createdAt".equals(currentSortBy) && "ASC".equals(currentSortOrder)) {
            radioOldest.setChecked(true);
        } else if ("price".equals(currentSortBy) && "ASC".equals(currentSortOrder)) {
            radioPriceLow.setChecked(true);
        } else if ("price".equals(currentSortBy) && "DESC".equals(currentSortOrder)) {
            radioPriceHigh.setChecked(true);
        } else if ("endDate".equals(currentSortBy) && "ASC".equals(currentSortOrder)) {
            radioEndingSoon.setChecked(true);
        } else if ("viewCount".equals(currentSortBy) && "DESC".equals(currentSortOrder)) {
            radioMostPopular.setChecked(true);
        } else if ("bidCount".equals(currentSortBy) && "DESC".equals(currentSortOrder)) {
            radioMostBids.setChecked(true);
        } else if ("title".equals(currentSortBy) && "ASC".equals(currentSortOrder)) {
            radioAlphabetical.setChecked(true);
        }
    }
    
    private void updateSelection(int checkedId) {
        if (checkedId == R.id.radio_newest) {
            currentSortBy = "createdAt";
            currentSortOrder = "DESC";
        } else if (checkedId == R.id.radio_oldest) {
            currentSortBy = "createdAt";
            currentSortOrder = "ASC";
        } else if (checkedId == R.id.radio_price_low) {
            currentSortBy = "price";
            currentSortOrder = "ASC";
        } else if (checkedId == R.id.radio_price_high) {
            currentSortBy = "price";
            currentSortOrder = "DESC";
        } else if (checkedId == R.id.radio_ending_soon) {
            currentSortBy = "endDate";
            currentSortOrder = "ASC";
        } else if (checkedId == R.id.radio_most_popular) {
            currentSortBy = "viewCount";
            currentSortOrder = "DESC";
        } else if (checkedId == R.id.radio_most_bids) {
            currentSortBy = "bidCount";
            currentSortOrder = "DESC";
        } else if (checkedId == R.id.radio_alphabetical) {
            currentSortBy = "title";
            currentSortOrder = "ASC";
        }
    }
    
    private void applySorting() {
        // Return result
        Intent resultIntent = new Intent();
        resultIntent.putExtra("SORT_BY", currentSortBy);
        resultIntent.putExtra("SORT_ORDER", currentSortOrder);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
    
    @Override
    protected boolean isCurrentActivity(int itemId) {
        return false; // This is not a main tab activity
    }
    
    @Override
    protected void setCurrentTabSelected() {
        // No tab selection for sort options activity
    }
    
    @Override
    public String getCurrentUserEmail() {
        return userEmail;
    }
}
