package com.cc106.bidhub;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class PaymentConfirmationActivity extends AppCompatActivity {

    private TextView tvCreditsPurchased, tvAmountPaid, tvPaymentMethod, tvTransactionId;
    private Button btnContinueBidding, btnViewHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_confirmation);
        
        initializeViews();
        setupClickListeners();
        populateData();
    }

    private void initializeViews() {
        tvCreditsPurchased = findViewById(R.id.tv_credits_purchased);
        tvAmountPaid = findViewById(R.id.tv_amount_paid);
        tvPaymentMethod = findViewById(R.id.tv_payment_method);
        tvTransactionId = findViewById(R.id.tv_transaction_id);
        btnContinueBidding = findViewById(R.id.btn_continue_bidding);
        btnViewHistory = findViewById(R.id.btn_view_history);
    }

    private void setupClickListeners() {
        btnContinueBidding.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate back to main activity
                Intent intent = new Intent(PaymentConfirmationActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        });

        btnViewHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Navigate to transaction history
                // For now, just show a toast
                android.widget.Toast.makeText(PaymentConfirmationActivity.this, "Transaction History - Coming Soon!", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateData() {
        // Set sample transaction data
        tvCreditsPurchased.setText("100 Credits");
        tvAmountPaid.setText("$10.00");
        tvPaymentMethod.setText("Credit Card");
        tvTransactionId.setText("#1234567890");
    }
}
