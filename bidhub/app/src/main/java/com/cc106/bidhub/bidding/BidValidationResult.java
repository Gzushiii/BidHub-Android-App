package com.cc106.bidhub.bidding;

/**
 * Bid Validation Result
 * Represents the result of bid validation
 */
public class BidValidationResult {
    private boolean valid;
    private String errorMessage;
    
    public BidValidationResult(boolean valid, String errorMessage) {
        this.valid = valid;
        this.errorMessage = errorMessage;
    }
    
    public boolean isValid() {
        return valid;
    }
    
    public void setValid(boolean valid) {
        this.valid = valid;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    @Override
    public String toString() {
        return "BidValidationResult{" +
                "valid=" + valid +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}

