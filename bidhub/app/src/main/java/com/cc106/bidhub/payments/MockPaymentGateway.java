package com.cc106.bidhub.payments;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Mock Payment Gateway Implementation
 * Handles payment processing for demo/testing purposes
 */
public class MockPaymentGateway implements PaymentGateway {
    private static final String TAG = "MockPaymentGateway";
    
    public static final String PAYMENT_METHOD_MOCK = "MOCK_PAYMENT";
    
    private Context context;
    
    public MockPaymentGateway() {
        // Mock gateway doesn't need context
    }
    
    public MockPaymentGateway(Context context) {
        this.context = context;
    }
    
    @Override
    public void processPayment(String userId, double amount, String currency, String description, PaymentCallback callback) {
        Log.i(TAG, "Processing mock payment: " + amount + " " + currency + " for user: " + userId);
        
        // Simulate payment processing delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                // Simulate successful payment for demo purposes
                String transactionId = "MOCK_" + System.currentTimeMillis();
                String reference = "REF_" + (int)(Math.random() * 10000);
                
                Log.i(TAG, "Mock payment successful: " + transactionId);
                callback.onPaymentSuccess(transactionId, reference);
                
            } catch (Exception e) {
                Log.e(TAG, "Mock payment failed", e);
                callback.onPaymentFailed("MOCK_ERROR", "Mock payment simulation failed: " + e.getMessage());
            }
        }, 1000); // 1 second delay to simulate network call
    }
    
    @Override
    public boolean isPaymentMethodSupported(String paymentMethod) {
        return PAYMENT_METHOD_MOCK.equals(paymentMethod) || 
               "stripe".equals(paymentMethod) || 
               "card".equals(paymentMethod);
    }
    
    @Override
    public String getGatewayName() {
        return "Mock Payment Gateway";
    }
}
