package com.cc106.bidhub.payments;

/**
 * Stripe Payment Response
 * Represents the response from Stripe payment operations
 */
public class StripeResponse {
    private String referenceId;
    private String paymentIntentId;
    private String clientSecret;
    private boolean success;
    private String status;
    private double amount;
    private String currency;
    private String errorCode;
    private String errorMessage;
    private String paymentUrl;
    
    public StripeResponse() {
        this.success = false;
    }
    
    // Factory methods
    public static StripeResponse pending(String referenceId, String paymentUrl) {
        StripeResponse response = new StripeResponse();
        response.referenceId = referenceId;
        response.paymentUrl = paymentUrl;
        response.success = true;
        response.status = "pending";
        return response;
    }
    
    public static StripeResponse success(String referenceId, String paymentIntentId, double amount, String currency) {
        StripeResponse response = new StripeResponse();
        response.referenceId = referenceId;
        response.paymentIntentId = paymentIntentId;
        response.amount = amount;
        response.currency = currency;
        response.success = true;
        response.status = "succeeded";
        return response;
    }
    
    public static StripeResponse failure(String referenceId, String errorCode, String errorMessage) {
        StripeResponse response = new StripeResponse();
        response.referenceId = referenceId;
        response.errorCode = errorCode;
        response.errorMessage = errorMessage;
        response.success = false;
        response.status = "failed";
        return response;
    }
    
    // Getters and Setters
    public String getReferenceId() {
        return referenceId;
    }
    
    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }
    
    public String getPaymentIntentId() {
        return paymentIntentId;
    }
    
    public void setPaymentIntentId(String paymentIntentId) {
        this.paymentIntentId = paymentIntentId;
    }
    
    public String getClientSecret() {
        return clientSecret;
    }
    
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
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
    
    public String getPaymentUrl() {
        return paymentUrl;
    }
    
    public void setPaymentUrl(String paymentUrl) {
        this.paymentUrl = paymentUrl;
    }
    
    @Override
    public String toString() {
        return "StripeResponse{" +
                "referenceId='" + referenceId + '\'' +
                ", paymentIntentId='" + paymentIntentId + '\'' +
                ", success=" + success +
                ", status='" + status + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", errorCode='" + errorCode + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
