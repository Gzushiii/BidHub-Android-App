package com.cc106.bidhub;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import com.cc106.bidhub.toast.ToastHelper;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputLayout;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText editTextFirstName, editTextLastName, editTextUsername, editTextEmail, editTextPhone, editTextPassword;
    private CheckBox checkboxTerms, checkboxPrivacy;
    private Button buttonRegister;
    private TextView textViewLoginLink;
    private DatabaseHelper dbHelper;
    
    // TextInputLayouts for validation feedback
    private TextInputLayout usernameInputLayout, emailInputLayout, passwordInputLayout;
    private PasswordStrengthIndicator passwordStrengthIndicator;
    
    // Validation state
    private boolean isEmailValid = false;
    private boolean isUsernameValid = false;
    private boolean isPasswordValid = false;
    private boolean isEmailAvailable = true;
    private boolean isUsernameAvailable = true;
    
    // Debouncing handlers for real-time validation
    private Handler emailValidationHandler = new Handler(Looper.getMainLooper());
    private Handler usernameValidationHandler = new Handler(Looper.getMainLooper());
    private Runnable emailValidationRunnable;
    private Runnable usernameValidationRunnable;
    private static final long VALIDATION_DELAY = 500; // 500ms delay

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        dbHelper = new DatabaseHelper(this);

        // Initialize all the UI components
        editTextFirstName = findViewById(R.id.editTextFirstName);
        editTextLastName = findViewById(R.id.editTextLastName);
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPhone = findViewById(R.id.editTextPhone);
        editTextPassword = findViewById(R.id.editTextPassword);
        checkboxTerms = findViewById(R.id.checkboxTerms);
        checkboxPrivacy = findViewById(R.id.checkboxPrivacy);
        buttonRegister = findViewById(R.id.buttonRegister);
        textViewLoginLink = findViewById(R.id.textViewLoginLink);
        
        // Initialize TextInputLayouts
        usernameInputLayout = findViewById(R.id.usernameInputLayout);
        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        passwordStrengthIndicator = findViewById(R.id.passwordStrengthIndicator);

        // Set up real-time validation
        setupValidationListeners();
        
        buttonRegister.setOnClickListener(v -> registerUser());
        textViewLoginLink.setOnClickListener(v -> finish());
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up handlers to prevent memory leaks
        if (emailValidationRunnable != null) {
            emailValidationHandler.removeCallbacks(emailValidationRunnable);
        }
        if (usernameValidationRunnable != null) {
            usernameValidationHandler.removeCallbacks(usernameValidationRunnable);
        }
    }

    /**
     * Set up real-time validation listeners
     */
    private void setupValidationListeners() {
        // Username validation
        editTextUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateUsername();
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // Email validation
        editTextEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateEmail();
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // Password validation
        editTextPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validatePassword();
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
    
    /**
     * Validate username in real-time with debouncing
     */
    private void validateUsername() {
        String username = editTextUsername.getText().toString().trim();
        
        // Cancel previous validation if still pending
        if (usernameValidationRunnable != null) {
            usernameValidationHandler.removeCallbacks(usernameValidationRunnable);
        }
        
        // Immediate format validation
        ValidationUtils.ValidationResult result = ValidationUtils.validateUsername(username);
        
        if (username.isEmpty()) {
            setInputLayoutError(usernameInputLayout, null);
            isUsernameValid = false;
            isUsernameAvailable = true;
            updateRegisterButton();
            return;
        }
        
        if (!result.isValid) {
            setInputLayoutError(usernameInputLayout, result.message);
            isUsernameValid = false;
            isUsernameAvailable = true;
            updateRegisterButton();
            return;
        }
        
        // Show checking status
        setInputLayoutError(usernameInputLayout, "Checking availability...");
        
        // Debounced availability check
        usernameValidationRunnable = () -> {
            new Thread(() -> {
                boolean available = !ValidationUtils.isUsernameTaken(RegisterActivity.this, username);
                runOnUiThread(() -> {
                    if (available) {
                        setInputLayoutError(usernameInputLayout, null);
                        isUsernameValid = true;
                        isUsernameAvailable = true;
                    } else {
                        setInputLayoutError(usernameInputLayout, "Username is already taken");
                        isUsernameValid = false;
                        isUsernameAvailable = false;
                    }
                    updateRegisterButton();
                });
            }).start();
        };
        
        usernameValidationHandler.postDelayed(usernameValidationRunnable, VALIDATION_DELAY);
    }
    
    /**
     * Validate email in real-time with debouncing
     */
    private void validateEmail() {
        String email = editTextEmail.getText().toString().trim();
        
        // Cancel previous validation if still pending
        if (emailValidationRunnable != null) {
            emailValidationHandler.removeCallbacks(emailValidationRunnable);
        }
        
        // Immediate format validation
        ValidationUtils.ValidationResult result = ValidationUtils.validateEmail(email);
        
        if (email.isEmpty()) {
            setInputLayoutError(emailInputLayout, null);
            isEmailValid = false;
            isEmailAvailable = true;
            updateRegisterButton();
            return;
        }
        
        if (!result.isValid) {
            setInputLayoutError(emailInputLayout, result.message);
            isEmailValid = false;
            isEmailAvailable = true;
            updateRegisterButton();
            return;
        }
        
        // Show checking status
        setInputLayoutError(emailInputLayout, "Checking availability...");
        
        // Debounced availability check
        emailValidationRunnable = () -> {
            new Thread(() -> {
                boolean available = !ValidationUtils.isEmailTaken(RegisterActivity.this, email);
                runOnUiThread(() -> {
                    if (available) {
                        setInputLayoutError(emailInputLayout, null);
                        isEmailValid = true;
                        isEmailAvailable = true;
                    } else {
                        setInputLayoutError(emailInputLayout, "Email is already registered");
                        isEmailValid = false;
                        isEmailAvailable = false;
                    }
                    updateRegisterButton();
                });
            }).start();
        };
        
        emailValidationHandler.postDelayed(emailValidationRunnable, VALIDATION_DELAY);
    }
    
    /**
     * Validate password in real-time with enhanced feedback
     */
    private void validatePassword() {
        String password = editTextPassword.getText().toString();
        ValidationUtils.PasswordStrengthResult result = ValidationUtils.validatePassword(password);
        
        passwordStrengthIndicator.updateStrength(result);
        
        if (password.isEmpty()) {
            setInputLayoutError(passwordInputLayout, null);
            isPasswordValid = false;
        } else if (result.isValid) {
            setInputLayoutError(passwordInputLayout, null);
            isPasswordValid = true;
        } else {
            // Show specific error message based on strength
            String errorMessage = "Password is too weak";
            if (result.score <= 1) {
                errorMessage = "Password is very weak - please strengthen it";
            } else if (result.score <= 2) {
                errorMessage = "Password is weak - please add more complexity";
            } else if (result.score <= 4) {
                errorMessage = "Password needs improvement";
            }
            setInputLayoutError(passwordInputLayout, errorMessage);
            isPasswordValid = false;
        }
        
        updateRegisterButton();
    }
    
    /**
     * Set error state for TextInputLayout
     */
    private void setInputLayoutError(TextInputLayout layout, String error) {
        if (layout != null) {
            layout.setError(error);
            layout.setErrorEnabled(error != null);
        }
    }
    
    /**
     * Update register button state based on validation
     */
    private void updateRegisterButton() {
        boolean canRegister = isEmailValid && isUsernameValid && isPasswordValid && 
                            isEmailAvailable && isUsernameAvailable &&
                            checkboxTerms.isChecked() && checkboxPrivacy.isChecked();
        
        buttonRegister.setEnabled(canRegister);
        buttonRegister.setAlpha(canRegister ? 1.0f : 0.6f);
    }

    private void registerUser() {
        try {
            // Get text from all fields
            String firstName = editTextFirstName.getText().toString().trim();
            String lastName = editTextLastName.getText().toString().trim();
            String username = editTextUsername.getText().toString().trim();
            String email = editTextEmail.getText().toString().trim();
            String phone = editTextPhone.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();

            // --- Enhanced Form Validation ---
            if (TextUtils.isEmpty(firstName) || TextUtils.isEmpty(lastName) || TextUtils.isEmpty(username) ||
                    TextUtils.isEmpty(email) || TextUtils.isEmpty(phone) || TextUtils.isEmpty(password)) {
                ToastHelper.showWarning(this, "Please fill all fields");
                return;
            }

            if (!checkboxTerms.isChecked() || !checkboxPrivacy.isChecked()) {
                ToastHelper.showWarning(this, "You must accept the Terms and Privacy Policy");
                return;
            }
            
            // Final validation check
            if (!isEmailValid || !isUsernameValid || !isPasswordValid) {
                ToastHelper.showWarning(this, "Please fix validation errors before registering");
                return;
            }
            
            if (!isEmailAvailable || !isUsernameAvailable) {
                ToastHelper.showError(this, "Email or username is already taken");
                return;
            }

            // Additional security validation
            ValidationUtils.ValidationResult emailResult = ValidationUtils.validateEmail(email);
            ValidationUtils.ValidationResult usernameResult = ValidationUtils.validateUsername(username);
            ValidationUtils.PasswordStrengthResult passwordResult = ValidationUtils.validatePassword(password);
            
            if (!emailResult.isValid || !usernameResult.isValid || !passwordResult.isValid) {
                ToastHelper.showWarning(this, "Please fix all validation errors before registering");
                return;
            }

            // Disable register button to prevent double submission
            buttonRegister.setEnabled(false);
            buttonRegister.setText("Creating Account...");

            // --- Database Insertion ---
            new Thread(() -> {
                try {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    ContentValues values = new ContentValues();

                    // Hash the password
                    Map<String, byte[]> hashingResult = PasswordHasher.hashPassword(password);
                    byte[] hashedPassword = hashingResult.get("hash");
                    byte[] salt = hashingResult.get("salt");

                    // Generate unique alias for anonymous bidding
                    String alias = AliasGenerator.generateAlias();

                    // Put all user data into the ContentValues object
                    values.put(DatabaseHelper.COLUMN_USER_FIRST_NAME, firstName);
                    values.put(DatabaseHelper.COLUMN_USER_LAST_NAME, lastName);
                    values.put(DatabaseHelper.COLUMN_USER_USERNAME, username.toLowerCase());
                    values.put(DatabaseHelper.COLUMN_USER_ALIAS, alias);
                    values.put(DatabaseHelper.COLUMN_USER_EMAIL, email.toLowerCase());
                    values.put(DatabaseHelper.COLUMN_USER_PHONE, phone);
                    values.put(DatabaseHelper.COLUMN_USER_PASSWORD, hashedPassword);
                    values.put(DatabaseHelper.COLUMN_USER_SALT, salt);

                    long newRowId = db.insert(DatabaseHelper.TABLE_USERS, null, values);
                    db.close();

                    runOnUiThread(() -> {
                        if (newRowId != -1) {
                            ToastHelper.showSuccess(this, "Registration successful! Your alias: " + alias);
                            finish(); // Go back to the login screen
                        } else {
                            ToastHelper.showError(this, "Registration failed. Please try again.");
                            buttonRegister.setEnabled(true);
                            buttonRegister.setText("Create Account");
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        ToastHelper.showError(this, "Registration failed: " + e.getMessage());
                        buttonRegister.setEnabled(true);
                        buttonRegister.setText("Create Account");
                    });
                }
            }).start();

        } catch (Exception e) {
            ToastHelper.showError(this, "Registration failed: " + e.getMessage());
            buttonRegister.setEnabled(true);
            buttonRegister.setText("Create Account");
        }
    }
}

