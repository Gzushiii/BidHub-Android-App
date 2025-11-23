package com.cc106.bidhub.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.cc106.bidhub.R;
import com.cc106.bidhub.api.AuthApiClient;
import com.cc106.bidhub.api.ApiClient;
import com.cc106.bidhub.models.User;
import com.cc106.bidhub.utils.TokenManager;

public class LoginActivity extends AppCompatActivity {
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        // Check if already logged in
        if (TokenManager.isLoggedIn(this)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        
        btnLogin.setOnClickListener(v -> login());
        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }
    
    private void login() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        
        btnLogin.setEnabled(false);
        btnLogin.setText("Logging in...");
        
        new AsyncTask<Void, Void, User>() {
            private String errorMessage = null;
            
            @Override
            protected User doInBackground(Void... voids) {
                try {
                    AuthApiClient apiClient = new AuthApiClient(LoginActivity.this);
                    return apiClient.login(email, password);
                } catch (ApiClient.ApiException e) {
                    errorMessage = "Login failed: " + e.getMessage();
                    try {
                        org.json.JSONObject errorJson = new org.json.JSONObject(e.getResponse());
                        if (errorJson.has("error")) {
                            errorMessage = errorJson.getString("error");
                        }
                    } catch (Exception ex) {
                        // Use default error message
                    }
                    return null;
                } catch (Exception e) {
                    errorMessage = "Network error. Please check your connection.";
                    return null;
                }
            }
            
            @Override
            protected void onPostExecute(User user) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Login");
                
                if (user != null) {
                    Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, 
                        errorMessage != null ? errorMessage : "Login failed", 
                        Toast.LENGTH_LONG).show();
                }
            }
        }.execute();
    }
}

