package com.cc106.bidhub.redemption;

import android.content.Context;
import android.util.Log;
import com.cc106.bidhub.credits.SimpleCreditManager;
import java.util.List;

/**
 * Redemption Integration
 * Integrates the redemption system with the existing credit system
 */
public class RedemptionIntegration {
    private static final String TAG = "RedemptionIntegration";
    
    private RedemptionCodeManager redemptionManager;
    private SimpleCreditManager creditManager;
    private Context context;
    
    public RedemptionIntegration(Context context) {
        this.context = context;
        this.redemptionManager = RedemptionCodeManager.getInstance(context);
        this.creditManager = new SimpleCreditManager(context);
    }
    
    /**
     * Generate and send redemption code
     */
    public String generateAndSendCode(String userId, double credits, String email, String phone) {
        Log.i(TAG, "Generating and sending redemption code for user: " + userId);
        
        try {
            // Generate redemption code
            String code = redemptionManager.generateRedemptionCode(userId, credits);
            
            // Send code via both email and SMS
            boolean sent = redemptionManager.sendCodeViaBoth(email, phone, code, credits);
            
            if (sent) {
                Log.i(TAG, "Redemption code sent successfully: " + code);
                return code;
            } else {
                Log.e(TAG, "Failed to send redemption code: " + code);
                return null;
            }
            
        } catch (RedemptionException e) {
            Log.e(TAG, "Failed to generate redemption code", e);
            return null;
        }
    }
    
    /**
     * Redeem code and add credits to account
     */
    public boolean redeemCodeAndAddCredits(String userId, String code) {
        Log.i(TAG, "Redeeming code and adding credits: " + code);
        
        try {
            // Get code details
            RedemptionCode redemptionCode = redemptionManager.getCodeDetails(code);
            if (redemptionCode == null) {
                Log.e(TAG, "Redemption code not found: " + code);
                return false;
            }
            
            // Redeem code
            boolean redeemed = redemptionManager.redeemCode(userId, code);
            if (!redeemed) {
                Log.e(TAG, "Failed to redeem code: " + code);
                return false;
            }
            
            // Add credits to account
            int credits = (int) redemptionCode.getCredits();
            boolean creditsAdded = creditManager.addCredits(userId, credits, SimpleCreditManager.TRANSACTION_REDEMPTION);
            
            if (creditsAdded) {
                Log.i(TAG, "Credits added successfully: " + credits + " for user " + userId);
                
                // Transaction is already logged by addCredits method
                
                return true;
            } else {
                Log.e(TAG, "Failed to add credits to account");
                return false;
            }
            
        } catch (RedemptionException e) {
            Log.e(TAG, "Code redemption failed", e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error during redemption", e);
            return false;
        }
    }
    
    /**
     * Get user's redemption codes
     */
    public List<RedemptionCode> getUserRedemptionCodes(String userId) {
        return redemptionManager.getUserCodes(userId);
    }
    
    /**
     * Check if code is valid
     */
    public boolean isCodeValid(String code) {
        return redemptionManager.validateRedemptionCode(code);
    }
    
    /**
     * Check if code is expired
     */
    public boolean isCodeExpired(String code) {
        return redemptionManager.isCodeExpired(code);
    }
    
    /**
     * Check if code is used
     */
    public boolean isCodeUsed(String code) {
        return redemptionManager.isCodeUsed(code);
    }
    
    /**
     * Get code details
     */
    public RedemptionCode getCodeDetails(String code) {
        return redemptionManager.getCodeDetails(code);
    }
    
    /**
     * Extend code expiry
     */
    public boolean extendCodeExpiry(String code, int hours) {
        return redemptionManager.extendCodeExpiry(code, hours);
    }
    
    /**
     * Invalidate code
     */
    public boolean invalidateCode(String code) {
        return redemptionManager.invalidateCode(code);
    }
    
    /**
     * Check delivery status
     */
    public DeliveryStatus checkDeliveryStatus(String code) {
        return redemptionManager.checkDeliveryStatus(code);
    }
    
    /**
     * Generate code for admin
     */
    public String generateAdminCode(String adminUserId, String targetUserId, double credits) {
        Log.i(TAG, "Generating admin code for user: " + targetUserId);
        
        try {
            String code = redemptionManager.generateRedemptionCode(adminUserId, credits);
            
            // Set target user
            RedemptionCode redemptionCode = redemptionManager.getCodeDetails(code);
            if (redemptionCode != null) {
                redemptionCode.setUserId(targetUserId);
                redemptionCode.setNotes("Generated by admin: " + adminUserId);
            }
            
            return code;
            
        } catch (RedemptionException e) {
            Log.e(TAG, "Failed to generate admin code", e);
            return null;
        }
    }
    
    /**
     * Bulk generate codes
     */
    public List<String> bulkGenerateCodes(String adminUserId, int count, double credits) {
        Log.i(TAG, "Bulk generating " + count + " codes");
        
        List<String> codes = new java.util.ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            try {
                String code = redemptionManager.generateRedemptionCode(adminUserId, credits);
                codes.add(code);
            } catch (RedemptionException e) {
                Log.e(TAG, "Failed to generate code " + (i + 1), e);
            }
        }
        
        Log.i(TAG, "Generated " + codes.size() + " out of " + count + " codes");
        return codes;
    }
    
