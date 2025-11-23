package com.cc106.bidhub.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.cc106.bidhub.R;
import com.cc106.bidhub.api.AuthApiClient;
import com.cc106.bidhub.api.ApiClient;
import com.cc106.bidhub.models.User;

public class RegisterActivity extends AppCompatActivity {
    private EditText etUsername, etEmail, etPhone, etPassword, etFirstName, etLastName, etAlias;
    private Button btnRegister;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etAlias = findViewById(R.id.etAlias);
        btnRegister = findViewById(R.id.btnRegister);
        
        btnRegister.setOnClickListener(v -> register());
    }
    
    private void register() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString();
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String alias = etAlias.getText().toString().trim();
        
        if (username.isEmpty() || email.isEmpty() || phone.isEmpty() || 
            password.isEmpty() || firstName.isEmpty() || lastName.isEmpty() || alias.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (password.length() < 8) {
            Toast.makeText(this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show();
            return;
        }
        
        btnRegister.setEnabled(false);
        btnRegister.setText("Registering...");
        
        new AsyncTask<Void, Void, User>() {
            private String errorMessage = null;
            
            @Override
            protected User doInBackground(Void... voids) {
                try {
                    AuthApiClient apiClient = new AuthApiClient(RegisterActivity.this);
                    return apiClient.register(username, email, phone, password, firstName, lastName, alias);
                } catch (ApiClient.ApiException e) {
                    errorMessage = "Registration failed: " + e.getMessage();
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
                btnRegister.setEnabled(true);
                btnRegister.setText("Register");
                
                if (user != null) {
                    Toast.makeText(RegisterActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                    finish();
                } else {
                    Toast.makeText(RegisterActivity.this, 
                        errorMessage != null ? errorMessage : "Registration failed", 
                        Toast.LENGTH_LONG).show();
                }
            }
        }.execute();
    }
}

