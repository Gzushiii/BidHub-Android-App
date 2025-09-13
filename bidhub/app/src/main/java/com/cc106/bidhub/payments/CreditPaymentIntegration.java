package com.cc106.bidhub.payments;

import android.content.Context;
import android.util.Log;
import com.cc106.bidhub.credits.SimpleCreditManager;

/**
 * Credit Payment Integration
 * Demonstrates how to integrate the PaymentManager with the existing credit system
 */
public class CreditPaymentIntegration {
    private static final String TAG = "CreditPaymentIntegration";
    
    private PaymentManager paymentManager;
    private SimpleCreditManager creditManager;
    private Context context;
    
    public CreditPaymentIntegration(Context context) {
        this.context = context;
        this.paymentManager = PaymentManager.getInstance(context);
        this.creditManager = new SimpleCreditManager(context);
    }
    
    /**
     * Process credit purchase with payment
     */
    public void purchaseCredits(String userId, String paymentMethod, double amount, int credits, String description) {
        Log.i(TAG, "Processing credit purchase: " + credits + " credits for " + amount);
        
        // Create payment request
        PaymentRequest request = new PaymentRequest();
        request.setUserId(userId);
        request.setPaymentMethod(paymentMethod);
        request.setAmount(amount);
        request.setCurrency("PHP");
        request.setDescription(description);
        request.setUserInfo(userId);
        
        // Add metadata for credit purchase
        request.addMetadata("credits", String.valueOf(credits));
        request.addMetadata("type", "credit_purchase");
        
        // Process payment
        PaymentResult result = paymentManager.processPayment(request);
        
        Log.i(TAG, "Payment result: " + result);
        
        // Handle payment result
        if (result.getStatus() == PaymentStatus.PENDING) {
            // Payment is pending, wait for completion
            waitForPaymentCompletion(userId, result.getTransactionId(), credits);
        } else if (result.isSuccess()) {
            // Payment successful, add credits immediately
            addCreditsToAccount(userId, credits, result.getTransactionId());
        } else {
            // Payment failed
            Log.e(TAG, "Payment failed: " + result.getErrorMessage());
        }
    }
    
    /**
     * Wait for payment completion and add credits
     */
    private void waitForPaymentCompletion(String userId, String transactionId, int credits) {
        new Thread(() -> {
            try {
                // Poll payment status until completion
                PaymentStatus status;
                int attempts = 0;
                int maxAttempts = 30; // 5 minutes max wait time
                
                do {
                    Thread.sleep(10000); // Wait 10 seconds between checks
                    status = paymentManager.checkPaymentStatus(transactionId);
                    attempts++;
                    
                    Log.i(TAG, "Payment status check " + attempts + ": " + status);
                    
                } while (!status.isFinalState() && attempts < maxAttempts);
                
                // Handle final status
                if (status == PaymentStatus.COMPLETED) {
                    addCreditsToAccount(userId, credits, transactionId);
                } else {
                    Log.e(TAG, "Payment not completed: " + status);
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.e(TAG, "Payment completion check interrupted", e);
            }
        }).start();
    }
    
    /**
     * Add credits to user account
     */
    private void addCreditsToAccount(String userId, int credits, String transactionId) {
        try {
            boolean success = creditManager.addCredits(userId, credits, SimpleCreditManager.TRANSACTION_PURCHASE);
            
            if (success) {
                Log.i(TAG, "Credits added successfully: " + credits + " for user " + userId);
                
                // Transaction is already logged by addCredits method
            } else {
                Log.e(TAG, "Failed to add credits to account");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adding credits to account", e);
        }
    }
    
    /**
     * Process refund for credit purchase
     */
    public boolean refundCreditPurchase(String transactionId, double amount) {
        Log.i(TAG, "Processing credit purchase refund: " + transactionId);
        
        // Process payment refund
        boolean refundSuccess = paymentManager.refundPayment(transactionId, amount);
        
        if (refundSuccess) {
            Log.i(TAG, "Payment refund successful: " + transactionId);
            
            // In a real implementation, you would also:
            // 1. Deduct credits from user account
            // 2. Log the refund transaction
            // 3. Send notification to user
            
            return true;
        } else {
            Log.e(TAG, "Payment refund failed: " + transactionId);
            return false;
        }
    }
    
    /**
     * Get payment status for a transaction
     */
    public PaymentStatus getPaymentStatus(String transactionId) {
        return paymentManager.checkPaymentStatus(transactionId);
    }
    
    /**
     * Check if payment method is supported
     */
    public boolean isPaymentMethodSupported(String paymentMethod) {
        return paymentManager.validatePaymentMethod(paymentMethod);
    }
    
    /**
     * Get available payment methods
     */
    public String[] getAvailablePaymentMethods() {
        return new String[]{
            PaymentManager.PAYMENT_METHOD_GCASH,
            PaymentManager.PAYMENT_METHOD_MAYA,
            PaymentManager.PAYMENT_METHOD_TEST
        };
    }
    
    /**
     * Validate payment amount
     */
    public boolean validatePaymentAmount(double amount) {
        try {
            paymentManager.validatePaymentAmount(amount);
            return true;
        } catch (PaymentException e) {
            Log.e(TAG, "Invalid payment amount: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check daily payment limits
     */
    public boolean checkDailyLimits(String userId, double amount) {
        return paymentManager.checkPaymentLimits(userId, amount);
    }
    
    /**
     * Get last payment error
     */
    public PaymentError getLastPaymentError() {
        return paymentManager.getLastPaymentError();
    }
    
    /**
     * Retry failed payment
     */
    public boolean retryPayment(String transactionId) {
        return paymentManager.retryPayment(transactionId);
    }
}
