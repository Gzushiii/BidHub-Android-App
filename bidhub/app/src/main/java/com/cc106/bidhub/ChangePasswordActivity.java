package com.cc106.bidhub;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.cc106.bidhub.toast.ToastHelper;
import com.cc106.bidhub.utils.PasswordUtils;

public class ChangePasswordActivity extends BaseActivity {

    private EditText editTextCurrentPassword, editTextNewPassword, editTextConfirmPassword;
    private Button buttonChangePassword, buttonCancel;
    private TextView textViewPasswordStrength;
    private DatabaseHelper dbHelper;
    private String loggedInUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inflate the change password content into the content frame
        getLayoutInflater().inflate(R.layout.activity_change_password_content, findViewById(R.id.content_frame));
        
        // Get the logged-in user's email from the Intent
        loggedInUserEmail = getIntent().getStringExtra("USER_EMAIL");
        
        dbHelper = new DatabaseHelper(this);
        
        // Animate content in after inflation
        animateContentIn();
        
        // Initialize Views
        initializeViews();
        
        // Set up click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        editTextCurrentPassword = findViewById(R.id.editTextCurrentPassword);
        editTextNewPassword = findViewById(R.id.editTextNewPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        buttonChangePassword = findViewById(R.id.buttonChangePassword);
        buttonCancel = findViewById(R.id.buttonCancel);
        textViewPasswordStrength = findViewById(R.id.textViewPasswordStrength);
    }

    private void setupClickListeners() {
        buttonChangePassword.setOnClickListener(v -> changePassword());
        buttonCancel.setOnClickListener(v -> finish());
        
        // Add password strength monitoring
        editTextNewPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                updatePasswordStrength();
            }
        });
    }

    private void changePassword() {
        // Validate input
        if (!validateInput()) {
            return;
        }

        String currentPassword = editTextCurrentPassword.getText().toString();
        String newPassword = editTextNewPassword.getText().toString();

        // Verify current password
        if (!verifyCurrentPassword(currentPassword)) {
            editTextCurrentPassword.setError("Current password is incorrect");
            return;
        }

        // Update password in database
        if (updatePassword(newPassword)) {
            ToastHelper.showSuccess(this, "Password changed successfully!");
            finish();
        } else {
            ToastHelper.showError(this, "Failed to change password");
        }
    }

    private boolean validateInput() {
        boolean isValid = true;

        String currentPassword = editTextCurrentPassword.getText().toString();
        String newPassword = editTextNewPassword.getText().toString();
        String confirmPassword = editTextConfirmPassword.getText().toString();

        // Validate current password
        if (TextUtils.isEmpty(currentPassword)) {
            editTextCurrentPassword.setError("Current password is required");
            isValid = false;
        }

        // Validate new password
        if (TextUtils.isEmpty(newPassword)) {
            editTextNewPassword.setError("New password is required");
            isValid = false;
        } else if (newPassword.length() < 8) {
            editTextNewPassword.setError("Password must be at least 8 characters");
            isValid = false;
        } else if (!PasswordUtils.isStrongPassword(newPassword)) {
            editTextNewPassword.setError("Password must contain uppercase, lowercase, number, and special character");
            isValid = false;
        }

        // Validate confirm password
        if (TextUtils.isEmpty(confirmPassword)) {
            editTextConfirmPassword.setError("Please confirm your password");
            isValid = false;
        } else if (!newPassword.equals(confirmPassword)) {
            editTextConfirmPassword.setError("Passwords do not match");
            isValid = false;
        }

        // Check if new password is different from current
        if (currentPassword.equals(newPassword)) {
            editTextNewPassword.setError("New password must be different from current password");
            isValid = false;
        }

        return isValid;
    }

    private boolean verifyCurrentPassword(String password) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
            DatabaseHelper.TABLE_USERS,
            new String[]{DatabaseHelper.COLUMN_USER_PASSWORD, DatabaseHelper.COLUMN_USER_SALT},
            DatabaseHelper.COLUMN_USER_EMAIL + " = ?",
            new String[]{loggedInUserEmail},
            null, null, null
        );

        boolean isValid = false;
        if (cursor != null && cursor.moveToFirst()) {
            byte[] storedPassword = cursor.getBlob(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_PASSWORD));
            byte[] salt = cursor.getBlob(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_SALT));
            
            isValid = PasswordUtils.verifyPassword(password, storedPassword, salt);
            cursor.close();
        }
        db.close();
        return isValid;
    }

    private boolean updatePassword(String newPassword) {
        try {
            // Generate new salt and hash
            byte[] salt = PasswordUtils.generateSalt();
            byte[] hashedPassword = PasswordUtils.hashPassword(newPassword, salt);

            // Update database
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            android.content.ContentValues values = new android.content.ContentValues();
            values.put(DatabaseHelper.COLUMN_USER_PASSWORD, hashedPassword);
            values.put(DatabaseHelper.COLUMN_USER_SALT, salt);

            int rowsAffected = db.update(
                DatabaseHelper.TABLE_USERS,
                values,
                DatabaseHelper.COLUMN_USER_EMAIL + " = ?",
                new String[]{loggedInUserEmail}
            );

            db.close();
            return rowsAffected > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void updatePasswordStrength() {
        String password = editTextNewPassword.getText().toString();
        if (TextUtils.isEmpty(password)) {
            textViewPasswordStrength.setVisibility(View.GONE);
            return;
        }

        textViewPasswordStrength.setVisibility(View.VISIBLE);
        
        int strength = PasswordUtils.calculatePasswordStrength(password);
        switch (strength) {
            case 1:
                textViewPasswordStrength.setText("Weak");
                textViewPasswordStrength.setTextColor(getResources().getColor(R.color.password_weak));
                break;
            case 2:
                textViewPasswordStrength.setText("Fair");
                textViewPasswordStrength.setTextColor(getResources().getColor(R.color.password_fair));
                break;
            case 3:
                textViewPasswordStrength.setText("Good");
                textViewPasswordStrength.setTextColor(getResources().getColor(R.color.password_good));
                break;
            case 4:
                textViewPasswordStrength.setText("Strong");
                textViewPasswordStrength.setTextColor(getResources().getColor(R.color.password_strong));
                break;
            default:
                textViewPasswordStrength.setText("Very Weak");
                textViewPasswordStrength.setTextColor(getResources().getColor(R.color.password_weak));
                break;
        }
    }

    @Override
    protected boolean isCurrentActivity(int itemId) {
        return false; // This is not a main navigation activity
    }

    @Override
    protected void setCurrentTabSelected() {
        // No tab selection for this activity
    }

    @Override
    public String getCurrentUserEmail() {
        return loggedInUserEmail;
    }
}
