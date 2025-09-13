package com.cc106.bidhub.items;

import java.util.List;
import java.util.ArrayList;
import java.util.Date;

/**
 * Item Data Model
 * Contains data for creating or updating items
 */
public class ItemData {
    private String title;
    private String description;
    private double startingPrice;
    private double buyNowPrice;
    private String currency;
    private String categoryId;
    private List<String> imagePaths;
    private List<String> tags;
    private String condition;
    private String location;
    private String shippingInfo;
    private Date startDate;
    private Date endDate;
    private String notes;
    private String metadata;
    
    public ItemData() {
        this.imagePaths = new ArrayList<>();
        this.tags = new ArrayList<>();
        this.currency = "PHP";
    }
    
    public ItemData(String title, String description, double startingPrice) {
        this();
        this.title = title;
        this.description = description;
        this.startingPrice = startingPrice;
    }
    
    // Getters and Setters
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
    
    public String getCategoryId() {
        return categoryId;
    }
    
    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
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
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
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
    
    @Override
    public String toString() {
        return "ItemData{" +
                "title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", startingPrice=" + startingPrice +
                ", buyNowPrice=" + buyNowPrice +
                ", currency='" + currency + '\'' +
                ", categoryId='" + categoryId + '\'' +
                ", condition='" + condition + '\'' +
                ", location='" + location + '\'' +
                '}';
    }
}
