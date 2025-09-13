package com.cc106.bidhub.redemption;

/**
 * Redemption Code Status Enumeration
 * Defines all possible states of a redemption code
 */
public enum RedemptionCodeStatus {
    GENERATED("Generated", "Code generated, not sent"),
    SENT("Sent", "Code sent to user"),
    DELIVERED("Delivered", "Code delivered successfully"),
    REDEEMED("Redeemed", "Code redeemed"),
    EXPIRED("Expired", "Code expired"),
    INVALID("Invalid", "Code invalid");
    
    private final String displayName;
    private final String description;
    
    RedemptionCodeStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if status is final (no further changes expected)
     */
    public boolean isFinalStatus() {
        return this == REDEEMED || this == EXPIRED || this == INVALID;
    }
    
    /**
     * Check if code can be redeemed
     */
    public boolean canBeRedeemed() {
        return this == DELIVERED;
    }
    
    /**
     * Check if code is active
     */
    public boolean isActive() {
        return this != EXPIRED && this != INVALID;
    }
    
    /**
     * Check if code is successfully completed
     */
    public boolean isCompleted() {
        return this == REDEEMED;
    }
}
