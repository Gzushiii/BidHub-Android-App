package com.cc106.bidhub;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.cc106.bidhub.adapters.ItemCardAdapter;
import com.cc106.bidhub.items.Item;
import com.cc106.bidhub.items.ItemManager;
import com.cc106.bidhub.items.FilterCriteria;
import com.cc106.bidhub.toast.ToastHelper;
import java.util.ArrayList;
import java.util.List;

public class SearchResultsActivity extends BaseActivity implements ItemCardAdapter.OnItemClickListener {
    
    private String searchQuery;
    private String userEmail;
    private ItemManager itemManager;
    
    // UI Components
    private EditText etSearch;
    private ImageButton btnBack;
    private ImageButton btnFilter;
    private ImageButton btnSort;
    private ImageButton btnViewToggle;
    private RecyclerView rvItems;
    private ProgressBar progressBar;
    private LinearLayout layoutEmptyState;
    private TextView tvResultsCount;
    private TextView tvSearchQuery;
    private ImageView ivEmptyIcon;
    private TextView tvEmptyTitle;
    private TextView tvEmptySubtitle;
    private Button btnClearSearch;
    private Button btnBrowseAll;
    
    // Adapter and Data
    private ItemCardAdapter itemAdapter;
    private List<Item> searchResults;
    private FilterCriteria currentFilter;
    
    // View state
    private boolean isGridView = true;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);
        
        // Get data from intent
        searchQuery = getIntent().getStringExtra("SEARCH_QUERY");
        userEmail = getIntent().getStringExtra("USER_EMAIL");
        
        if (searchQuery == null) {
            searchQuery = "";
        }
        
        initializeViews();
        setupRecyclerView();
        setupSearch();
        performSearch();
    }
    
    private void initializeViews() {
        etSearch = findViewById(R.id.et_search);
        btnBack = findViewById(R.id.btn_back);
        btnFilter = findViewById(R.id.btn_filter);
        btnSort = findViewById(R.id.btn_sort);
        btnViewToggle = findViewById(R.id.btn_view_toggle);
        rvItems = findViewById(R.id.rv_items);
        progressBar = findViewById(R.id.progress_bar);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        tvResultsCount = findViewById(R.id.tv_results_count);
        tvSearchQuery = findViewById(R.id.tv_search_query);
        ivEmptyIcon = findViewById(R.id.iv_empty_icon);
        tvEmptyTitle = findViewById(R.id.tv_empty_title);
        tvEmptySubtitle = findViewById(R.id.tv_empty_subtitle);
        btnClearSearch = findViewById(R.id.btn_clear_search);
        btnBrowseAll = findViewById(R.id.btn_browse_all);
        
        itemManager = ItemManager.getInstance(this);
        searchResults = new ArrayList<>();
        currentFilter = new FilterCriteria();
        
        // Set search query
        etSearch.setText(searchQuery);
        tvSearchQuery.setText("\"" + searchQuery + "\"");
        
        // Setup click listeners
        btnBack.setOnClickListener(v -> finish());
        btnFilter.setOnClickListener(v -> showFilterDialog());
        btnSort.setOnClickListener(v -> showSortDialog());
        btnViewToggle.setOnClickListener(v -> toggleView());
        btnClearSearch.setOnClickListener(v -> clearSearch());
        btnBrowseAll.setOnClickListener(v -> browseAllItems());
    }
    
    private void setupRecyclerView() {
        itemAdapter = new ItemCardAdapter(searchResults);
        itemAdapter.setOnItemClickListener(this);
        
        // Set layout manager based on view type
        if (isGridView) {
            GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
            rvItems.setLayoutManager(layoutManager);
        } else {
            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            rvItems.setLayoutManager(layoutManager);
        }
        
        rvItems.setAdapter(itemAdapter);
    }
    
    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Update search query
                searchQuery = s.toString().trim();
                tvSearchQuery.setText("\"" + searchQuery + "\"");
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
    
    private void performSearch() {
        showLoading(true);
        
        // Perform search on background thread
        new Thread(() -> {
            currentFilter.setQuery(searchQuery);
            List<Item> results = itemManager.filterItems(currentFilter);
            
            // Update UI on main thread
            runOnUiThread(() -> {
                searchResults.clear();
                searchResults.addAll(results);
                itemAdapter.notifyDataSetChanged();
                
                showLoading(false);
                updateEmptyState();
                updateResultsCount();
            });
        }).start();
    }
    
    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            rvItems.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            rvItems.setVisibility(View.VISIBLE);
        }
    }
    
    private void updateEmptyState() {
        if (searchResults.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            rvItems.setVisibility(View.GONE);
            
            if (searchQuery.isEmpty()) {
                tvEmptyTitle.setText("No search query");
                tvEmptySubtitle.setText("Enter a search term to find items");
                ivEmptyIcon.setImageResource(R.drawable.ic_search);
                btnClearSearch.setVisibility(View.GONE);
                btnBrowseAll.setVisibility(View.VISIBLE);
            } else {
                tvEmptyTitle.setText("No results found");
                tvEmptySubtitle.setText("No items match your search for \"" + searchQuery + "\"");
                ivEmptyIcon.setImageResource(R.drawable.ic_search);
                btnClearSearch.setVisibility(View.VISIBLE);
                btnBrowseAll.setVisibility(View.VISIBLE);
            }
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            rvItems.setVisibility(View.VISIBLE);
        }
    }
    
    private void updateResultsCount() {
        if (tvResultsCount != null) {
            int count = searchResults.size();
            if (count == 1) {
                tvResultsCount.setText("1 result found");
            } else {
                tvResultsCount.setText(count + " results found");
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
    
    private void showFilterDialog() {
        // TODO: Implement filter dialog
        ToastHelper.showInfo(this, "Filter functionality coming soon!");
    }
    
    private void showSortDialog() {
        // TODO: Implement sort dialog
        ToastHelper.showInfo(this, "Sort functionality coming soon!");
    }
    
    private void clearSearch() {
        etSearch.setText("");
        searchQuery = "";
        tvSearchQuery.setText("\"\"");
        performSearch();
    }
    
    private void browseAllItems() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("USER_EMAIL", userEmail);
        intent.putExtra("TAB_INDEX", 1); // Browse tab
        startActivity(intent);
        finish();
    }
    
    @Override
    public void onItemClick(Item item) {
        // Navigate to item detail activity
        Intent intent = new Intent(this, ItemDetailActivity.class);
        intent.putExtra("ITEM_ID", item.getItemId());
        intent.putExtra("USER_EMAIL", userEmail);
        startActivity(intent);
    }
    
    @Override
    protected boolean isCurrentActivity(int itemId) {
        return false; // This is not a main tab activity
    }
    
    @Override
    protected void setCurrentTabSelected() {
        // No tab selection for search results activity
    }
    
    @Override
    public String getCurrentUserEmail() {
        return userEmail;
    }
}
