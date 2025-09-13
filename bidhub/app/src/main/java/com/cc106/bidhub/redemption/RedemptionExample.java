package com.cc106.bidhub.redemption;

import android.content.Context;
import android.util.Log;
import java.util.List;

/**
 * Redemption System Usage Examples
 * Demonstrates how to use the secure credit redemption system
 */
public class RedemptionExample {
    private static final String TAG = "RedemptionExample";
    
    private RedemptionCodeManager redemptionManager;
    private RedemptionIntegration redemptionIntegration;
    private Context context;
    
    public RedemptionExample(Context context) {
        this.context = context;
        this.redemptionManager = RedemptionCodeManager.getInstance(context);
        this.redemptionIntegration = new RedemptionIntegration(context);
    }
    
    /**
     * Example: Generate and send redemption code
     */
    public void generateAndSendCodeExample() {
        Log.i(TAG, "=== Generate and Send Code Example ===");
        
        String userId = "user123";
        double credits = 100.0;
        String email = "user@example.com";
        String phone = "+639123456789";
        
        try {
            // Generate redemption code
            String code = redemptionManager.generateRedemptionCode(userId, credits);
            Log.i(TAG, "Generated code: " + code);
            
            // Send via email
            boolean emailSent = redemptionManager.sendCodeViaEmail(email, code, credits);
            Log.i(TAG, "Email sent: " + emailSent);
            
            // Send via SMS
            boolean smsSent = redemptionManager.sendCodeViaSMS(phone, code, credits);
            Log.i(TAG, "SMS sent: " + smsSent);
            
            // Send via both
            boolean bothSent = redemptionManager.sendCodeViaBoth(email, phone, code, credits);
            Log.i(TAG, "Both sent: " + bothSent);
            
        } catch (RedemptionException e) {
            Log.e(TAG, "Code generation failed", e);
        }
    }
    
    /**
     * Example: Code redemption
     */
    public void redeemCodeExample() {
        Log.i(TAG, "=== Redeem Code Example ===");
        
        String userId = "user123";
        String code = "RC1234567890";
        
        try {
            // Check if code is valid
            boolean isValid = redemptionManager.validateRedemptionCode(code);
            Log.i(TAG, "Code valid: " + isValid);
            
            if (isValid) {
                // Check if code is expired
                boolean isExpired = redemptionManager.isCodeExpired(code);
                Log.i(TAG, "Code expired: " + isExpired);
                
                if (!isExpired) {
                    // Check if code is used
                    boolean isUsed = redemptionManager.isCodeUsed(code);
                    Log.i(TAG, "Code used: " + isUsed);
                    
                    if (!isUsed) {
                        // Redeem code
                        boolean redeemed = redemptionManager.redeemCode(userId, code);
                        Log.i(TAG, "Code redeemed: " + redeemed);
                        
                        if (redeemed) {
                            // Add credits using integration
                            boolean creditsAdded = redemptionIntegration.redeemCodeAndAddCredits(userId, code);
                            Log.i(TAG, "Credits added: " + creditsAdded);
                        }
                    }
                }
            }
            
        } catch (RedemptionException e) {
            Log.e(TAG, "Code redemption failed", e);
        }
    }
    
    /**
     * Example: Code management
     */
    public void codeManagementExample() {
        Log.i(TAG, "=== Code Management Example ===");
        
        String userId = "user123";
        
        // Get user codes
        List<RedemptionCode> userCodes = redemptionManager.getUserCodes(userId);
        Log.i(TAG, "User has " + userCodes.size() + " codes");
        
        for (RedemptionCode code : userCodes) {
            Log.i(TAG, "Code: " + code.getCode() + 
                  " Status: " + code.getStatus() + 
                  " Credits: " + code.getCredits() +
                  " Expires: " + code.getHoursUntilExpiry() + " hours");
        }
        
        // Get code details
        if (!userCodes.isEmpty()) {
            String code = userCodes.get(0).getCode();
            RedemptionCode details = redemptionManager.getCodeDetails(code);
            Log.i(TAG, "Code details: " + details);
            
            // Extend expiry
            boolean extended = redemptionManager.extendCodeExpiry(code, 24);
            Log.i(TAG, "Expiry extended: " + extended);
            
            // Invalidate code
            boolean invalidated = redemptionManager.invalidateCode(code);
            Log.i(TAG, "Code invalidated: " + invalidated);
        }
    }
    
