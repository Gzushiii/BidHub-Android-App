package com.cc106.bidhub.payments;

import android.content.Context;
import android.util.Log;

/**
 * PaymentManager Usage Examples
 * Demonstrates how to use the unified payment processing system
 */
public class PaymentManagerExample {
    private static final String TAG = "PaymentManagerExample";
    
    private PaymentManager paymentManager;
    private Context context;
    
    public PaymentManagerExample(Context context) {
        this.context = context;
        this.paymentManager = PaymentManager.getInstance(context);
    }
    
    /**
     * Example: Process a basic payment
     */
    public void processBasicPayment() {
        Log.i(TAG, "=== Processing Basic Payment ===");
        
        // Create payment request
        PaymentRequest request = new PaymentRequest();
        request.setUserId("user123");
        request.setPaymentMethod(PaymentManager.PAYMENT_METHOD_GCASH);
        request.setAmount(100.0);
        request.setCurrency("PHP");
        request.setDescription("Credit purchase");
        request.setUserInfo("John Doe");
        
        // Process payment
        PaymentResult result = paymentManager.processPayment(request);
        
        Log.i(TAG, "Payment Result: " + result);
        
        // Check payment status
        if (result.getStatus() == PaymentStatus.PENDING) {
            Log.i(TAG, "Payment is pending, checking status...");
            
            // Simulate status check after delay
            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    PaymentStatus status = paymentManager.checkPaymentStatus(result.getTransactionId());
                    Log.i(TAG, "Payment status: " + status);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }
    
    /**
     * Example: Process GCash payment
     */
    public void processGCashPayment() {
        Log.i(TAG, "=== Processing GCash Payment ===");
        
        double amount = 250.0;
        String userInfo = "Jane Smith";
        
        // Initiate GCash payment
        GCashResponse response = paymentManager.initiateGCashPayment(amount, userInfo);
        
        Log.i(TAG, "GCash Response: " + response);
        
        if (response.isSuccess() && response.getStatus() == PaymentStatus.PENDING) {
            // Verify payment after user completes it
            new Thread(() -> {
                try {
                    Thread.sleep(5000); // Simulate user completing payment
                    
                    boolean verified = paymentManager.verifyGCashPayment(response.getReferenceId());
                    Log.i(TAG, "GCash payment verified: " + verified);
                    
                    if (verified) {
                        GCashResponse statusResponse = paymentManager.getGCashPaymentStatus(response.getReferenceId());
                        Log.i(TAG, "GCash status: " + statusResponse);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }
    
    /**
     * Example: Process Maya payment
     */
    public void processMayaPayment() {
        Log.i(TAG, "=== Processing Maya Payment ===");
        
        double amount = 500.0;
        String userInfo = "Bob Johnson";
        
        // Initiate Maya payment
        MayaResponse response = paymentManager.initiateMayaPayment(amount, userInfo);
        
        Log.i(TAG, "Maya Response: " + response);
        
        if (response.isSuccess() && response.getStatus() == PaymentStatus.PENDING) {
            // Verify payment after user completes it
            new Thread(() -> {
                try {
                    Thread.sleep(5000); // Simulate user completing payment
                    
                    boolean verified = paymentManager.verifyMayaPayment(response.getReferenceId());
                    Log.i(TAG, "Maya payment verified: " + verified);
                    
                    if (verified) {
                        MayaResponse statusResponse = paymentManager.getMayaPaymentStatus(response.getReferenceId());
                        Log.i(TAG, "Maya status: " + statusResponse);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }
    
    /**
     * Example: Payment validation
     */
    public void demonstratePaymentValidation() {
        Log.i(TAG, "=== Payment Validation Examples ===");
        
        // Validate payment method
        boolean isGCashSupported = paymentManager.validatePaymentMethod(PaymentManager.PAYMENT_METHOD_GCASH);
        Log.i(TAG, "GCash supported: " + isGCashSupported);
        
        boolean isPayPalSupported = paymentManager.validatePaymentMethod("paypal");
        Log.i(TAG, "PayPal supported: " + isPayPalSupported);
        
        // Validate payment amount
        try {
            paymentManager.validatePaymentAmount(50.0);
            Log.i(TAG, "Amount 50.0 is valid");
        } catch (PaymentException e) {
            Log.e(TAG, "Amount validation failed: " + e.getMessage());
        }
        
        try {
            paymentManager.validatePaymentAmount(0.5);
            Log.i(TAG, "Amount 0.5 is valid");
        } catch (PaymentException e) {
            Log.e(TAG, "Amount validation failed: " + e.getMessage());
        }
        
        // Check payment limits
        boolean withinLimits = paymentManager.checkPaymentLimits("user123", 1000.0);
        Log.i(TAG, "Payment within limits: " + withinLimits);
        
        // Validate payment reference
        boolean validRef = paymentManager.validatePaymentReference("REF12345678");
        Log.i(TAG, "Reference valid: " + validRef);
        
        boolean invalidRef = paymentManager.validatePaymentReference("invalid-ref");
        Log.i(TAG, "Invalid reference valid: " + invalidRef);
    }
    
    /**
     * Example: Error handling and retry
     */
    public void demonstrateErrorHandling() {
        Log.i(TAG, "=== Error Handling Examples ===");
        
        // Create a payment request that might fail
        PaymentRequest request = new PaymentRequest();
        request.setUserId("user123");
        request.setPaymentMethod(PaymentManager.PAYMENT_METHOD_TEST);
        request.setAmount(100.0);
        request.setCurrency("PHP");
        request.setDescription("Test payment for error handling");
        
        PaymentResult result = paymentManager.processPayment(request);
        Log.i(TAG, "Payment Result: " + result);
        
        // Check for errors
        PaymentError lastError = paymentManager.getLastPaymentError();
        if (lastError != null) {
            Log.e(TAG, "Last payment error: " + lastError);
            
            // Retry if possible
            if (lastError.isRetryable()) {
                Log.i(TAG, "Attempting to retry payment...");
                boolean retrySuccess = paymentManager.retryPayment(result.getTransactionId());
                Log.i(TAG, "Retry initiated: " + retrySuccess);
            }
        }
    }
    
    /**
     * Example: Refund processing
     */
    public void demonstrateRefund() {
        Log.i(TAG, "=== Refund Processing Example ===");
        
        // First, process a payment
        PaymentRequest request = new PaymentRequest();
        request.setUserId("user123");
        request.setPaymentMethod(PaymentManager.PAYMENT_METHOD_GCASH);
        request.setAmount(200.0);
        request.setCurrency("PHP");
        request.setDescription("Payment for refund test");
        
        PaymentResult result = paymentManager.processPayment(request);
        Log.i(TAG, "Original payment: " + result);
        
        // Wait for payment to complete, then refund
        new Thread(() -> {
            try {
                Thread.sleep(3000); // Wait for payment to complete
                
                // Check if payment can be refunded
                PaymentStatus status = paymentManager.checkPaymentStatus(result.getTransactionId());
                Log.i(TAG, "Payment status before refund: " + status);
                
                if (status.canBeRefunded()) {
                    boolean refundSuccess = paymentManager.refundPayment(result.getTransactionId(), 200.0);
                    Log.i(TAG, "Refund successful: " + refundSuccess);
                    
                    // Check status after refund
                    PaymentStatus refundStatus = paymentManager.checkPaymentStatus(result.getTransactionId());
                    Log.i(TAG, "Payment status after refund: " + refundStatus);
                } else {
                    Log.w(TAG, "Payment cannot be refunded: " + status);
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    /**
     * Example: Signature verification
     */
    public void demonstrateSignatureVerification() {
        Log.i(TAG, "=== Signature Verification Example ===");
        
        String data = "payment_data_123";
        String signature = "abc123def456"; // This would be generated by the payment gateway
        
        boolean isValid = paymentManager.verifyPaymentSignature(signature, data);
        Log.i(TAG, "Signature valid: " + isValid);
    }
    
    /**
     * Run all examples
     */
    public void runAllExamples() {
        Log.i(TAG, "Starting PaymentManager Examples...");
        
        processBasicPayment();
        processGCashPayment();
        processMayaPayment();
        demonstratePaymentValidation();
        demonstrateErrorHandling();
        demonstrateRefund();
        demonstrateSignatureVerification();
        
        Log.i(TAG, "PaymentManager Examples completed!");
    }
}
