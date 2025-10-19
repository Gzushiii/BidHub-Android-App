package com.cc106.bidhub.bidding;

import java.util.Date;
import java.util.UUID;

/**
 * Bid Model
 * Represents a bid placed on an item in the auction system
 */
public class Bid {
    private String bidId;
    private String itemId;
    private String bidderId;
    private String bidderAlias;
    private double amount;
    private Date placedAt;
    private boolean isWinning;
    private BidStatus status;
    private String notes;
    private String metadata;
    
    // Bid validation constants
    public static final double MIN_BID_INCREMENT = 1.0;
    public static final double MAX_BID_AMOUNT = 1000000.0;
    
    public Bid() {
        this.bidId = UUID.randomUUID().toString();
        this.placedAt = new Date();
        this.isWinning = false;
        this.status = BidStatus.PENDING;
    }
    
    public Bid(String itemId, String bidderId, String bidderAlias, double amount) {
        this();
        this.itemId = itemId;
        this.bidderId = bidderId;
        this.bidderAlias = bidderAlias;
        this.amount = amount;
    }
    
    // Getters and Setters
    public String getBidId() {
        return bidId;
    }
    
    public void setBidId(String bidId) {
        this.bidId = bidId;
    }
    
    public String getItemId() {
        return itemId;
    }
    
    public void setItemId(String itemId) {
        this.itemId = itemId;
    }
    
    public String getBidderId() {
        return bidderId;
    }
    
    public void setBidderId(String bidderId) {
        this.bidderId = bidderId;
    }
    
    public String getBidderAlias() {
        return bidderAlias;
    }
    
    public void setBidderAlias(String bidderAlias) {
        this.bidderAlias = bidderAlias;
    }
    
    public double getAmount() {
        return amount;
    }
    
    public void setAmount(double amount) {
        this.amount = amount;
    }
    
    public Date getPlacedAt() {
        return placedAt;
    }
    
    public void setPlacedAt(Date placedAt) {
        this.placedAt = placedAt;
    }
    
    public boolean isWinning() {
        return isWinning;
    }
    
    public void setWinning(boolean winning) {
        this.isWinning = winning;
    }
    
    public BidStatus getStatus() {
        return status;
    }
    
    public void setStatus(BidStatus status) {
        this.status = status;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public String getMetadata() {
        return metadata;
    }
    
    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
    
    /**
     * Check if bid is valid
     */
    public boolean isValid() {
        return bidId != null && !bidId.trim().isEmpty() &&
               itemId != null && !itemId.trim().isEmpty() &&
               bidderId != null && !bidderId.trim().isEmpty() &&
               bidderAlias != null && !bidderAlias.trim().isEmpty() &&
               amount >= MIN_BID_INCREMENT && amount <= MAX_BID_AMOUNT &&
               placedAt != null;
    }
    
    /**
     * Check if bid is active
     */
    public boolean isActive() {
        return status == BidStatus.ACTIVE || status == BidStatus.WINNING;
    }
    
    /**
     * Check if bid is completed
     */
    public boolean isCompleted() {
        return status == BidStatus.WINNING || status == BidStatus.OUTBID || status == BidStatus.CANCELLED;
    }
    
    /**
     * Get time since bid was placed in milliseconds
     */
    public long getTimeSincePlaced() {
        if (placedAt == null) {
            return 0;
        }
        return System.currentTimeMillis() - placedAt.getTime();
    }
    
    /**
     * Get time since bid was placed in minutes
     */
    public double getMinutesSincePlaced() {
        return getTimeSincePlaced() / (60.0 * 1000.0);
    }
    
    @Override
    public String toString() {
        return "Bid{" +
                "bidId='" + bidId + '\'' +
                ", itemId='" + itemId + '\'' +
                ", bidderId='" + bidderId + '\'' +
                ", bidderAlias='" + bidderAlias + '\'' +
                ", amount=" + amount +
                ", placedAt=" + placedAt +
                ", isWinning=" + isWinning +
                ", status=" + status +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Bid bid = (Bid) obj;
        return bidId != null ? bidId.equals(bid.bidId) : bid.bidId == null;
    }
    
    @Override
    public int hashCode() {
        return bidId != null ? bidId.hashCode() : 0;
    }
}


