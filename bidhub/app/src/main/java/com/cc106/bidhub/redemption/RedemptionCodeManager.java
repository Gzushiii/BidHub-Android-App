package com.cc106.bidhub.redemption;

import android.content.Context;
import android.util.Log;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.security.SecureRandom;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Secure Credit Redemption System
 * Manages redemption code generation, delivery, validation, and redemption
 * Includes comprehensive security features and abuse detection
 */
public class RedemptionCodeManager {
    private static final String TAG = "RedemptionCodeManager";
    private static RedemptionCodeManager instance;
    
    // Configuration constants
    private static final int CODE_LENGTH = 12;
    private static final int MAX_DAILY_CODES_PER_USER = 10;
    private static final int MAX_HOURLY_CODES_PER_USER = 3;
    private static final long DEFAULT_EXPIRY_HOURS = 24;
    private static final long MAX_EXPIRY_HOURS = 168; // 7 days
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 5000; // 5 seconds
    
    // Code format validation
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z0-9]{12}$");
    private static final String CODE_PREFIX = "RC";
    
    // Threading
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutor;
    
    // Storage
    private final Map<String, RedemptionCode> redemptionCodes;
    private final Map<String, DeliveryStatus> deliveryStatuses;
    private final Map<String, List<RedemptionActivity>> userActivities;
    private final Map<String, AtomicInteger> dailyCodeCounts;
    private final Map<String, AtomicInteger> hourlyCodeCounts;
    private final Map<String, Long> userBlockedUntil;
    
    // Context
    private Context context;
    
    // Security
    private final SecureRandom secureRandom;
    private final Map<String, Integer> failedAttempts;
    private final Map<String, Long> lastAttemptTime;
    
    private RedemptionCodeManager(Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newCachedThreadPool();
        this.scheduledExecutor = Executors.newScheduledThreadPool(2);
        this.redemptionCodes = new ConcurrentHashMap<>();
        this.deliveryStatuses = new ConcurrentHashMap<>();
        this.userActivities = new ConcurrentHashMap<>();
        this.dailyCodeCounts = new ConcurrentHashMap<>();
        this.hourlyCodeCounts = new ConcurrentHashMap<>();
        this.userBlockedUntil = new ConcurrentHashMap<>();
        this.secureRandom = new SecureRandom();
        this.failedAttempts = new ConcurrentHashMap<>();
        this.lastAttemptTime = new ConcurrentHashMap<>();
        
        // Start cleanup task
        startCleanupTask();
    }
    
    public static synchronized RedemptionCodeManager getInstance(Context context) {
        if (instance == null) {
            instance = new RedemptionCodeManager(context);
        }
        return instance;
    }
    
    // ==================== CODE GENERATION ====================
    
    /**
     * Generate redemption code for user
     */
    public String generateRedemptionCode(String userId, double credits) throws RedemptionException {
        Log.i(TAG, "Generating redemption code for user: " + userId + " credits: " + credits);
        
        try {
            // Validate input
            validateUserId(userId);
            validateCredits(credits);
            
            // Check if user is blocked
            if (isUserBlocked(userId)) {
                throw new RedemptionException("USER_BLOCKED", "User is blocked from generating codes");
            }
            
            // Check daily limits
            if (!checkDailyCodeLimit(userId)) {
                throw new RedemptionException("DAILY_LIMIT_EXCEEDED", "Daily code generation limit exceeded");
            }
            
            // Check hourly limits
            if (!checkHourlyCodeLimit(userId)) {
                throw new RedemptionException("HOURLY_LIMIT_EXCEEDED", "Hourly code generation limit exceeded");
            }
            
            // Generate secure code
            String code = createSecureCode();
            
            // Check uniqueness
            if (!isCodeUnique(code)) {
                // Retry with new code
                code = createSecureCode();
                if (!isCodeUnique(code)) {
                    throw new RedemptionException("CODE_GENERATION_FAILED", "Unable to generate unique code");
                }
            }
            
            // Create redemption code
            RedemptionCode redemptionCode = new RedemptionCode(code, userId, credits);
            redemptionCode.setGeneratedBy(userId);
            redemptionCode.setExpiresAt(System.currentTimeMillis() + (DEFAULT_EXPIRY_HOURS * 60 * 60 * 1000));
            
            // Store code
            redemptionCodes.put(code, redemptionCode);
            
            // Update counters
            updateCodeCounters(userId);
            
            // Log activity
            logCodeActivity(code, "CODE_GENERATED", "Generated code for " + credits + " credits");
            
            Log.i(TAG, "Redemption code generated: " + code);
            return code;
            
        } catch (RedemptionException e) {
            Log.e(TAG, "Code generation failed", e);
            throw e;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error during code generation", e);
            throw new RedemptionException("GENERATION_ERROR", "Code generation failed", e);
        }
    }
    
    /**
     * Validate code format
     */
    public boolean validateCodeFormat(String code) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }
        
        return CODE_PATTERN.matcher(code.toUpperCase()).matches();
    }
    
    /**
     * Create secure random code
     */
    public String createSecureCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        code.append(CODE_PREFIX);
        
        // Generate remaining characters
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        for (int i = 2; i < CODE_LENGTH; i++) {
            code.append(chars.charAt(secureRandom.nextInt(chars.length())));
        }
        
        return code.toString();
    }
    
    /**
     * Check if code is unique
     */
    public boolean isCodeUnique(String code) {
        return !redemptionCodes.containsKey(code.toUpperCase());
    }
    
    // ==================== CODE DELIVERY ====================
    
    /**
     * Send code via email
     */
    public boolean sendCodeViaEmail(String email, String code, double credits) {
        Log.i(TAG, "Sending code via email: " + email);
        
        try {
            validateEmail(email);
            validateCode(code);
            
            RedemptionCode redemptionCode = redemptionCodes.get(code);
            if (redemptionCode == null) {
                Log.e(TAG, "Redemption code not found: " + code);
                return false;
            }
            
            // Create delivery status
            DeliveryStatus deliveryStatus = new DeliveryStatus(code, "EMAIL", email);
            deliveryStatuses.put(code, deliveryStatus);
            
            // Update redemption code
            redemptionCode.setDeliveryMethod("EMAIL");
            redemptionCode.setDeliveryAddress(email);
            redemptionCode.setStatus(RedemptionCodeStatus.SENT);
            
            // Simulate email sending
            executorService.submit(() -> {
                try {
                    Thread.sleep(2000); // Simulate network delay
                    
                    // Simulate email delivery (90% success rate)
                    boolean success = Math.random() > 0.1;
                    
                    if (success) {
                        deliveryStatus.setState(DeliveryState.DELIVERED);
                        deliveryStatus.setDeliveredAt(System.currentTimeMillis());
                        deliveryStatus.setSuccessful(true);
                        redemptionCode.setStatus(RedemptionCodeStatus.DELIVERED);
                        
                        Log.i(TAG, "Code delivered via email: " + code);
                    } else {
                        deliveryStatus.setState(DeliveryState.FAILED);
                        deliveryStatus.setFailedAt(System.currentTimeMillis());
                        deliveryStatus.setErrorMessage("Email delivery failed");
                        
                        Log.w(TAG, "Email delivery failed: " + code);
                    }
                    
                    deliveryStatuses.put(code, deliveryStatus);
                    redemptionCodes.put(code, redemptionCode);
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Log.e(TAG, "Email delivery interrupted", e);
                }
            });
            
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Email delivery failed", e);
            return false;
        }
    }
    
    /**
     * Send code via SMS
     */
    public boolean sendCodeViaSMS(String phone, String code, double credits) {
        Log.i(TAG, "Sending code via SMS: " + phone);
        
        try {
            validatePhone(phone);
            validateCode(code);
            
            RedemptionCode redemptionCode = redemptionCodes.get(code);
            if (redemptionCode == null) {
                Log.e(TAG, "Redemption code not found: " + code);
                return false;
            }
            
            // Create delivery status
            DeliveryStatus deliveryStatus = new DeliveryStatus(code, "SMS", phone);
            deliveryStatuses.put(code, deliveryStatus);
            
            // Update redemption code
            redemptionCode.setDeliveryMethod("SMS");
            redemptionCode.setDeliveryAddress(phone);
            redemptionCode.setStatus(RedemptionCodeStatus.SENT);
            
            // Simulate SMS sending
            executorService.submit(() -> {
                try {
                    Thread.sleep(1500); // Simulate network delay
                    
                    // Simulate SMS delivery (95% success rate)
                    boolean success = Math.random() > 0.05;
                    
                    if (success) {
                        deliveryStatus.setState(DeliveryState.DELIVERED);
                        deliveryStatus.setDeliveredAt(System.currentTimeMillis());
                        deliveryStatus.setSuccessful(true);
                        redemptionCode.setStatus(RedemptionCodeStatus.DELIVERED);
                        
                        Log.i(TAG, "Code delivered via SMS: " + code);
                    } else {
                        deliveryStatus.setState(DeliveryState.FAILED);
                        deliveryStatus.setFailedAt(System.currentTimeMillis());
                        deliveryStatus.setErrorMessage("SMS delivery failed");
                        
                        Log.w(TAG, "SMS delivery failed: " + code);
                    }
                    
                    deliveryStatuses.put(code, deliveryStatus);
                    redemptionCodes.put(code, redemptionCode);
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Log.e(TAG, "SMS delivery interrupted", e);
                }
            });
            
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "SMS delivery failed", e);
            return false;
        }
    }
    
    /**
     * Send code via both email and SMS
     */
    public boolean sendCodeViaBoth(String email, String phone, String code, double credits) {
        Log.i(TAG, "Sending code via both email and SMS");
        
        boolean emailSuccess = sendCodeViaEmail(email, code, credits);
        boolean smsSuccess = sendCodeViaSMS(phone, code, credits);
        
        return emailSuccess && smsSuccess;
    }
    
    /**
     * Check delivery status
     */
    public DeliveryStatus checkDeliveryStatus(String code) {
        return deliveryStatuses.get(code);
    }
    
    // ==================== CODE REDEMPTION ====================
    
    /**
     * Redeem code
     */
    public boolean redeemCode(String userId, String code) throws RedemptionException {
        Log.i(TAG, "Redeeming code: " + code + " for user: " + userId);
        
        try {
            // Validate input
            validateUserId(userId);
            validateCode(code);
            
            // Get redemption code
            RedemptionCode redemptionCode = redemptionCodes.get(code.toUpperCase());
            if (redemptionCode == null) {
                throw new RedemptionException("CODE_NOT_FOUND", "Redemption code not found");
            }
            
            // Validate redemption code
            if (!validateRedemptionCode(code)) {
                throw new RedemptionException("INVALID_CODE", "Invalid redemption code");
            }
            
            // Check if code is expired
            if (isCodeExpired(code)) {
                throw new RedemptionException("CODE_EXPIRED", "Redemption code has expired");
            }
            
            // Check if code is used
            if (isCodeUsed(code)) {
                throw new RedemptionException("CODE_USED", "Redemption code has already been used");
            }
            
            // Check if code can be redeemed
            if (!redemptionCode.canBeRedeemed()) {
                throw new RedemptionException("CODE_NOT_REDEEMABLE", "Code cannot be redeemed");
            }
            
            // Check for abuse
            if (detectCodeAbuse(userId)) {
                throw new RedemptionException("ABUSE_DETECTED", "Suspicious activity detected");
            }
            
            // Redeem code
            redemptionCode.setStatus(RedemptionCodeStatus.REDEEMED);
            redemptionCode.setRedeemedAt(System.currentTimeMillis());
            redemptionCode.setRedeemedBy(userId);
            redemptionCode.incrementUsageCount();
            
            // Update storage
            redemptionCodes.put(code, redemptionCode);
            
            // Log activity
            logCodeActivity(code, "CODE_REDEEMED", "Code redeemed by " + userId);
            
            Log.i(TAG, "Code redeemed successfully: " + code);
            return true;
            
        } catch (RedemptionException e) {
            Log.e(TAG, "Code redemption failed", e);
            recordFailedAttempt(userId);
            throw e;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error during redemption", e);
            recordFailedAttempt(userId);
            throw new RedemptionException("REDEMPTION_ERROR", "Code redemption failed", e);
        }
    }
    
    /**
     * Validate redemption code
     */
    public boolean validateRedemptionCode(String code) {
        if (!validateCodeFormat(code)) {
            return false;
        }
        
        RedemptionCode redemptionCode = redemptionCodes.get(code.toUpperCase());
        if (redemptionCode == null) {
            return false;
        }
        
        return redemptionCode.isActive() && redemptionCode.getStatus() != RedemptionCodeStatus.INVALID;
    }
    
    /**
     * Check if code is expired
     */
    public boolean isCodeExpired(String code) {
        RedemptionCode redemptionCode = redemptionCodes.get(code.toUpperCase());
        if (redemptionCode == null) {
            return true;
        }
        
        return redemptionCode.isExpired();
    }
    
    /**
     * Check if code is used
     */
    public boolean isCodeUsed(String code) {
        RedemptionCode redemptionCode = redemptionCodes.get(code.toUpperCase());
        if (redemptionCode == null) {
            return true;
        }
        
        return redemptionCode.isUsed();
    }
    
    // ==================== CODE MANAGEMENT ====================
    
    /**
     * Get user codes
     */
    public List<RedemptionCode> getUserCodes(String userId) {
        List<RedemptionCode> userCodes = new ArrayList<>();
        
        for (RedemptionCode code : redemptionCodes.values()) {
            if (userId.equals(code.getUserId()) || userId.equals(code.getGeneratedBy())) {
                userCodes.add(code);
            }
        }
        
        return userCodes;
    }
    
    /**
     * Get code details
     */
    public RedemptionCode getCodeDetails(String code) {
        return redemptionCodes.get(code.toUpperCase());
    }
    
    /**
     * Extend code expiry
     */
    public boolean extendCodeExpiry(String code, int hours) {
        Log.i(TAG, "Extending code expiry: " + code + " by " + hours + " hours");
        
        try {
            RedemptionCode redemptionCode = redemptionCodes.get(code.toUpperCase());
            if (redemptionCode == null) {
                Log.e(TAG, "Code not found: " + code);
                return false;
            }
            
            if (redemptionCode.getStatus().isFinalStatus()) {
                Log.w(TAG, "Cannot extend expiry for final status code: " + code);
                return false;
            }
            
            long newExpiry = redemptionCode.getExpiresAt() + (hours * 60 * 60 * 1000);
            long maxExpiry = System.currentTimeMillis() + (MAX_EXPIRY_HOURS * 60 * 60 * 1000);
            
            if (newExpiry > maxExpiry) {
                newExpiry = maxExpiry;
            }
            
            redemptionCode.setExpiresAt(newExpiry);
            redemptionCodes.put(code, redemptionCode);
            
            logCodeActivity(code, "EXPIRY_EXTENDED", "Extended by " + hours + " hours");
            
            Log.i(TAG, "Code expiry extended: " + code);
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to extend code expiry", e);
            return false;
        }
    }
    
    /**
     * Invalidate code
     */
    public boolean invalidateCode(String code) {
        Log.i(TAG, "Invalidating code: " + code);
        
        try {
            RedemptionCode redemptionCode = redemptionCodes.get(code.toUpperCase());
            if (redemptionCode == null) {
                Log.e(TAG, "Code not found: " + code);
                return false;
            }
            
            redemptionCode.setStatus(RedemptionCodeStatus.INVALID);
            redemptionCode.setActive(false);
            redemptionCodes.put(code, redemptionCode);
            
            logCodeActivity(code, "CODE_INVALIDATED", "Code invalidated");
            
            Log.i(TAG, "Code invalidated: " + code);
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to invalidate code", e);
            return false;
        }
    }
    
    // ==================== SECURITY & MONITORING ====================
    
    /**
     * Audit code usage
     */
    public boolean auditCodeUsage(String code) {
        Log.i(TAG, "Auditing code usage: " + code);
        
        try {
            RedemptionCode redemptionCode = redemptionCodes.get(code.toUpperCase());
            if (redemptionCode == null) {
                Log.e(TAG, "Code not found for audit: " + code);
                return false;
            }
            
            // Check for suspicious patterns
            List<RedemptionActivity> activities = userActivities.get(redemptionCode.getUserId());
            if (activities != null) {
                int recentAttempts = 0;
                long oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000);
                
                for (RedemptionActivity activity : activities) {
                    if (activity.getTimestamp() > oneHourAgo) {
                        recentAttempts++;
                    }
                }
                
                if (recentAttempts > 10) {
                    Log.w(TAG, "Suspicious activity detected for code: " + code);
                    logCodeActivity(code, "SUSPICIOUS_ACTIVITY", "High frequency of attempts");
                    return false;
                }
            }
            
            Log.i(TAG, "Code audit completed: " + code);
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Code audit failed", e);
            return false;
        }
    }
    
    /**
     * Log code activity
     */
    public void logCodeActivity(String code, String action, String details) {
        try {
            RedemptionCode redemptionCode = redemptionCodes.get(code.toUpperCase());
            String userId = redemptionCode != null ? redemptionCode.getUserId() : "UNKNOWN";
            
            RedemptionActivity activity = new RedemptionActivity(code, userId, action);
            activity.setDetails(details);
            activity.setTimestamp(System.currentTimeMillis());
            
            // Store activity
            userActivities.computeIfAbsent(userId, k -> new ArrayList<>()).add(activity);
            
            // Keep only last 100 activities per user
            List<RedemptionActivity> activities = userActivities.get(userId);
            if (activities.size() > 100) {
                activities.subList(0, activities.size() - 100).clear();
            }
            
            Log.d(TAG, "Code activity logged: " + action + " for " + code);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to log code activity", e);
        }
    }
    
    /**
     * Detect code abuse
     */
    public boolean detectCodeAbuse(String userId) {
        try {
            List<RedemptionActivity> activities = userActivities.get(userId);
            if (activities == null) {
                return false;
            }
            
            long oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000);
            int recentFailedAttempts = 0;
            int recentSuccessfulRedemptions = 0;
            
            for (RedemptionActivity activity : activities) {
                if (activity.getTimestamp() > oneHourAgo) {
                    if ("CODE_REDEMPTION_FAILED".equals(activity.getAction())) {
                        recentFailedAttempts++;
                    } else if ("CODE_REDEEMED".equals(activity.getAction())) {
                        recentSuccessfulRedemptions++;
                    }
                }
            }
            
            // Check for abuse patterns
            if (recentFailedAttempts > 5) {
                Log.w(TAG, "Abuse detected: Too many failed attempts for user: " + userId);
                return true;
            }
            
            if (recentSuccessfulRedemptions > 20) {
                Log.w(TAG, "Abuse detected: Too many successful redemptions for user: " + userId);
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "Abuse detection failed", e);
            return false;
        }
    }
    
    /**
     * Block code generation for user
     */
    public void blockCodeGeneration(String userId) {
        Log.w(TAG, "Blocking code generation for user: " + userId);
        
        long blockUntil = System.currentTimeMillis() + (24 * 60 * 60 * 1000); // 24 hours
        userBlockedUntil.put(userId, blockUntil);
        
        logCodeActivity("SYSTEM", "USER_BLOCKED", "Code generation blocked for " + userId);
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Validate user ID
     */
    private void validateUserId(String userId) throws RedemptionException {
        if (userId == null || userId.trim().isEmpty()) {
            throw new RedemptionException("INVALID_USER_ID", "User ID cannot be empty");
        }
    }
    
    /**
     * Validate credits amount
     */
    private void validateCredits(double credits) throws RedemptionException {
        if (credits <= 0) {
            throw new RedemptionException("INVALID_CREDITS", "Credits must be positive");
        }
        
        if (credits > 10000) {
            throw new RedemptionException("CREDITS_TOO_HIGH", "Credits amount too high");
        }
    }
    
    /**
     * Validate email address
     */
    private void validateEmail(String email) throws RedemptionException {
        if (email == null || email.trim().isEmpty()) {
            throw new RedemptionException("INVALID_EMAIL", "Email cannot be empty");
        }
        
        if (!email.contains("@") || !email.contains(".")) {
            throw new RedemptionException("INVALID_EMAIL", "Invalid email format");
        }
    }
    
    /**
     * Validate phone number
     */
    private void validatePhone(String phone) throws RedemptionException {
        if (phone == null || phone.trim().isEmpty()) {
            throw new RedemptionException("INVALID_PHONE", "Phone cannot be empty");
        }
        
        if (!phone.matches("^\\+?[1-9]\\d{1,14}$")) {
            throw new RedemptionException("INVALID_PHONE", "Invalid phone format");
        }
    }
    
    /**
     * Validate code
     */
    private void validateCode(String code) throws RedemptionException {
        if (code == null || code.trim().isEmpty()) {
            throw new RedemptionException("INVALID_CODE", "Code cannot be empty");
        }
        
        if (!validateCodeFormat(code)) {
            throw new RedemptionException("INVALID_CODE", "Invalid code format");
        }
    }
    
    /**
     * Check daily code limit
     */
    private boolean checkDailyCodeLimit(String userId) {
        AtomicInteger dailyCount = dailyCodeCounts.computeIfAbsent(userId, k -> new AtomicInteger(0));
        return dailyCount.get() < MAX_DAILY_CODES_PER_USER;
    }
    
    /**
     * Check hourly code limit
     */
    private boolean checkHourlyCodeLimit(String userId) {
        AtomicInteger hourlyCount = hourlyCodeCounts.computeIfAbsent(userId, k -> new AtomicInteger(0));
        return hourlyCount.get() < MAX_HOURLY_CODES_PER_USER;
    }
    
    /**
     * Update code counters
     */
    private void updateCodeCounters(String userId) {
        dailyCodeCounts.computeIfAbsent(userId, k -> new AtomicInteger(0)).incrementAndGet();
        hourlyCodeCounts.computeIfAbsent(userId, k -> new AtomicInteger(0)).incrementAndGet();
    }
    
    /**
     * Check if user is blocked
     */
    private boolean isUserBlocked(String userId) {
        Long blockUntil = userBlockedUntil.get(userId);
        if (blockUntil == null) {
            return false;
        }
        
        if (System.currentTimeMillis() > blockUntil) {
            userBlockedUntil.remove(userId);
            return false;
        }
        
        return true;
    }
    
    /**
     * Record failed attempt
     */
    private void recordFailedAttempt(String userId) {
        failedAttempts.put(userId, failedAttempts.getOrDefault(userId, 0) + 1);
        lastAttemptTime.put(userId, System.currentTimeMillis());
        
        // Block user if too many failed attempts
        if (failedAttempts.get(userId) > 10) {
            blockCodeGeneration(userId);
        }
    }
    
    /**
     * Start cleanup task
     */
    private void startCleanupTask() {
        scheduledExecutor.scheduleAtFixedRate(() -> {
            try {
                cleanupExpiredCodes();
                cleanupOldActivities();
                resetHourlyCounters();
            } catch (Exception e) {
                Log.e(TAG, "Cleanup task failed", e);
            }
        }, 1, 1, TimeUnit.HOURS);
    }
    
    /**
     * Cleanup expired codes
     */
    private void cleanupExpiredCodes() {
        long now = System.currentTimeMillis();
        int cleaned = 0;
        
        for (Map.Entry<String, RedemptionCode> entry : redemptionCodes.entrySet()) {
            RedemptionCode code = entry.getValue();
            if (code.isExpired() && code.getStatus() != RedemptionCodeStatus.REDEEMED) {
                code.setStatus(RedemptionCodeStatus.EXPIRED);
                cleaned++;
            }
        }
        
        if (cleaned > 0) {
            Log.i(TAG, "Cleaned up " + cleaned + " expired codes");
        }
    }
    
    /**
     * Cleanup old activities
     */
    private void cleanupOldActivities() {
        long oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000);
        int cleaned = 0;
        
        for (List<RedemptionActivity> activities : userActivities.values()) {
            activities.removeIf(activity -> activity.getTimestamp() < oneWeekAgo);
            cleaned++;
        }
        
        if (cleaned > 0) {
            Log.i(TAG, "Cleaned up old activities");
        }
    }
    
    /**
     * Reset hourly counters
     */
    private void resetHourlyCounters() {
        hourlyCodeCounts.clear();
        Log.d(TAG, "Reset hourly code counters");
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        executorService.shutdown();
        scheduledExecutor.shutdown();
        
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            scheduledExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
