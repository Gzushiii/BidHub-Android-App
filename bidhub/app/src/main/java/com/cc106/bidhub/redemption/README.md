# Secure Credit Redemption System

A comprehensive redemption code system for Android applications that enables secure credit distribution through unique codes with multiple delivery methods and advanced security features.

## Overview

The RedemptionCodeManager provides a complete solution for generating, delivering, validating, and redeeming credit codes with built-in security measures, abuse detection, and comprehensive audit logging.

## Features

- **Secure Code Generation**: Cryptographically secure random code generation
- **Multiple Delivery Methods**: Email, SMS, and combined delivery
- **Comprehensive Validation**: Code format, expiry, and usage validation
- **Security & Monitoring**: Abuse detection, audit logging, and user blocking
- **Code Management**: Expiry extension, invalidation, and tracking
- **Integration Ready**: Seamless integration with existing credit system
- **Admin Tools**: Bulk operations and administrative controls

## Architecture

### Core Classes

- **RedemptionCodeManager**: Main redemption system manager
- **RedemptionCode**: Redemption code model with full lifecycle tracking
- **RedemptionCodeStatus**: Code state enumeration
- **DeliveryStatus**: Delivery tracking and status management
- **RedemptionActivity**: Audit logging and activity tracking
- **RedemptionException**: Custom exception handling

### Integration Classes

- **RedemptionIntegration**: Credit system integration
- **RedemptionExample**: Usage examples and testing

## Code States

- **GENERATED**: Code generated, not sent
- **SENT**: Code sent to user
- **DELIVERED**: Code delivered successfully
- **REDEEMED**: Code redeemed
- **EXPIRED**: Code expired
- **INVALID**: Code invalid

## Usage

### Basic Code Generation

```java
// Get RedemptionCodeManager instance
RedemptionCodeManager redemptionManager = RedemptionCodeManager.getInstance(context);

// Generate redemption code
String code = redemptionManager.generateRedemptionCode("user123", 100.0);
Log.i("Redemption", "Generated code: " + code);
```

### Code Delivery

```java
// Send via email
boolean emailSent = redemptionManager.sendCodeViaEmail("user@example.com", code, 100.0);

// Send via SMS
boolean smsSent = redemptionManager.sendCodeViaSMS("+639123456789", code, 100.0);

// Send via both
boolean bothSent = redemptionManager.sendCodeViaBoth("user@example.com", "+639123456789", code, 100.0);

// Check delivery status
DeliveryStatus status = redemptionManager.checkDeliveryStatus(code);
if (status != null) {
    Log.i("Delivery", "Status: " + status.getState());
    Log.i("Delivery", "Successful: " + status.isSuccessful());
}
```

### Code Redemption

```java
// Validate code
boolean isValid = redemptionManager.validateRedemptionCode(code);
boolean isExpired = redemptionManager.isCodeExpired(code);
boolean isUsed = redemptionManager.isCodeUsed(code);

if (isValid && !isExpired && !isUsed) {
    // Redeem code
    boolean redeemed = redemptionManager.redeemCode("user123", code);
    if (redeemed) {
        Log.i("Redemption", "Code redeemed successfully");
    }
}
```

### Code Management

```java
// Get user codes
List<RedemptionCode> userCodes = redemptionManager.getUserCodes("user123");

// Get code details
RedemptionCode details = redemptionManager.getCodeDetails(code);

// Extend code expiry
boolean extended = redemptionManager.extendCodeExpiry(code, 24); // 24 hours

// Invalidate code
boolean invalidated = redemptionManager.invalidateCode(code);
```

### Security & Monitoring

```java
// Audit code usage
boolean auditPassed = redemptionManager.auditCodeUsage(code);

// Detect abuse
boolean abuseDetected = redemptionManager.detectCodeAbuse("user123");

// Block user
if (abuseDetected) {
    redemptionManager.blockCodeGeneration("user123");
}

// Log activity
redemptionManager.logCodeActivity(code, "CUSTOM_ACTION", "Custom activity details");
```

### Integration with Credit System

```java
// Get integration instance
RedemptionIntegration integration = new RedemptionIntegration(context);

// Generate and send code
String code = integration.generateAndSendCode("user123", 100.0, "user@example.com", "+639123456789");

// Redeem code and add credits
boolean success = integration.redeemCodeAndAddCredits("user123", code);

// Get redemption statistics
RedemptionIntegration.RedemptionStats stats = integration.getRedemptionStats("user123");
Log.i("Stats", "Redemption rate: " + stats.getRedemptionRate());
```

### Admin Operations

