package com.cc106.bidhub;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

public class PostActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);
        
        // Inflate post content into the base layout
        View postContent = LayoutInflater.from(this).inflate(R.layout.activity_post, null);
        ((android.widget.FrameLayout) findViewById(R.id.content_frame)).addView(postContent);
        
        // Set selected navigation item
        setSelectedNavItem(R.id.nav_post);
        
        // TODO: Implement post item functionality
        Toast.makeText(this, "Post Item - Coming Soon!", Toast.LENGTH_SHORT).show();
    }
}
