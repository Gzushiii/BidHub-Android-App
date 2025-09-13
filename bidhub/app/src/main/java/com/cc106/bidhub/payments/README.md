# Unified Payment Processing System

A comprehensive payment processing system for Android applications that supports multiple payment gateways including GCash, Maya, and test payments.

## Overview

The PaymentManager provides a unified interface for processing payments across different payment methods with comprehensive error handling, validation, retry mechanisms, and fraud detection.

## Features

- **Multi-Gateway Support**: GCash, Maya, and Test payment methods
- **Comprehensive Validation**: Amount, method, reference, and signature validation
- **Error Handling**: Detailed error tracking and retry mechanisms
- **Security**: Payment signature verification and fraud detection
- **Analytics**: Payment tracking and daily limit management
- **Async Processing**: Non-blocking payment operations
- **Refund Support**: Full refund processing for completed payments

## Architecture

### Core Classes

- **PaymentManager**: Main payment processing class
- **PaymentRequest**: Payment request model
- **PaymentResult**: Payment result model
- **PaymentStatus**: Payment state enumeration
- **PaymentError**: Error handling model
- **PaymentException**: Custom exception class

### Gateway Classes

- **PaymentGateway**: Interface for payment gateways
- **GcashPaymentGateway**: GCash payment implementation
- **MayaPaymentGateway**: Maya payment implementation
- **TestPaymentGateway**: Test payment implementation

### Response Models

- **GCashResponse**: GCash-specific response data
- **MayaResponse**: Maya-specific response data

## Usage

### Basic Payment Processing

```java
// Get PaymentManager instance
PaymentManager paymentManager = PaymentManager.getInstance(context);

// Create payment request
PaymentRequest request = new PaymentRequest();
request.setUserId("user123");
request.setPaymentMethod(PaymentManager.PAYMENT_METHOD_GCASH);
request.setAmount(100.0);
request.setCurrency("PHP");
request.setDescription("Credit purchase");

// Process payment
PaymentResult result = paymentManager.processPayment(request);

// Check result
if (result.isSuccess()) {
    Log.i("Payment", "Payment successful: " + result.getTransactionId());
} else {
    Log.e("Payment", "Payment failed: " + result.getErrorMessage());
}
```

### GCash Payment

```java
// Initiate GCash payment
GCashResponse response = paymentManager.initiateGCashPayment(250.0, "John Doe");

if (response.isSuccess()) {
    // Show QR code or payment URL to user
    String qrCode = response.getQrCode();
    String paymentUrl = response.getPaymentUrl();
    
    // Verify payment after user completes it
    boolean verified = paymentManager.verifyGCashPayment(response.getReferenceId());
    
    if (verified) {
        // Get final status
        GCashResponse status = paymentManager.getGCashPaymentStatus(response.getReferenceId());
    }
}
```

### Maya Payment

```java
// Initiate Maya payment
MayaResponse response = paymentManager.initiateMayaPayment(500.0, "Jane Smith");

if (response.isSuccess()) {
    // Show QR code or payment URL to user
    String qrCode = response.getQrCode();
    String paymentUrl = response.getPaymentUrl();
    
    // Verify payment after user completes it
    boolean verified = paymentManager.verifyMayaPayment(response.getReferenceId());
    
    if (verified) {
        // Get final status
        MayaResponse status = paymentManager.getMayaPaymentStatus(response.getReferenceId());
    }
}
```

### Payment Validation

```java
// Validate payment method
boolean isValidMethod = paymentManager.validatePaymentMethod("gcash");

// Validate payment amount
try {
    paymentManager.validatePaymentAmount(100.0);
} catch (PaymentException e) {
    Log.e("Validation", "Invalid amount: " + e.getMessage());
}

// Check payment limits
boolean withinLimits = paymentManager.checkPaymentLimits("user123", 1000.0);

// Validate payment reference
boolean validRef = paymentManager.validatePaymentReference("REF12345678");
```

