package com.cc106.bidhub.credits;

/**
 * Payment Gateway Interface
 * Defines the contract for payment processing that can be implemented
 * by different payment providers (GCash, Maya, etc.)
 */
public interface PaymentGateway {
    
    /**
     * Process a payment
     * @param userId User ID making the payment
     * @param amount Amount to be paid
     * @param currency Currency code (e.g., "PHP")
     * @param description Payment description
     * @param callback Payment result callback
     */
    void processPayment(String userId, double amount, String currency, String description, PaymentCallback callback);
    
    /**
     * Check if payment method is supported
     * @param paymentMethod Payment method identifier
     * @return true if supported, false otherwise
     */
    boolean isPaymentMethodSupported(String paymentMethod);
    
    /**
     * Get payment gateway name
     * @return Gateway name
     */
    String getGatewayName();
    
    /**
     * Payment result callback interface
     */
    interface PaymentCallback {
        /**
         * Called when payment is successful
         * @param transactionId Unique transaction ID
         * @param reference Payment reference number
         */
        void onPaymentSuccess(String transactionId, String reference);
        
        /**
         * Called when payment fails
         * @param errorCode Error code
         * @param errorMessage Error message
         */
        void onPaymentFailed(String errorCode, String errorMessage);
        
        /**
         * Called when payment is cancelled by user
         */
        void onPaymentCancelled();
    }
}
