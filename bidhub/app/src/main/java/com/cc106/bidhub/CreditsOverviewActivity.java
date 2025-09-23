package com.cc106.bidhub;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.cc106.bidhub.toast.ToastHelper;

public class CreditsOverviewActivity extends AppCompatActivity {

    private TextView tvCreditsBalance;
    private Button btnStarter, btnPopular, btnPremium, btnBuyCredits, btnRedeem;
    private EditText etRedeemCode;
    private RadioButton rbGcash, rbMaya, rbBank;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credits_overview);
        
        initializeViews();
        setupClickListeners();
        populateData();
    }

    private void initializeViews() {
        tvCreditsBalance = findViewById(R.id.tv_credits_balance);
        btnStarter = findViewById(R.id.btn_starter);
        btnPopular = findViewById(R.id.btn_popular);
        btnPremium = findViewById(R.id.btn_premium);
        btnBuyCredits = findViewById(R.id.btn_buy_credits);
        btnRedeem = findViewById(R.id.btn_redeem);
        etRedeemCode = findViewById(R.id.et_redeem_code);
        rbGcash = findViewById(R.id.rb_gcash);
        rbMaya = findViewById(R.id.rb_maya);
        rbBank = findViewById(R.id.rb_bank);
    }

    private void setupClickListeners() {
        btnStarter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectPackage("starter");
            }
        });

        btnPopular.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectPackage("popular");
            }
        });

        btnPremium.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectPackage("premium");
            }
        });

        btnBuyCredits.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to payment confirmation
                Intent intent = new Intent(CreditsOverviewActivity.this, PaymentConfirmationActivity.class);
                startActivity(intent);
            }
        });

        btnRedeem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String code = etRedeemCode.getText().toString().trim();
                if (code.isEmpty()) {
                    etRedeemCode.setError("Please enter a redemption code");
                    return;
                }
                // TODO: Implement redemption logic
                ToastHelper.showInfo(CreditsOverviewActivity.this, "Code redeemed successfully!");
                etRedeemCode.setText("");
            }
        });
    }

    private void populateData() {
        // Set current credits balance
        tvCreditsBalance.setText("100");
        
        // Set default payment method
        rbGcash.setChecked(true);
    }

    private void selectPackage(String packageType) {
        // Reset all button states
        btnStarter.setSelected(false);
        btnPopular.setSelected(false);
        btnPremium.setSelected(false);
        
        // Select the clicked package
        switch (packageType) {
            case "starter":
                btnStarter.setSelected(true);
                break;
            case "popular":
                btnPopular.setSelected(true);
                break;
            case "premium":
                btnPremium.setSelected(true);
                break;
        }
    }
}
