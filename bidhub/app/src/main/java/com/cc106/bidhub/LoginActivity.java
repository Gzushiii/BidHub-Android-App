package com.cc106.bidhub;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.cc106.bidhub.api.AuthApiClient;
import com.cc106.bidhub.api.ApiResponse;
import com.cc106.bidhub.toast.ToastHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextPassword;
    private MaterialButton buttonLogin;
    private TextView textViewRegisterLink;
    private TextInputLayout emailInputLayout, passwordInputLayout;
    private ProgressBar progressBar;
    private DatabaseHelper dbHelper;
    private AuthApiClient authApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        dbHelper = new DatabaseHelper(this);

        // Initialize UI components
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewRegisterLink = findViewById(R.id.textViewRegisterLink);
        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        progressBar = findViewById(R.id.progressBar);
        
        // Initialize API client
        authApiClient = new AuthApiClient(this);

        // Set up input validation
        setupInputValidation();

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        textViewRegisterLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Intent to open RegisterActivity
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }

    /**
     * Set up real-time input validation
     */
    private void setupInputValidation() {
        editTextEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validateEmail();
            }
        });

        editTextPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validatePassword();
            }
        });
    }

    /**
     * Validate email format
     */
    private boolean validateEmail() {
        String email = editTextEmail.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            emailInputLayout.setError("Email is required");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.setError("Please enter a valid email address");
            return false;
        } else {
            emailInputLayout.setError(null);
            return true;
        }
    }

    /**
     * Validate password
     */
    private boolean validatePassword() {
        String password = editTextPassword.getText().toString().trim();
        if (TextUtils.isEmpty(password)) {
            passwordInputLayout.setError("Password is required");
            return false;
        } else if (password.length() < 6) {
            passwordInputLayout.setError("Password must be at least 6 characters");
            return false;
        } else {
            passwordInputLayout.setError(null);
            return true;
        }
    }

    /**
     * Show loading state
     */
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        buttonLogin.setEnabled(!show);
        buttonLogin.setText(show ? "Signing In..." : "Sign In");
    }

    private void loginUser() {
        // Validate inputs first
        boolean isEmailValid = validateEmail();
        boolean isPasswordValid = validatePassword();
        
        if (!isEmailValid || !isPasswordValid) {
            return;
        }

        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // Show loading state
        showLoading(true);

        // Use backend API for authentication
        new Thread(() -> {
            try {
                ApiResponse response = authApiClient.login(email, password);
                
                // Run UI updates on main thread
                runOnUiThread(() -> {
                    if (response.isSuccess()) {
                        ToastHelper.showSuccess(this, "Login Successful!");
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.putExtra("USER_EMAIL", email);
                        startActivity(intent);
                        finish();
                    } else {
                        // Login failed
                        emailInputLayout.setError("Invalid credentials");
                        passwordInputLayout.setError("Invalid credentials");
                        ToastHelper.showError(this, response.getMessage());
                    }
                    showLoading(false);
                });
                
            } catch (Exception e) {
                runOnUiThread(() -> {
                    ToastHelper.showError(this, "Login failed. Please try again.");
                    showLoading(false);
                });
            }
        }).start();
    }
}

