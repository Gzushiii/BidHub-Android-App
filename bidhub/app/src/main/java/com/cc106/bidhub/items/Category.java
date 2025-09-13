package com.cc106.bidhub.items;

import java.util.List;
import java.util.ArrayList;
import java.util.Date;

/**
 * Category Model
 * Represents an item category
 */
public class Category {
    private String categoryId;
    private String name;
    private String description;
    private String parentCategoryId;
    private String iconPath;
    private String color;
    private int sortOrder;
    private boolean isActive;
    private int itemCount;
    private Date createdAt;
    private Date updatedAt;
    private List<Category> subCategories;
    
    public Category() {
        this.subCategories = new ArrayList<>();
        this.isActive = true;
        this.itemCount = 0;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }
    
    public Category(String name, String description) {
        this();
        this.name = name;
        this.description = description;
    }
    
    // Getters and Setters
    public String getCategoryId() {
        return categoryId;
    }
    
    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getParentCategoryId() {
        return parentCategoryId;
    }
    
    public void setParentCategoryId(String parentCategoryId) {
        this.parentCategoryId = parentCategoryId;
    }
    
    public String getIconPath() {
        return iconPath;
    }
    
    public void setIconPath(String iconPath) {
        this.iconPath = iconPath;
    }
    
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
    }
    
    public int getSortOrder() {
        return sortOrder;
    }
    
    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        this.isActive = active;
    }
    
    public int getItemCount() {
        return itemCount;
    }
    
    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }
    
    public void incrementItemCount() {
        this.itemCount++;
    }
    
    public void decrementItemCount() {
        this.itemCount = Math.max(0, this.itemCount - 1);
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
    
    public List<Category> getSubCategories() {
        return subCategories;
    }
    
    public void setSubCategories(List<Category> subCategories) {
        this.subCategories = subCategories;
    }
    
    public void addSubCategory(Category subCategory) {
        if (this.subCategories == null) {
            this.subCategories = new ArrayList<>();
        }
        this.subCategories.add(subCategory);
    }
    
    /**
     * Check if category has subcategories
     */
    public boolean hasSubCategories() {
        return subCategories != null && !subCategories.isEmpty();
    }
    
    /**
     * Check if category is a parent category
     */
    public boolean isParentCategory() {
        return parentCategoryId == null || parentCategoryId.trim().isEmpty();
    }
    
    @Override
    public String toString() {
        return "Category{" +
                "categoryId='" + categoryId + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", parentCategoryId='" + parentCategoryId + '\'' +
                ", isActive=" + isActive +
                ", itemCount=" + itemCount +
                '}';
    }
}
