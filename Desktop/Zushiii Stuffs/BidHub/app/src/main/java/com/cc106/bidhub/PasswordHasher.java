package com.cc106.bidhub;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class PasswordHasher {

    private static final String HASH_ALGORITHM = "SHA-256";

    // Generates a salt and hashes the password with it
    public static Map<String, byte[]> hashPassword(String password) {
        try {
            // Generate a random salt
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);

            // Create the hasher
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            md.update(salt); // Add salt to the hasher

            // Hash the password
            byte[] hashedPassword = md.digest(password.getBytes());

            // Return both the hash and the salt
            Map<String, byte[]> result = new HashMap<>();
            result.put("hash", hashedPassword);
            result.put("salt", salt);
            return result;

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing algorithm not found", e);
        }
    }

    // Verifies a password against a stored hash and salt
    public static boolean verifyPassword(String password, byte[] storedHash, byte[] salt) {
        try {
            // Create a hasher with the same salt
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            md.update(salt);

            // Hash the incoming password attempt
            byte[] attemptHash = md.digest(password.getBytes());

            // Compare the new hash with the stored hash
            return Arrays.equals(storedHash, attemptHash);

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing algorithm not found", e);
        }
    }
}

