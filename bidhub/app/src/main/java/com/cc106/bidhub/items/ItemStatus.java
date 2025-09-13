package com.cc106.bidhub.items;

/**
 * Item Status Enumeration
 * Defines all possible states of an item
 */
public enum ItemStatus {
    DRAFT("Draft", "Item being created"),
    ACTIVE("Active", "Item available for bidding"),
    PAUSED("Paused", "Item temporarily paused"),
    ENDED("Ended", "Item auction ended"),
    SOLD("Sold", "Item sold"),
    CANCELLED("Cancelled", "Item cancelled");
    
    private final String displayName;
    private final String description;
    
    ItemStatus(String displayName, String description) {
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
        return this == ENDED || this == SOLD || this == CANCELLED;
    }
    
    /**
     * Check if item can be bid on
     */
    public boolean canBeBidOn() {
        return this == ACTIVE;
    }
    
    /**
     * Check if item can be edited
     */
    public boolean canBeEdited() {
        return this == DRAFT || this == PAUSED;
    }
    
    /**
     * Check if item is active
     */
    public boolean isActive() {
        return this == ACTIVE;
    }
    
    /**
     * Check if item is completed
     */
    public boolean isCompleted() {
        return this == SOLD || this == ENDED;
    }
}
