# Credit Transfer Implementation - Complete Guide

## Overview

This document describes the comprehensive credit transfer system implementation for BidHub, ensuring accurate, secure, and consistent credit transfers across all transaction types.

## Implementation Summary

### ✅ Completed Features

1. **Stored Procedures Updated**
   - `PlaceBid`: Deducts credits from bidder, refunds previous bidder, returns updated balance
   - `BuyNow`: Transfers credits from buyer to seller immediately, returns updated balances
   - `EndAuction`: Transfers credits to seller when auction ends, handles winner determination

2. **API Endpoints Enhanced**
   - `POST /api/bids/place`: Returns updated balance after bid placement
   - `POST /api/items/:id/buy-now`: Returns updated balances for both buyer and seller
   - `GET /api/credits/balance`: Returns real-time balance with version tracking

3. **AuctionEndService Refactored**
   - Now uses `EndAuction` stored procedure instead of duplicate logic
   - Ensures consistency and prevents duplicate transfers

4. **Validation & Safeguards**
   - Idempotency keys prevent duplicate transactions
   - Balance version tracking for consistency
   - Insufficient balance validation
   - Duplicate transaction detection

## Credit Transfer Flows

### 1. Placing a Bid

**Flow:**
1. User places bid → Credits deducted immediately
2. If outbidding previous bidder → Previous bidder gets refund
3. Credits held until auction ends
4. When auction ends → Credits transferred to seller

**API Response:**
```json
{
  "message": "Bid placed successfully",
  "bid_amount": 100.00,
  "item_id": "uuid-here",
  "previous_balance": 500.00,
  "new_balance": 400.00,
  "refunded_amount": 0.00
}
```

### 2. Buy Now Purchase

**Flow:**
1. User clicks "Buy Now" → Credits deducted from buyer
2. Credits immediately transferred to seller
3. Item marked as sold

**API Response:**
```json
{
  "message": "Purchase completed successfully",
  "item_id": "uuid-here",
  "amount": 250.00,
  "buyer": {
    "previous_balance": 500.00,
    "new_balance": 250.00
  },
  "seller": {
    "new_balance": 750.00
  }
}
```

### 3. Auction End (Winning Bid)

**Flow:**
1. Auction end time reached → System processes ended auctions
2. Winning bidder determined → Credits already deducted when bid placed
3. Credits transferred to seller
4. Item marked as sold

**Processed by:**
- `AuctionEndService.processEndedAuctions()` (called periodically)
- Uses `EndAuction` stored procedure for credit transfer

## Database Schema

### Key Tables

**users**
- `credits`: Current balance (DECIMAL(10,2))
- `balance_version`: Version number for consistency checking

**credit_transactions**
- `idempotency_key`: Unique key to prevent duplicate transactions
- `type`: Transaction type (bid, buy_now, bonus, outbid_refund, etc.)
- `status`: Transaction status (pending, completed, failed, cancelled)
- `item_id`: Related item (if applicable)

**bids**
- `status`: Bid status (pending, active, winning, outbid, won, lost)
- `bidder_alias`: Bidder's alias for display

## API Endpoints

### Get Balance
```
GET /api/credits/balance
Authorization: Bearer <token>
```

**Response:**
```json
{
  "credits": 500.00,
  "balance_version": 42,
  "available_balance": 500.00,
  "pending_balance": 0.00,
  "recent_transactions": [...],
  "last_updated": "2024-01-15T10:30:00.000Z"
}
```

### Place Bid
```
POST /api/bids/place
Authorization: Bearer <token>
Content-Type: application/json

{
  "item_id": "uuid-here",
  "amount": 100.00
}
```

**Response:**
```json
{
  "message": "Bid placed successfully",
  "bid_amount": 100.00,
  "item_id": "uuid-here",
  "previous_balance": 500.00,
  "new_balance": 400.00,
  "refunded_amount": 0.00
}
```

### Buy Now
```
POST /api/items/:id/buy-now
Authorization: Bearer <token>
Content-Type: application/json

{
  "amount": 250.00
}
```

**Response:**
```json
{
  "message": "Purchase completed successfully",
  "item_id": "uuid-here",
  "amount": 250.00,
  "buyer": {
    "previous_balance": 500.00,
    "new_balance": 250.00
  },
  "seller": {
    "new_balance": 750.00
  }
}
```

## Safety Features

### 1. Idempotency Keys
Every transaction includes a unique idempotency key to prevent duplicate processing:
- Format: `{TYPE}_{ITEM_ID}_{USER_ID}_{TIMESTAMP}_{RANDOM}`
- Stored in `credit_transactions.idempotency_key`
- Database constraint prevents duplicates

### 2. Balance Version Tracking
- `balance_version` increments on each balance change
- Helps detect concurrent modifications
- Used for optimistic locking

### 3. Row Locking
- `FOR UPDATE` locks prevent race conditions
- Ensures atomic operations
- Prevents double-spending

