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
import android.widget.ProgressBar;
import android.widget.TextView;
import com.cc106.bidhub.api.AuthApiClient;
import com.cc106.bidhub.api.ApiResponse;
import com.cc106.bidhub.toast.ToastHelper;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;
import com.google.android.material.textfield.TextInputLayout;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText editTextFirstName, editTextLastName, editTextUsername, editTextEmail, editTextPhone, editTextPassword;
    private CheckBox checkboxTerms, checkboxPrivacy;
    private com.google.android.material.button.MaterialButton buttonRegister;
    private TextView textViewLoginLink;
    private DatabaseHelper dbHelper;
    private AuthApiClient authApiClient;
    
    // TextInputLayouts for validation feedback
    private TextInputLayout usernameInputLayout, emailInputLayout, passwordInputLayout;
    private PasswordStrengthIndicator passwordStrengthIndicator;
    private ProgressBar progressBar;
    
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
        
        try {
            setContentView(R.layout.activity_register);

            dbHelper = new DatabaseHelper(this);
            authApiClient = new AuthApiClient(this);

            // Initialize all the UI components with null checks
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
            progressBar = findViewById(R.id.progressBar);

            // Validate critical components
            if (editTextFirstName == null || editTextLastName == null || 
                editTextUsername == null || editTextEmail == null || 
                editTextPhone == null || editTextPassword == null ||
                checkboxTerms == null || checkboxPrivacy == null ||
                buttonRegister == null || textViewLoginLink == null) {
                ToastHelper.showError(this, "Error loading registration form. Please try again.");
                finish();
                return;
            }

            // Set up real-time validation only if components are available
            if (passwordStrengthIndicator != null) {
                setupValidationListeners();
            } else {
                // If password strength indicator is missing, set up basic validation
                setupBasicValidationListeners();
            }
            
            // Setup back button handling - return to login
            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    finish();
                }
            });
            
            if (buttonRegister != null) {
                buttonRegister.setOnClickListener(v -> registerUser());
            }
            if (textViewLoginLink != null) {
                textViewLoginLink.setOnClickListener(v -> finish());
            }
        } catch (Exception e) {
            ToastHelper.showError(this, "Error initializing registration: " + e.getMessage());
            e.printStackTrace();
            finish();
        }
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
     * Show loading state
     */
    private void showLoading(boolean show) {
        try {
            if (progressBar != null) {
                progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            }
            if (buttonRegister != null) {
                buttonRegister.setEnabled(!show);
                buttonRegister.setText(show ? "Creating Account..." : "Create Account");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Set up real-time validation listeners
     */
    private void setupValidationListeners() {
        // Create a common TextWatcher for button updates
        TextWatcher buttonUpdateWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateRegisterButton();
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        };
        
        // Username validation
        editTextUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateUsername();
                updateRegisterButton();
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
                updateRegisterButton();
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
                updateRegisterButton();
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // Add button update listeners to all fields
        editTextFirstName.addTextChangedListener(buttonUpdateWatcher);
        editTextLastName.addTextChangedListener(buttonUpdateWatcher);
        editTextPhone.addTextChangedListener(buttonUpdateWatcher);
        
        // Add checkbox listeners
        checkboxTerms.setOnCheckedChangeListener((buttonView, isChecked) -> updateRegisterButton());
        checkboxPrivacy.setOnCheckedChangeListener((buttonView, isChecked) -> updateRegisterButton());
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
        try {
            String password = editTextPassword.getText().toString();
            ValidationUtils.PasswordStrengthResult result = ValidationUtils.validatePassword(password);
            
            if (passwordStrengthIndicator != null) {
                passwordStrengthIndicator.updateStrength(result);
            }
            
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
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback to basic validation
            String password = editTextPassword.getText().toString();
            if (password.length() >= 6) {
                isPasswordValid = true;
                setInputLayoutError(passwordInputLayout, null);
            } else {
                isPasswordValid = false;
                setInputLayoutError(passwordInputLayout, "Password must be at least 6 characters");
            }
            updateRegisterButton();
        }
    }
    
    /**
     * Set up basic validation listeners (fallback if password strength indicator is missing)
     */
    private void setupBasicValidationListeners() {
        // Username validation
        if (editTextUsername != null) {
            editTextUsername.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    validateUsername();
                    updateRegisterButton();
                }
                
                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
        
        // Email validation
        if (editTextEmail != null) {
            editTextEmail.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    validateEmail();
                    updateRegisterButton();
                }
                
                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
        
        // Password validation
        if (editTextPassword != null) {
            editTextPassword.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    validatePassword();
                    updateRegisterButton();
                }
                
                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
        
        // Add checkbox listeners
        if (checkboxTerms != null) {
            checkboxTerms.setOnCheckedChangeListener((buttonView, isChecked) -> updateRegisterButton());
        }
        if (checkboxPrivacy != null) {
            checkboxPrivacy.setOnCheckedChangeListener((buttonView, isChecked) -> updateRegisterButton());
        }
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
        try {
            if (buttonRegister == null) {
                return;
            }
            
            // Check if all required fields have content (not just validation)
            boolean hasRequiredContent = editTextFirstName != null && !TextUtils.isEmpty(editTextFirstName.getText()) &&
                                       editTextLastName != null && !TextUtils.isEmpty(editTextLastName.getText()) &&
                                       editTextUsername != null && !TextUtils.isEmpty(editTextUsername.getText()) &&
                                       editTextEmail != null && !TextUtils.isEmpty(editTextEmail.getText()) &&
                                       editTextPhone != null && !TextUtils.isEmpty(editTextPhone.getText()) &&
                                       editTextPassword != null && !TextUtils.isEmpty(editTextPassword.getText());
            
            boolean canRegister = hasRequiredContent && isEmailValid && isUsernameValid && isPasswordValid && 
                                isEmailAvailable && isUsernameAvailable &&
                                checkboxTerms != null && checkboxTerms.isChecked() && 
                                checkboxPrivacy != null && checkboxPrivacy.isChecked();
            
            buttonRegister.setEnabled(canRegister);
            buttonRegister.setAlpha(canRegister ? 1.0f : 0.6f);
        } catch (Exception e) {
            e.printStackTrace();
            // If there's an error, disable the button for safety
            if (buttonRegister != null) {
                buttonRegister.setEnabled(false);
            }
        }
    }

    private void registerUser() {
        try {
            // Validate critical components exist
            if (editTextFirstName == null || editTextLastName == null || 
                editTextUsername == null || editTextEmail == null || 
                editTextPhone == null || editTextPassword == null ||
                checkboxTerms == null || checkboxPrivacy == null) {
                ToastHelper.showError(this, "Registration form error. Please restart the app.");
                return;
            }
            
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

            // Show loading state
            showLoading(true);

            // Use backend API for registration
            new Thread(() -> {
                try {
                    String alias = AliasGenerator.generateAlias();
                    ApiResponse response = authApiClient.register(
                        username.toLowerCase(),
                        email.toLowerCase(),
                        password,
                        phone,
                        firstName,
                        lastName,
                        alias // Generate alias for anonymous bidding
                    );
                    
                    // Run UI updates on main thread
                    runOnUiThread(() -> {
                        try {
                            if (response.isSuccess()) {
                                ToastHelper.showSuccess(this, "Registration successful!");
                                finish(); // Go back to the login screen
                            } else {
                                ToastHelper.showError(this, response.getMessage());
                                showLoading(false);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            ToastHelper.showError(this, "Error processing registration response");
                            showLoading(false);
                        }
                    });
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        ToastHelper.showError(this, "Registration failed: " + e.getMessage());
                        showLoading(false);
                    });
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
            ToastHelper.showError(this, "Registration failed: " + e.getMessage());
            showLoading(false);
        }
    }
}

