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
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
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

        // Show loading state
        loadingHelper.setLoading(true, "Signing In...");

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
                        FormValidationHelper.setError(emailInputLayout, "Invalid credentials");
                        FormValidationHelper.setError(passwordInputLayout, "Invalid credentials");
                        ToastHelper.showError(this, response.getMessage());
                    }
                    loadingHelper.setLoading(false);
                });
                
            } catch (Exception e) {
                runOnUiThread(() -> {
                    ToastHelper.showError(this, "Login failed. Please try again.");
                    loadingHelper.setLoading(false);
                });
            }
        }).start();
    }
}

