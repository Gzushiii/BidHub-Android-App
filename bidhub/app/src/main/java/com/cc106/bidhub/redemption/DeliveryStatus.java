package com.cc106.bidhub.redemption;

/**
 * Delivery Status Model
 * Contains information about code delivery status
 */
public class DeliveryStatus {
    private String code;
    private String deliveryMethod;
    private String deliveryAddress;
    private DeliveryState state;
    private long sentAt;
    private long deliveredAt;
    private long failedAt;
    private String errorMessage;
    private int retryCount;
    private int maxRetries;
    private boolean isSuccessful;
    private String trackingId;
    private String providerResponse;
    
    public DeliveryStatus() {
        this.retryCount = 0;
        this.maxRetries = 3;
        this.isSuccessful = false;
    }
    
    public DeliveryStatus(String code, String deliveryMethod, String deliveryAddress) {
        this();
        this.code = code;
        this.deliveryMethod = deliveryMethod;
        this.deliveryAddress = deliveryAddress;
        this.state = DeliveryState.PENDING;
        this.sentAt = System.currentTimeMillis();
    }
    
    // Getters and Setters
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getDeliveryMethod() {
        return deliveryMethod;
    }
    
    public void setDeliveryMethod(String deliveryMethod) {
        this.deliveryMethod = deliveryMethod;
    }
    
    public String getDeliveryAddress() {
        return deliveryAddress;
    }
    
    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }
    
    public DeliveryState getState() {
        return state;
    }
    
    public void setState(DeliveryState state) {
        this.state = state;
    }
    
    public long getSentAt() {
        return sentAt;
    }
    
    public void setSentAt(long sentAt) {
        this.sentAt = sentAt;
    }
    
    public long getDeliveredAt() {
        return deliveredAt;
    }
    
    public void setDeliveredAt(long deliveredAt) {
        this.deliveredAt = deliveredAt;
    }
    
    public long getFailedAt() {
        return failedAt;
    }
    
    public void setFailedAt(long failedAt) {
        this.failedAt = failedAt;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
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
    
    public int getMaxRetries() {
        return maxRetries;
    }
    
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }
    
    public boolean isSuccessful() {
        return isSuccessful;
    }
    
    public void setSuccessful(boolean successful) {
        this.isSuccessful = successful;
    }
    
    public String getTrackingId() {
        return trackingId;
    }
    
    public void setTrackingId(String trackingId) {
        this.trackingId = trackingId;
    }
    
    public String getProviderResponse() {
        return providerResponse;
    }
    
    public void setProviderResponse(String providerResponse) {
        this.providerResponse = providerResponse;
    }
    
    /**
     * Check if delivery can be retried
     */
    public boolean canRetry() {
        return retryCount < maxRetries && state == DeliveryState.FAILED;
    }
    
    /**
     * Get delivery duration in milliseconds
     */
    public long getDeliveryDuration() {
        if (deliveredAt > 0 && sentAt > 0) {
            return deliveredAt - sentAt;
        }
        return 0;
    }
    
    @Override
    public String toString() {
        return "DeliveryStatus{" +
                "code='" + code + '\'' +
                ", deliveryMethod='" + deliveryMethod + '\'' +
                ", deliveryAddress='" + deliveryAddress + '\'' +
                ", state=" + state +
                ", sentAt=" + sentAt +
                ", deliveredAt=" + deliveredAt +
                ", failedAt=" + failedAt +
                ", isSuccessful=" + isSuccessful +
                ", retryCount=" + retryCount +
                '}';
    }
}

/**
 * Delivery State Enumeration
 */
enum DeliveryState {
    PENDING("Pending", "Delivery pending"),
    SENT("Sent", "Delivery sent"),
    DELIVERED("Delivered", "Delivery successful"),
    FAILED("Failed", "Delivery failed"),
    RETRYING("Retrying", "Retrying delivery");
    
    private final String displayName;
    private final String description;
    
    DeliveryState(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
}
