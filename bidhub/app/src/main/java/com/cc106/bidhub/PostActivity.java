package com.cc106.bidhub;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class PostActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);
        
        // TODO: Implement post item functionality
        Toast.makeText(this, "Post Item - Coming Soon!", Toast.LENGTH_SHORT).show();
    }
}
