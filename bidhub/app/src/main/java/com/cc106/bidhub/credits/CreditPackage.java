package com.cc106.bidhub.credits;

/**
 * Represents a credit package available for purchase
 */
public class CreditPackage {
    private int packageId;
    private String name;
    private String description;
    private double credits;
    private double price;
    private String currency;
    private boolean isAvailable;
    private double discountPercentage;
    private String bonusDescription;
    private int validityDays; // How long the package is valid
    private String category; // starter, premium, enterprise, etc.

    // Constructors
    public CreditPackage() {}

    public CreditPackage(int packageId, String name, String description, double credits, 
                        double price, String currency, boolean isAvailable) {
        this.packageId = packageId;
        this.name = name;
        this.description = description;
        this.credits = credits;
        this.price = price;
        this.currency = currency;
        this.isAvailable = isAvailable;
    }

    // Getters and Setters
    public int getPackageId() {
        return packageId;
    }

    public void setPackageId(int packageId) {
        this.packageId = packageId;
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

    public double getCredits() {
        return credits;
    }

    public void setCredits(double credits) {
        this.credits = credits;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    public double getDiscountPercentage() {
        return discountPercentage;
    }

    public void setDiscountPercentage(double discountPercentage) {
        this.discountPercentage = discountPercentage;
    }

    public String getBonusDescription() {
        return bonusDescription;
    }

    public void setBonusDescription(String bonusDescription) {
        this.bonusDescription = bonusDescription;
    }

    public int getValidityDays() {
        return validityDays;
    }

    public void setValidityDays(int validityDays) {
        this.validityDays = validityDays;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * Calculate the effective price after discount
     */
    public double getEffectivePrice() {
        if (discountPercentage > 0) {
            return price * (1 - discountPercentage / 100);
        }
        return price;
    }

    /**
     * Calculate the effective credits including bonus
     */
    public double getEffectiveCredits() {
        return credits; // Could add bonus calculation here
    }

    @Override
    public String toString() {
        return "CreditPackage{" +
                "packageId=" + packageId +
                ", name='" + name + '\'' +
                ", credits=" + credits +
                ", price=" + price +
                ", isAvailable=" + isAvailable +
                '}';
    }
}
