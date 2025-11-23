package com.cc106.bidhub.activities;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.cc106.bidhub.R;
import com.cc106.bidhub.api.ApiClient;
import com.cc106.bidhub.api.CategoryApiClient;
import com.cc106.bidhub.api.ItemApiClient;
import com.cc106.bidhub.models.Category;
import com.cc106.bidhub.models.Item;
import java.util.ArrayList;
import java.util.List;

public class PostItemActivity extends AppCompatActivity {
    private EditText etTitle, etDescription, etStartingPrice, etReservePrice, etDurationDays;
    private Spinner spinnerCategory;
    private Button btnPublish, btnSaveDraft;
    private ProgressBar progressBar;
    private List<Category> categories = new ArrayList<>();
    private String createdItemId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_item);
        
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etStartingPrice = findViewById(R.id.etStartingPrice);
        etReservePrice = findViewById(R.id.etReservePrice);
        etDurationDays = findViewById(R.id.etDurationDays);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        btnPublish = findViewById(R.id.btnPublish);
        btnSaveDraft = findViewById(R.id.btnSaveDraft);
        progressBar = findViewById(R.id.progressBar);
        
        btnPublish.setOnClickListener(v -> createAndPublishItem());
        btnSaveDraft.setOnClickListener(v -> createDraftItem());
        
        loadCategories();
    }
    
    private void loadCategories() {
        new AsyncTask<Void, Void, List<Category>>() {
            @Override
            protected List<Category> doInBackground(Void... voids) {
                try {
                    CategoryApiClient apiClient = new CategoryApiClient(PostItemActivity.this);
                    return apiClient.getCategories();
                } catch (Exception e) {
                    return new ArrayList<>();
                }
            }
            
            @Override
            protected void onPostExecute(List<Category> result) {
                categories = result;
                // TODO: Populate spinner with categories
            }
        }.execute();
    }
    
    private void createAndPublishItem() {
        if (!validateInput()) return;
        
        progressBar.setVisibility(View.VISIBLE);
        btnPublish.setEnabled(false);
        
        new AsyncTask<Void, Void, Boolean>() {
            private String errorMessage = null;
            
            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    ItemApiClient apiClient = new ItemApiClient(PostItemActivity.this);
                    int categoryId = categories.isEmpty() ? 1 : categories.get(0).getId();
                    double startingPrice = Double.parseDouble(etStartingPrice.getText().toString());
                    Double reservePrice = etReservePrice.getText().toString().isEmpty() ? 
                        null : Double.parseDouble(etReservePrice.getText().toString());
                    int durationDays = etDurationDays.getText().toString().isEmpty() ? 
                        7 : Integer.parseInt(etDurationDays.getText().toString());
                    
                    Item item = apiClient.createItem(
                        etTitle.getText().toString(),
                        etDescription.getText().toString(),
                        categoryId,
                        startingPrice,
                        reservePrice,
                        durationDays,
                        null
                    );
                    
                    createdItemId = item.getId() != null ? item.getId() : item.getUuidId();
                    apiClient.publishItem(createdItemId, durationDays);
                    return true;
                } catch (ApiClient.ApiException e) {
                    errorMessage = "Failed to create item: " + e.getMessage();
                    return false;
                } catch (Exception e) {
                    errorMessage = "Network error. Please check your connection.";
                    return false;
                }
            }
            
            @Override
            protected void onPostExecute(Boolean success) {
                progressBar.setVisibility(View.GONE);
                btnPublish.setEnabled(true);
                
                if (success) {
                    Toast.makeText(PostItemActivity.this, "Item published successfully", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(PostItemActivity.this, 
                        errorMessage != null ? errorMessage : "Failed to publish item", 
                        Toast.LENGTH_LONG).show();
                }
            }
        }.execute();
    }
    
    private void createDraftItem() {
        if (!validateInput()) return;
        
        progressBar.setVisibility(View.VISIBLE);
        btnSaveDraft.setEnabled(false);
        
        new AsyncTask<Void, Void, Boolean>() {
            private String errorMessage = null;
            
            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    ItemApiClient apiClient = new ItemApiClient(PostItemActivity.this);
                    int categoryId = categories.isEmpty() ? 1 : categories.get(0).getId();
                    double startingPrice = Double.parseDouble(etStartingPrice.getText().toString());
                    Double reservePrice = etReservePrice.getText().toString().isEmpty() ? 
                        null : Double.parseDouble(etReservePrice.getText().toString());
                    
                    apiClient.createItem(
                        etTitle.getText().toString(),
                        etDescription.getText().toString(),
                        categoryId,
                        startingPrice,
                        reservePrice,
                        null,
                        null
                    );
                    return true;
                } catch (ApiClient.ApiException e) {
                    errorMessage = "Failed to save draft: " + e.getMessage();
                    return false;
                } catch (Exception e) {
                    errorMessage = "Network error. Please check your connection.";
                    return false;
                }
            }
            
            @Override
            protected void onPostExecute(Boolean success) {
                progressBar.setVisibility(View.GONE);
                btnSaveDraft.setEnabled(true);
                
                if (success) {
                    Toast.makeText(PostItemActivity.this, "Draft saved successfully", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(PostItemActivity.this, 
                        errorMessage != null ? errorMessage : "Failed to save draft", 
                        Toast.LENGTH_LONG).show();
                }
            }
        }.execute();
    }
    
    private boolean validateInput() {
        if (etTitle.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (etDescription.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Description is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (etStartingPrice.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Starting price is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        try {
            double price = Double.parseDouble(etStartingPrice.getText().toString());
            if (price <= 0) {
                Toast.makeText(this, "Starting price must be greater than 0", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid starting price", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}

