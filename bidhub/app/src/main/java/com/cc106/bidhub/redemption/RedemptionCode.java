package com.cc106.bidhub.redemption;

import java.util.UUID;

/**
 * Redemption Code Model
 * Represents a credit redemption code with all associated data
 */
public class RedemptionCode {
    private String code;
    private String userId;
    private double credits;
    private RedemptionCodeStatus status;
    private long generatedAt;
    private long expiresAt;
    private long redeemedAt;
    private String redeemedBy;
    private String deliveryMethod;
    private String deliveryAddress;
    private String generatedBy;
    private String notes;
    private int usageCount;
    private int maxUsage;
    private String transactionId;
    private boolean isActive;
    private long createdAt;
    private long updatedAt;
    
    public RedemptionCode() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.isActive = true;
        this.usageCount = 0;
        this.maxUsage = 1;
        this.status = RedemptionCodeStatus.GENERATED;
    }
    
    public RedemptionCode(String code, String userId, double credits) {
        this();
        this.code = code;
        this.userId = userId;
        this.credits = credits;
        this.generatedAt = System.currentTimeMillis();
        this.expiresAt = generatedAt + (24 * 60 * 60 * 1000); // 24 hours default
    }
    
    // Getters and Setters
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public double getCredits() {
        return credits;
    }
    
    public void setCredits(double credits) {
        this.credits = credits;
    }
    
    public RedemptionCodeStatus getStatus() {
        return status;
    }
    
    public void setStatus(RedemptionCodeStatus status) {
        this.status = status;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public long getGeneratedAt() {
        return generatedAt;
    }
    
    public void setGeneratedAt(long generatedAt) {
        this.generatedAt = generatedAt;
    }
    
    public long getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public long getRedeemedAt() {
        return redeemedAt;
    }
    
    public void setRedeemedAt(long redeemedAt) {
        this.redeemedAt = redeemedAt;
    }
    
    public String getRedeemedBy() {
        return redeemedBy;
    }
    
    public void setRedeemedBy(String redeemedBy) {
        this.redeemedBy = redeemedBy;
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
    
    public String getGeneratedBy() {
        return generatedBy;
    }
    
    public void setGeneratedBy(String generatedBy) {
        this.generatedBy = generatedBy;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public int getUsageCount() {
        return usageCount;
    }
    
    public void setUsageCount(int usageCount) {
        this.usageCount = usageCount;
    }
    
    public void incrementUsageCount() {
        this.usageCount++;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public int getMaxUsage() {
        return maxUsage;
    }
    
    public void setMaxUsage(int maxUsage) {
        this.maxUsage = maxUsage;
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        this.isActive = active;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    public long getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    /**
     * Check if code is expired
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }
    
    /**
     * Check if code is used
     */
    public boolean isUsed() {
        return usageCount >= maxUsage;
    }
    
    /**
     * Check if code can be redeemed
     */
    public boolean canBeRedeemed() {
        return isActive && !isExpired() && !isUsed() && status != RedemptionCodeStatus.INVALID;
    }
    
    /**
     * Get time until expiry in milliseconds
     */
    public long getTimeUntilExpiry() {
        return Math.max(0, expiresAt - System.currentTimeMillis());
    }
    
    /**
     * Get time until expiry in hours
     */
    public double getHoursUntilExpiry() {
        return getTimeUntilExpiry() / (60.0 * 60.0 * 1000.0);
    }
    
    @Override
    public String toString() {
        return "RedemptionCode{" +
                "code='" + code + '\'' +
                ", userId='" + userId + '\'' +
                ", credits=" + credits +
                ", status=" + status +
                ", generatedAt=" + generatedAt +
                ", expiresAt=" + expiresAt +
                ", redeemedAt=" + redeemedAt +
                ", redeemedBy='" + redeemedBy + '\'' +
                ", deliveryMethod='" + deliveryMethod + '\'' +
                ", usageCount=" + usageCount +
                ", maxUsage=" + maxUsage +
                ", isActive=" + isActive +
                '}';
    }
}
