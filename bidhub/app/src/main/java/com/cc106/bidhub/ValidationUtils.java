package com.cc106.bidhub;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Patterns;
import java.util.regex.Pattern;

public class ValidationUtils {
    
    // Email validation patterns
    private static final Pattern EMAIL_PATTERN = Patterns.EMAIL_ADDRESS;
    private static final Pattern STRICT_EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    
    // Username validation patterns
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");
    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_USERNAME_LENGTH = 20;
    
    // Password validation patterns
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final Pattern PASSWORD_UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern PASSWORD_LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern PASSWORD_DIGIT = Pattern.compile("[0-9]");
    private static final Pattern PASSWORD_SPECIAL = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]");
    
    /**
     * Validates email format and completeness with comprehensive checks
     */
    public static ValidationResult validateEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            return new ValidationResult(false, "Email is required");
        }
        
        email = email.trim();
        
        // Check for basic email structure
        if (!email.contains("@")) {
            return new ValidationResult(false, "Email must contain @ symbol");
        }
        
        if (email.startsWith("@") || email.endsWith("@")) {
            return new ValidationResult(false, "Email cannot start or end with @");
        }
        
        String[] parts = email.split("@");
        if (parts.length != 2) {
            return new ValidationResult(false, "Email must have exactly one @ symbol");
        }
        
        String localPart = parts[0];
        String domainPart = parts[1];
        
        if (TextUtils.isEmpty(localPart)) {
            return new ValidationResult(false, "Email must have a local part before @");
        }
        
        if (TextUtils.isEmpty(domainPart)) {
            return new ValidationResult(false, "Email must have a domain after @");
        }
        
        // Enhanced local part validation
        if (localPart.length() > 64) {
            return new ValidationResult(false, "Email local part cannot exceed 64 characters");
        }
        
        if (localPart.startsWith(".") || localPart.endsWith(".")) {
            return new ValidationResult(false, "Email local part cannot start or end with a dot");
        }
        
        if (localPart.contains("..")) {
            return new ValidationResult(false, "Email local part cannot contain consecutive dots");
        }
        
        // Enhanced domain part validation
        if (domainPart.length() > 253) {
            return new ValidationResult(false, "Email domain cannot exceed 253 characters");
        }
        
        if (!domainPart.contains(".")) {
            return new ValidationResult(false, "Email domain must contain a dot");
        }
        
        if (domainPart.startsWith(".") || domainPart.endsWith(".")) {
            return new ValidationResult(false, "Email domain cannot start or end with a dot");
        }
        
        if (domainPart.contains("..")) {
            return new ValidationResult(false, "Email domain cannot contain consecutive dots");
        }
        
        // Check for valid TLD (at least 2 characters)
        String[] domainParts = domainPart.split("\\.");
        if (domainParts.length < 2) {
            return new ValidationResult(false, "Email domain must have a valid top-level domain");
        }
        
        String tld = domainParts[domainParts.length - 1];
        if (tld.length() < 2) {
            return new ValidationResult(false, "Email top-level domain must be at least 2 characters");
        }
        
        // Check for valid email format using Android's built-in pattern
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return new ValidationResult(false, "Please enter a valid email address");
        }
        
        // Additional validation for common invalid patterns
        if (email.contains(" ")) {
            return new ValidationResult(false, "Email cannot contain spaces");
        }
        
        if (email.contains("..")) {
            return new ValidationResult(false, "Email cannot contain consecutive dots");
        }
        
        return new ValidationResult(true, "Valid email");
    }
    
    /**
     * Validates username format and length with enhanced rules
     */
    public static ValidationResult validateUsername(String username) {
        if (TextUtils.isEmpty(username)) {
            return new ValidationResult(false, "Username is required");
        }
        
        username = username.trim();
        
        if (username.length() < MIN_USERNAME_LENGTH) {
            return new ValidationResult(false, "Username must be at least " + MIN_USERNAME_LENGTH + " characters");
        }
        
        if (username.length() > MAX_USERNAME_LENGTH) {
            return new ValidationResult(false, "Username must be no more than " + MAX_USERNAME_LENGTH + " characters");
        }
        
        // Check for spaces
        if (username.contains(" ")) {
            return new ValidationResult(false, "Username cannot contain spaces");
        }
        
        // Check for consecutive underscores
        if (username.contains("__")) {
            return new ValidationResult(false, "Username cannot contain consecutive underscores");
        }
        
        // Check if starts or ends with underscore
        if (username.startsWith("_") || username.endsWith("_")) {
            return new ValidationResult(false, "Username cannot start or end with underscore");
        }
        
        // Check for only numbers
        if (username.matches("^[0-9]+$")) {
            return new ValidationResult(false, "Username cannot contain only numbers");
        }
        
        // Check for only underscores
        if (username.matches("^_+$")) {
            return new ValidationResult(false, "Username cannot contain only underscores");
        }
        
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            return new ValidationResult(false, "Username can only contain letters, numbers, and underscores");
        }
        
        // Check for reserved usernames (expanded list)
        String[] reservedUsernames = {
            "admin", "administrator", "root", "user", "guest", "test", "demo", "bidhub", 
            "support", "help", "api", "www", "mail", "ftp", "blog", "news", "forum",
            "contact", "about", "privacy", "terms", "login", "register", "signup",
            "signin", "logout", "profile", "settings", "account", "billing", "payment",
            "auction", "bid", "bidding", "seller", "buyer", "moderator", "staff"
        };
        
        for (String reserved : reservedUsernames) {
            if (username.equalsIgnoreCase(reserved)) {
                return new ValidationResult(false, "This username is reserved and cannot be used");
            }
        }
        
        // Check for common patterns that might be confusing
        if (username.toLowerCase().contains("bidhub")) {
            return new ValidationResult(false, "Username cannot contain 'bidhub'");
        }
        
        return new ValidationResult(true, "Valid username");
    }
    
    /**
     * Validates password and returns comprehensive strength analysis
     */
    public static PasswordStrengthResult validatePassword(String password) {
        if (TextUtils.isEmpty(password)) {
            return new PasswordStrengthResult(false, "Password is required", 0, "Please enter a password");
        }
        
        int score = 0;
        StringBuilder recommendations = new StringBuilder();
        
        // Length check (more granular scoring)
        if (password.length() >= MIN_PASSWORD_LENGTH) {
            score += 2;
            if (password.length() >= 12) {
                score += 1; // Bonus for longer passwords
            }
            if (password.length() >= 16) {
                score += 1; // Additional bonus for very long passwords
            }
        } else {
            recommendations.append("• Use at least ").append(MIN_PASSWORD_LENGTH).append(" characters\n");
        }
        
        // Character variety checks
        boolean hasUppercase = PASSWORD_UPPERCASE.matcher(password).find();
        boolean hasLowercase = PASSWORD_LOWERCASE.matcher(password).find();
        boolean hasDigit = PASSWORD_DIGIT.matcher(password).find();
        boolean hasSpecial = PASSWORD_SPECIAL.matcher(password).find();
        
        if (hasUppercase) {
            score += 1;
        } else {
            recommendations.append("• Add uppercase letters (A-Z)\n");
        }
        
        if (hasLowercase) {
            score += 1;
        } else {
            recommendations.append("• Add lowercase letters (a-z)\n");
        }
        
        if (hasDigit) {
            score += 1;
        } else {
            recommendations.append("• Add numbers (0-9)\n");
        }
        
        if (hasSpecial) {
            score += 1;
        } else {
            recommendations.append("• Add special characters (!@#$%^&*)\n");
        }
        
        // Additional security checks
        if (password.length() >= 8) {
            // Check for common patterns
            if (password.matches(".*(.)\\1{2,}.*")) {
                score -= 1;
                recommendations.append("• Avoid repeating characters (aaa, 111)\n");
            }
            
            // Check for sequential patterns
            if (password.matches(".*(abc|bcd|cde|def|efg|fgh|ghi|hij|ijk|jkl|klm|lmn|mno|nop|opq|pqr|qrs|rst|stu|tuv|uvw|vwx|wxy|xyz).*") ||
                password.matches(".*(123|234|345|456|567|678|789|890|012).*")) {
                score -= 1;
                recommendations.append("• Avoid sequential patterns (abc, 123)\n");
            }
            
            // Check for keyboard patterns
            if (password.matches(".*(qwerty|asdfgh|zxcvbn|qwertyuiop|asdfghjkl|zxcvbnm).*")) {
                score -= 1;
                recommendations.append("• Avoid keyboard patterns (qwerty, asdf)\n");
            }
        }
        
        // Check for common weak passwords
        String[] commonPasswords = {
            "password", "123456", "123456789", "qwerty", "abc123", "password123",
            "admin", "letmein", "welcome", "monkey", "1234567890", "password1",
            "qwerty123", "dragon", "master", "hello", "freedom", "whatever"
        };
        
        for (String common : commonPasswords) {
            if (password.toLowerCase().equals(common)) {
                score = 0;
                recommendations = new StringBuilder("• This is a very common password - choose something unique\n");
                break;
            }
        }
        
        // Determine strength level and validation
        String strengthLevel;
        boolean isValid = score >= 4;
        
        if (score <= 1) {
            strengthLevel = "Very Weak";
        } else if (score <= 2) {
            strengthLevel = "Weak";
        } else if (score <= 4) {
            strengthLevel = "Fair";
        } else if (score <= 6) {
            strengthLevel = "Good";
        } else {
            strengthLevel = "Strong";
        }
        
        String message = isValid ? "Password strength: " + strengthLevel : "Password too weak - " + strengthLevel;
        
        return new PasswordStrengthResult(isValid, message, Math.max(0, score), recommendations.toString());
    }
    
    /**
     * Checks if email already exists in database
     */
    public static boolean isEmailTaken(Context context, String email) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        String[] columns = {DatabaseHelper.COLUMN_USER_EMAIL};
        String selection = DatabaseHelper.COLUMN_USER_EMAIL + " = ?";
        String[] selectionArgs = {email.trim().toLowerCase()};
        
        Cursor cursor = db.query(
            DatabaseHelper.TABLE_USERS,
            columns,
            selection,
            selectionArgs,
            null,
            null,
            null
        );
        
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        
        return exists;
    }
    
    /**
     * Checks if username already exists in database
     */
    public static boolean isUsernameTaken(Context context, String username) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        String[] columns = {DatabaseHelper.COLUMN_USER_USERNAME};
        String selection = DatabaseHelper.COLUMN_USER_USERNAME + " = ?";
        String[] selectionArgs = {username.trim().toLowerCase()};
        
        Cursor cursor = db.query(
            DatabaseHelper.TABLE_USERS,
            columns,
            selection,
            selectionArgs,
            null,
            null,
            null
        );
        
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        
        return exists;
    }
    
    /**
     * Validation result class
     */
    public static class ValidationResult {
        public final boolean isValid;
        public final String message;
        
        public ValidationResult(boolean isValid, String message) {
            this.isValid = isValid;
            this.message = message;
        }
    }
    
    /**
     * Password strength result class
     */
    public static class PasswordStrengthResult {
        public final boolean isValid;
        public final String message;
        public final int score;
        public final String recommendations;
        
        public PasswordStrengthResult(boolean isValid, String message, int score, String recommendations) {
            this.isValid = isValid;
            this.message = message;
            this.score = score;
            this.recommendations = recommendations;
        }
    }
}
