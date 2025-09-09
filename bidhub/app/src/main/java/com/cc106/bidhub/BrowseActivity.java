package com.cc106.bidhub;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class BrowseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse);
        
        // TODO: Implement browse functionality
        Toast.makeText(this, "Browse Items - Coming Soon!", Toast.LENGTH_SHORT).show();
    }
}
