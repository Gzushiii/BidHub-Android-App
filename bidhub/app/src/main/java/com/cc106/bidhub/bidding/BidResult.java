package com.cc106.bidhub.bidding;

/**
 * Bid Result
 * Represents the result of a bid placement operation
 */
public class BidResult {
    private boolean success;
    private String message;
    private Bid bid;
    
    public BidResult(boolean success, String message, Bid bid) {
        this.success = success;
        this.message = message;
        this.bid = bid;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Bid getBid() {
        return bid;
    }
    
    public void setBid(Bid bid) {
        this.bid = bid;
    }
    
    @Override
    public String toString() {
        return "BidResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", bid=" + bid +
                '}';
    }
}
