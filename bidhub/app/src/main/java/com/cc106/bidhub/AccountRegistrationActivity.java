package com.cc106.bidhub;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.cc106.bidhub.toast.ToastHelper;

public class AccountRegistrationActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName, etEmail, etPhone, etPassword;
    private Button btnCreateAccount;
    private TextView tvTermsLink, tvPrivacyLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_registration);
        
        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        etFirstName = findViewById(R.id.et_first_name);
        etLastName = findViewById(R.id.et_last_name);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        etPassword = findViewById(R.id.et_password);
        btnCreateAccount = findViewById(R.id.btn_create_account);
        tvTermsLink = findViewById(R.id.tv_terms_link);
        tvPrivacyLink = findViewById(R.id.tv_privacy_link);
    }

    private void setupClickListeners() {
        btnCreateAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateForm()) {
                    // Navigate to account summary
                    Intent intent = new Intent(AccountRegistrationActivity.this, AccountSummaryActivity.class);
                    intent.putExtra("FIRST_NAME", etFirstName.getText().toString().trim());
                    intent.putExtra("LAST_NAME", etLastName.getText().toString().trim());
                    intent.putExtra("EMAIL", etEmail.getText().toString().trim());
                    intent.putExtra("PHONE", etPhone.getText().toString().trim());
                    startActivity(intent);
                }
            }
        });

        tvTermsLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Open terms of service
                ToastHelper.showInfo(AccountRegistrationActivity.this, "Terms of Service - Coming Soon!");
            }
        });

        tvPrivacyLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Open privacy policy
                ToastHelper.showInfo(AccountRegistrationActivity.this, "Privacy Policy - Coming Soon!");
            }
        });
    }

    private boolean validateForm() {
        boolean isValid = true;

        // Validate first name
        if (TextUtils.isEmpty(etFirstName.getText().toString().trim())) {
            etFirstName.setError("First name is required");
            isValid = false;
        }

        // Validate last name
        if (TextUtils.isEmpty(etLastName.getText().toString().trim())) {
            etLastName.setError("Last name is required");
            isValid = false;
        }

        // Validate email
        String email = etEmail.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email address");
            isValid = false;
        }

        // Validate phone
        if (TextUtils.isEmpty(etPhone.getText().toString().trim())) {
            etPhone.setError("Phone number is required");
            isValid = false;
        }

        // Validate password
        String password = etPassword.getText().toString().trim();
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            isValid = false;
        } else if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            isValid = false;
        }

        return isValid;
    }
}
