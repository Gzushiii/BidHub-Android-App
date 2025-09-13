package com.cc106.bidhub.payments;

/**
 * Payment Status Enumeration
 * Defines all possible payment states in the system
 */
public enum PaymentStatus {
    PENDING("Pending", "Payment initiated, waiting for confirmation"),
    PROCESSING("Processing", "Payment being processed"),
    COMPLETED("Completed", "Payment successful"),
    FAILED("Failed", "Payment failed"),
    REFUNDED("Refunded", "Payment refunded"),
    CANCELLED("Cancelled", "Payment cancelled");
    
    private final String displayName;
    private final String description;
    
    PaymentStatus(String displayName, String description) {
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
     * Check if payment is in a final state (no further changes expected)
     */
    public boolean isFinalState() {
        return this == COMPLETED || this == FAILED || this == REFUNDED || this == CANCELLED;
    }
    
    /**
     * Check if payment is successful
     */
    public boolean isSuccessful() {
        return this == COMPLETED;
    }
    
    /**
     * Check if payment can be refunded
     */
    public boolean canBeRefunded() {
        return this == COMPLETED;
    }
    
    /**
     * Check if payment can be retried
     */
    public boolean canBeRetried() {
        return this == FAILED;
    }
}
