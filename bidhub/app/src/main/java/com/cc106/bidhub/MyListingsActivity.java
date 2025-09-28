package com.cc106.bidhub;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.cc106.bidhub.adapters.ItemCardAdapter;
import com.cc106.bidhub.items.Item;
import com.cc106.bidhub.items.ItemManager;
import com.cc106.bidhub.toast.ToastHelper;

import java.util.ArrayList;
import java.util.List;

public class MyListingsActivity extends AppCompatActivity implements ItemCardAdapter.OnItemClickListener {

    private ImageButton btnBack;
    private TextView tvTitle, tvEmptyState;
    private RecyclerView rvMyListings;
    private ProgressBar progressBar;
    private ItemCardAdapter itemAdapter;
    private ItemManager itemManager;
    private String loggedInUserEmail;
    private List<Item> myListings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_listings);
        
        // Get user email from intent
        loggedInUserEmail = getIntent().getStringExtra("USER_EMAIL");
        if (loggedInUserEmail == null) {
            ToastHelper.showError(this, "User session expired. Please login again.");
            finish();
            return;
        }
        
        initializeViews();
        setupClickListeners();
        loadMyListings();
    }
    
    private void initializeViews() {
        btnBack = findViewById(R.id.btn_back);
        tvTitle = findViewById(R.id.tv_title);
        tvEmptyState = findViewById(R.id.tv_empty_state);
        rvMyListings = findViewById(R.id.rv_my_listings);
        progressBar = findViewById(R.id.progress_bar);
        
        // Set title
        tvTitle.setText("My Listings");
        
        // Initialize RecyclerView
        rvMyListings.setLayoutManager(new LinearLayoutManager(this));
        myListings = new ArrayList<>();
        itemAdapter = new ItemCardAdapter(myListings);
        itemAdapter.setOnItemClickListener(this);
        rvMyListings.setAdapter(itemAdapter);
        
        // Initialize ItemManager
        try {
            itemManager = ItemManager.getInstance(this);
        } catch (Exception e) {
            ToastHelper.showError(this, "Error initializing item manager: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
    }
    
    private void loadMyListings() {
        if (itemManager == null) {
            ToastHelper.showError(this, "Item manager not available");
            return;
        }
        
        progressBar.setVisibility(View.VISIBLE);
        
        // Load user's listings in a background thread
        new Thread(() -> {
            try {
                List<Item> userListings = itemManager.getItemsBySeller(loggedInUserEmail);
                
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    
                    if (userListings != null && !userListings.isEmpty()) {
                        myListings.clear();
                        myListings.addAll(userListings);
                        itemAdapter.notifyDataSetChanged();
                        tvEmptyState.setVisibility(View.GONE);
                        rvMyListings.setVisibility(View.VISIBLE);
                    } else {
                        // No listings found
                        tvEmptyState.setVisibility(View.VISIBLE);
                        rvMyListings.setVisibility(View.GONE);
                        tvEmptyState.setText("You haven't posted any items yet.\n\nStart by posting your first item!");
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    ToastHelper.showError(this, "Error loading your listings: " + e.getMessage());
                    e.printStackTrace();
                });
            }
        }).start();
    }
    
    @Override
    public void onItemClick(Item item) {
        // Navigate to item detail view
        Intent intent = new Intent(this, ItemDetailActivity.class);
        intent.putExtra("ITEM_ID", item.getItemId());
        intent.putExtra("USER_EMAIL", loggedInUserEmail);
        startActivity(intent);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh listings when returning to this activity
        loadMyListings();
    }
}
