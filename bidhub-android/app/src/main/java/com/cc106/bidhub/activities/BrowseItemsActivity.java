package com.cc106.bidhub.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.cc106.bidhub.R;
import com.cc106.bidhub.adapters.ItemAdapter;
import com.cc106.bidhub.api.ApiClient;
import com.cc106.bidhub.api.ItemApiClient;
import com.cc106.bidhub.models.Item;
import java.util.ArrayList;
import java.util.List;

public class BrowseItemsActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ItemAdapter adapter;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefresh;
    private List<Item> items = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse);
        
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        
        adapter = new ItemAdapter(items, item -> {
            Intent intent = new Intent(this, ItemDetailActivity.class);
            intent.putExtra("item_id", item.getId() != null ? item.getId() : item.getUuidId());
            startActivity(intent);
        });
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        
        swipeRefresh.setOnRefreshListener(this::loadItems);
        
        loadItems();
    }
    
    private void loadItems() {
        progressBar.setVisibility(View.VISIBLE);
        swipeRefresh.setRefreshing(true);
        
        new AsyncTask<Void, Void, List<Item>>() {
            private String errorMessage = null;
            
            @Override
            protected List<Item> doInBackground(Void... voids) {
                try {
                    ItemApiClient apiClient = new ItemApiClient(BrowseItemsActivity.this);
                    return apiClient.getItems("active", null, null, null, null, 50, 0);
                } catch (ApiClient.ApiException e) {
                    errorMessage = "Failed to load items: " + e.getMessage();
                    return null;
                } catch (Exception e) {
                    errorMessage = "Network error. Please check your connection.";
                    return null;
                }
            }
            
            @Override
            protected void onPostExecute(List<Item> result) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                
                if (result != null) {
                    items.clear();
                    items.addAll(result);
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(BrowseItemsActivity.this, 
                        errorMessage != null ? errorMessage : "Failed to load items", 
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

