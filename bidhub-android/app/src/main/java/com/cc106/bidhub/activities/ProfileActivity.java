package com.cc106.bidhub.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.cc106.bidhub.R;
import com.cc106.bidhub.utils.TokenManager;

public class ProfileActivity extends AppCompatActivity {
    private TextView tvEmail, tvUserId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        tvEmail = findViewById(R.id.tvEmail);
        tvUserId = findViewById(R.id.tvUserId);
        
        String email = TokenManager.getUserEmail(this);
        int userId = TokenManager.getUserId(this);
        
        tvEmail.setText("Email: " + (email != null ? email : "N/A"));
        tvUserId.setText("User ID: " + (userId != -1 ? String.valueOf(userId) : "N/A"));
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

