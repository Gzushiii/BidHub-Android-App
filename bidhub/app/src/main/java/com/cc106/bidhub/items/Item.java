package com.cc106.bidhub.items;

import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

/**
 * Item Model
 * Represents an item in the auction system
 */
public class Item {
    private String itemId;
    private String title;
    private String description;
    private double startingPrice;
    private double currentPrice;
    private double buyNowPrice;
    private String currency;
    private String sellerId;
    private String sellerName;
    private String categoryId;
    private String categoryName;
    private ItemStatus status;
    private List<String> imagePaths;
    private List<String> tags;
    private String condition;
    private String shippingInfo;
    private Date startDate;
    private Date endDate;
    private Date createdAt;
    private Date updatedAt;
    private int viewCount;
    private int bidCount;
    private String highestBidderId;
    private String highestBidderName;
    private boolean isFeatured;
    private boolean isTrending;
    private double rating;
    private int reviewCount;
    private String notes;
    private String metadata;
    
    public Item() {
        this.itemId = UUID.randomUUID().toString();
        this.imagePaths = new ArrayList<>();
        this.tags = new ArrayList<>();
        this.status = ItemStatus.DRAFT;
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.viewCount = 0;
        this.bidCount = 0;
        this.isFeatured = false;
        this.isTrending = false;
        this.rating = 0.0;
        this.reviewCount = 0;
        this.currency = "PHP";
    }
    
    public Item(String title, String description, double startingPrice, String sellerId) {
        this();
        this.title = title;
        this.description = description;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice;
        this.sellerId = sellerId;
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
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public double getStartingPrice() {
        return startingPrice;
    }
    
    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }
    
    public double getCurrentPrice() {
        return currentPrice;
    }
    
    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }
    
    public double getBuyNowPrice() {
        return buyNowPrice;
    }
    
    public void setBuyNowPrice(double buyNowPrice) {
        this.buyNowPrice = buyNowPrice;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getSellerId() {
        return sellerId;
    }
    
    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }
    
    public String getSellerName() {
        return sellerName;
    }
    
    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }
    
    public String getCategoryId() {
        return categoryId;
    }
    
    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }
    
    public String getCategoryName() {
        return categoryName;
    }
    
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
    
    public ItemStatus getStatus() {
        return status;
    }
    
    public void setStatus(ItemStatus status) {
        this.status = status;
        this.updatedAt = new Date();
    }
    
    public List<String> getImagePaths() {
        return imagePaths;
    }
    
    public void setImagePaths(List<String> imagePaths) {
        this.imagePaths = imagePaths;
    }
    
    public void addImagePath(String imagePath) {
        if (this.imagePaths == null) {
            this.imagePaths = new ArrayList<>();
        }
        this.imagePaths.add(imagePath);
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    public void addTag(String tag) {
        if (this.tags == null) {
            this.tags = new ArrayList<>();
        }
        this.tags.add(tag);
    }
    
    public String getCondition() {
        return condition;
    }
    
    public void setCondition(String condition) {
        this.condition = condition;
    }
    
    
    public String getShippingInfo() {
        return shippingInfo;
    }
    
    public void setShippingInfo(String shippingInfo) {
        this.shippingInfo = shippingInfo;
    }
    
    public Date getStartDate() {
        return startDate;
    }
    
    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }
    
    public Date getEndDate() {
        return endDate;
    }
    
    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    public Date getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public int getViewCount() {
        return viewCount;
    }
    
    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }
    
    public void incrementViewCount() {
        this.viewCount++;
    }
    
    public int getBidCount() {
        return bidCount;
    }
    
    public void setBidCount(int bidCount) {
        this.bidCount = bidCount;
    }
    
    public void incrementBidCount() {
        this.bidCount++;
    }
    
    public String getHighestBidderId() {
        return highestBidderId;
    }
    
    public void setHighestBidderId(String highestBidderId) {
        this.highestBidderId = highestBidderId;
    }
    
    public String getHighestBidderName() {
        return highestBidderName;
    }
    
    public void setHighestBidderName(String highestBidderName) {
        this.highestBidderName = highestBidderName;
    }
    
    public void setCurrentBidderId(String currentBidderId) {
        this.highestBidderId = currentBidderId;
    }
    
    public boolean isFeatured() {
        return isFeatured;
    }
    
    public void setFeatured(boolean featured) {
        this.isFeatured = featured;
    }
    
    public boolean isTrending() {
        return isTrending;
    }
    
    public void setTrending(boolean trending) {
        this.isTrending = trending;
    }
    
    public double getRating() {
        return rating;
    }
    
    public void setRating(double rating) {
        this.rating = rating;
    }
    
    public int getReviewCount() {
        return reviewCount;
    }
    
    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
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
     * Check if item is active
     */
    public boolean isActive() {
        return status == ItemStatus.ACTIVE;
    }
    
    /**
     * Check if item is available for bidding
     */
    public boolean isAvailableForBidding() {
        return status == ItemStatus.ACTIVE && 
               (endDate == null || endDate.after(new Date()));
    }
    
    /**
     * Check if item has ended
     */
    public boolean hasEnded() {
        return status == ItemStatus.ENDED || 
               (endDate != null && endDate.before(new Date()));
    }
    
    /**
     * Get time remaining in milliseconds
     */
    public long getTimeRemaining() {
        if (endDate == null) {
            return 0;
        }
        return Math.max(0, endDate.getTime() - System.currentTimeMillis());
    }
    
    /**
     * Get time remaining in hours
     */
    public double getHoursRemaining() {
        return getTimeRemaining() / (60.0 * 60.0 * 1000.0);
    }
    
    @Override
    public String toString() {
        return "Item{" +
                "itemId='" + itemId + '\'' +
                ", title='" + title + '\'' +
                ", startingPrice=" + startingPrice +
                ", currentPrice=" + currentPrice +
                ", sellerId='" + sellerId + '\'' +
                ", status=" + status +
                ", viewCount=" + viewCount +
                ", bidCount=" + bidCount +
                '}';
    }
}
