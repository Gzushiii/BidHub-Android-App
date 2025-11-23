package com.cc106.bidhub.models;

import java.util.List;

public class Item {
    private String id;
    private String uuidId;
    private String title;
    private String description;
    private int categoryId;
    private double startingBid;
    private double currentBid;
    private Double buyNowPrice;
    private String status;
    private String endDate;
    private List<String> images;
    private User seller;
    
    public Item() {}
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getUuidId() {
        return uuidId;
    }
    
    public void setUuidId(String uuidId) {
        this.uuidId = uuidId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public int getCategoryId() {
        return categoryId;
    }
    
    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }
    
    public double getStartingBid() {
        return startingBid;
    }
    
    public void setStartingBid(double startingBid) {
        this.startingBid = startingBid;
    }
    
    public double getCurrentBid() {
        return currentBid;
    }
    
    public void setCurrentBid(double currentBid) {
        this.currentBid = currentBid;
    }
    
    public Double getBuyNowPrice() {
        return buyNowPrice;
    }
    
    public void setBuyNowPrice(Double buyNowPrice) {
        this.buyNowPrice = buyNowPrice;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getEndDate() {
        return endDate;
    }
    
    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }
    
    public List<String> getImages() {
        return images;
    }
    
    public void setImages(List<String> images) {
        this.images = images;
    }
    
    public User getSeller() {
        return seller;
    }
    
    public void setSeller(User seller) {
        this.seller = seller;
    }
    
    public String getPrimaryImage() {
        if (images != null && !images.isEmpty()) {
            return images.get(0);
        }
        return null;
    }
}

