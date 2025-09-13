package com.cc106.bidhub.payments;

/**
 * Payment Error Model
 * Contains detailed error information for payment operations
 */
public class PaymentError {
    private String errorCode;
    private String errorMessage;
    private String transactionId;
    private String paymentMethod;
    private long timestamp;
    private String details;
    private String stackTrace;
    private boolean retryable;
    private int retryCount;
    
    public PaymentError() {
        this.timestamp = System.currentTimeMillis();
        this.retryable = false;
        this.retryCount = 0;
    }
    
    public PaymentError(String errorCode, String errorMessage) {
        this();
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
    
    public PaymentError(String errorCode, String errorMessage, String transactionId) {
        this(errorCode, errorMessage);
        this.transactionId = transactionId;
    }
    
    // Static factory methods for common errors
    public static PaymentError invalidAmount(double amount) {
        return new PaymentError("INVALID_AMOUNT", "Invalid payment amount: " + amount);
    }
    
    public static PaymentError invalidPaymentMethod(String method) {
        return new PaymentError("INVALID_PAYMENT_METHOD", "Unsupported payment method: " + method);
    }
    
    public static PaymentError networkError(String details) {
        PaymentError error = new PaymentError("NETWORK_ERROR", "Network connection failed");
        error.setDetails(details);
        error.setRetryable(true);
        return error;
    }
    
    public static PaymentError gatewayError(String gateway, String details) {
        PaymentError error = new PaymentError("GATEWAY_ERROR", "Payment gateway error: " + gateway);
        error.setDetails(details);
        error.setRetryable(true);
        return error;
    }
    
    public static PaymentError validationError(String field, String reason) {
        return new PaymentError("VALIDATION_ERROR", "Validation failed for " + field + ": " + reason);
    }
    
    public static PaymentError fraudDetection(String reason) {
        PaymentError error = new PaymentError("FRAUD_DETECTED", "Fraud detection triggered: " + reason);
        error.setRetryable(false);
        return error;
    }
    
    public static PaymentError timeout(String operation) {
        PaymentError error = new PaymentError("TIMEOUT", "Operation timeout: " + operation);
        error.setRetryable(true);
        return error;
    }
    
    // Getters and Setters
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
    
    public String getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getDetails() {
        return details;
    }
    
    public void setDetails(String details) {
        this.details = details;
    }
    
    public String getStackTrace() {
        return stackTrace;
    }
    
    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }
    
    public boolean isRetryable() {
        return retryable;
    }
    
    public void setRetryable(boolean retryable) {
        this.retryable = retryable;
    }
    
    public int getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
    
    public void incrementRetryCount() {
        this.retryCount++;
    }
    
    @Override
    public String toString() {
        return "PaymentError{" +
                "errorCode='" + errorCode + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", transactionId='" + transactionId + '\'' +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", retryable=" + retryable +
                ", retryCount=" + retryCount +
                ", timestamp=" + timestamp +
                '}';
    }
}
