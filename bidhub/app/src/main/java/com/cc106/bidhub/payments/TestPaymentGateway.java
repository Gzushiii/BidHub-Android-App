package com.cc106.bidhub.payments;

import android.util.Log;

import java.util.UUID;

/**
 * Test Payment Gateway Implementation
 * Simulates payment processing for development and testing
 */
public class TestPaymentGateway implements PaymentGateway {
    private static final String TAG = "TestPaymentGateway";
    
    public static final String PAYMENT_METHOD_TEST = "test";
    public static final String PAYMENT_METHOD_GCASH = "gcash";
    public static final String PAYMENT_METHOD_MAYA = "maya";
    
    @Override
    public void processPayment(String userId, double amount, String currency, String description, PaymentCallback callback) {
        Log.i(TAG, "Processing test payment: " + amount + " " + currency + " for user: " + userId);
        
        // Simulate payment processing delay
        new Thread(() -> {
            try {
                Thread.sleep(1000); // Simulate network delay
                
                // Simulate payment success (90% success rate for testing)
                if (Math.random() > 0.1) {
                    String transactionId = "TXN_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
                    String reference = "REF_" + System.currentTimeMillis();
                    
                    Log.i(TAG, "Test payment successful: " + transactionId);
                    callback.onPaymentSuccess(transactionId, reference);
                } else {
                    Log.w(TAG, "Test payment failed (simulated)");
                    callback.onPaymentFailed("TEST_ERROR", "Simulated payment failure for testing");
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                callback.onPaymentFailed("INTERRUPTED", "Payment processing was interrupted");
            }
        }).start();
    }
    
    @Override
    public boolean isPaymentMethodSupported(String paymentMethod) {
        return PAYMENT_METHOD_TEST.equals(paymentMethod) || 
               PAYMENT_METHOD_GCASH.equals(paymentMethod) || 
               PAYMENT_METHOD_MAYA.equals(paymentMethod);
    }
    
    @Override
    public String getGatewayName() {
        return "Test Payment Gateway";
    }
}
