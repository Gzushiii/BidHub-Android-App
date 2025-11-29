package com.cc106.bidhub.utils;

import android.text.TextUtils;
import android.util.Patterns;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Helper class to reduce duplicate form validation code
 * Provides common validation methods for forms
 */
public class FormValidationHelper {
    
    /**
     * Validate email and set error on TextInputLayout
     * @param email email string to validate
     * @param emailInputLayout TextInputLayout to show error on
     * @return true if valid, false otherwise
     */
    public static boolean validateEmail(String email, TextInputLayout emailInputLayout) {
        if (emailInputLayout == null) {
            return false;
        }
        
        email = email != null ? email.trim() : "";
        
        if (TextUtils.isEmpty(email)) {
            setError(emailInputLayout, "Email is required");
            return false;
        }
        
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            setError(emailInputLayout, "Please enter a valid email address");
            return false;
        }
        
        clearError(emailInputLayout);
        return true;
    }
    
    /**
     * Validate password and set error on TextInputLayout
     * @param password password string to validate
     * @param passwordInputLayout TextInputLayout to show error on
     * @param minLength minimum password length (default 6)
     * @return true if valid, false otherwise
     */
    public static boolean validatePassword(String password, TextInputLayout passwordInputLayout, int minLength) {
        if (passwordInputLayout == null) {
            return false;
        }
        
        password = password != null ? password.trim() : "";
        
        if (TextUtils.isEmpty(password)) {
            setError(passwordInputLayout, "Password is required");
            return false;
        }
        
        if (password.length() < minLength) {
            setError(passwordInputLayout, "Password must be at least " + minLength + " characters");
            return false;
        }
        
        clearError(passwordInputLayout);
        return true;
    }
    
    /**
     * Validate password with default minimum length of 6
     */
    public static boolean validatePassword(String password, TextInputLayout passwordInputLayout) {
        return validatePassword(password, passwordInputLayout, 6);
    }
    
    /**
     * Validate non-empty field
     * @param value field value to validate
     * @param inputLayout TextInputLayout to show error on
     * @param fieldName name of the field for error message
     * @return true if valid, false otherwise
     */
    public static boolean validateRequired(String value, TextInputLayout inputLayout, String fieldName) {
        if (inputLayout == null) {
            return false;
        }
        
        value = value != null ? value.trim() : "";
        
        if (TextUtils.isEmpty(value)) {
            setError(inputLayout, fieldName + " is required");
            return false;
        }
        
        clearError(inputLayout);
        return true;
    }
    
    /**
     * Set error on TextInputLayout
     */
    public static void setError(TextInputLayout layout, String error) {
        if (layout != null) {
            layout.setError(error);
            layout.setErrorEnabled(error != null);
        }
    }
    
    /**
     * Clear error on TextInputLayout
     */
    public static void clearError(TextInputLayout layout) {
        if (layout != null) {
            layout.setError(null);
            layout.setErrorEnabled(false);
        }
    }
    
    /**
     * Clear errors on multiple TextInputLayouts
     */
    public static void clearErrors(TextInputLayout... layouts) {
        for (TextInputLayout layout : layouts) {
            clearError(layout);
        }
    }
}

