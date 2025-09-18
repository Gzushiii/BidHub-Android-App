package com.cc106.bidhub;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.cc106.bidhub.toast.ToastHelper;

/**
 * Test activity to demonstrate password recovery flow
 * This can be removed in production
 */
public class PasswordRecoveryTestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_recovery_test);

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        // Views are already initialized in the layout
    }

    private void setupListeners() {
        // Test email recovery
        findViewById(R.id.buttonTestEmailRecovery).setOnClickListener(v -> {
            Intent intent = new Intent(this, PasswordRecoveryRequestActivity.class);
            startActivity(intent);
        });

        // Test SMS recovery
        findViewById(R.id.buttonTestSMSRecovery).setOnClickListener(v -> {
            Intent intent = new Intent(this, PasswordRecoveryRequestActivity.class);
            startActivity(intent);
        });

        // Back to login
        findViewById(R.id.buttonBackToLogin).setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }
}

