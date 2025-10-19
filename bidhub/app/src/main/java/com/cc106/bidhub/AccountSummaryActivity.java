package com.cc106.bidhub;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import androidx.appcompat.app.AppCompatActivity;

public class AccountSummaryActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private TextView tvWelcome, tvAlias, tvCreditBalance;
    private ToggleButton toggleEmailNotifications, togglePushNotifications;
    private Button btnStartBidding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_summary);
        
        initializeViews();
        setupClickListeners();
        populateUserData();
    }

    private void initializeViews() {
        progressBar = findViewById(R.id.progress_bar);
        tvWelcome = findViewById(R.id.tv_welcome);
        tvAlias = findViewById(R.id.tv_alias);
        tvCreditBalance = findViewById(R.id.tv_credit_balance);
        toggleEmailNotifications = findViewById(R.id.toggle_email_notifications);
        togglePushNotifications = findViewById(R.id.toggle_push_notifications);
        btnStartBidding = findViewById(R.id.btn_start_bidding);
    }

    private void setupClickListeners() {
        btnStartBidding.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to main activity
                Intent intent = new Intent(AccountSummaryActivity.this, MainActivity.class);
                intent.putExtra("USER_EMAIL", getIntent().getStringExtra("EMAIL"));
                startActivity(intent);
                finish();
            }
        });
    }

    private void populateUserData() {
        // Get user data from intent
        String firstName = getIntent().getStringExtra("FIRST_NAME");
        String email = getIntent().getStringExtra("EMAIL");
        
        if (firstName != null) {
            tvWelcome.setText("Welcome to BidHub, " + firstName + "!");
        }
        
        if (email != null) {
            // Generate alias from email
            String alias = "@" + email.split("@")[0];
            tvAlias.setText(alias);
        }
        
        // Set initial credit balance
        tvCreditBalance.setText("$0.00");
        
        // Set progress to 100% (step 4 of 4)
        progressBar.setProgress(100);
    }
}
