package com.cc106.bidhub.credits;

/**
 * Represents a user's credit balance with different states
 */
public class CreditBalance {
    private String userId;
    private double availableCredits;
    private double reservedCredits;
    private double pendingCredits;
    private double frozenCredits;
    private double totalCredits;
    private long lastUpdated;

    // Constructors
    public CreditBalance() {}

    public CreditBalance(String userId, double availableCredits, double reservedCredits, 
                        double pendingCredits, double frozenCredits) {
        this.userId = userId;
        this.availableCredits = availableCredits;
        this.reservedCredits = reservedCredits;
        this.pendingCredits = pendingCredits;
        this.frozenCredits = frozenCredits;
        this.totalCredits = availableCredits + reservedCredits + pendingCredits + frozenCredits;
        this.lastUpdated = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public double getAvailableCredits() {
        return availableCredits;
    }

    public void setAvailableCredits(double availableCredits) {
        this.availableCredits = availableCredits;
        updateTotalCredits();
    }

    public double getReservedCredits() {
        return reservedCredits;
    }

    public void setReservedCredits(double reservedCredits) {
        this.reservedCredits = reservedCredits;
        updateTotalCredits();
    }

    public double getPendingCredits() {
        return pendingCredits;
    }

    public void setPendingCredits(double pendingCredits) {
        this.pendingCredits = pendingCredits;
        updateTotalCredits();
    }

    public double getFrozenCredits() {
        return frozenCredits;
    }

    public void setFrozenCredits(double frozenCredits) {
        this.frozenCredits = frozenCredits;
        updateTotalCredits();
    }

    public double getTotalCredits() {
        return totalCredits;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    /**
     * Update the total credits when any component changes
     */
    private void updateTotalCredits() {
        this.totalCredits = availableCredits + reservedCredits + pendingCredits + frozenCredits;
        this.lastUpdated = System.currentTimeMillis();
    }

    /**
     * Get credits by state
     */
    public double getCreditsByState(CreditState state) {
        switch (state) {
            case AVAILABLE:
                return availableCredits;
            case RESERVED:
                return reservedCredits;
            case PENDING:
                return pendingCredits;
            case FROZEN:
                return frozenCredits;
            default:
                return 0.0;
        }
    }

    /**
     * Set credits by state
     */
    public void setCreditsByState(CreditState state, double amount) {
        switch (state) {
            case AVAILABLE:
                setAvailableCredits(amount);
                break;
            case RESERVED:
                setReservedCredits(amount);
                break;
            case PENDING:
                setPendingCredits(amount);
                break;
            case FROZEN:
                setFrozenCredits(amount);
                break;
        }
    }

    /**
     * Transfer credits between states
     */
    public boolean transferCredits(CreditState fromState, CreditState toState, double amount) {
        double fromAmount = getCreditsByState(fromState);
        if (fromAmount < amount) {
            return false; // Insufficient credits in source state
        }

        setCreditsByState(fromState, fromAmount - amount);
        setCreditsByState(toState, getCreditsByState(toState) + amount);
        return true;
    }

    @Override
    public String toString() {
        return "CreditBalance{" +
                "userId='" + userId + '\'' +
                ", availableCredits=" + availableCredits +
                ", reservedCredits=" + reservedCredits +
                ", pendingCredits=" + pendingCredits +
                ", frozenCredits=" + frozenCredits +
                ", totalCredits=" + totalCredits +
                '}';
    }
}
