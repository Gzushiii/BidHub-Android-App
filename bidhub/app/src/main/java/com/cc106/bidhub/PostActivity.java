package com.cc106.bidhub;

import android.os.Bundle;
import android.widget.Toast;

public class PostActivity extends BaseActivity {

    private String loggedInUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inflate the post content into the content frame
        getLayoutInflater().inflate(R.layout.activity_post_content, findViewById(R.id.content_frame));
        
        // Get the logged-in user's email from the Intent
        loggedInUserEmail = getIntent().getStringExtra("USER_EMAIL");
        
        // TODO: Implement post item functionality
        Toast.makeText(this, "Post Item - Coming Soon!", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected boolean isCurrentActivity(int itemId) {
        return itemId == R.id.nav_post;
    }

    @Override
    protected void setCurrentTabSelected() {
        bottomNavigationView.setSelectedItemId(R.id.nav_post);
    }

    @Override
    protected String getCurrentUserEmail() {
        return loggedInUserEmail;
    }
}