```java
// Generate admin code
String adminCode = integration.generateAdminCode("admin123", "user123", 500.0);

// Bulk generate codes
List<String> codes = integration.bulkGenerateCodes("admin123", 10, 100.0);

// Get user statistics
RedemptionIntegration.RedemptionStats stats = integration.getRedemptionStats("user123");
```

## Configuration

### Code Generation

- **Code Length**: 12 characters (2 prefix + 10 random)
- **Code Format**: RC + 10 alphanumeric characters
- **Character Set**: A-Z, 0-9
- **Uniqueness**: Guaranteed unique codes

### Limits and Restrictions

- **Daily Code Limit**: 10 codes per user per day
- **Hourly Code Limit**: 3 codes per user per hour
- **Default Expiry**: 24 hours
- **Maximum Expiry**: 7 days (168 hours)
- **Maximum Credits**: 10,000 credits per code

### Security Features

- **Abuse Detection**: Monitors failed attempts and suspicious patterns
- **User Blocking**: Automatic blocking for abuse detection
- **Audit Logging**: Comprehensive activity tracking
- **Rate Limiting**: Prevents code generation abuse
- **Secure Random**: Cryptographically secure code generation

## Code Format

Redemption codes follow the format: `RC` + 10 alphanumeric characters

Examples:
- `RC1234567890`
- `RCABCDEFGHIJ`
- `RC1A2B3C4D5E`

## Delivery Methods

### Email Delivery
- Validates email format
- Simulates email sending (90% success rate)
- Tracks delivery status
- Supports retry mechanisms

### SMS Delivery
- Validates phone number format
- Simulates SMS sending (95% success rate)
- Tracks delivery status
- Supports retry mechanisms

### Combined Delivery
- Sends via both email and SMS
- Requires both methods to succeed
- Provides redundancy

## Error Handling

The system provides comprehensive error handling with specific error codes:

- **USER_BLOCKED**: User is blocked from generating codes
- **DAILY_LIMIT_EXCEEDED**: Daily code generation limit exceeded
- **HOURLY_LIMIT_EXCEEDED**: Hourly code generation limit exceeded
- **CODE_NOT_FOUND**: Redemption code not found
- **INVALID_CODE**: Invalid redemption code format
- **CODE_EXPIRED**: Redemption code has expired
- **CODE_USED**: Redemption code already used
- **CODE_NOT_REDEEMABLE**: Code cannot be redeemed
- **ABUSE_DETECTED**: Suspicious activity detected

## Threading

The RedemptionCodeManager uses thread pools for async operations:

- **ExecutorService**: For code generation and delivery
- **ScheduledExecutorService**: For cleanup tasks
- **Thread-safe Collections**: For storing codes and activities

## Cleanup and Maintenance

The system includes automatic cleanup tasks:

- **Expired Code Cleanup**: Marks expired codes as expired
- **Activity Cleanup**: Removes old activity logs (7+ days)
- **Counter Reset**: Resets hourly counters
- **Resource Cleanup**: Proper shutdown of thread pools

## Integration with Existing Code

The redemption system integrates seamlessly with the existing credit system:

```java
// In CreditsFragment or similar
RedemptionIntegration integration = new RedemptionIntegration(getContext());

// Generate redemption code
String code = integration.generateAndSendCode(userId, credits, email, phone);

// Redeem code and add credits
boolean success = integration.redeemCodeAndAddCredits(userId, code);
```

## Testing

Use the `RedemptionExample` class to test all functionality:

```java
RedemptionExample example = new RedemptionExample(context);
example.runAllExamples();
```

## Production Considerations

For production deployment:

1. **Real Delivery Services**: Integrate with actual email and SMS providers
2. **Database Storage**: Store codes and activities persistently
3. **Enhanced Security**: Implement additional security measures
4. **Monitoring**: Add real-time monitoring and alerting
5. **Performance**: Optimize for high-volume operations
6. **Backup**: Implement data backup and recovery
7. **Compliance**: Ensure compliance with data protection regulations

## Dependencies

- Android Context
- Java 8+ (for lambda expressions and streams)
- No external dependencies required

## Security Best Practices

1. **Code Uniqueness**: Always verify code uniqueness before generation
2. **Rate Limiting**: Enforce daily and hourly limits
3. **Abuse Detection**: Monitor for suspicious patterns
4. **Audit Logging**: Log all activities for security analysis
5. **User Blocking**: Block users who abuse the system
6. **Secure Random**: Use cryptographically secure random generation
7. **Input Validation**: Validate all inputs thoroughly
8. **Error Handling**: Handle errors gracefully without exposing sensitive information

## License

This redemption system is part of the BidHub application and follows the same licensing terms.
