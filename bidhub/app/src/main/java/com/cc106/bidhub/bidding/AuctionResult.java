package com.cc106.bidhub.bidding;

/**
 * Auction Result
 * Represents the result of auction processing
 */
public class AuctionResult {
    private boolean success;
    private String message;
    private Bid winningBid;
    
    public AuctionResult(boolean success, String message, Bid winningBid) {
        this.success = success;
        this.message = message;
        this.winningBid = winningBid;
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
    
    public Bid getWinningBid() {
        return winningBid;
    }
    
    public void setWinningBid(Bid winningBid) {
        this.winningBid = winningBid;
    }
    
    @Override
    public String toString() {
        return "AuctionResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", winningBid=" + winningBid +
                '}';
    }
}


