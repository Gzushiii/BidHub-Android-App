package com.cc106.bidhub.models;

public class Topup {
    private int id;
    private double amount;
    private String generatedRef;
    private String paymentMethod;
    private String status;
    private String instructions;
    private String paymentNumber;
    private String userReceiptRef;
    
    public Topup() {}
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public double getAmount() {
        return amount;
    }
    
    public void setAmount(double amount) {
        this.amount = amount;
    }
    
    public String getGeneratedRef() {
        return generatedRef;
    }
    
    public void setGeneratedRef(String generatedRef) {
        this.generatedRef = generatedRef;
    }
    
    public String getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getInstructions() {
        return instructions;
    }
    
    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }
    
    public String getPaymentNumber() {
        return paymentNumber;
    }
    
    public void setPaymentNumber(String paymentNumber) {
        this.paymentNumber = paymentNumber;
    }
    
    public String getUserReceiptRef() {
        return userReceiptRef;
    }
    
    public void setUserReceiptRef(String userReceiptRef) {
        this.userReceiptRef = userReceiptRef;
    }
}

