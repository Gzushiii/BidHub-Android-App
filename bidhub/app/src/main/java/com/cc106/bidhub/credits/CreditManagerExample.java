package com.cc106.bidhub.credits;

import android.content.Context;
import android.util.Log;

import java.util.List;

/**
 * Example usage of the CreditManager class
 * This demonstrates how to use all the features of the credit management system
 */
public class CreditManagerExample {
    private static final String TAG = "CreditManagerExample";
    
    private CreditManager creditManager;
    private Context context;
    
    public CreditManagerExample(Context context) {
        this.context = context;
        this.creditManager = new CreditManager(context);
    }
    
    /**
     * Demonstrate credit purchase functionality
     */
    public void demonstrateCreditPurchase() {
        Log.i(TAG, "=== Credit Purchase Demo ===");
        
        String userId = "user123";
        
        // Get available packages
        List<CreditPackage> packages = creditManager.getAvailablePackages();
        Log.i(TAG, "Available packages: " + packages.size());
        
        for (CreditPackage pkg : packages) {
            Log.i(TAG, "Package: " + pkg.getName() + " - " + pkg.getCredits() + " credits for " + pkg.getPrice() + " " + pkg.getCurrency());
        }
        
        // Purchase a credit package
        int packageId = 1; // Starter Pack
        String paymentMethod = CreditManager.PAYMENT_GCASH;
        
        boolean success = creditManager.purchaseCredits(userId, packageId, paymentMethod);
        Log.i(TAG, "Purchase result: " + success);
        
        if (success) {
            double balance = creditManager.getCreditBalance(userId);
            Log.i(TAG, "New balance: " + balance);
        }
    }
    
    /**
     * Demonstrate credit operations
     */
    public void demonstrateCreditOperations() {
        Log.i(TAG, "=== Credit Operations Demo ===");
        
        String userId = "user123";
        
        // Check current balance
        double balance = creditManager.getCreditBalance(userId);
        Log.i(TAG, "Current balance: " + balance);
        
        // Validate balance for a transaction
        double requiredAmount = 50.0;
        boolean hasEnough = creditManager.validateCreditBalance(userId, requiredAmount);
        Log.i(TAG, "Has enough credits for " + requiredAmount + ": " + hasEnough);
        
        // Deduct credits (e.g., for a bid)
        if (hasEnough) {
            boolean deducted = creditManager.deductCredits(userId, requiredAmount, CreditManager.TRANSACTION_BID);
            Log.i(TAG, "Credits deducted: " + deducted);
            
            if (deducted) {
                double newBalance = creditManager.getCreditBalance(userId);
                Log.i(TAG, "New balance after deduction: " + newBalance);
            }
        }
        
        // Add credits (e.g., from a refund)
        double refundAmount = 25.0;
        boolean added = creditManager.addCredits(userId, refundAmount, CreditManager.TRANSACTION_REFUND);
        Log.i(TAG, "Credits added: " + added);
        
        if (added) {
            double finalBalance = creditManager.getCreditBalance(userId);
            Log.i(TAG, "Final balance: " + finalBalance);
        }
    }
    
    /**
     * Demonstrate transaction management
     */
    public void demonstrateTransactionManagement() {
        Log.i(TAG, "=== Transaction Management Demo ===");
        
        String userId = "user123";
        
        // Get transaction history
        List<CreditTransaction> transactions = creditManager.getTransactionHistory(userId);
        Log.i(TAG, "Transaction history count: " + transactions.size());
        
        for (CreditTransaction transaction : transactions) {
            Log.i(TAG, "Transaction: " + transaction.getType() + " - " + 
                  transaction.getAmount() + " - " + transaction.getStatus() + " - " + 
                  transaction.getCreatedAt());
        }
        
        // Get specific transaction
        if (!transactions.isEmpty()) {
            String transactionId = transactions.get(0).getTransactionId();
            CreditTransaction specificTransaction = creditManager.getTransactionById(transactionId);
            Log.i(TAG, "Specific transaction: " + specificTransaction);
            
            // Audit the transaction
            boolean isValid = creditManager.auditCreditTransaction(transactionId);
            Log.i(TAG, "Transaction audit result: " + isValid);
        }
    }
    
