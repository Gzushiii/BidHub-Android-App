package com.cc106.bidhub;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        
        // TODO: Implement profile functionality
        Toast.makeText(this, "Profile Management - Coming Soon!", Toast.LENGTH_SHORT).show();
    }
}
