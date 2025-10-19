package com.cc106.bidhub;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.cc106.bidhub.adapters.CategoryAdapter;
import com.cc106.bidhub.items.Category;
import com.cc106.bidhub.items.CategoryManager;
import com.cc106.bidhub.toast.ToastHelper;
import java.util.ArrayList;
import java.util.List;

public class CategorySelectionActivity extends BaseActivity implements CategoryAdapter.OnCategoryClickListener {
    
    private String userEmail;
    private CategoryManager categoryManager;
    
    // UI Components
    private ImageButton btnBack;
    private ImageButton btnViewToggle;
    private RecyclerView rvCategories;
    private ProgressBar progressBar;
    private TextView tvCategoryCount;
    
    // Adapter and Data
    private CategoryAdapter categoryAdapter;
    private List<Category> categories;
    private List<Category> filteredCategories;
    
    // View state
    private boolean isGridView = true;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_selection);
        
        // Get data from intent
        userEmail = getIntent().getStringExtra("USER_EMAIL");
        
        initializeViews();
        setupRecyclerView();
        loadCategories();
    }
    
    private void initializeViews() {
        btnBack = findViewById(R.id.btn_back);
        btnViewToggle = findViewById(R.id.btn_view_toggle);
        rvCategories = findViewById(R.id.rv_categories);
        progressBar = findViewById(R.id.progress_bar);
        tvCategoryCount = findViewById(R.id.tv_category_count);
        
        categoryManager = CategoryManager.getInstance();
        categories = new ArrayList<>();
        filteredCategories = new ArrayList<>();
        
        // Setup click listeners
        btnBack.setOnClickListener(v -> finish());
        btnViewToggle.setOnClickListener(v -> toggleView());
    }
    
    private void setupRecyclerView() {
        categoryAdapter = new CategoryAdapter(filteredCategories);
        categoryAdapter.setOnCategoryClickListener(this);
        
        // Set layout manager based on view type
        if (isGridView) {
            GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
            rvCategories.setLayoutManager(layoutManager);
        } else {
            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            rvCategories.setLayoutManager(layoutManager);
        }
        
        rvCategories.setAdapter(categoryAdapter);
    }
    
    private void loadCategories() {
        showLoading(true);
        
        // Load categories on background thread
        new Thread(() -> {
            List<Category> allCategories = categoryManager.getAllCategories();
            
            runOnUiThread(() -> {
                categories.clear();
                categories.addAll(allCategories);
                
                filteredCategories.clear();
                filteredCategories.addAll(categories);
                categoryAdapter.notifyDataSetChanged();
                
                showLoading(false);
                updateCategoryCount();
            });
        }).start();
    }
    
    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            rvCategories.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            rvCategories.setVisibility(View.VISIBLE);
        }
    }
    
    private void updateCategoryCount() {
        if (tvCategoryCount != null) {
            int count = filteredCategories.size();
            if (count == 1) {
                tvCategoryCount.setText("1 category available");
            } else {
                tvCategoryCount.setText(count + " categories available");
            }
        }
    }
    
    private void toggleView() {
        isGridView = !isGridView;
        
        // Update button icon
        if (isGridView) {
            btnViewToggle.setImageResource(R.drawable.ic_grid_view);
        } else {
            btnViewToggle.setImageResource(R.drawable.ic_list_view);
        }
        
        // Update RecyclerView layout
        setupRecyclerView();
    }
    
    @Override
    public void onCategoryClick(Category category) {
        // Return selected category
        Intent resultIntent = new Intent();
        resultIntent.putExtra("SELECTED_CATEGORY", category);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
    
    @Override
    protected boolean isCurrentActivity(int itemId) {
        return false; // This is not a main tab activity
    }
    
    @Override
    protected void setCurrentTabSelected() {
        // No tab selection for category selection activity
    }
    
    @Override
    public String getCurrentUserEmail() {
        return userEmail;
    }
}
