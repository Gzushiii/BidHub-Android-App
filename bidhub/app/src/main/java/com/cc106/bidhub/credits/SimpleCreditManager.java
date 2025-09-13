package com.cc106.bidhub.credits;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.cc106.bidhub.DatabaseHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Simplified Credit Manager for MVP implementation
 * Focuses on core functionality with test payment gateway support
 */
public class SimpleCreditManager {
    private static final String TAG = "SimpleCreditManager";
    
    // Transaction types
    public static final String TRANSACTION_PURCHASE = "purchase";
    public static final String TRANSACTION_BID = "bid";
    public static final String TRANSACTION_REFUND = "refund";
    public static final String TRANSACTION_TRANSFER = "transfer";
    public static final String TRANSACTION_REDEMPTION = "redemption";
    
    // Transaction statuses
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_FAILED = "failed";
    
    // Payment methods
    public static final String PAYMENT_GCASH = "gcash";
    public static final String PAYMENT_MAYA = "maya";
    public static final String PAYMENT_TEST = "test";
    
    // Test credit packages
    private static final CreditPackage[] TEST_PACKAGES = {
        new CreditPackage(1, "Starter Pack", "Perfect for beginners", 100.0, 100.0, "PHP", true),
        new CreditPackage(2, "Basic Pack", "Great value for regular users", 500.0, 450.0, "PHP", true),
        new CreditPackage(3, "Premium Pack", "Best value with bonus credits", 1000.0, 800.0, "PHP", true),
        new CreditPackage(4, "Enterprise Pack", "For power users", 5000.0, 4000.0, "PHP", true)
    };
    
    private Context context;
    private DatabaseHelper dbHelper;
    
    public SimpleCreditManager(Context context) {
        this.context = context;
        this.dbHelper = new DatabaseHelper(context);
    }
    
    // ==================== CORE CREDIT OPERATIONS ====================
    
    /**
     * Get current credit balance for user
     */
    public double getCreditBalance(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return 0.0;
        }
        
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT " + DatabaseHelper.COLUMN_USER_CREDITS + 
                      " FROM " + DatabaseHelper.TABLE_USERS + 
                      " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?";
        
        Cursor cursor = db.rawQuery(query, new String[]{userId});
        double balance = 0.0;
        
        if (cursor.moveToFirst()) {
            balance = cursor.getDouble(0);
        }
        cursor.close();
        
