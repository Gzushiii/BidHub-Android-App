package com.cc106.bidhub;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.cc106.bidhub.api.AuthApiClient;
import com.cc106.bidhub.api.ApiResponse;
import com.cc106.bidhub.toast.ToastHelper;
import com.cc106.bidhub.utils.FormValidationHelper;
import com.cc106.bidhub.utils.LoadingStateHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextPassword;
    private MaterialButton buttonLogin;
    private TextView textViewRegisterLink;
    private TextInputLayout emailInputLayout, passwordInputLayout;
    private ProgressBar progressBar;
    private AuthApiClient authApiClient;
    private LoadingStateHelper loadingHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        // Setup back button handling - exit app on back press from login
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishAffinity();
            }
        });

        // Initialize UI components
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewRegisterLink = findViewById(R.id.textViewRegisterLink);
        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        progressBar = findViewById(R.id.progressBar);
        
        // Initialize helpers
        loadingHelper = new LoadingStateHelper(progressBar, buttonLogin);
        authApiClient = new AuthApiClient(this);

        // Set up input validation
        setupInputValidation();

        buttonLogin.setOnClickListener(v -> loginUser());
        textViewRegisterLink.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
                ToastHelper.showError(this, "Error opening registration: " + e.getMessage());
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
     * Validate email format using helper
     */
    private boolean validateEmail() {
        String email = editTextEmail.getText().toString();
        return FormValidationHelper.validateEmail(email, emailInputLayout);
    }

    /**
     * Validate password using helper
     */
    private boolean validatePassword() {
        String password = editTextPassword.getText().toString();
        return FormValidationHelper.validatePassword(password, passwordInputLayout, 6);
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

        // Show loading state with initial message
        loadingHelper.setLoading(true, "Connecting to server...");

        // Use backend API for authentication with timeout handling
        new Thread(() -> {
            try {
                // Update loading message
                runOnUiThread(() -> loadingHelper.setLoading(true, "Signing In..."));
                
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
                        // Login failed - check if it's a timeout/connection error
                        String errorMsg = response.getMessage();
                        boolean isConnectionError = errorMsg != null && 
                            (errorMsg.contains("timeout") || 
                             errorMsg.contains("Connection") || 
                             errorMsg.contains("Network") ||
                             errorMsg.contains("Unable to connect"));
                        
                        if (isConnectionError) {
                            // Don't show invalid credentials for connection errors
                            FormValidationHelper.clearError(emailInputLayout);
                            FormValidationHelper.clearError(passwordInputLayout);
                        } else {
                            // Invalid credentials
                            FormValidationHelper.setError(emailInputLayout, "Invalid credentials");
                            FormValidationHelper.setError(passwordInputLayout, "Invalid credentials");
                        }
                        
                        ToastHelper.showError(this, errorMsg != null ? errorMsg : "Login failed. Please try again.");
                    }
                    loadingHelper.setLoading(false);
                });
                
            } catch (Exception e) {
                android.util.Log.e("LoginActivity", "Login exception: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    String errorMsg = "Login failed. Please try again.";
                    if (e.getMessage() != null && e.getMessage().contains("timeout")) {
                        errorMsg = "Connection timed out. The server may be starting up. Please try again.";
                    }
                    ToastHelper.showError(this, errorMsg);
                    loadingHelper.setLoading(false);
                });
            }
        }).start();
    }
}

