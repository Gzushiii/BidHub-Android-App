package com.cc106.bidhub.redemption;

/**
 * Redemption Exception
 * Custom exception for redemption code operations
 */
public class RedemptionException extends Exception {
    private String errorCode;
    private String details;
    
    public RedemptionException(String message) {
        super(message);
        this.errorCode = "REDEMPTION_ERROR";
    }
    
    public RedemptionException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public RedemptionException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public String getDetails() {
        return details;
    }
    
    public void setDetails(String details) {
        this.details = details;
    }
    
    @Override
    public String toString() {
        return "RedemptionException{" +
                "errorCode='" + errorCode + '\'' +
                ", message='" + getMessage() + '\'' +
                ", details='" + details + '\'' +
                '}';
    }
}
