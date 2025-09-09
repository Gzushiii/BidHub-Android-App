package com.cc106.bidhub;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

public class BrowseActivity extends BaseActivity {

    private String loggedInUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inflate the browse content into the content frame
        getLayoutInflater().inflate(R.layout.activity_browse_content, findViewById(R.id.content_frame));
        
        // Get the logged-in user's email from the Intent
        loggedInUserEmail = getIntent().getStringExtra("USER_EMAIL");
        
        // TODO: Implement browse functionality
        Toast.makeText(this, "Browse Items - Coming Soon!", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected boolean isCurrentActivity(int itemId) {
        return itemId == R.id.nav_browse;
    }

    @Override
    protected void setCurrentTabSelected() {
        bottomNavigationView.setSelectedItemId(R.id.nav_browse);
    }

    @Override
    protected String getCurrentUserEmail() {
        return loggedInUserEmail;
    }
}
