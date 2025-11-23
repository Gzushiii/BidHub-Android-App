package com.cc106.bidhub.models;

public class User {
    private int id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String alias;
    private double credits;
    
    public User() {}
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public String getAlias() {
        return alias;
    }
    
    public void setAlias(String alias) {
        this.alias = alias;
    }
    
    public double getCredits() {
        return credits;
    }
    
    public void setCredits(double credits) {
        this.credits = credits;
    }
    
    public String getFullName() {
        return firstName + " " + lastName;
    }
}

