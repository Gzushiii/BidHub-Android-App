# Password Recovery System

This document describes the password recovery functionality implemented for the BidHub Android application.

## Overview

The password recovery system allows users to reset their passwords through email or SMS verification. It consists of three main activities and a verification service.

## Features

### 🔐 **Password Recovery Flow**
- **Email-based recovery**: Users can reset their password using their registered email address
- **SMS-based recovery**: Users can reset their password using their registered phone number
- **Secure verification**: 6-digit verification codes with 15-minute expiration
- **Password strength validation**: Ensures new passwords meet security requirements
- **Real-time validation**: Immediate feedback on password strength and confirmation

### 🛡️ **Security Features**
- **Secure password hashing**: Uses SHA-256 with salt for password storage
- **Time-limited codes**: Verification codes expire after 15 minutes
- **One-time use**: Verification codes are invalidated after successful password reset
- **Input validation**: Comprehensive validation for email, phone, and password formats
- **Rate limiting**: Resend cooldown prevents spam

## Architecture

### Activities

1. **PasswordRecoveryRequestActivity**
   - User selects recovery method (email/SMS)
   - Input validation for contact information
   - Verification code generation and storage
   - Integration with verification service

2. **PasswordRecoveryVerificationActivity**
   - Code input and validation
   - Auto-verification when 6 digits are entered
   - Resend functionality with cooldown
   - Masked contact information display

3. **PasswordResetActivity**
   - New password input with strength indicator
   - Password confirmation validation
   - Secure password update in database
   - Cleanup of verification codes

### Database Schema

**password_recovery table:**
```sql
CREATE TABLE password_recovery (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    email TEXT,
    phone TEXT,
    verification_code TEXT NOT NULL,
    expires_at INTEGER NOT NULL,
    is_email INTEGER NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

### Services

**VerificationService**
- Abstracted email/SMS sending functionality
- Callback-based architecture for async operations
- Easy integration with production services (SendGrid, Twilio, etc.)
- Simulated service for testing

## Usage

### For Users

1. **Start Recovery Process**
   - Tap "Forgot Password?" on the login screen
   - Select email or SMS recovery method
   - Enter your registered email or phone number

2. **Verify Identity**
   - Check your email/SMS for the 6-digit code
   - Enter the verification code
   - Code expires in 15 minutes

3. **Set New Password**
   - Enter a strong new password
   - Confirm the password
   - Password must meet security requirements

### For Developers

#### Testing the Flow

1. **Access Test Interface**
   - Tap "Test Password Recovery" on the login screen
   - Use the test interface to verify the complete flow

2. **Test Scenarios**
   - Test with valid email addresses
   - Test with valid phone numbers
   - Test with invalid credentials
   - Test code expiration
   - Test password strength validation

#### Integration with Production Services

To integrate with real email/SMS services, update the `VerificationService` class:

```java
// Example: SendGrid integration
private static boolean simulateEmailService(String email, String code) {
    try {
        Email email = new Email();
        email.setFrom(new Email("noreply@bidhub.com", "BidHub"));
        email.setSubject("Password Reset Verification Code");
        email.addTo(new Email(email));
        
        Content content = new Content("text/html", 
            "<h2>Password Reset Verification</h2>" +
            "<p>Your verification code is: <strong>" + code + "</strong></p>" +
            "<p>This code will expire in 15 minutes.</p>");
        email.addContent(content);
        
        SendGrid sg = new SendGrid(API_KEY);
        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(email.build());
        
        Response response = sg.api(request);
        return response.getStatusCode() == 202;
    } catch (Exception e) {
        Log.e(TAG, "Email service error", e);
        return false;
    }
}
```

## Security Considerations

### Password Requirements
- Minimum 8 characters
- Must contain letters and numbers
- Must contain at least one special character
- Real-time strength validation

### Verification Code Security
- 6-digit random codes
- 15-minute expiration
- One-time use only
- Rate limiting on resend

### Data Protection
- Passwords are hashed with SHA-256 and salt
- Verification codes are stored temporarily
- Automatic cleanup of expired codes
- No sensitive data in logs

## Error Handling

### User-Friendly Messages
- Clear validation error messages
- Helpful hints for password requirements
- Informative feedback for each step

### Graceful Degradation
- Network error handling
- Service unavailability fallbacks
- Timeout management

## Testing

### Manual Testing
1. Use the test interface to verify the complete flow
2. Test with various email/phone formats
3. Test password strength requirements
4. Test code expiration scenarios

### Automated Testing
- Unit tests for validation logic
- Integration tests for database operations
- UI tests for user interactions

## Future Enhancements

### Planned Features
- **Multi-language support**: Localized error messages and UI
- **Advanced security**: CAPTCHA integration for bot protection
- **Analytics**: Track recovery success rates and user behavior
- **Audit logging**: Comprehensive security event logging

### Production Readiness
- **Email templates**: Professional HTML email templates
- **SMS templates**: Branded SMS messages
- **Monitoring**: Real-time service health monitoring
- **Scaling**: Support for high-volume recovery requests

## Troubleshooting

### Common Issues

1. **Verification code not received**
   - Check spam folder for emails
   - Verify phone number format
   - Wait for resend cooldown

2. **Code expired**
   - Request a new verification code
   - Complete the process within 15 minutes

3. **Password not meeting requirements**
   - Ensure minimum 8 characters
   - Include letters, numbers, and special characters
   - Check for typos in confirmation

### Developer Issues

1. **Database errors**
   - Check database version and migration
   - Verify table creation
   - Check foreign key constraints

2. **Service integration**
   - Verify API keys and credentials
   - Check network connectivity
   - Review service logs

## Support

For technical support or questions about the password recovery system, please contact the development team or refer to the main project documentation.

---

**Note**: This password recovery system is designed for the BidHub Android application and follows security best practices. Always test thoroughly before deploying to production.

