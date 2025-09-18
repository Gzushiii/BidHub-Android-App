package com.cc106.bidhub;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cc106.bidhub.toast.ToastHelper;

import java.util.Random;

public class PasswordRecoveryVerificationActivity extends AppCompatActivity {

    private EditText editTextVerificationCode;
    private Button buttonVerify;
    private TextView textViewVerificationInfo, textViewResendCode;
    private DatabaseHelper dbHelper;
    
    private String contact;
    private boolean isEmail;
    private CountDownTimer resendTimer;
    private static final int RESEND_COOLDOWN = 60000; // 60 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_recovery_verification);

        dbHelper = new DatabaseHelper(this);
        
        // Get data from previous activity
        contact = getIntent().getStringExtra("CONTACT");
        isEmail = getIntent().getBooleanExtra("IS_EMAIL", true);
        
        initializeViews();
        setupListeners();
        updateVerificationInfo();
    }

    private void initializeViews() {
        editTextVerificationCode = findViewById(R.id.editTextVerificationCode);
        buttonVerify = findViewById(R.id.buttonVerify);
        textViewVerificationInfo = findViewById(R.id.textViewVerificationInfo);
        textViewResendCode = findViewById(R.id.textViewResendCode);
    }

    private void setupListeners() {
        // Back button
        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());

        // Verify button
        buttonVerify.setOnClickListener(v -> verifyCode());

        // Resend code
        textViewResendCode.setOnClickListener(v -> {
            if (resendTimer == null) {
                resendVerificationCode();
            }
        });

        // Auto-verify when 6 digits are entered
        editTextVerificationCode.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (s.length() == 6) {
                    verifyCode();
                }
            }
        });
    }

    private void updateVerificationInfo() {
        String method = isEmail ? "email" : "phone";
        String maskedContact = maskContact(contact);
        textViewVerificationInfo.setText("We sent a 6-digit code to your " + method + "\n" + maskedContact);
    }

    private String maskContact(String contact) {
        if (isEmail) {
            String[] parts = contact.split("@");
            if (parts.length == 2) {
                String username = parts[0];
                String domain = parts[1];
                if (username.length() > 2) {
                    return username.substring(0, 2) + "***@" + domain;
                } else {
                    return "***@" + domain;
                }
            }
        } else {
            if (contact.length() > 4) {
                return "***" + contact.substring(contact.length() - 4);
            }
        }
        return "***";
    }

    private void verifyCode() {
        String code = editTextVerificationCode.getText().toString().trim();
        
        if (TextUtils.isEmpty(code)) {
            ToastHelper.showWarning(this, "Please enter the verification code");
            return;
        }

        if (code.length() != 6) {
            ToastHelper.showError(this, "Please enter a valid 6-digit code");
            return;
        }

        if (verifyCodeInDatabase(code)) {
            // Code is valid, proceed to password reset
            Intent intent = new Intent(this, PasswordResetActivity.class);
            intent.putExtra("CONTACT", contact);
            intent.putExtra("IS_EMAIL", isEmail);
            startActivity(intent);
            finish();
        } else {
            ToastHelper.showError(this, "Invalid verification code. Please try again.");
        }
    }

    private boolean verifyCodeInDatabase(String code) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] columns = {DatabaseHelper.COLUMN_RECOVERY_ID};
        String selection = (isEmail ? DatabaseHelper.COLUMN_RECOVERY_EMAIL : DatabaseHelper.COLUMN_RECOVERY_PHONE) + " = ? AND " + 
                          DatabaseHelper.COLUMN_RECOVERY_CODE + " = ? AND " +
                          DatabaseHelper.COLUMN_RECOVERY_EXPIRES_AT + " > ?";
        String[] selectionArgs = {contact.toLowerCase(), code, String.valueOf(System.currentTimeMillis())};

        Cursor cursor = db.query(DatabaseHelper.TABLE_PASSWORD_RECOVERY, columns, selection, selectionArgs, null, null, null);
        boolean isValid = cursor != null && cursor.getCount() > 0;
        
        if (cursor != null) {
            cursor.close();
        }
        db.close();
        
        return isValid;
    }

    private void resendVerificationCode() {
        // Generate new verification code
        String verificationCode = generateVerificationCode();
        
        // Store new verification code in database
        if (storeVerificationCode(contact, verificationCode, isEmail)) {
            // Simulate sending verification code
            simulateSendVerificationCode(contact, verificationCode, isEmail);
            
            // Start resend cooldown
            startResendCooldown();
            
            ToastHelper.showSuccess(this, "New verification code sent!");
        } else {
            ToastHelper.showError(this, "Failed to send verification code. Please try again.");
        }
    }

    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // 6-digit code
        return String.valueOf(code);
    }

    private boolean storeVerificationCode(String contact, String code, boolean isEmail) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            
            // First, delete any existing verification codes for this contact
            String deleteSelection = isEmail ? 
                DatabaseHelper.COLUMN_RECOVERY_EMAIL + " = ?" : 
                DatabaseHelper.COLUMN_RECOVERY_PHONE + " = ?";
            String[] deleteArgs = {contact.toLowerCase()};
            db.delete(DatabaseHelper.TABLE_PASSWORD_RECOVERY, deleteSelection, deleteArgs);
            
            // Insert new verification code
            android.content.ContentValues values = new android.content.ContentValues();
            values.put(DatabaseHelper.COLUMN_RECOVERY_CODE, code);
            values.put(DatabaseHelper.COLUMN_RECOVERY_EXPIRES_AT, System.currentTimeMillis() + (15 * 60 * 1000)); // 15 minutes
            values.put(DatabaseHelper.COLUMN_RECOVERY_IS_EMAIL, isEmail ? 1 : 0);
            
            if (isEmail) {
                values.put(DatabaseHelper.COLUMN_RECOVERY_EMAIL, contact.toLowerCase());
            } else {
                values.put(DatabaseHelper.COLUMN_RECOVERY_PHONE, contact);
            }
            
            long result = db.insert(DatabaseHelper.TABLE_PASSWORD_RECOVERY, null, values);
            db.close();
            
            return result != -1;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void simulateSendVerificationCode(String contact, String code, boolean isEmail) {
        // Use the verification service
        VerificationService.sendVerificationCode(this, contact, code, isEmail, new VerificationService.VerificationCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    String method = isEmail ? "email" : "SMS";
                    ToastHelper.showSuccess(PasswordRecoveryVerificationActivity.this, "New verification code sent to " + method);
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    ToastHelper.showError(PasswordRecoveryVerificationActivity.this, "Failed to send verification code: " + errorMessage);
                });
            }
        });
    }

    private void startResendCooldown() {
        textViewResendCode.setEnabled(false);
        textViewResendCode.setTextColor(getResources().getColor(android.R.color.darker_gray));
        
        resendTimer = new CountDownTimer(RESEND_COOLDOWN, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int seconds = (int) (millisUntilFinished / 1000);
                textViewResendCode.setText("Resend Code (" + seconds + "s)");
            }

            @Override
            public void onFinish() {
                textViewResendCode.setEnabled(true);
                textViewResendCode.setText("Resend Code");
                textViewResendCode.setTextColor(getResources().getColor(R.color.primary_blue));
                resendTimer = null;
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (resendTimer != null) {
            resendTimer.cancel();
        }
    }
}
