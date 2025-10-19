package com.cc106.bidhub.items;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;

/**
 * Filter Criteria Model
 * Contains criteria for filtering items
 */
public class FilterCriteria implements Serializable {
    private String query;
    private String categoryId;
    private List<String> tags;
    private Double minPrice;
    private Double maxPrice;
    private String condition;
    private ItemStatus status;
    private String sellerId;
    private Boolean isFeatured;
    private Boolean isTrending;
    private Date startDateFrom;
    private Date startDateTo;
    private Date endDateFrom;
    private Date endDateTo;
    private String sortBy;
    private String sortOrder;
    private int limit;
    private int offset;
    
    public FilterCriteria() {
        this.tags = new ArrayList<>();
        this.sortBy = "createdAt";
        this.sortOrder = "DESC";
        this.limit = 20;
        this.offset = 0;
    }
    
    // Getters and Setters
    public String getQuery() {
        return query;
    }
    
    public void setQuery(String query) {
        this.query = query;
    }
    
    public String getCategoryId() {
        return categoryId;
    }
    
    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
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
    
    public Double getMinPrice() {
        return minPrice;
    }
    
    public void setMinPrice(Double minPrice) {
        this.minPrice = minPrice;
    }
    
    public Double getMaxPrice() {
        return maxPrice;
    }
    
    public void setMaxPrice(Double maxPrice) {
        this.maxPrice = maxPrice;
    }
    
    public String getCondition() {
        return condition;
    }
    
    public void setCondition(String condition) {
        this.condition = condition;
    }
    
    
    public ItemStatus getStatus() {
        return status;
    }
    
    public void setStatus(ItemStatus status) {
        this.status = status;
    }
    
    public String getSellerId() {
        return sellerId;
    }
    
    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }
    
    public Boolean getIsFeatured() {
        return isFeatured;
    }
    
    public void setIsFeatured(Boolean isFeatured) {
        this.isFeatured = isFeatured;
    }
    
    public Boolean getIsTrending() {
        return isTrending;
    }
    
    public void setIsTrending(Boolean isTrending) {
        this.isTrending = isTrending;
    }
    
    public Date getStartDateFrom() {
        return startDateFrom;
    }
    
    public void setStartDateFrom(Date startDateFrom) {
        this.startDateFrom = startDateFrom;
    }
    
    public Date getStartDateTo() {
        return startDateTo;
    }
    
    public void setStartDateTo(Date startDateTo) {
        this.startDateTo = startDateTo;
    }
    
    public Date getEndDateFrom() {
        return endDateFrom;
    }
    
    public void setEndDateFrom(Date endDateFrom) {
        this.endDateFrom = endDateFrom;
    }
    
    public Date getEndDateTo() {
        return endDateTo;
    }
    
    public void setEndDateTo(Date endDateTo) {
        this.endDateTo = endDateTo;
    }
    
    public String getSortBy() {
        return sortBy;
    }
    
    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }
    
    public String getSortOrder() {
        return sortOrder;
    }
    
    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }
    
    public int getLimit() {
        return limit;
    }
    
    public void setLimit(int limit) {
        this.limit = limit;
    }
    
    public int getOffset() {
        return offset;
    }
    
    public void setOffset(int offset) {
        this.offset = offset;
    }
    
    /**
     * Check if criteria has price range
     */
    public boolean hasPriceRange() {
        return minPrice != null || maxPrice != null;
    }
    
    /**
     * Check if criteria has date range
     */
    public boolean hasDateRange() {
        return startDateFrom != null || startDateTo != null || 
               endDateFrom != null || endDateTo != null;
    }
    
    /**
     * Check if criteria has tags
     */
    public boolean hasTags() {
        return tags != null && !tags.isEmpty();
    }
    
    @Override
    public String toString() {
        return "FilterCriteria{" +
                "query='" + query + '\'' +
                ", categoryId='" + categoryId + '\'' +
                ", minPrice=" + minPrice +
                ", maxPrice=" + maxPrice +
                ", condition='" + condition + '\'' +
                ", status=" + status +
                ", sortBy='" + sortBy + '\'' +
                ", sortOrder='" + sortOrder + '\'' +
                ", limit=" + limit +
                ", offset=" + offset +
                '}';
    }
}
