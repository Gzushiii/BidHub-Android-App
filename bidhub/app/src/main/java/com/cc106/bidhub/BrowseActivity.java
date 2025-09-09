package com.cc106.bidhub;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

public class BrowseActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);
        
        // Inflate browse content into the base layout
        View browseContent = LayoutInflater.from(this).inflate(R.layout.activity_browse, null);
        ((android.widget.FrameLayout) findViewById(R.id.content_frame)).addView(browseContent);
        
        // Set selected navigation item
        setSelectedNavItem(R.id.nav_browse);
        
        // TODO: Implement browse functionality
        Toast.makeText(this, "Browse Items - Coming Soon!", Toast.LENGTH_SHORT).show();
    }
}
