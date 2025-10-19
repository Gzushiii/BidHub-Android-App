package com.cc106.bidhub.credits;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.cc106.bidhub.DatabaseHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Comprehensive Credit Management System
 * Handles all credit operations including purchases, transactions, reservations, and transfers
 */
public class CreditManager {
    private static final String TAG = "CreditManager";
    
    // Transaction types
    public static final String TRANSACTION_PURCHASE = "purchase";
    public static final String TRANSACTION_REDEMPTION = "redemption";
    public static final String TRANSACTION_BID = "bid";
    public static final String TRANSACTION_REFUND = "refund";
    public static final String TRANSACTION_TRANSFER = "transfer";
    public static final String TRANSACTION_RESERVE = "reserve";
    public static final String TRANSACTION_RELEASE = "release";
    
    // Transaction statuses
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_FAILED = "failed";
    public static final String STATUS_CANCELLED = "cancelled";
    
    // Payment methods
    public static final String PAYMENT_GCASH = "gcash";
    public static final String PAYMENT_MAYA = "maya";
    public static final String PAYMENT_TRANSFER = "transfer";
    
    // Credit limits and validation
    private static final double MIN_CREDIT_AMOUNT = 0.01;
    private static final double MAX_CREDIT_AMOUNT = 1000000.0;
    private static final double MAX_DAILY_PURCHASE_LIMIT = 50000.0;
    private static final double MAX_DAILY_TRANSFER_LIMIT = 10000.0;
    
    private Context context;
    private DatabaseHelper dbHelper;
    private Map<String, CreditBalance> balanceCache;
    private Map<String, List<CreditTransaction>> transactionCache;
    private Map<String, Double> dailyLimits;
    
    // Credit packages
    private List<CreditPackage> creditPackages;
    
    public CreditManager(Context context) {
        this.context = context;
        this.dbHelper = new DatabaseHelper(context);
        this.balanceCache = new ConcurrentHashMap<>();
        this.transactionCache = new ConcurrentHashMap<>();
        this.dailyLimits = new ConcurrentHashMap<>();
        initializeCreditPackages();
    }
    
    // ==================== CREDIT OPERATIONS ====================
    
    /**
     * Purchase credits using a credit package
     */
    public boolean purchaseCredits(String userId, int packageId, String paymentMethod) {
        if (!validateUserId(userId) || !validatePaymentMethod(paymentMethod)) {
            return false;
        }
        
        CreditPackage creditPackage = getPackageById(packageId);
        if (creditPackage == null || !creditPackage.isAvailable()) {
            Log.e(TAG, "Invalid or unavailable credit package: " + packageId);
            return false;
        }
        
        double amount = creditPackage.getEffectiveCredits();
        double price = creditPackage.getEffectivePrice();
        
        // Check daily purchase limits
        if (!checkDailyPurchaseLimit(userId, price)) {
            Log.e(TAG, "Daily purchase limit exceeded for user: " + userId);
            return false;
        }
        
        // Create transaction
        String transactionId = generateTransactionId();
        CreditTransaction transaction = new CreditTransaction(
            transactionId, userId, TRANSACTION_PURCHASE, amount,
            "Purchase: " + creditPackage.getName(), paymentMethod, STATUS_PENDING,
            new Date(), String.valueOf(packageId)
        );
        
        // Process payment (simplified - in real app, integrate with payment gateway)
        if (processPayment(userId, price, paymentMethod, transactionId)) {
            // Add credits to user account
            if (addCredits(userId, amount, TRANSACTION_PURCHASE)) {
                transaction.setStatus(STATUS_COMPLETED);
                saveTransaction(transaction);
                updateDailyLimit(userId, price, "purchase");
                logCreditActivity(userId, "PURCHASE", amount);
                Log.i(TAG, "Credits purchased successfully: " + amount + " for user: " + userId);
                return true;
            } else {
                transaction.setStatus(STATUS_FAILED);
                saveTransaction(transaction);
                Log.e(TAG, "Failed to add credits after payment");
                return false;
            }
        } else {
            transaction.setStatus(STATUS_FAILED);
            saveTransaction(transaction);
            Log.e(TAG, "Payment processing failed");
            return false;
        }
    }
    