    /**
     * Example: Delivery status tracking
     */
    public void deliveryStatusExample() {
        Log.i(TAG, "=== Delivery Status Example ===");
        
        String code = "RC1234567890";
        
        // Check delivery status
        DeliveryStatus status = redemptionManager.checkDeliveryStatus(code);
        if (status != null) {
            Log.i(TAG, "Delivery status: " + status);
            Log.i(TAG, "State: " + status.getState());
            Log.i(TAG, "Method: " + status.getDeliveryMethod());
            Log.i(TAG, "Address: " + status.getDeliveryAddress());
            Log.i(TAG, "Successful: " + status.isSuccessful());
            Log.i(TAG, "Retry count: " + status.getRetryCount());
        } else {
            Log.i(TAG, "No delivery status found for code: " + code);
        }
    }
    
    /**
     * Example: Security and monitoring
     */
    public void securityMonitoringExample() {
        Log.i(TAG, "=== Security and Monitoring Example ===");
        
        String userId = "user123";
        String code = "RC1234567890";
        
        // Audit code usage
        boolean auditPassed = redemptionManager.auditCodeUsage(code);
        Log.i(TAG, "Code audit passed: " + auditPassed);
        
        // Detect abuse
        boolean abuseDetected = redemptionManager.detectCodeAbuse(userId);
        Log.i(TAG, "Abuse detected: " + abuseDetected);
        
        if (abuseDetected) {
            // Block user
            redemptionManager.blockCodeGeneration(userId);
            Log.i(TAG, "User blocked from generating codes");
        }
        
        // Log activities
        redemptionManager.logCodeActivity(code, "TEST_ACTIVITY", "Testing security monitoring");
    }
    
    /**
     * Example: Integration with credit system
     */
    public void creditIntegrationExample() {
        Log.i(TAG, "=== Credit Integration Example ===");
        
        String userId = "user123";
        double credits = 250.0;
        String email = "user@example.com";
        String phone = "+639123456789";
        
        // Generate and send code using integration
        String code = redemptionIntegration.generateAndSendCode(userId, credits, email, phone);
        if (code != null) {
            Log.i(TAG, "Code generated and sent: " + code);
            
            // Wait for delivery and redeem
            new Thread(() -> {
                try {
                    Thread.sleep(5000); // Wait for delivery
                    
                    // Redeem code and add credits
                    boolean success = redemptionIntegration.redeemCodeAndAddCredits(userId, code);
                    Log.i(TAG, "Redemption and credit addition: " + success);
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }
    
    /**
     * Example: Admin operations
     */
    public void adminOperationsExample() {
        Log.i(TAG, "=== Admin Operations Example ===");
        
        String adminUserId = "admin123";
        String targetUserId = "user123";
        double credits = 500.0;
        
        // Generate admin code
        String adminCode = redemptionIntegration.generateAdminCode(adminUserId, targetUserId, credits);
        if (adminCode != null) {
            Log.i(TAG, "Admin code generated: " + adminCode);
        }
        
        // Bulk generate codes
        List<String> bulkCodes = redemptionIntegration.bulkGenerateCodes(adminUserId, 5, 100.0);
        Log.i(TAG, "Bulk generated " + bulkCodes.size() + " codes");
        
        // Get redemption statistics
        RedemptionIntegration.RedemptionStats stats = redemptionIntegration.getRedemptionStats(targetUserId);
        Log.i(TAG, "Redemption stats: " + stats);
    }
    
    /**
     * Example: Code validation
     */
    public void codeValidationExample() {
        Log.i(TAG, "=== Code Validation Example ===");
        
        // Test valid codes
        String[] validCodes = {"RC1234567890", "RCABCDEFGHIJ", "RC1234567890"};
        
        for (String code : validCodes) {
            boolean isValid = redemptionManager.validateCodeFormat(code);
            Log.i(TAG, "Code " + code + " format valid: " + isValid);
        }
        
        // Test invalid codes
        String[] invalidCodes = {"1234567890", "RC123456789", "RC12345678901", "rc1234567890", ""};
        
        for (String code : invalidCodes) {
            boolean isValid = redemptionManager.validateCodeFormat(code);
            Log.i(TAG, "Code " + code + " format valid: " + isValid);
        }
    }
    
    /**
     * Run all examples
     */
    public void runAllExamples() {
        Log.i(TAG, "Starting Redemption System Examples...");
        
        generateAndSendCodeExample();
        redeemCodeExample();
        codeManagementExample();
        deliveryStatusExample();
        securityMonitoringExample();
        creditIntegrationExample();
        adminOperationsExample();
        codeValidationExample();
        
        Log.i(TAG, "Redemption System Examples completed!");
    }
}