    /**
     * Get redemption statistics
     */
    public RedemptionStats getRedemptionStats(String userId) {
        List<RedemptionCode> codes = redemptionManager.getUserCodes(userId);
        
        int totalCodes = codes.size();
        int redeemedCodes = 0;
        int expiredCodes = 0;
        double totalCredits = 0;
        double redeemedCredits = 0;
        
        for (RedemptionCode code : codes) {
            totalCredits += code.getCredits();
            
            if (code.getStatus() == RedemptionCodeStatus.REDEEMED) {
                redeemedCodes++;
                redeemedCredits += code.getCredits();
            } else if (code.getStatus() == RedemptionCodeStatus.EXPIRED) {
                expiredCodes++;
            }
        }
        
        return new RedemptionStats(totalCodes, redeemedCodes, expiredCodes, totalCredits, redeemedCredits);
    }
    
    /**
     * Redemption Statistics Model
     */
    public static class RedemptionStats {
        private int totalCodes;
        private int redeemedCodes;
        private int expiredCodes;
        private double totalCredits;
        private double redeemedCredits;
        
        public RedemptionStats(int totalCodes, int redeemedCodes, int expiredCodes, 
                              double totalCredits, double redeemedCredits) {
            this.totalCodes = totalCodes;
            this.redeemedCodes = redeemedCodes;
            this.expiredCodes = expiredCodes;
            this.totalCredits = totalCredits;
            this.redeemedCredits = redeemedCredits;
        }
        
        // Getters
        public int getTotalCodes() { return totalCodes; }
        public int getRedeemedCodes() { return redeemedCodes; }
        public int getExpiredCodes() { return expiredCodes; }
        public double getTotalCredits() { return totalCredits; }
        public double getRedeemedCredits() { return redeemedCredits; }
        
        public double getRedemptionRate() {
            return totalCodes > 0 ? (double) redeemedCodes / totalCodes : 0.0;
        }
        
        public double getExpirationRate() {
            return totalCodes > 0 ? (double) expiredCodes / totalCodes : 0.0;
        }
        
        @Override
        public String toString() {
            return "RedemptionStats{" +
                    "totalCodes=" + totalCodes +
                    ", redeemedCodes=" + redeemedCodes +
                    ", expiredCodes=" + expiredCodes +
                    ", totalCredits=" + totalCredits +
                    ", redeemedCredits=" + redeemedCredits +
                    ", redemptionRate=" + getRedemptionRate() +
                    '}';
        }
    }
}