    /**
     * Demonstrate balance management
     */
    public void demonstrateBalanceManagement() {
        Log.i(TAG, "=== Balance Management Demo ===");
        
        String userId = "user123";
        double amount = 100.0;
        
        // Reserve credits
        boolean reserved = creditManager.reserveCredits(userId, amount);
        Log.i(TAG, "Credits reserved: " + reserved);
        
        if (reserved) {
            double reservedAmount = creditManager.getReservedCredits(userId);
            Log.i(TAG, "Reserved credits: " + reservedAmount);
            
            // Release reserved credits
            boolean released = creditManager.releaseCredits(userId, amount);
            Log.i(TAG, "Credits released: " + released);
        }
    }
    
    /**
     * Demonstrate credit transfer
     */
    public void demonstrateCreditTransfer() {
        Log.i(TAG, "=== Credit Transfer Demo ===");
        
        String fromUserId = "user123";
        String toUserId = "user456";
        double transferAmount = 50.0;
        
        // Check if sender has enough credits
        boolean hasEnough = creditManager.validateCreditBalance(fromUserId, transferAmount);
        Log.i(TAG, "Sender has enough credits: " + hasEnough);
        
        if (hasEnough) {
            // Perform transfer
            boolean transferred = creditManager.transferCredits(fromUserId, toUserId, transferAmount);
            Log.i(TAG, "Transfer successful: " + transferred);
            
            if (transferred) {
                double senderBalance = creditManager.getCreditBalance(fromUserId);
                double receiverBalance = creditManager.getCreditBalance(toUserId);
                Log.i(TAG, "Sender balance: " + senderBalance);
                Log.i(TAG, "Receiver balance: " + receiverBalance);
            }
        }
    }
    
    /**
     * Demonstrate validation and security features
     */
    public void demonstrateValidationAndSecurity() {
        Log.i(TAG, "=== Validation & Security Demo ===");
        
        String userId = "user123";
        double amount = 50.0;
        
        // Validate credit amount
        boolean validAmount = creditManager.validateCreditAmount(amount);
        Log.i(TAG, "Valid amount: " + validAmount);
        
        // Check credit limits
        boolean withinLimits = creditManager.checkCreditLimits(userId, amount);
        Log.i(TAG, "Within limits: " + withinLimits);
        
        // Test invalid amounts
        boolean invalidAmount1 = creditManager.validateCreditAmount(-10.0);
        boolean invalidAmount2 = creditManager.validateCreditAmount(0.0);
        boolean invalidAmount3 = creditManager.validateCreditAmount(2000000.0);
        
        Log.i(TAG, "Negative amount valid: " + invalidAmount1);
        Log.i(TAG, "Zero amount valid: " + invalidAmount2);
        Log.i(TAG, "Excessive amount valid: " + invalidAmount3);
    }
    
    /**
     * Demonstrate package management
     */
    public void demonstratePackageManagement() {
        Log.i(TAG, "=== Package Management Demo ===");
        
        // Get all available packages
        List<CreditPackage> packages = creditManager.getAvailablePackages();
        Log.i(TAG, "Available packages: " + packages.size());
        
        // Get specific package
        int packageId = 1;
        CreditPackage pkg = creditManager.getPackageById(packageId);
        if (pkg != null) {
            Log.i(TAG, "Package details: " + pkg.getName() + " - " + pkg.getCredits() + " credits");
            Log.i(TAG, "Package price: " + creditManager.calculatePackagePrice(packageId));
            Log.i(TAG, "Package available: " + creditManager.isPackageAvailable(packageId));
        }
    }
    
    /**
     * Run all demonstrations
     */
    public void runAllDemonstrations() {
        Log.i(TAG, "Starting CreditManager demonstrations...");
        
        demonstratePackageManagement();
        demonstrateCreditPurchase();
        demonstrateCreditOperations();
        demonstrateTransactionManagement();
        demonstrateBalanceManagement();
        demonstrateCreditTransfer();
        demonstrateValidationAndSecurity();
        
        Log.i(TAG, "All demonstrations completed!");
    }
}
