package com.cc106.bidhub.bidding;

/**
 * Auction Statistics
 * Contains statistical information about auctions
 */
public class AuctionStatistics {
    private int totalAuctions;
    private int activeAuctions;
    private int endedAuctions;
    private int cancelledAuctions;
    private double totalBidValue;
    private int totalBids;
    
    public AuctionStatistics(int totalAuctions, int activeAuctions, int endedAuctions, 
                           int cancelledAuctions, double totalBidValue, int totalBids) {
        this.totalAuctions = totalAuctions;
        this.activeAuctions = activeAuctions;
        this.endedAuctions = endedAuctions;
        this.cancelledAuctions = cancelledAuctions;
        this.totalBidValue = totalBidValue;
        this.totalBids = totalBids;
    }
    
    // Getters and Setters
    public int getTotalAuctions() {
        return totalAuctions;
    }
    
    public void setTotalAuctions(int totalAuctions) {
        this.totalAuctions = totalAuctions;
    }
    
    public int getActiveAuctions() {
        return activeAuctions;
    }
    
    public void setActiveAuctions(int activeAuctions) {
        this.activeAuctions = activeAuctions;
    }
    
    public int getEndedAuctions() {
        return endedAuctions;
    }
    
    public void setEndedAuctions(int endedAuctions) {
        this.endedAuctions = endedAuctions;
    }
    
    public int getCancelledAuctions() {
        return cancelledAuctions;
    }
    
    public void setCancelledAuctions(int cancelledAuctions) {
        this.cancelledAuctions = cancelledAuctions;
    }
    
    public double getTotalBidValue() {
        return totalBidValue;
    }
    
    public void setTotalBidValue(double totalBidValue) {
        this.totalBidValue = totalBidValue;
    }
    
    public int getTotalBids() {
        return totalBids;
    }
    
    public void setTotalBids(int totalBids) {
        this.totalBids = totalBids;
    }
    
    /**
     * Get average bid value
     */
    public double getAverageBidValue() {
        return totalBids > 0 ? totalBidValue / totalBids : 0.0;
    }
    
    /**
     * Get completion rate
     */
    public double getCompletionRate() {
        return totalAuctions > 0 ? (double) endedAuctions / totalAuctions : 0.0;
    }
    
    /**
     * Get cancellation rate
     */
    public double getCancellationRate() {
        return totalAuctions > 0 ? (double) cancelledAuctions / totalAuctions : 0.0;
    }
    
    @Override
    public String toString() {
        return "AuctionStatistics{" +
                "totalAuctions=" + totalAuctions +
                ", activeAuctions=" + activeAuctions +
                ", endedAuctions=" + endedAuctions +
                ", cancelledAuctions=" + cancelledAuctions +
                ", totalBidValue=" + totalBidValue +
                ", totalBids=" + totalBids +
                '}';
    }
}

