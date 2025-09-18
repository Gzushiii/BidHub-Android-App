package com.cc106.bidhub.bidding;

/**
 * Bid Status Enumeration
 * Represents the current status of a bid in the auction system
 */
public enum BidStatus {
    PENDING("Pending", "Bid is being processed"),
    ACTIVE("Active", "Bid is currently active and valid"),
    WINNING("Winning", "Bid is currently the highest bid"),
    OUTBID("Outbid", "Bid has been outbid by a higher amount"),
    CANCELLED("Cancelled", "Bid was cancelled"),
    EXPIRED("Expired", "Bid expired due to auction end"),
    INVALID("Invalid", "Bid was marked as invalid");
    
    private final String displayName;
    private final String description;
    
    BidStatus(String displayName, String description) {
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
     * Check if bid status allows editing
     */
    public boolean canBeEdited() {
        return this == PENDING || this == ACTIVE;
    }
    
    /**
     * Check if bid status is final
     */
    public boolean isFinal() {
        return this == WINNING || this == OUTBID || this == CANCELLED || this == EXPIRED || this == INVALID;
    }
    
    /**
     * Check if bid status is active
     */
    public boolean isActive() {
        return this == ACTIVE || this == WINNING;
    }
    
    /**
     * Get next possible statuses
     */
    public BidStatus[] getNextPossibleStatuses() {
        switch (this) {
            case PENDING:
                return new BidStatus[]{ACTIVE, INVALID, CANCELLED};
            case ACTIVE:
                return new BidStatus[]{WINNING, OUTBID, CANCELLED, EXPIRED};
            case WINNING:
                return new BidStatus[]{OUTBID, EXPIRED};
            case OUTBID:
            case CANCELLED:
            case EXPIRED:
            case INVALID:
                return new BidStatus[]{}; // No further transitions
            default:
                return new BidStatus[]{};
        }
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}

