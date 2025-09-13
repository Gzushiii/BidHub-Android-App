package com.cc106.bidhub.credits;

/**
 * Enum representing different states of credits in the system
 */
public enum CreditState {
    /**
     * Credits available for immediate use
     */
    AVAILABLE("Available"),
    
    /**
     * Credits reserved for pending transaction
     */
    RESERVED("Reserved"),
    
    /**
     * Credits pending approval or verification
     */
    PENDING("Pending"),
    
    /**
     * Credits frozen due to security concerns or disputes
     */
    FROZEN("Frozen");

    private final String displayName;

    CreditState(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
