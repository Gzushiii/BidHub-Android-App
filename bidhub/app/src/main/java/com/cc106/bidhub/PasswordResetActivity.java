package com.cc106.bidhub;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.cc106.bidhub.toast.ToastHelper;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Map;

public class PasswordResetActivity extends AppCompatActivity {

    private TextInputLayout inputLayoutNewPassword, inputLayoutConfirmPassword;
    private EditText editTextNewPassword, editTextConfirmPassword;
    private Button buttonResetPassword;
    private PasswordStrengthIndicator passwordStrengthIndicator;
    private DatabaseHelper dbHelper;
    
    private String contact;
    private boolean isEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_reset);

        dbHelper = new DatabaseHelper(this);
        
        // Get data from previous activity
        contact = getIntent().getStringExtra("CONTACT");
        isEmail = getIntent().getBooleanExtra("IS_EMAIL", true);
        
        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        inputLayoutNewPassword = findViewById(R.id.inputLayoutNewPassword);
        inputLayoutConfirmPassword = findViewById(R.id.inputLayoutConfirmPassword);
        editTextNewPassword = findViewById(R.id.editTextNewPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        buttonResetPassword = findViewById(R.id.buttonResetPassword);
        passwordStrengthIndicator = findViewById(R.id.passwordStrengthIndicator);
    }

    private void setupListeners() {
        // Back button
        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());

        // Password strength monitoring
        editTextNewPassword.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                updatePasswordStrength(s.toString());
                validatePasswords();
            }
        });

        // Confirm password monitoring
        editTextConfirmPassword.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                validatePasswords();
            }
        });

        // Reset password button
        buttonResetPassword.setOnClickListener(v -> resetPassword());
    }

    private void updatePasswordStrength(String password) {
        if (passwordStrengthIndicator != null) {
            ValidationUtils.PasswordStrengthResult result = ValidationUtils.validatePassword(password);
            passwordStrengthIndicator.updateStrength(result);
        }
    }

    private void validatePasswords() {
        String newPassword = editTextNewPassword.getText().toString();
        String confirmPassword = editTextConfirmPassword.getText().toString();
        
        // Clear previous errors
        inputLayoutNewPassword.setError(null);
        inputLayoutConfirmPassword.setError(null);
        
        // Validate new password
        if (!TextUtils.isEmpty(newPassword) && !isValidPassword(newPassword)) {
            inputLayoutNewPassword.setError("Password must be at least 8 characters with letters, numbers, and special characters");
            buttonResetPassword.setEnabled(false);
            return;
        }
        
        // Validate password confirmation
        if (!TextUtils.isEmpty(confirmPassword) && !newPassword.equals(confirmPassword)) {
            inputLayoutConfirmPassword.setError("Passwords do not match");
            buttonResetPassword.setEnabled(false);
            return;
        }
        
        // Enable button if both passwords are valid and match
        buttonResetPassword.setEnabled(!TextUtils.isEmpty(newPassword) && 
                                     !TextUtils.isEmpty(confirmPassword) && 
                                     newPassword.equals(confirmPassword) && 
                                     isValidPassword(newPassword));
    }

    private boolean isValidPassword(String password) {
        if (password.length() < 8) {
            return false;
        }
        
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecial = password.matches(".*[!@#$%^&*(),.?\":{}|<>].*");
        
        return hasLetter && hasDigit && hasSpecial;
    }

    private void resetPassword() {
        String newPassword = editTextNewPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();
        
        if (TextUtils.isEmpty(newPassword)) {
            ToastHelper.showWarning(this, "Please enter a new password");
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            ToastHelper.showWarning(this, "Please confirm your password");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            ToastHelper.showError(this, "Passwords do not match");
            return;
        }

        if (!isValidPassword(newPassword)) {
            ToastHelper.showError(this, "Password does not meet requirements");
            return;
        }

        // Update password in database
        if (updateUserPassword(contact, newPassword, isEmail)) {
            // Clear verification codes for this contact
            clearVerificationCodes(contact, isEmail);
            
            ToastHelper.showSuccess(this, "Password reset successfully!");
            
            // Navigate back to login
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            ToastHelper.showError(this, "Failed to reset password. Please try again.");
        }
    }

    private boolean updateUserPassword(String contact, String newPassword, boolean isEmail) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            
            // Hash the new password
            Map<String, byte[]> hashingResult = PasswordHasher.hashPassword(newPassword);
            byte[] hashedPassword = hashingResult.get("hash");
            byte[] salt = hashingResult.get("salt");
            
            // Update password in users table
            android.content.ContentValues values = new android.content.ContentValues();
            values.put(DatabaseHelper.COLUMN_USER_PASSWORD, hashedPassword);
            values.put(DatabaseHelper.COLUMN_USER_SALT, salt);
            
            String whereClause = isEmail ? 
                DatabaseHelper.COLUMN_USER_EMAIL + " = ?" : 
                DatabaseHelper.COLUMN_USER_PHONE + " = ?";
            String[] whereArgs = {contact.toLowerCase()};
            
            int rowsAffected = db.update(DatabaseHelper.TABLE_USERS, values, whereClause, whereArgs);
            db.close();
            
            return rowsAffected > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void clearVerificationCodes(String contact, boolean isEmail) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            String whereClause = isEmail ? 
                DatabaseHelper.COLUMN_RECOVERY_EMAIL + " = ?" : 
                DatabaseHelper.COLUMN_RECOVERY_PHONE + " = ?";
            String[] whereArgs = {contact.toLowerCase()};
            db.delete(DatabaseHelper.TABLE_PASSWORD_RECOVERY, whereClause, whereArgs);
            db.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