    /**
     * Validate if user has sufficient credit balance
     */
    public boolean validateCreditBalance(String userId, double amount) {
        if (!validateUserId(userId) || !validateCreditAmount(amount)) {
            return false;
        }
        
        CreditBalance balance = getCreditBalanceObject(userId);
        return balance.getAvailableCredits() >= amount;
    }
    
    /**
     * Deduct credits from user account
     */
    public boolean deductCredits(String userId, double amount, String transactionType) {
        if (!validateUserId(userId) || !validateCreditAmount(amount) || !validateTransactionType(transactionType)) {
            return false;
        }
        
        if (!validateCreditBalance(userId, amount)) {
            Log.e(TAG, "Insufficient credit balance for user: " + userId);
            return false;
        }
        
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            db.beginTransaction();
            
            // Get current balance
            CreditBalance balance = getCreditBalanceObject(userId);
            
            // Deduct from available credits
            double newAvailableCredits = balance.getAvailableCredits() - amount;
            
            // Update user credits in database
            String updateQuery = "UPDATE " + DatabaseHelper.TABLE_USERS + 
                               " SET " + DatabaseHelper.COLUMN_USER_CREDITS + " = ? " +
                               " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?";
            db.execSQL(updateQuery, new Object[]{newAvailableCredits, userId});
            
            // Create transaction record
            String transactionId = generateTransactionId();
            CreditTransaction transaction = new CreditTransaction(
                transactionId, userId, transactionType, -amount,
                "Credit deduction: " + transactionType, null, STATUS_COMPLETED,
                new Date(), null
            );
            saveTransaction(transaction);
            
            // Update cache
            balance.setAvailableCredits(newAvailableCredits);
            balanceCache.put(userId, balance);
            
            db.setTransactionSuccessful();
            logCreditActivity(userId, "DEDUCT", amount);
            Log.i(TAG, "Credits deducted successfully: " + amount + " for user: " + userId);
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error deducting credits", e);
            return false;
        } finally {
            db.endTransaction();
        }
    }
    
    /**
     * Add credits to user account
     */
    public boolean addCredits(String userId, double amount, String transactionType) {
        if (!validateUserId(userId) || !validateCreditAmount(amount) || !validateTransactionType(transactionType)) {
            return false;
        }
        
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            db.beginTransaction();
            
            // Get current balance
            CreditBalance balance = getCreditBalanceObject(userId);
            
            // Add to available credits
            double newAvailableCredits = balance.getAvailableCredits() + amount;
            
            // Update user credits in database
            String updateQuery = "UPDATE " + DatabaseHelper.TABLE_USERS + 
                               " SET " + DatabaseHelper.COLUMN_USER_CREDITS + " = ? " +
                               " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?";
            db.execSQL(updateQuery, new Object[]{newAvailableCredits, userId});
            
            // Create transaction record
            String transactionId = generateTransactionId();
            CreditTransaction transaction = new CreditTransaction(
                transactionId, userId, transactionType, amount,
                "Credit addition: " + transactionType, null, STATUS_COMPLETED,
                new Date(), null
            );
            saveTransaction(transaction);
            
            // Update cache
            balance.setAvailableCredits(newAvailableCredits);
            balanceCache.put(userId, balance);
            
            db.setTransactionSuccessful();
            logCreditActivity(userId, "ADD", amount);
            Log.i(TAG, "Credits added successfully: " + amount + " for user: " + userId);
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error adding credits", e);
            return false;
        } finally {
            db.endTransaction();
        }
    }
    
    /**
     * Get current credit balance for user
     */
    public double getCreditBalance(String userId) {
        if (!validateUserId(userId)) {
            return 0.0;
        }
        
        CreditBalance balance = getCreditBalanceObject(userId);
        return balance.getAvailableCredits();
    }
    
    /**
     * Get detailed credit balance object
     */
    public CreditBalance getCreditBalanceObject(String userId) {
        if (!validateUserId(userId)) {
            return new CreditBalance(userId, 0.0, 0.0, 0.0, 0.0);
        }
        
        // Check cache first
        if (balanceCache.containsKey(userId)) {
            return balanceCache.get(userId);
        }
        
        // Load from database
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT " + DatabaseHelper.COLUMN_USER_CREDITS + 
                      " FROM " + DatabaseHelper.TABLE_USERS + 
                      " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?";
        
        Cursor cursor = db.rawQuery(query, new String[]{userId});
        double availableCredits = 0.0;
        
        if (cursor.moveToFirst()) {
            availableCredits = cursor.getDouble(0);
        }
        cursor.close();
        
        // For now, all credits are available (simplified implementation)
        CreditBalance balance = new CreditBalance(userId, availableCredits, 0.0, 0.0, 0.0);
        balanceCache.put(userId, balance);
        
        return balance;
    }
    
    // ==================== TRANSACTION MANAGEMENT ====================
    
    /**
     * Get transaction history for user
     */
    public List<CreditTransaction> getTransactionHistory(String userId) {
        if (!validateUserId(userId)) {
            return new ArrayList<>();
        }
        
        // Check cache first
        if (transactionCache.containsKey(userId)) {
            return new ArrayList<>(transactionCache.get(userId));
        }
        
        List<CreditTransaction> transactions = new ArrayList<>();
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
        transactionCache.put(userId, transactions);
        return transactions;
    }
    
    /**
     * Get specific transaction by ID
     */
    public CreditTransaction getTransactionById(String transactionId) {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            return null;
        }
        
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT * FROM " + DatabaseHelper.TABLE_CREDIT_TRANSACTIONS + 
                      " WHERE " + DatabaseHelper.COLUMN_TRANSACTION_ID + " = ?";
        
        Cursor cursor = db.rawQuery(query, new String[]{transactionId});
        CreditTransaction transaction = null;
        
        if (cursor.moveToFirst()) {
            transaction = new CreditTransaction();
            transaction.setTransactionId(cursor.getString(0));
            transaction.setUserId(cursor.getString(1));
            transaction.setType(cursor.getString(2));
            transaction.setAmount(cursor.getDouble(3));
            transaction.setDescription(cursor.getString(4));
            transaction.setPaymentMethod(cursor.getString(5));
            transaction.setStatus(cursor.getString(6));
            transaction.setCreatedAt(new Date(cursor.getLong(7)));
            transaction.setReference(cursor.getString(8));
        }
        
        cursor.close();
        return transaction;
    }
    
    /**
     * Refund a transaction
     */
    public boolean refundTransaction(String transactionId) {
        CreditTransaction transaction = getTransactionById(transactionId);
        if (transaction == null || !STATUS_COMPLETED.equals(transaction.getStatus())) {
            Log.e(TAG, "Cannot refund transaction: " + transactionId);
            return false;
        }
        
        // Create refund transaction
        String refundTransactionId = generateTransactionId();
        CreditTransaction refund = new CreditTransaction(
            refundTransactionId, transaction.getUserId(), TRANSACTION_REFUND, 
            -transaction.getAmount(), "Refund for transaction: " + transactionId,
            null, STATUS_COMPLETED, new Date(), transactionId
        );
        
        // Deduct credits (refund amount is negative, so we add the positive amount)
        if (addCredits(transaction.getUserId(), Math.abs(transaction.getAmount()), TRANSACTION_REFUND)) {
            saveTransaction(refund);
            logCreditActivity(transaction.getUserId(), "REFUND", Math.abs(transaction.getAmount()));
            Log.i(TAG, "Transaction refunded successfully: " + transactionId);
            return true;
        }
        
        return false;
    }
    
    /**
     * Cancel a pending transaction
     */
    public boolean cancelTransaction(String transactionId) {
        CreditTransaction transaction = getTransactionById(transactionId);
        if (transaction == null || !STATUS_PENDING.equals(transaction.getStatus())) {
            Log.e(TAG, "Cannot cancel transaction: " + transactionId);
            return false;
        }
        
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String updateQuery = "UPDATE " + DatabaseHelper.TABLE_CREDIT_TRANSACTIONS + 
                           " SET " + DatabaseHelper.COLUMN_TRANSACTION_STATUS + " = ? " +
                           " WHERE " + DatabaseHelper.COLUMN_TRANSACTION_ID + " = ?";
        
        try {
            db.execSQL(updateQuery, new Object[]{STATUS_CANCELLED, transactionId});
            logCreditActivity(transaction.getUserId(), "CANCEL", 0);
            Log.i(TAG, "Transaction cancelled successfully: " + transactionId);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling transaction", e);
            return false;
        }
    }
    
    // ==================== CREDIT PACKAGES ====================
    
    /**
     * Get all available credit packages
     */
    public List<CreditPackage> getAvailablePackages() {
        List<CreditPackage> available = new ArrayList<>();
        for (CreditPackage pkg : creditPackages) {
            if (pkg.isAvailable()) {
                available.add(pkg);
            }
        }
        return available;
    }
    
    /**
     * Get credit package by ID
     */
    public CreditPackage getPackageById(int packageId) {
        for (CreditPackage pkg : creditPackages) {
            if (pkg.getPackageId() == packageId) {
                return pkg;
            }
        }
        return null;
    }
    
    /**
     * Calculate package price
     */
    public double calculatePackagePrice(int packageId) {
        CreditPackage pkg = getPackageById(packageId);
        return pkg != null ? pkg.getEffectivePrice() : 0.0;
    }
    
    /**
     * Check if package is available
     */
    public boolean isPackageAvailable(int packageId) {
        CreditPackage pkg = getPackageById(packageId);
        return pkg != null && pkg.isAvailable();
    }
    
    // ==================== BALANCE MANAGEMENT ====================
    
    /**
     * Reserve credits for pending transaction
     */
    public boolean reserveCredits(String userId, double amount) {
        if (!validateUserId(userId) || !validateCreditAmount(amount)) {
            return false;
        }
        
        if (!validateCreditBalance(userId, amount)) {
            Log.e(TAG, "Insufficient credits to reserve for user: " + userId);
            return false;
        }
        
        CreditBalance balance = getCreditBalanceObject(userId);
        
        // Transfer from available to reserved
        if (balance.transferCredits(CreditState.AVAILABLE, CreditState.RESERVED, amount)) {
            updateBalanceInDatabase(userId, balance);
            logCreditActivity(userId, "RESERVE", amount);
            Log.i(TAG, "Credits reserved successfully: " + amount + " for user: " + userId);
            return true;
        }
        
        return false;
    }
    
    /**
     * Release reserved credits
     */
    public boolean releaseCredits(String userId, double amount) {
        if (!validateUserId(userId) || !validateCreditAmount(amount)) {
            return false;
        }
        
        CreditBalance balance = getCreditBalanceObject(userId);
        
        // Transfer from reserved to available
        if (balance.transferCredits(CreditState.RESERVED, CreditState.AVAILABLE, amount)) {
            updateBalanceInDatabase(userId, balance);
            logCreditActivity(userId, "RELEASE", amount);
            Log.i(TAG, "Credits released successfully: " + amount + " for user: " + userId);
            return true;
        }
        
        return false;
    }
    
    /**
     * Get reserved credits amount
     */
    public double getReservedCredits(String userId) {
        if (!validateUserId(userId)) {
            return 0.0;
        }
        
        CreditBalance balance = getCreditBalanceObject(userId);
        return balance.getReservedCredits();
    }
    
    /**
     * Transfer credits between users
     */
    public boolean transferCredits(String fromUserId, String toUserId, double amount) {
        if (!validateUserId(fromUserId) || !validateUserId(toUserId) || !validateCreditAmount(amount)) {
            return false;
        }
        
        if (fromUserId.equals(toUserId)) {
            Log.e(TAG, "Cannot transfer credits to same user");
            return false;
        }
        
        // Check daily transfer limit
        if (!checkDailyTransferLimit(fromUserId, amount)) {
            Log.e(TAG, "Daily transfer limit exceeded for user: " + fromUserId);
            return false;
        }
        
        if (!validateCreditBalance(fromUserId, amount)) {
            Log.e(TAG, "Insufficient credits for transfer from user: " + fromUserId);
            return false;
        }
        
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            db.beginTransaction();
            
            // Deduct from sender
            if (!deductCredits(fromUserId, amount, TRANSACTION_TRANSFER)) {
                return false;
            }
            
            // Add to receiver
            if (!addCredits(toUserId, amount, TRANSACTION_TRANSFER)) {
                // Rollback sender deduction
                addCredits(fromUserId, amount, TRANSACTION_REFUND);
                return false;
            }
            
            // Create transfer transaction record
            String transactionId = generateTransactionId();
            CreditTransaction transfer = new CreditTransaction(
                transactionId, fromUserId, TRANSACTION_TRANSFER, -amount,
                "Transfer to user: " + toUserId, null, STATUS_COMPLETED,
                new Date(), toUserId
            );
            transfer.setToUserId(toUserId);
            saveTransaction(transfer);
            
            updateDailyLimit(fromUserId, amount, "transfer");
            logCreditActivity(fromUserId, "TRANSFER_OUT", amount);
            logCreditActivity(toUserId, "TRANSFER_IN", amount);
            
            db.setTransactionSuccessful();
            Log.i(TAG, "Credits transferred successfully: " + amount + " from " + fromUserId + " to " + toUserId);
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error transferring credits", e);
            return false;
        } finally {
            db.endTransaction();
        }
    }
    
    // ==================== VALIDATION & SECURITY ====================
    
    /**
     * Validate credit amount
     */
    public boolean validateCreditAmount(double amount) {
        return amount >= MIN_CREDIT_AMOUNT && amount <= MAX_CREDIT_AMOUNT;
    }
    
    /**
     * Check credit limits for user
     */
    public boolean checkCreditLimits(String userId, double amount) {
        if (!validateUserId(userId) || !validateCreditAmount(amount)) {
            return false;
        }
        
        // Check if user has sufficient balance
        if (!validateCreditBalance(userId, amount)) {
            return false;
        }
        
        // Additional limit checks can be added here
        return true;
    }
    
    /**
     * Audit credit transaction
     */
    public boolean auditCreditTransaction(String transactionId) {
        CreditTransaction transaction = getTransactionById(transactionId);
        if (transaction == null) {
            return false;
        }
        
        // Perform audit checks
        boolean isValid = true;
        StringBuilder auditTrail = new StringBuilder();
        
        // Check transaction amount validity
        if (!validateCreditAmount(Math.abs(transaction.getAmount()))) {
            isValid = false;
            auditTrail.append("Invalid amount; ");
        }
        
        // Check user exists
        if (!validateUserId(transaction.getUserId())) {
            isValid = false;
            auditTrail.append("Invalid user; ");
        }
        
        // Check transaction type
        if (!validateTransactionType(transaction.getType())) {
            isValid = false;
            auditTrail.append("Invalid transaction type; ");
        }
        
        // Update audit trail
        transaction.setAuditTrail(auditTrail.toString());
        updateTransactionInDatabase(transaction);
        
        logCreditActivity(transaction.getUserId(), "AUDIT", 0);
        Log.i(TAG, "Transaction audited: " + transactionId + " - Valid: " + isValid);
        
        return isValid;
    }
    
    /**
     * Log credit activity
     */
    public void logCreditActivity(String userId, String action, double amount) {
        String logMessage = String.format("User: %s, Action: %s, Amount: %.2f, Time: %s",
                userId, action, amount, new Date().toString());
        Log.i(TAG, logMessage);
        
        // In a real implementation, you might want to store this in a separate audit log table
    }
    
    // ==================== PRIVATE HELPER METHODS ====================
    
    private void initializeCreditPackages() {
        creditPackages = new ArrayList<>();
        
        // Starter packages
        creditPackages.add(new CreditPackage(1, "Starter Pack", "Perfect for beginners", 100.0, 100.0, "PHP", true));
        creditPackages.add(new CreditPackage(2, "Basic Pack", "Great value for regular users", 500.0, 450.0, "PHP", true));
        creditPackages.add(new CreditPackage(3, "Premium Pack", "Best value with bonus credits", 1000.0, 800.0, "PHP", true));
        creditPackages.add(new CreditPackage(4, "Enterprise Pack", "For power users and businesses", 5000.0, 4000.0, "PHP", true));
        
        // Set bonus information with bounds checking
        if (creditPackages.size() > 2) {
            creditPackages.get(2).setDiscountPercentage(10.0); // 10% discount
        }
        if (creditPackages.size() > 3) {
            creditPackages.get(3).setDiscountPercentage(20.0); // 20% discount
            creditPackages.get(3).setBonusDescription("20% bonus credits included");
        }
        // Note: Index 4 would be out of bounds since we only have 4 packages (indices 0-3)
        // The original code was trying to access index 4 which doesn't exist
    }
    
    private boolean validateUserId(String userId) {
        return userId != null && !userId.trim().isEmpty();
    }
    
    private boolean validatePaymentMethod(String paymentMethod) {
        return paymentMethod != null && (PAYMENT_GCASH.equals(paymentMethod) || 
                                        PAYMENT_MAYA.equals(paymentMethod) || 
                                        PAYMENT_TRANSFER.equals(paymentMethod));
    }
    
    private boolean validateTransactionType(String transactionType) {
        return transactionType != null && (TRANSACTION_PURCHASE.equals(transactionType) ||
                                         TRANSACTION_REDEMPTION.equals(transactionType) ||
                                         TRANSACTION_BID.equals(transactionType) ||
                                         TRANSACTION_REFUND.equals(transactionType) ||
                                         TRANSACTION_TRANSFER.equals(transactionType) ||
                                         TRANSACTION_RESERVE.equals(transactionType) ||
                                         TRANSACTION_RELEASE.equals(transactionType));
    }
    
    private String generateTransactionId() {
        return "TXN_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    private boolean processPayment(String userId, double amount, String paymentMethod, String transactionId) {
        // Simplified payment processing
        // In a real implementation, this would integrate with payment gateways
        Log.i(TAG, "Processing payment: " + amount + " via " + paymentMethod + " for user: " + userId);
        
        // Simulate payment processing delay
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // For demo purposes, always return true
        // In real implementation, this would call payment gateway APIs
        return true;
    }
    
    private void saveTransaction(CreditTransaction transaction) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
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
            transaction.getUserId(),
            transaction.getType(),
            transaction.getAmount(),
            transaction.getDescription(),
            transaction.getPaymentMethod(),
            transaction.getStatus(),
            transaction.getCreatedAt().getTime(),
            transaction.getReference()
        });
        
        // Update cache
        if (transactionCache.containsKey(transaction.getUserId())) {
            transactionCache.get(transaction.getUserId()).add(0, transaction);
        }
    }
    
    private void updateTransactionInDatabase(CreditTransaction transaction) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String updateQuery = "UPDATE " + DatabaseHelper.TABLE_CREDIT_TRANSACTIONS + 
                           " SET " + DatabaseHelper.COLUMN_TRANSACTION_STATUS + " = ? " +
                           " WHERE " + DatabaseHelper.COLUMN_TRANSACTION_ID + " = ?";
        
        db.execSQL(updateQuery, new Object[]{transaction.getStatus(), transaction.getTransactionId()});
    }
    
    private void updateBalanceInDatabase(String userId, CreditBalance balance) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String updateQuery = "UPDATE " + DatabaseHelper.TABLE_USERS + 
                           " SET " + DatabaseHelper.COLUMN_USER_CREDITS + " = ? " +
                           " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?";
        
        db.execSQL(updateQuery, new Object[]{balance.getAvailableCredits(), userId});
        balanceCache.put(userId, balance);
    }
    
    private boolean checkDailyPurchaseLimit(String userId, double amount) {
        String key = userId + "_purchase";
        double dailyTotal = dailyLimits.getOrDefault(key, 0.0);
        return (dailyTotal + amount) <= MAX_DAILY_PURCHASE_LIMIT;
    }
    
    private boolean checkDailyTransferLimit(String userId, double amount) {
        String key = userId + "_transfer";
        double dailyTotal = dailyLimits.getOrDefault(key, 0.0);
        return (dailyTotal + amount) <= MAX_DAILY_TRANSFER_LIMIT;
    }
    
    private void updateDailyLimit(String userId, double amount, String type) {
        String key = userId + "_" + type;
        double current = dailyLimits.getOrDefault(key, 0.0);
        dailyLimits.put(key, current + amount);
    }
    
    /**
     * Clear daily limits (call this at midnight or when needed)
     */
    public void clearDailyLimits() {
        dailyLimits.clear();
        Log.i(TAG, "Daily limits cleared");
    }
    
    /**
     * Clear caches (call this when needed to free memory)
     */
    public void clearCaches() {
        balanceCache.clear();
        transactionCache.clear();
        Log.i(TAG, "Caches cleared");
    }
}
