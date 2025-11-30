package com.cc106.bidhub;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.cc106.bidhub.adapters.FullScreenImageAdapter;
import com.cc106.bidhub.utils.ImageLoader;
import java.util.ArrayList;
import java.util.List;

public class FullScreenImageActivity extends AppCompatActivity {
    
    private ViewPager2 viewPager;
    private ImageButton btnClose;
    private FullScreenImageAdapter adapter;
    private List<String> images;
    private int initialPosition;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Hide system UI for immersive experience
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        
        setContentView(R.layout.activity_fullscreen_image);
        
        // Get images and initial position from intent
        images = getIntent().getStringArrayListExtra("IMAGES");
        initialPosition = getIntent().getIntExtra("POSITION", 0);
        
        if (images == null || images.isEmpty()) {
            finish();
            return;
        }
        
        initializeViews();
        setupViewPager();
    }
    
    private void initializeViews() {
        viewPager = findViewById(R.id.viewpager_fullscreen);
        btnClose = findViewById(R.id.btn_close_fullscreen);
        
        btnClose.setOnClickListener(v -> finish());
    }
    
    private void setupViewPager() {
        adapter = new FullScreenImageAdapter(images);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(initialPosition, false);
        
        // Enable zoom functionality through PhotoView in adapter
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Re-hide system UI
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
}

