package com.cc106.bidhub.payments;

/**
 * Payment Result Model
 * Contains the result of a payment processing operation
 */
public class PaymentResult {
    private boolean success;
    private String transactionId;
    private String referenceId;
    private PaymentStatus status;
    private String errorCode;
    private String errorMessage;
    private double amount;
    private String currency;
    private long timestamp;
    private String gatewayResponse;
    private String signature;
    
    public PaymentResult() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public PaymentResult(boolean success, String transactionId, PaymentStatus status) {
        this();
        this.success = success;
        this.transactionId = transactionId;
        this.status = status;
    }
    
    // Static factory methods for common results
    public static PaymentResult success(String transactionId, String referenceId, double amount, String currency) {
        PaymentResult result = new PaymentResult(true, transactionId, PaymentStatus.COMPLETED);
        result.setReferenceId(referenceId);
        result.setAmount(amount);
        result.setCurrency(currency);
        return result;
    }
    
    public static PaymentResult failure(String errorCode, String errorMessage) {
        PaymentResult result = new PaymentResult(false, null, PaymentStatus.FAILED);
        result.setErrorCode(errorCode);
        result.setErrorMessage(errorMessage);
        return result;
    }
    
    public static PaymentResult pending(String transactionId) {
        return new PaymentResult(true, transactionId, PaymentStatus.PENDING);
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
    
    public String getReferenceId() {
        return referenceId;
    }
    
    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }
    
    public PaymentStatus getStatus() {
        return status;
    }
    
    public void setStatus(PaymentStatus status) {
        this.status = status;
    }
    
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
    
    public double getAmount() {
        return amount;
    }
    
    public void setAmount(double amount) {
        this.amount = amount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getGatewayResponse() {
        return gatewayResponse;
    }
    
    public void setGatewayResponse(String gatewayResponse) {
        this.gatewayResponse = gatewayResponse;
    }
    
    public String getSignature() {
        return signature;
    }
    
    public void setSignature(String signature) {
        this.signature = signature;
    }
    
    @Override
    public String toString() {
        return "PaymentResult{" +
                "success=" + success +
                ", transactionId='" + transactionId + '\'' +
                ", referenceId='" + referenceId + '\'' +
                ", status=" + status +
                ", errorCode='" + errorCode + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
