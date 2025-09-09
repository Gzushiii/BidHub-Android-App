package com.cc106.bidhub;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class CreditsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credits);
        
        // TODO: Implement credits functionality
        Toast.makeText(this, "Credits Management - Coming Soon!", Toast.LENGTH_SHORT).show();
    }
}
