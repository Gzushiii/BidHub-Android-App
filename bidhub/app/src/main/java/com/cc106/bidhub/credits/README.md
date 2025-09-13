# Credit Management System

A comprehensive credit management system for the BidHub Android application that handles all credit-related operations including purchases, transactions, reservations, and transfers.

## Overview

The Credit Management System provides a robust foundation for managing virtual credits in the BidHub marketplace. It supports atomic operations, transaction logging, fraud detection, and comprehensive audit trails.

## Architecture

### Core Classes

- **CreditManager**: Main service class handling all credit operations
- **CreditTransaction**: Model representing individual credit transactions
- **CreditPackage**: Model representing available credit packages for purchase
- **CreditBalance**: Model representing user's credit balance with different states
- **CreditState**: Enum defining credit states (AVAILABLE, RESERVED, PENDING, FROZEN)

### Key Features

1. **Atomic Credit Operations**: All credit operations are atomic and thread-safe
2. **Transaction Management**: Complete transaction history and audit trails
3. **Credit Packages**: Flexible package system with discounts and bonuses
4. **Balance Management**: Multi-state balance tracking with reservations
5. **Security & Validation**: Comprehensive validation and fraud detection
6. **Transfer System**: Peer-to-peer credit transfers between users
7. **Real-time Updates**: Cached balance updates for performance

## Usage

### Basic Setup

```java
// Initialize the credit manager
CreditManager creditManager = new CreditManager(context);

// Purchase credits
boolean success = creditManager.purchaseCredits(userId, packageId, paymentMethod);

// Check balance
double balance = creditManager.getCreditBalance(userId);

// Deduct credits
boolean deducted = creditManager.deductCredits(userId, amount, transactionType);
```

### Credit Operations

#### Purchase Credits
```java
// Get available packages
List<CreditPackage> packages = creditManager.getAvailablePackages();

// Purchase a package
boolean success = creditManager.purchaseCredits(userId, packageId, CreditManager.PAYMENT_GCASH);
```

#### Balance Management
```java
// Check if user has sufficient credits
boolean hasEnough = creditManager.validateCreditBalance(userId, amount);

// Get detailed balance information
CreditBalance balance = creditManager.getCreditBalanceObject(userId);
double available = balance.getAvailableCredits();
double reserved = balance.getReservedCredits();
```

#### Transaction Management
```java
// Get transaction history
List<CreditTransaction> transactions = creditManager.getTransactionHistory(userId);

// Get specific transaction
CreditTransaction transaction = creditManager.getTransactionById(transactionId);

// Refund a transaction
boolean refunded = creditManager.refundTransaction(transactionId);
```

### Advanced Features

#### Credit Reservations
```java
// Reserve credits for pending transaction
boolean reserved = creditManager.reserveCredits(userId, amount);

// Release reserved credits
boolean released = creditManager.releaseCredits(userId, amount);
```

#### Credit Transfers
```java
// Transfer credits between users
boolean transferred = creditManager.transferCredits(fromUserId, toUserId, amount);
```

#### Security & Validation
```java
// Validate credit amount
boolean valid = creditManager.validateCreditAmount(amount);

// Check credit limits
boolean withinLimits = creditManager.checkCreditLimits(userId, amount);

// Audit transaction
boolean isValid = creditManager.auditCreditTransaction(transactionId);
```

## Credit States

- **AVAILABLE**: Credits immediately available for use
- **RESERVED**: Credits reserved for pending transactions
- **PENDING**: Credits pending approval or verification
- **FROZEN**: Credits frozen due to security concerns

## Transaction Types

- `purchase`: Credit package purchases
- `redemption`: Redemption code usage
- `bid`: Auction bid deductions
- `refund`: Transaction refunds
- `transfer`: Peer-to-peer transfers
- `reserve`: Credit reservations
- `release`: Credit reservation releases

## Payment Methods

- `gcash`: GCash payment integration
- `maya`: Maya payment integration
- `transfer`: Bank transfer payments

## Security Features

1. **Amount Validation**: Enforces minimum and maximum credit amounts
2. **Daily Limits**: Prevents excessive purchases and transfers
3. **Transaction Auditing**: Comprehensive audit trails for all operations
4. **Fraud Detection**: Built-in mechanisms for detecting suspicious activity
5. **Atomic Operations**: All operations are atomic to prevent data corruption

## Database Integration

The system integrates with the existing SQLite database through the `DatabaseHelper` class. It uses the following tables:

- `users`: Stores user credit balances
- `credit_transactions`: Stores all credit transaction records

## Performance Optimizations

1. **Caching**: Balance and transaction caches for improved performance
2. **Batch Operations**: Efficient batch processing for multiple operations
3. **Connection Pooling**: Optimized database connection management
4. **Memory Management**: Automatic cache clearing and memory optimization

## Error Handling

The system provides comprehensive error handling with:

- Input validation for all parameters
- Database transaction rollback on failures
- Detailed logging for debugging
- Graceful degradation on errors

## Example Implementation

See `CreditManagerExample.java` for comprehensive usage examples demonstrating all features of the credit management system.

## Thread Safety

All operations are thread-safe and can be safely called from multiple threads. The system uses concurrent data structures and proper synchronization to ensure data integrity.

## Future Enhancements

1. **Payment Gateway Integration**: Real payment processing integration
2. **Advanced Fraud Detection**: Machine learning-based fraud detection
3. **Credit Expiration**: Time-based credit expiration system
4. **Loyalty Programs**: Credit-based loyalty and rewards system
5. **Analytics**: Comprehensive credit usage analytics and reporting
