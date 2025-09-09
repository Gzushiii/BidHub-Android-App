package com.cc106.bidhub;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public abstract class BaseActivity extends AppCompatActivity implements BottomNavigationView.OnItemSelectedListener {

    protected BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_with_bottom_nav);
        
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setOnItemSelectedListener(this);
        
        // Set the current tab as selected
        setCurrentTabSelected();
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        
        // Don't navigate if already on the current activity
        if (isCurrentActivity(itemId)) {
            return true;
        }
        
        if (itemId == R.id.nav_home) {
            if (!(this instanceof MainActivity)) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("USER_EMAIL", getCurrentUserEmail());
                startActivity(intent);
                finish();
            }
        } else if (itemId == R.id.nav_browse) {
            if (!(this instanceof BrowseActivity)) {
                Intent intent = new Intent(this, BrowseActivity.class);
                intent.putExtra("USER_EMAIL", getCurrentUserEmail());
                startActivity(intent);
                finish();
            }
        } else if (itemId == R.id.nav_post) {
            if (!(this instanceof PostActivity)) {
                Intent intent = new Intent(this, PostActivity.class);
                intent.putExtra("USER_EMAIL", getCurrentUserEmail());
                startActivity(intent);
                finish();
            }
        } else if (itemId == R.id.nav_credits) {
            if (!(this instanceof CreditsActivity)) {
                Intent intent = new Intent(this, CreditsActivity.class);
                intent.putExtra("USER_EMAIL", getCurrentUserEmail());
                startActivity(intent);
                finish();
            }
        } else if (itemId == R.id.nav_profile) {
            if (!(this instanceof ProfileActivity)) {
                Intent intent = new Intent(this, ProfileActivity.class);
                intent.putExtra("USER_EMAIL", getCurrentUserEmail());
                startActivity(intent);
                finish();
            }
        }
        
        return true;
    }

    protected abstract boolean isCurrentActivity(int itemId);
    protected abstract void setCurrentTabSelected();
    protected abstract String getCurrentUserEmail();
}