### Error Handling

```java
// Process payment with error handling
try {
    PaymentResult result = paymentManager.processPayment(request);
    
    if (!result.isSuccess()) {
        // Get last error
        PaymentError error = paymentManager.getLastPaymentError();
        
        if (error.isRetryable()) {
            // Retry payment
            boolean retrySuccess = paymentManager.retryPayment(result.getTransactionId());
        }
    }
} catch (PaymentException e) {
    Log.e("Payment", "Payment error: " + e.getPaymentError());
}
```

### Refund Processing

```java
// Process refund
boolean refundSuccess = paymentManager.refundPayment(transactionId, 100.0);

if (refundSuccess) {
    Log.i("Refund", "Refund successful");
} else {
    Log.e("Refund", "Refund failed");
}
```

## Payment States

- **PENDING**: Payment initiated, waiting for confirmation
- **PROCESSING**: Payment being processed
- **COMPLETED**: Payment successful
- **FAILED**: Payment failed
- **REFUNDED**: Payment refunded
- **CANCELLED**: Payment cancelled

## Configuration

### Payment Limits

- **Minimum Amount**: 1.0 PHP
- **Maximum Amount**: 100,000.0 PHP
- **Daily Limit**: 50,000.0 PHP per user

### Retry Settings

- **Max Retry Attempts**: 3
- **Retry Delay**: 5 seconds
- **Payment Timeout**: 30 seconds

## Security Features

- **Signature Verification**: SHA-256 based signature verification
- **Fraud Detection**: Basic fraud detection mechanisms
- **Payment Validation**: Comprehensive input validation
- **Secure Storage**: Thread-safe payment result storage

## Error Codes

- **INVALID_AMOUNT**: Payment amount is invalid
- **INVALID_PAYMENT_METHOD**: Unsupported payment method
- **VALIDATION_ERROR**: Input validation failed
- **NETWORK_ERROR**: Network connection failed
- **GATEWAY_ERROR**: Payment gateway error
- **FRAUD_DETECTED**: Fraud detection triggered
- **TIMEOUT**: Operation timeout
- **LIMIT_EXCEEDED**: Daily payment limit exceeded

## Threading

The PaymentManager uses a thread pool for async operations:

- **ExecutorService**: For payment processing
- **ScheduledExecutorService**: For retry operations
- **Thread-safe Collections**: For storing payment results and errors

## Integration with Existing Code

The PaymentManager can be easily integrated with the existing credit system:

```java
// In CreditsFragment or similar
PaymentManager paymentManager = PaymentManager.getInstance(getContext());

PaymentRequest request = new PaymentRequest();
request.setUserId(userId);
request.setPaymentMethod(PaymentManager.PAYMENT_METHOD_GCASH);
request.setAmount(pkg.getPrice());
request.setCurrency("PHP");
request.setDescription("Purchase: " + pkg.getName());

PaymentResult result = paymentManager.processPayment(request);

if (result.isSuccess()) {
    // Add credits to account
    creditManager.addCredits(userId, pkg.getCredits(), SimpleCreditManager.TRANSACTION_PURCHASE);
}
```

## Testing

Use the `PaymentManagerExample` class to test all payment functionality:

```java
PaymentManagerExample example = new PaymentManagerExample(context);
example.runAllExamples();
```

## Production Considerations

For production deployment:

1. **Replace Simulated Gateways**: Implement real GCash and Maya SDKs
2. **Add Database Storage**: Store payment results persistently
3. **Implement Webhooks**: Handle payment notifications from gateways
4. **Add Logging**: Implement proper logging system
5. **Security Review**: Review and enhance security measures
6. **Performance Optimization**: Optimize for high-volume transactions
7. **Monitoring**: Add payment monitoring and alerting

## Dependencies

- Android Context
- Java 8+ (for lambda expressions and streams)
- No external dependencies required

## License

This payment system is part of the BidHub application and follows the same licensing terms.