### 4. Transaction Validation
- Insufficient balance checks before processing
- Duplicate transaction detection
- Balance consistency verification

## SQL Scripts

### Primary Fix Script
**File:** `sql/fix_credit_transfer_comprehensive.sql`

**What it does:**
- Updates `PlaceBid` procedure with idempotency and balance returns
- Updates `BuyNow` procedure with idempotency and balance returns
- Updates `EndAuction` procedure with proper credit transfer
- Adds validation and error handling

**Run this first:**
```sql
-- Copy entire file into Render's SQL Editor
```

### Supporting Scripts
- `sql/fix_bids_table_add_bidder_alias.sql` - Adds missing column
- `sql/fix_placebid_groupby_error_fixed.sql` - Fixes GROUP BY errors

## Testing Checklist

### ✅ Place Bid
- [ ] Credits deducted from bidder
- [ ] Previous bidder refunded (if outbid)
- [ ] Balance returned in API response
- [ ] Transaction recorded in credit_transactions
- [ ] Idempotency key prevents duplicates

### ✅ Buy Now
- [ ] Credits deducted from buyer
- [ ] Credits added to seller
- [ ] Both balances returned in API response
- [ ] Item marked as sold
- [ ] Transactions recorded for both parties

### ✅ Auction End
- [ ] Winning bidder determined correctly
- [ ] Credits transferred to seller
- [ ] Item status updated to 'sold'
- [ ] Bid statuses updated (won/lost)
- [ ] Notifications sent

### ✅ Balance Endpoint
- [ ] Returns current balance
- [ ] Includes balance_version
- [ ] Shows pending transactions
- [ ] Lists recent transactions

## Error Handling

### Insufficient Credits
```json
{
  "error": "insufficient_credits",
  "details": "balance_too_low",
  "message": "Insufficient credits. Required: ₱100.00, Available: ₱50.00",
  "required": 100.00,
  "available": 50.00
}
```

### Duplicate Transaction
```json
{
  "error": "Duplicate transaction detected",
  "existingTransactionId": 123
}
```

### Item Not Found
```json
{
  "error": "item_not_found",
  "message": "Item not found or not active"
}
```

## Frontend Integration

### Updating Balance After Transaction

```javascript
// After placing bid
const response = await fetch('/api/bids/place', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({ item_id, amount })
});

const data = await response.json();
// Update UI with new balance
updateUserBalance(data.new_balance);
```

### Refreshing Balance

```javascript
// Get current balance
const response = await fetch('/api/credits/balance', {
  headers: {
    'Authorization': `Bearer ${token}`
  }
});

const data = await response.json();
updateUserBalance(data.credits);
```

## Monitoring & Debugging

### Check Transaction History
```sql
SELECT * FROM credit_transactions 
WHERE user_id = ? 
ORDER BY created_at DESC 
LIMIT 20;
```

### Verify Balance Consistency
```sql
SELECT 
  u.id,
  u.credits,
  u.balance_version,
  (SELECT SUM(amount) FROM credit_transactions 
   WHERE user_id = u.id AND type IN ('bonus', 'purchase', 'outbid_refund') 
   AND status = 'completed') as total_received,
  (SELECT SUM(amount) FROM credit_transactions 
   WHERE user_id = u.id AND type IN ('bid', 'buy_now') 
   AND status = 'completed') as total_spent
FROM users u
WHERE u.id = ?;
```

### Check Pending Transactions
```sql
SELECT * FROM credit_transactions 
WHERE status = 'pending' 
ORDER BY created_at DESC;
```

## Deployment Steps

1. **Run SQL Scripts** (in order):
   - `sql/fix_bids_table_add_bidder_alias.sql`
   - `sql/fix_credit_transfer_comprehensive.sql`

2. **Deploy Backend Changes**:
   - Updated `bidhub-backend/src/routes/bids.js`
   - Updated `bidhub-backend/src/routes/items.js`
   - Updated `bidhub-backend/src/services/auctionEndService.js`
   - Updated `bidhub-backend/src/routes/credits.js`
   - New `bidhub-backend/src/utils/creditValidation.js`

3. **Test Each Flow**:
   - Place bid and verify balance update
   - Buy now and verify both balances
   - Wait for auction end and verify seller receives credits

4. **Monitor**:
   - Check logs for any errors
   - Verify balance consistency
   - Monitor transaction success rate

## Troubleshooting

### Issue: Balance not updating
- Check if transaction was recorded in `credit_transactions`
- Verify `status = 'completed'`
- Check for errors in stored procedure execution

### Issue: Duplicate transactions
- Verify idempotency keys are unique
- Check for concurrent requests
- Review transaction logs

### Issue: Insufficient credits error
- Verify user has sufficient balance
- Check for pending transactions
- Review recent transaction history

## Support

For issues or questions:
1. Check transaction logs in `credit_transactions` table
2. Review stored procedure execution logs
3. Verify balance consistency using SQL queries above
4. Check API response for error details