        return balance;
    }
    
    /**
     * Add credits to user account
     */
    public boolean addCredits(String userId, double amount, String transactionType) {
        if (userId == null || userId.trim().isEmpty() || amount <= 0) {
            return false;
        }
        
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            db.beginTransaction();
            
            // Get current balance
            double currentBalance = getCreditBalance(userId);
            double newBalance = currentBalance + amount;
            
            // Update user credits
            String updateQuery = "UPDATE " + DatabaseHelper.TABLE_USERS + 
                               " SET " + DatabaseHelper.COLUMN_USER_CREDITS + " = ? " +
                               " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?";
            db.execSQL(updateQuery, new Object[]{newBalance, userId});
            
            // Create transaction record
            createTransaction(userId, transactionType, amount, "Credit addition", STATUS_COMPLETED);
            
            db.setTransactionSuccessful();
            Log.i(TAG, "Credits added: " + amount + " for user: " + userId);
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error adding credits", e);
            return false;
        } finally {
            db.endTransaction();
        }
    }
    
    /**
     * Deduct credits from user account
     */
    public boolean deductCredits(String userId, double amount, String transactionType) {
        if (userId == null || userId.trim().isEmpty() || amount <= 0) {
            return false;
        }
        
        double currentBalance = getCreditBalance(userId);
        if (currentBalance < amount) {
            Log.e(TAG, "Insufficient credits for user: " + userId);
            return false;
        }
        
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            db.beginTransaction();
            
            double newBalance = currentBalance - amount;
            
            // Update user credits
            String updateQuery = "UPDATE " + DatabaseHelper.TABLE_USERS + 
                               " SET " + DatabaseHelper.COLUMN_USER_CREDITS + " = ? " +
                               " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?";
            db.execSQL(updateQuery, new Object[]{newBalance, userId});
            
            // Create transaction record
            createTransaction(userId, transactionType, -amount, "Credit deduction", STATUS_COMPLETED);
            
            db.setTransactionSuccessful();
            Log.i(TAG, "Credits deducted: " + amount + " for user: " + userId);
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error deducting credits", e);
            return false;
        } finally {
            db.endTransaction();
        }
    }
    
    /**
     * Check if user has sufficient credits
     */
    public boolean hasSufficientCredits(String userId, double amount) {
        return getCreditBalance(userId) >= amount;
    }
    
    // ==================== CREDIT PACKAGES ====================
    
    /**
     * Get available credit packages
     */
    public List<CreditPackage> getAvailablePackages() {
        List<CreditPackage> packages = new ArrayList<>();
        for (CreditPackage pkg : TEST_PACKAGES) {
            if (pkg.isAvailable()) {
                packages.add(pkg);
            }
        }
        return packages;
    }
    
    /**
     * Get package by ID
     */
    public CreditPackage getPackageById(int packageId) {
        for (CreditPackage pkg : TEST_PACKAGES) {
            if (pkg.getPackageId() == packageId) {
                return pkg;
            }
        }
        return null;
    }
    
    // ==================== PURCHASE FLOW ====================
    
    /**
     * Purchase credits using a package
     */
    public boolean purchaseCredits(String userId, int packageId, String paymentMethod) {
        CreditPackage pkg = getPackageById(packageId);
        if (pkg == null || !pkg.isAvailable()) {
            Log.e(TAG, "Invalid package: " + packageId);
            return false;
        }
        
        // Process payment (test mode)
        if (processTestPayment(userId, pkg.getPrice(), paymentMethod)) {
            // Add credits to account
            boolean success = addCredits(userId, pkg.getCredits(), TRANSACTION_PURCHASE);
            if (success) {
                Log.i(TAG, "Purchase successful: " + pkg.getName() + " for user: " + userId);
            }
            return success;
        }
        
        return false;
    }
    
    /**
     * Test payment processing (simulates payment gateway)
     */
    private boolean processTestPayment(String userId, double amount, String paymentMethod) {
        Log.i(TAG, "Processing test payment: " + amount + " via " + paymentMethod + " for user: " + userId);
        
        // Simulate payment processing delay
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // For test mode, always succeed
        // In production, this would call actual payment gateway APIs
        return true;
    }
    
    // ==================== TRANSACTION MANAGEMENT ====================
    
    /**
     * Get transaction history for user
     */
    public List<CreditTransaction> getTransactionHistory(String userId) {
        List<CreditTransaction> transactions = new ArrayList<>();
        
        if (userId == null || userId.trim().isEmpty()) {
            return transactions;
        }
        
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT * FROM " + DatabaseHelper.TABLE_CREDIT_TRANSACTIONS + 
                      " WHERE " + DatabaseHelper.COLUMN_TRANSACTION_USER_ID + " = ? " +
                      " ORDER BY " + DatabaseHelper.COLUMN_TRANSACTION_CREATED_AT + " DESC";
        
        Cursor cursor = db.rawQuery(query, new String[]{userId});
        
        while (cursor.moveToNext()) {
            CreditTransaction transaction = new CreditTransaction();
            transaction.setTransactionId(cursor.getString(0));
            transaction.setUserId(cursor.getString(1));
            transaction.setType(cursor.getString(2));
            transaction.setAmount(cursor.getDouble(3));
            transaction.setDescription(cursor.getString(4));
            transaction.setPaymentMethod(cursor.getString(5));
            transaction.setStatus(cursor.getString(6));
            transaction.setCreatedAt(new Date(cursor.getLong(7)));
            transaction.setReference(cursor.getString(8));
            transactions.add(transaction);
        }
        
        cursor.close();
        return transactions;
    }
    
    /**
     * Create a transaction record
     */
    private void createTransaction(String userId, String type, double amount, String description, String status) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String transactionId = "TXN_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
        
        String insertQuery = "INSERT INTO " + DatabaseHelper.TABLE_CREDIT_TRANSACTIONS + 
                           " (" + DatabaseHelper.COLUMN_TRANSACTION_USER_ID + ", " +
                           DatabaseHelper.COLUMN_TRANSACTION_TYPE + ", " +
                           DatabaseHelper.COLUMN_TRANSACTION_AMOUNT + ", " +
                           DatabaseHelper.COLUMN_TRANSACTION_DESCRIPTION + ", " +
                           DatabaseHelper.COLUMN_TRANSACTION_PAYMENT_METHOD + ", " +
                           DatabaseHelper.COLUMN_TRANSACTION_STATUS + ", " +
                           DatabaseHelper.COLUMN_TRANSACTION_CREATED_AT + ", " +
                           DatabaseHelper.COLUMN_TRANSACTION_REFERENCE + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        db.execSQL(insertQuery, new Object[]{
            userId, type, amount, description, null, status, 
            System.currentTimeMillis(), null
        });
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Get user ID from email (simplified implementation)
     */
    public String getUserIdFromEmail(String email) {
        Log.d(TAG, "getUserIdFromEmail called with email: " + email);
        
        if (email == null || email.trim().isEmpty()) {
            Log.e(TAG, "Email is null or empty");
            return null;
        }
        
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT " + DatabaseHelper.COLUMN_USER_ID + 
                      " FROM " + DatabaseHelper.TABLE_USERS + 
                      " WHERE " + DatabaseHelper.COLUMN_USER_EMAIL + " = ?";
        
        Log.d(TAG, "Executing query: " + query + " with email: " + email);
        
        Cursor cursor = db.rawQuery(query, new String[]{email});
        String userId = null;
        
        if (cursor.moveToFirst()) {
            userId = cursor.getString(0);
            Log.d(TAG, "Found userId: " + userId);
        } else {
            Log.e(TAG, "No user found with email: " + email);
        }
        cursor.close();
        
        return userId;
    }
    
    /**
     * Format currency amount
     */
    public String formatCurrency(double amount) {
        return String.format("₱%.2f", amount);
    }
    
    /**
     * Validate payment method
     */
    public boolean isValidPaymentMethod(String paymentMethod) {
        return PAYMENT_GCASH.equals(paymentMethod) || 
               PAYMENT_MAYA.equals(paymentMethod) || 
               PAYMENT_TEST.equals(paymentMethod);
    }
}
