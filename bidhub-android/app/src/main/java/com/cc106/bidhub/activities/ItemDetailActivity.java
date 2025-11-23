package com.cc106.bidhub.activities;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.cc106.bidhub.R;
import com.cc106.bidhub.api.ApiClient;
import com.cc106.bidhub.api.BidApiClient;
import com.cc106.bidhub.api.ItemApiClient;
import com.cc106.bidhub.models.Item;
import com.cc106.bidhub.utils.DateUtils;

public class ItemDetailActivity extends AppCompatActivity {
    private TextView tvTitle, tvDescription, tvCurrentBid, tvStartingBid, tvBuyNowPrice, tvEndDate;
    private EditText etBidAmount;
    private Button btnPlaceBid, btnBuyNow;
    private ProgressBar progressBar;
    private Item item;
    private String itemId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);
        
        itemId = getIntent().getStringExtra("item_id");
        if (itemId == null) {
            Toast.makeText(this, "Item ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        tvTitle = findViewById(R.id.tvTitle);
        tvDescription = findViewById(R.id.tvDescription);
        tvCurrentBid = findViewById(R.id.tvCurrentBid);
        tvStartingBid = findViewById(R.id.tvStartingBid);
        tvBuyNowPrice = findViewById(R.id.tvBuyNowPrice);
        tvEndDate = findViewById(R.id.tvEndDate);
        etBidAmount = findViewById(R.id.etBidAmount);
        btnPlaceBid = findViewById(R.id.btnPlaceBid);
        btnBuyNow = findViewById(R.id.btnBuyNow);
        progressBar = findViewById(R.id.progressBar);
        
        btnPlaceBid.setOnClickListener(v -> placeBid());
        btnBuyNow.setOnClickListener(v -> buyNow());
        
        loadItem();
    }
    
    private void loadItem() {
        progressBar.setVisibility(View.VISIBLE);
        
        new AsyncTask<Void, Void, Item>() {
            private String errorMessage = null;
            
            @Override
            protected Item doInBackground(Void... voids) {
                try {
                    ItemApiClient apiClient = new ItemApiClient(ItemDetailActivity.this);
                    return apiClient.getItemById(itemId);
                } catch (ApiClient.ApiException e) {
                    errorMessage = "Failed to load item: " + e.getMessage();
                    return null;
                } catch (Exception e) {
                    errorMessage = "Network error. Please check your connection.";
                    return null;
                }
            }
            
            @Override
            protected void onPostExecute(Item result) {
                progressBar.setVisibility(View.GONE);
                
                if (result != null) {
                    item = result;
                    displayItem();
                } else {
                    Toast.makeText(ItemDetailActivity.this, 
                        errorMessage != null ? errorMessage : "Failed to load item", 
                        Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        }.execute();
    }
    
    private void displayItem() {
        tvTitle.setText(item.getTitle());
        tvDescription.setText(item.getDescription());
        tvStartingBid.setText("₱" + String.format("%.2f", item.getStartingBid()));
        tvCurrentBid.setText("₱" + String.format("%.2f", item.getCurrentBid()));
        
        if (item.getBuyNowPrice() != null) {
            tvBuyNowPrice.setText("₱" + String.format("%.2f", item.getBuyNowPrice()));
            btnBuyNow.setVisibility(View.VISIBLE);
        } else {
            tvBuyNowPrice.setText("N/A");
            btnBuyNow.setVisibility(View.GONE);
        }
        
        if (item.getEndDate() != null) {
            tvEndDate.setText("Ends: " + DateUtils.formatDate(item.getEndDate()));
        }
        
        etBidAmount.setHint("Minimum: ₱" + String.format("%.2f", item.getCurrentBid() + 1));
    }
    
    private void placeBid() {
        String amountStr = etBidAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please enter bid amount", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= item.getCurrentBid()) {
                Toast.makeText(this, "Bid must be higher than current bid", Toast.LENGTH_SHORT).show();
                return;
            }
            
            btnPlaceBid.setEnabled(false);
            btnPlaceBid.setText("Placing bid...");
            
            new AsyncTask<Void, Void, Boolean>() {
                private String errorMessage = null;
                
                @Override
                protected Boolean doInBackground(Void... voids) {
                    try {
                        BidApiClient apiClient = new BidApiClient(ItemDetailActivity.this);
                        apiClient.placeBid(itemId, amount);
                        return true;
                    } catch (ApiClient.ApiException e) {
                        errorMessage = "Failed to place bid: " + e.getMessage();
                        try {
                            org.json.JSONObject errorJson = new org.json.JSONObject(e.getResponse());
                            if (errorJson.has("message")) {
                                errorMessage = errorJson.getString("message");
                            } else if (errorJson.has("error")) {
                                errorMessage = errorJson.getString("error");
                            }
                        } catch (Exception ex) {
                            // Use default error message
                        }
                        return false;
                    } catch (Exception e) {
                        errorMessage = "Network error. Please check your connection.";
                        return false;
                    }
                }
                
                @Override
                protected void onPostExecute(Boolean success) {
                    btnPlaceBid.setEnabled(true);
                    btnPlaceBid.setText("Place Bid");
                    
                    if (success) {
                        Toast.makeText(ItemDetailActivity.this, "Bid placed successfully", Toast.LENGTH_SHORT).show();
                        loadItem(); // Refresh item details
                    } else {
                        Toast.makeText(ItemDetailActivity.this, 
                            errorMessage != null ? errorMessage : "Failed to place bid", 
                            Toast.LENGTH_LONG).show();
                    }
                }
            }.execute();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void buyNow() {
        if (item.getBuyNowPrice() == null) {
            Toast.makeText(this, "Buy now not available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        btnBuyNow.setEnabled(false);
        btnBuyNow.setText("Processing...");
        
        new AsyncTask<Void, Void, Boolean>() {
            private String errorMessage = null;
            
            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    ItemApiClient apiClient = new ItemApiClient(ItemDetailActivity.this);
                    apiClient.buyNow(itemId, item.getBuyNowPrice());
                    return true;
                } catch (ApiClient.ApiException e) {
                    errorMessage = "Failed to buy: " + e.getMessage();
                    try {
                        org.json.JSONObject errorJson = new org.json.JSONObject(e.getResponse());
                        if (errorJson.has("message")) {
                            errorMessage = errorJson.getString("message");
                        } else if (errorJson.has("error")) {
                            errorMessage = errorJson.getString("error");
                        }
                    } catch (Exception ex) {
                        // Use default error message
                    }
                    return false;
                } catch (Exception e) {
                    errorMessage = "Network error. Please check your connection.";
                    return false;
                }
            }
            
            @Override
            protected void onPostExecute(Boolean success) {
                btnBuyNow.setEnabled(true);
                btnBuyNow.setText("Buy Now");
                
                if (success) {
                    Toast.makeText(ItemDetailActivity.this, "Purchase completed successfully", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(ItemDetailActivity.this, 
                        errorMessage != null ? errorMessage : "Failed to complete purchase", 
                        Toast.LENGTH_LONG).show();
                }
            }
        }.execute();
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}

