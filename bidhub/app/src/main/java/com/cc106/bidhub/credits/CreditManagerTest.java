package com.cc106.bidhub.credits;

import android.content.Context;
import android.util.Log;

import java.util.List;

/**
 * Comprehensive test class for the Credit Management System
 * This class validates all functionality and edge cases
 */
public class CreditManagerTest {
    private static final String TAG = "CreditManagerTest";
    
    private CreditManager creditManager;
    private Context context;
    
    // Test user IDs
    private static final String TEST_USER_1 = "test_user_1";
    private static final String TEST_USER_2 = "test_user_2";
    private static final String INVALID_USER = "";
    
    public CreditManagerTest(Context context) {
        this.context = context;
        this.creditManager = new CreditManager(context);
    }
    
    /**
     * Run all tests
     */
    public boolean runAllTests() {
        Log.i(TAG, "Starting CreditManager comprehensive tests...");
        
        boolean allTestsPassed = true;
        
        // Test credit operations
        allTestsPassed &= testCreditOperations();
        
        // Test transaction management
        allTestsPassed &= testTransactionManagement();
        
        // Test credit packages
        allTestsPassed &= testCreditPackages();
        
        // Test balance management
        allTestsPassed &= testBalanceManagement();
        
        // Test validation and security
        allTestsPassed &= testValidationAndSecurity();
        
        // Test error handling
        allTestsPassed &= testErrorHandling();
        
        // Test edge cases
        allTestsPassed &= testEdgeCases();
        
        Log.i(TAG, "All tests completed. Result: " + (allTestsPassed ? "PASSED" : "FAILED"));
        return allTestsPassed;
    }
    
    /**
     * Test basic credit operations
     */
    private boolean testCreditOperations() {
        Log.i(TAG, "Testing credit operations...");
        boolean passed = true;
        
        try {
            // Test initial balance
            double initialBalance = creditManager.getCreditBalance(TEST_USER_1);
            Log.i(TAG, "Initial balance: " + initialBalance);
            
            // Test adding credits
            boolean added = creditManager.addCredits(TEST_USER_1, 100.0, CreditManager.TRANSACTION_PURCHASE);
            passed &= added;
            Log.i(TAG, "Add credits test: " + (added ? "PASSED" : "FAILED"));
            
            if (added) {
                double newBalance = creditManager.getCreditBalance(TEST_USER_1);
                passed &= (newBalance == initialBalance + 100.0);
                Log.i(TAG, "Balance update test: " + (passed ? "PASSED" : "FAILED"));
            }
            
            // Test deducting credits
            boolean deducted = creditManager.deductCredits(TEST_USER_1, 50.0, CreditManager.TRANSACTION_BID);
            passed &= deducted;
            Log.i(TAG, "Deduct credits test: " + (deducted ? "PASSED" : "FAILED"));
            
            if (deducted) {
                double finalBalance = creditManager.getCreditBalance(TEST_USER_1);
                passed &= (finalBalance == initialBalance + 50.0);
                Log.i(TAG, "Final balance test: " + (passed ? "PASSED" : "FAILED"));
            }
            
            // Test balance validation
            boolean hasEnough = creditManager.validateCreditBalance(TEST_USER_1, 25.0);
            passed &= hasEnough;
            Log.i(TAG, "Balance validation test: " + (hasEnough ? "PASSED" : "FAILED"));
            
            boolean notEnough = creditManager.validateCreditBalance(TEST_USER_1, 1000.0);
            passed &= !notEnough;
            Log.i(TAG, "Insufficient balance test: " + (!notEnough ? "PASSED" : "FAILED"));
            
        } catch (Exception e) {
            Log.e(TAG, "Credit operations test failed", e);
            passed = false;
        }
        
        Log.i(TAG, "Credit operations test result: " + (passed ? "PASSED" : "FAILED"));
        return passed;
    }
    
