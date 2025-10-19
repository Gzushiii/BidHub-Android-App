package com.cc106.bidhub.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.regex.Pattern;

public class PasswordUtils {
    
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH = 32;
    private static final SecureRandom secureRandom = new SecureRandom();
    
    // Password strength patterns
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]");
    
    /**
     * Generate a random salt for password hashing
     */
    public static byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);
        return salt;
    }
    
    /**
     * Hash a password with salt using SHA-256
     */
    public static byte[] hashPassword(String password, byte[] salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            digest.update(salt);
            byte[] hashedPassword = digest.digest(password.getBytes());
            return hashedPassword;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hash algorithm not available", e);
        }
    }
    
    /**
     * Verify a password against its hash and salt
     */
    public static boolean verifyPassword(String password, byte[] storedHash, byte[] salt) {
        byte[] hashedPassword = hashPassword(password, salt);
        return MessageDigest.isEqual(hashedPassword, storedHash);
    }
    
    /**
     * Check if a password meets strength requirements
     */
    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        
        boolean hasUppercase = UPPERCASE_PATTERN.matcher(password).find();
        boolean hasLowercase = LOWERCASE_PATTERN.matcher(password).find();
        boolean hasDigit = DIGIT_PATTERN.matcher(password).find();
        boolean hasSpecialChar = SPECIAL_CHAR_PATTERN.matcher(password).find();
        
        return hasUppercase && hasLowercase && hasDigit && hasSpecialChar;
    }
    
    /**
     * Calculate password strength score (1-4)
     */
    public static int calculatePasswordStrength(String password) {
        if (password == null || password.length() < 6) {
            return 0;
        }
        
        int score = 0;
        
        // Length check
        if (password.length() >= 8) score++;
        if (password.length() >= 12) score++;
        
        // Character variety checks
        if (UPPERCASE_PATTERN.matcher(password).find()) score++;
        if (LOWERCASE_PATTERN.matcher(password).find()) score++;
        if (DIGIT_PATTERN.matcher(password).find()) score++;
        if (SPECIAL_CHAR_PATTERN.matcher(password).find()) score++;
        
        // Normalize to 1-4 scale
        if (score <= 2) return 1;      // Weak
        if (score <= 4) return 2;      // Fair
        if (score <= 6) return 3;      // Good
        return 4;                      // Strong
    }
    
    /**
     * Get password strength description
     */
    public static String getPasswordStrengthDescription(int strength) {
        switch (strength) {
            case 1: return "Weak";
            case 2: return "Fair";
            case 3: return "Good";
            case 4: return "Strong";
            default: return "Very Weak";
        }
    }
}
