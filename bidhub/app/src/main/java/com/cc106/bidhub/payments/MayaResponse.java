package com.cc106.bidhub.payments;

/**
 * Maya Payment Response Model
 * Contains Maya-specific payment response data
 */
public class MayaResponse {
    private boolean success;
    private String referenceId;
    private String transactionId;
    private PaymentStatus status;
    private String errorCode;
    private String errorMessage;
    private double amount;
    private String currency;
    private String mayaReference;
    private String paymentUrl;
    private String qrCode;
    private long timestamp;
    private String signature;
    
    public MayaResponse() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public MayaResponse(boolean success, String referenceId, PaymentStatus status) {
        this();
        this.success = success;
        this.referenceId = referenceId;
        this.status = status;
    }
    
    // Static factory methods
    public static MayaResponse success(String referenceId, String transactionId, double amount, String currency) {
        MayaResponse response = new MayaResponse(true, referenceId, PaymentStatus.COMPLETED);
        response.setTransactionId(transactionId);
        response.setAmount(amount);
        response.setCurrency(currency);
        return response;
    }
    
    public static MayaResponse failure(String referenceId, String errorCode, String errorMessage) {
        MayaResponse response = new MayaResponse(false, referenceId, PaymentStatus.FAILED);
        response.setErrorCode(errorCode);
        response.setErrorMessage(errorMessage);
        return response;
    }
    
    public static MayaResponse pending(String referenceId, String paymentUrl) {
        MayaResponse response = new MayaResponse(true, referenceId, PaymentStatus.PENDING);
        response.setPaymentUrl(paymentUrl);
        return response;
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getReferenceId() {
        return referenceId;
    }
    
    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
    
    public PaymentStatus getStatus() {
        return status;
    }
    
    public void setStatus(PaymentStatus status) {
        this.status = status;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public double getAmount() {
        return amount;
    }
    
    public void setAmount(double amount) {
        this.amount = amount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getMayaReference() {
        return mayaReference;
    }
    
    public void setMayaReference(String mayaReference) {
        this.mayaReference = mayaReference;
    }
    
    public String getPaymentUrl() {
        return paymentUrl;
    }
    
    public void setPaymentUrl(String paymentUrl) {
        this.paymentUrl = paymentUrl;
    }
    
    public String getQrCode() {
        return qrCode;
    }
    
    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getSignature() {
        return signature;
    }
    
    public void setSignature(String signature) {
        this.signature = signature;
    }
    
    @Override
    public String toString() {
        return "MayaResponse{" +
                "success=" + success +
                ", referenceId='" + referenceId + '\'' +
                ", transactionId='" + transactionId + '\'' +
                ", status=" + status +
                ", errorCode='" + errorCode + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", mayaReference='" + mayaReference + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