    /**
     * Test transaction management
     */
    private boolean testTransactionManagement() {
        Log.i(TAG, "Testing transaction management...");
        boolean passed = true;
        
        try {
            // Test transaction history
            List<CreditTransaction> transactions = creditManager.getTransactionHistory(TEST_USER_1);
            Log.i(TAG, "Transaction history count: " + transactions.size());
            
            // Test getting specific transaction
            if (!transactions.isEmpty()) {
                String transactionId = transactions.get(0).getTransactionId();
                CreditTransaction transaction = creditManager.getTransactionById(transactionId);
                passed &= (transaction != null);
                Log.i(TAG, "Get transaction by ID test: " + (passed ? "PASSED" : "FAILED"));
                
                // Test transaction auditing
                boolean audited = creditManager.auditCreditTransaction(transactionId);
                Log.i(TAG, "Transaction audit test: " + (audited ? "PASSED" : "FAILED"));
            }
            
            // Test refund (if we have a completed transaction)
            for (CreditTransaction transaction : transactions) {
                if (CreditManager.STATUS_COMPLETED.equals(transaction.getStatus()) && 
                    transaction.getAmount() > 0) {
                    boolean refunded = creditManager.refundTransaction(transaction.getTransactionId());
                    Log.i(TAG, "Refund transaction test: " + (refunded ? "PASSED" : "FAILED"));
                    break;
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Transaction management test failed", e);
            passed = false;
        }
        
        Log.i(TAG, "Transaction management test result: " + (passed ? "PASSED" : "FAILED"));
        return passed;
    }
    
    /**
     * Test credit packages
     */
    private boolean testCreditPackages() {
        Log.i(TAG, "Testing credit packages...");
        boolean passed = true;
        
        try {
            // Test getting available packages
            List<CreditPackage> packages = creditManager.getAvailablePackages();
            passed &= (packages.size() > 0);
            Log.i(TAG, "Available packages test: " + (passed ? "PASSED" : "FAILED"));
            
            // Test package details
            if (!packages.isEmpty()) {
                CreditPackage pkg = packages.get(0);
                int packageId = pkg.getPackageId();
                
                CreditPackage retrievedPkg = creditManager.getPackageById(packageId);
                passed &= (retrievedPkg != null);
                Log.i(TAG, "Get package by ID test: " + (passed ? "PASSED" : "FAILED"));
                
                double price = creditManager.calculatePackagePrice(packageId);
                passed &= (price > 0);
                Log.i(TAG, "Calculate package price test: " + (passed ? "PASSED" : "FAILED"));
                
                boolean available = creditManager.isPackageAvailable(packageId);
                passed &= available;
                Log.i(TAG, "Package availability test: " + (passed ? "PASSED" : "FAILED"));
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Credit packages test failed", e);
            passed = false;
        }
        
        Log.i(TAG, "Credit packages test result: " + (passed ? "PASSED" : "FAILED"));
        return passed;
    }
    
    /**
     * Test balance management
     */
    private boolean testBalanceManagement() {
        Log.i(TAG, "Testing balance management...");
        boolean passed = true;
        
        try {
            // Test credit reservation
            boolean reserved = creditManager.reserveCredits(TEST_USER_1, 25.0);
            passed &= reserved;
            Log.i(TAG, "Reserve credits test: " + (reserved ? "PASSED" : "FAILED"));
            
            if (reserved) {
                double reservedAmount = creditManager.getReservedCredits(TEST_USER_1);
                passed &= (reservedAmount == 25.0);
                Log.i(TAG, "Get reserved credits test: " + (passed ? "PASSED" : "FAILED"));
                
                // Test releasing reserved credits
                boolean released = creditManager.releaseCredits(TEST_USER_1, 25.0);
                passed &= released;
                Log.i(TAG, "Release credits test: " + (released ? "PASSED" : "FAILED"));
            }
            
            // Test credit transfer
            creditManager.addCredits(TEST_USER_2, 100.0, CreditManager.TRANSACTION_PURCHASE);
            boolean transferred = creditManager.transferCredits(TEST_USER_1, TEST_USER_2, 25.0);
            passed &= transferred;
            Log.i(TAG, "Transfer credits test: " + (transferred ? "PASSED" : "FAILED"));
            
        } catch (Exception e) {
            Log.e(TAG, "Balance management test failed", e);
            passed = false;
        }
        
        Log.i(TAG, "Balance management test result: " + (passed ? "PASSED" : "FAILED"));
        return passed;
    }
    
    /**
     * Test validation and security
     */
    private boolean testValidationAndSecurity() {
        Log.i(TAG, "Testing validation and security...");
        boolean passed = true;
        
        try {
            // Test amount validation
            boolean validAmount = creditManager.validateCreditAmount(50.0);
            passed &= validAmount;
            Log.i(TAG, "Valid amount test: " + (validAmount ? "PASSED" : "FAILED"));
            
            boolean invalidAmount = creditManager.validateCreditAmount(-10.0);
            passed &= !invalidAmount;
            Log.i(TAG, "Invalid amount test: " + (!invalidAmount ? "PASSED" : "FAILED"));
            
            boolean excessiveAmount = creditManager.validateCreditAmount(2000000.0);
            passed &= !excessiveAmount;
            Log.i(TAG, "Excessive amount test: " + (!excessiveAmount ? "PASSED" : "FAILED"));
            
            // Test credit limits
            boolean withinLimits = creditManager.checkCreditLimits(TEST_USER_1, 50.0);
            Log.i(TAG, "Credit limits test: " + (withinLimits ? "PASSED" : "FAILED"));
            
        } catch (Exception e) {
            Log.e(TAG, "Validation and security test failed", e);
            passed = false;
        }
        
        Log.i(TAG, "Validation and security test result: " + (passed ? "PASSED" : "FAILED"));
        return passed;
    }
    
    /**
     * Test error handling
     */
    private boolean testErrorHandling() {
        Log.i(TAG, "Testing error handling...");
        boolean passed = true;
        
        try {
            // Test invalid user ID
            double balance = creditManager.getCreditBalance(INVALID_USER);
            passed &= (balance == 0.0);
            Log.i(TAG, "Invalid user ID test: " + (passed ? "PASSED" : "FAILED"));
            
            // Test invalid amount
            boolean added = creditManager.addCredits(TEST_USER_1, -10.0, CreditManager.TRANSACTION_PURCHASE);
            passed &= !added;
            Log.i(TAG, "Invalid amount test: " + (!added ? "PASSED" : "FAILED"));
            
            // Test invalid transaction type
            boolean deducted = creditManager.deductCredits(TEST_USER_1, 10.0, "invalid_type");
            passed &= !deducted;
            Log.i(TAG, "Invalid transaction type test: " + (!deducted ? "PASSED" : "FAILED"));
            
            // Test insufficient balance
            boolean deductedTooMuch = creditManager.deductCredits(TEST_USER_1, 10000.0, CreditManager.TRANSACTION_BID);
            passed &= !deductedTooMuch;
            Log.i(TAG, "Insufficient balance test: " + (!deductedTooMuch ? "PASSED" : "FAILED"));
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling test failed", e);
            passed = false;
        }
        
        Log.i(TAG, "Error handling test result: " + (passed ? "PASSED" : "FAILED"));
        return passed;
    }
    
    /**
     * Test edge cases
     */
    private boolean testEdgeCases() {
        Log.i(TAG, "Testing edge cases...");
        boolean passed = true;
        
        try {
            // Test zero amount
            boolean zeroAmount = creditManager.validateCreditAmount(0.0);
            passed &= !zeroAmount;
            Log.i(TAG, "Zero amount test: " + (!zeroAmount ? "PASSED" : "FAILED"));
            
            // Test minimum amount
            boolean minAmount = creditManager.validateCreditAmount(0.01);
            passed &= minAmount;
            Log.i(TAG, "Minimum amount test: " + (minAmount ? "PASSED" : "FAILED"));
            
            // Test maximum amount
            boolean maxAmount = creditManager.validateCreditAmount(1000000.0);
            passed &= maxAmount;
            Log.i(TAG, "Maximum amount test: " + (maxAmount ? "PASSED" : "FAILED"));
            
            // Test self-transfer
            boolean selfTransfer = creditManager.transferCredits(TEST_USER_1, TEST_USER_1, 10.0);
            passed &= !selfTransfer;
            Log.i(TAG, "Self-transfer test: " + (!selfTransfer ? "PASSED" : "FAILED"));
            
            // Test null transaction ID
            CreditTransaction nullTransaction = creditManager.getTransactionById(null);
            passed &= (nullTransaction == null);
            Log.i(TAG, "Null transaction ID test: " + (passed ? "PASSED" : "FAILED"));
            
        } catch (Exception e) {
            Log.e(TAG, "Edge cases test failed", e);
            passed = false;
        }
        
        Log.i(TAG, "Edge cases test result: " + (passed ? "PASSED" : "FAILED"));
        return passed;
    }
    
    /**
     * Test performance with multiple operations
     */
    public boolean testPerformance() {
        Log.i(TAG, "Testing performance...");
        boolean passed = true;
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Perform multiple operations
            for (int i = 0; i < 100; i++) {
                creditManager.addCredits(TEST_USER_1, 1.0, CreditManager.TRANSACTION_PURCHASE);
                creditManager.deductCredits(TEST_USER_1, 1.0, CreditManager.TRANSACTION_BID);
            }
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            Log.i(TAG, "Performance test completed in " + duration + "ms");
            passed &= (duration < 5000); // Should complete within 5 seconds
            
        } catch (Exception e) {
            Log.e(TAG, "Performance test failed", e);
            passed = false;
        }
        
        Log.i(TAG, "Performance test result: " + (passed ? "PASSED" : "FAILED"));
        return passed;
    }
}
