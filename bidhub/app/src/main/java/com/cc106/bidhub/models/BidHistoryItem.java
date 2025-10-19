package com.cc106.bidhub.models;

public class BidHistoryItem {
    private String bidderName;
    private String bidTime;
    private String bidAmount;

    public BidHistoryItem(String bidderName, String bidTime, String bidAmount) {
        this.bidderName = bidderName;
        this.bidTime = bidTime;
        this.bidAmount = bidAmount;
    }

    public String getBidderName() {
        return bidderName;
    }

    public void setBidderName(String bidderName) {
        this.bidderName = bidderName;
    }

    public String getBidTime() {
        return bidTime;
    }

    public void setBidTime(String bidTime) {
        this.bidTime = bidTime;
    }

    public String getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(String bidAmount) {
        this.bidAmount = bidAmount;
    }
}
