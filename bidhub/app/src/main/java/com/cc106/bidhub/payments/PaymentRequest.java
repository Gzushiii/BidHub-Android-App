package com.cc106.bidhub.payments;

import java.util.Map;
import java.util.HashMap;

/**
 * Payment Request Model
 * Contains all necessary information for processing a payment
 */
public class PaymentRequest {
    private String userId;
    private String paymentMethod;
    private double amount;
    private String currency;
    private String description;
    private String userInfo;
    private Map<String, String> metadata;
    private String callbackUrl;
    private int retryCount;
    private long timestamp;
    
    public PaymentRequest() {
        this.metadata = new HashMap<>();
        this.retryCount = 0;
        this.timestamp = System.currentTimeMillis();
    }
    
    public PaymentRequest(String userId, String paymentMethod, double amount, String currency, String description) {
        this();
        this.userId = userId;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
    }
    
    // Getters and Setters
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
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
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getUserInfo() {
        return userInfo;
    }
    
    public void setUserInfo(String userInfo) {
        this.userInfo = userInfo;
    }
    
    public Map<String, String> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
    
    public void addMetadata(String key, String value) {
        this.metadata.put(key, value);
    }
    
    public String getCallbackUrl() {
        return callbackUrl;
    }
    
    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
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
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return "PaymentRequest{" +
                "userId='" + userId + '\'' +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", description='" + description + '\'' +
                ", retryCount=" + retryCount +
                ", timestamp=" + timestamp +
                '}';
    }
}
