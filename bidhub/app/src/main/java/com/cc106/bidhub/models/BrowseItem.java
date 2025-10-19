package com.cc106.bidhub.models;

public class BrowseItem {
    private String itemId;
    private String title;
    private String currentBid;
    private String timeLeft;
    private String imageUrl;
    private String category;
    private boolean isBuyNow;
    private String status; // "ending_soon", "new_listing", "buy_now", etc.

    public BrowseItem() {
    }

    public BrowseItem(String itemId, String title, String currentBid, String timeLeft, String imageUrl, String category, boolean isBuyNow, String status) {
        this.itemId = itemId;
        this.title = title;
        this.currentBid = currentBid;
        this.timeLeft = timeLeft;
        this.imageUrl = imageUrl;
        this.category = category;
        this.isBuyNow = isBuyNow;
        this.status = status;
    }

    // Getters and Setters
    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCurrentBid() {
        return currentBid;
    }

    public void setCurrentBid(String currentBid) {
        this.currentBid = currentBid;
    }

    public String getTimeLeft() {
        return timeLeft;
    }

    public void setTimeLeft(String timeLeft) {
        this.timeLeft = timeLeft;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isBuyNow() {
        return isBuyNow;
    }

    public void setBuyNow(boolean buyNow) {
        isBuyNow = buyNow;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
