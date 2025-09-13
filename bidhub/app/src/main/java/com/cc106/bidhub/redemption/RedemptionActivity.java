package com.cc106.bidhub.redemption;

import java.util.Date;

/**
 * Redemption Activity Model
 * Tracks redemption code activities and audit logs
 */
public class RedemptionActivity {
    private String id;
    private String code;
    private String userId;
    private String action;
    private String details;
    private long timestamp;
    private String ipAddress;
    private String userAgent;
    private String sessionId;
    private boolean isSuspicious;
    private String riskLevel;
    private String metadata;
    
    public RedemptionActivity() {
        this.timestamp = System.currentTimeMillis();
        this.isSuspicious = false;
        this.riskLevel = "LOW";
    }
    
    public RedemptionActivity(String code, String userId, String action) {
        this();
        this.code = code;
        this.userId = userId;
        this.action = action;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public String getDetails() {
        return details;
    }
    
    public void setDetails(String details) {
        this.details = details;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public boolean isSuspicious() {
        return isSuspicious;
    }
    
    public void setSuspicious(boolean suspicious) {
        this.isSuspicious = suspicious;
    }
    
    public String getRiskLevel() {
        return riskLevel;
    }
    
    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }
    
    public String getMetadata() {
        return metadata;
    }
    
    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
    
    /**
     * Get formatted timestamp
     */
    public String getFormattedTimestamp() {
        return new Date(timestamp).toString();
    }
    
    @Override
    public String toString() {
        return "RedemptionActivity{" +
                "id='" + id + '\'' +
                ", code='" + code + '\'' +
                ", userId='" + userId + '\'' +
                ", action='" + action + '\'' +
                ", timestamp=" + timestamp +
                ", isSuspicious=" + isSuspicious +
                ", riskLevel='" + riskLevel + '\'' +
                '}';
    }
}
