package com.cc106.bidhub;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cc106.bidhub.toast.ToastHelper;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Random;

public class PasswordRecoveryRequestActivity extends AppCompatActivity {

    private RadioGroup radioGroupRecoveryMethod;
    private RadioButton radioEmail, radioSMS;
    private TextInputLayout inputLayoutRecoveryContact;
    private EditText editTextRecoveryContact;
    private Button buttonSendCode;
    private TextView textViewResendCode;
    private DatabaseHelper dbHelper;
    
    private boolean isEmailMethod = true;
    private CountDownTimer resendTimer;
    private static final int RESEND_COOLDOWN = 60000; // 60 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_recovery_request);

        dbHelper = new DatabaseHelper(this);
        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        radioGroupRecoveryMethod = findViewById(R.id.radioGroupRecoveryMethod);
        radioEmail = findViewById(R.id.radioEmail);
        radioSMS = findViewById(R.id.radioSMS);
        inputLayoutRecoveryContact = findViewById(R.id.inputLayoutRecoveryContact);
        editTextRecoveryContact = findViewById(R.id.editTextRecoveryContact);
        buttonSendCode = findViewById(R.id.buttonSendCode);
        textViewResendCode = findViewById(R.id.textViewResendCode);
    }

    private void setupListeners() {
        // Back button
        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());

        // Recovery method selection
        radioGroupRecoveryMethod.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioEmail) {
                isEmailMethod = true;
                inputLayoutRecoveryContact.setHint("Email Address");
                inputLayoutRecoveryContact.setStartIconDrawable(getResources().getDrawable(android.R.drawable.ic_dialog_email));
                editTextRecoveryContact.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                editTextRecoveryContact.setText("");
            } else if (checkedId == R.id.radioSMS) {
                isEmailMethod = false;
                inputLayoutRecoveryContact.setHint("Phone Number");
                inputLayoutRecoveryContact.setStartIconDrawable(getResources().getDrawable(android.R.drawable.ic_menu_call));
                editTextRecoveryContact.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
                editTextRecoveryContact.setText("");
            }
        });

        // Send code button
        buttonSendCode.setOnClickListener(v -> sendVerificationCode());

        // Resend code
        textViewResendCode.setOnClickListener(v -> {
            if (resendTimer == null) {
                sendVerificationCode();
            }
        });
    }

    private void sendVerificationCode() {
        String contact = editTextRecoveryContact.getText().toString().trim();
        
        if (TextUtils.isEmpty(contact)) {
            ToastHelper.showWarning(this, "Please enter your " + (isEmailMethod ? "email" : "phone number"));
            return;
        }

        if (isEmailMethod && !isValidEmail(contact)) {
            ToastHelper.showError(this, "Please enter a valid email address");
            return;
        }

        if (!isEmailMethod && !isValidPhone(contact)) {
            ToastHelper.showError(this, "Please enter a valid phone number");
            return;
        }

        // Check if user exists
        if (!userExists(contact, isEmailMethod)) {
            ToastHelper.showError(this, "No account found with this " + (isEmailMethod ? "email" : "phone number"));
            return;
        }

        // Generate verification code
        String verificationCode = generateVerificationCode();
        
        // Store verification code in database
        if (storeVerificationCode(contact, verificationCode, isEmailMethod)) {
            // Simulate sending verification code
            simulateSendVerificationCode(contact, verificationCode, isEmailMethod);
            
            // Start resend cooldown
            startResendCooldown();
            
            // Navigate to verification screen
            Intent intent = new Intent(this, PasswordRecoveryVerificationActivity.class);
            intent.putExtra("CONTACT", contact);
            intent.putExtra("IS_EMAIL", isEmailMethod);
            startActivity(intent);
        } else {
            ToastHelper.showError(this, "Failed to send verification code. Please try again.");
        }
    }

    private boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isValidPhone(String phone) {
        // Basic phone validation - can be enhanced for specific formats
        return phone.matches("^[+]?[0-9]{10,15}$");
    }

    private boolean userExists(String contact, boolean isEmail) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String column = isEmail ? DatabaseHelper.COLUMN_USER_EMAIL : DatabaseHelper.COLUMN_USER_PHONE;
        String[] columns = {DatabaseHelper.COLUMN_USER_ID};
        String selection = column + " = ?";
        String[] selectionArgs = {contact.toLowerCase()};

        Cursor cursor = db.query(DatabaseHelper.TABLE_USERS, columns, selection, selectionArgs, null, null, null);
        boolean exists = cursor != null && cursor.getCount() > 0;
        
        if (cursor != null) {
            cursor.close();
        }
        db.close();
        
        return exists;
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
                    ToastHelper.showSuccess(PasswordRecoveryRequestActivity.this, "Verification code sent to " + method);
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    ToastHelper.showError(PasswordRecoveryRequestActivity.this, "Failed to send verification code: " + errorMessage);
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
