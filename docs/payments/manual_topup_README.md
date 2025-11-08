# BidHub Manual Top-Up System

**Version**: 1.0  
**Last Updated**: November 3, 2025  
**Status**: Production Ready

---

## Overview

The BidHub Manual Top-Up System is a simplified payment integration that replaces complex payment SDK implementations (GCash/Maya APIs) with a QR code + reference number confirmation flow. This approach:

- ✅ **Simplifies Development**: No third-party SDK integration required
- ✅ **Reduces Dependencies**: Fewer external services to maintain
- ✅ **Improves Testing**: Easy to test without real payment APIs
- ✅ **Faster MVP Launch**: Can go live before securing payment partnerships
- ✅ **Better Control**: Full control over payment verification process

---

## How It Works

### User Flow

```
1. User Opens Credit Shop
   ↓
2. User Selects Amount (₱500, ₱1,000, ₱5,000)
   ↓
3. System Generates Unique Reference Code
   ↓
4. User Sees Payment Instructions Modal:
   - Official GCash/Maya Number
   - QR Code for Quick Payment
   - Generated Reference Code
   ↓
5. User Transfers Money via GCash/Maya App
   ↓
6. User Enters Transaction Receipt Number
   ↓
7. Top-Up Status: PENDING → UNDER_REVIEW
   ↓
8. Admin Confirms Payment
   ↓
9. Credits Added to User Account
   ↓
10. User Gets Notification + Credits Updated
```

---

## System Architecture

### Backend Components

1. **`topups` Table**: Tracks top-up requests and status
2. **`credit_ledger` Table**: Immutable audit trail for all credit changes
3. **Stored Procedures**: `sp_confirm_topup()`, `sp_reject_topup()`
4. **API Endpoints**: REST endpoints for top-up operations
5. **Admin Dashboard**: Manual review and confirmation interface

### Frontend Components

1. **Credit Shop UI**: Package selection and top-up initiation
2. **Payment Modal**: QR code and instruction display
3. **Receipt Entry Form**: User input for receipt reference
4. **Status Tracking**: Polling or WebSocket for status updates

---

## Database Schema

### Topups Table

Stores all top-up requests with their status and references.

| Field | Type | Description |
|-------|------|-------------|
| `id` | INT UNSIGNED | Primary key |
| `user_id` | INT UNSIGNED | Foreign key to users |
| `user_email` | VARCHAR(255) | User email (denormalized) |
| `amount` | DECIMAL(10,2) | Top-up amount |
| `generated_ref` | VARCHAR(16) | System-generated unique reference |
| `user_receipt_ref` | VARCHAR(64) | User-entered receipt code |
| `payment_method` | ENUM | 'gcash', 'maya', 'bank_transfer', 'other' |
| `status` | ENUM | 'PENDING', 'UNDER_REVIEW', 'CONFIRMED', 'REJECTED' |
| `confirmed_by` | INT UNSIGNED | Admin user_id who confirmed |
| `confirmed_at` | TIMESTAMP | Confirmation timestamp |
| `created_at` | TIMESTAMP | Request creation time |

### Credit Ledger Table

Immutable audit trail for all credit adjustments.

| Field | Type | Description |
|-------|------|-------------|
| `id` | BIGINT UNSIGNED | Primary key |
| `user_id` | INT UNSIGNED | Foreign key to users |
| `delta` | DECIMAL(10,2) | Credit change (+ or -) |
| `balance_before` | DECIMAL(10,2) | Balance before transaction |
| `balance_after` | DECIMAL(10,2) | Balance after transaction |
| `reason` | ENUM | 'TOPUP', 'BID', 'REFUND', etc. |
| `ref_id` | INT UNSIGNED | Reference to related record |
| `ref_type` | ENUM | 'topup', 'bid', 'item', etc. |
| `credit_transaction_id` | INT UNSIGNED | Link to credit_transactions |
| `created_at` | TIMESTAMP | Transaction timestamp |

See `manual_topup_schema.sql` for complete schema definition.

---

## API Endpoints

### 1. Initiate Top-Up

**POST** `/api/topups`

Creates a new top-up request and generates reference code.

**Request Body**:
```json
{
  "amount": 500.00,
  "payment_method": "gcash"
}
```

**Response** (201 Created):
```json
{
  "success": true,
  "topup_id": 123,
  "generated_ref": "TOPUP202400123",
  "instructions": "Please pay ₱500.00 to GCash +63 916 123 4567 with reference TOPUP202400123",
  "qr_code_data": "data:image/png;base64,iVBORw0KG...",
  "payment_number": "+63 916 123 4567",
  "status": "PENDING"
}
```

---

### 2. Submit Receipt Reference

**POST** `/api/topups/:id/submit`

User submits their receipt reference number.

**Request Body**:
```json
{
  "user_receipt_ref": "RECEIPT1234567890"
}
```

**Response** (200 OK):
```json
{
  "success": true,
  "status": "UNDER_REVIEW",
  "message": "Top-up submitted for review"
}
```

---

### 3. Check Top-Up Status

**GET** `/api/topups/:id`

Get current status of a top-up.

**Response** (200 OK):
```json
{
  "id": 123,
  "amount": 500.00,
  "generated_ref": "TOPUP202400123",
  "user_receipt_ref": "RECEIPT1234567890",
  "status": "UNDER_REVIEW",
  "created_at": "2024-11-03T10:30:00Z",
  "submitted_at": "2024-11-03T10:32:00Z"
}
```

---

### 4. List User Top-Ups

**GET** `/api/topups`

Get user's top-up history.

**Query Parameters**:
- `status`: Filter by status
- `limit`: Number of results (default: 20)
- `offset`: Pagination offset

**Response** (200 OK):
```json
{
  "topups": [
    {
      "id": 123,
      "amount": 500.00,
      "status": "CONFIRMED",
      "created_at": "2024-11-03T10:30:00Z",
      "confirmed_at": "2024-11-03T11:00:00Z"
    }
  ],
  "total": 5,
  "limit": 20,
  "offset": 0
}
```

---

### 5. Admin: Confirm Top-Up

**POST** `/api/admin/topups/:id/confirm`

Admin confirms top-up and credits user account.

**Response** (200 OK):
```json
{
  "success": true,
  "new_balance": 1500.00,
  "message": "Top-up confirmed and credits added"
}
```

---

### 6. Admin: Reject Top-Up

**POST** `/api/admin/topups/:id/reject`

Admin rejects top-up request.

**Request Body**:
```json
{
  "reason": "Receipt reference doesn't match payment records"
}
```

**Response** (200 OK):
```json
{
  "success": true,
  "message": "Top-up rejected"
}
```

---

## Configuration

### Payment Numbers

Set in environment variables:

```bash
# .env
PAYMENT_GCASH_NUMBER=+63 916 123 4567
PAYMENT_MAYA_NUMBER=+63 917 789 0123
PAYMENT_BANK_ACCOUNT=1234567890
```

### Admin Review Thresholds

Configure automatic vs manual review:

```javascript
// config/topup.js
{
  autoConfirmEnabled: false,  // Set to true to auto-confirm < 1000 PHP
  autoConfirmThreshold: 1000.00,  // Amount threshold
  reviewQueueLimit: 50,  // Max pending reviews
  confirmationTimeoutHours: 24  // Auto-reject after 24 hours
}
```

---

## QR Code Generation

QR codes contain payment information in JSON format.

**QR Code Content**:
```json
{
  "payment_method": "gcash",
  "amount": 500.00,
  "reference": "TOPUP202400123",
  "recipient": "+63 916 123 4567"
}
```

**Implementation** (Node.js with `qrcode` package):
```javascript
const QRCode = require('qrcode');

async function generateQRCode(topupData) {
  const qrData = {
    payment_method: topupData.payment_method,
    amount: topupData.amount,
    reference: topupData.generated_ref,
    recipient: getPaymentNumber(topupData.payment_method)
  };
  
  const dataURL = await QRCode.toDataURL(JSON.stringify(qrData));
  return dataURL;
}
```

---

## Security Considerations

### Reference Code Generation

- **Format**: `TOPUP` + `YYYYMMDD` + `SEQUENCE`
- **Uniqueness**: Enforced via database UNIQUE constraint
- **Length**: 16 characters (reasonable for user input)
- **Character Set**: Alphanumeric uppercase

**Implementation**:
```javascript
function generateTopupRef() {
  const date = new Date().toISOString().split('T')[0].replace(/-/g, '');
  const sequence = Math.floor(Math.random() * 10000).toString().padStart(4, '0');
  return `TOPUP${date}${sequence}`;
}
```

### Receipt Reference Validation

- **Length**: 8-32 characters
- **Format**: Flexible (users may have different receipt formats)
- **Uniqueness**: Not enforced (same receipt could be used multiple times, handled by admin review)

### Admin Authentication

All admin endpoints require special admin role:

```javascript
// middleware/adminAuth.js
function requireAdmin(req, res, next) {
  if (!req.user || !req.user.is_admin) {
    return res.status(403).json({ error: 'Admin access required' });
  }
  next();
}
```

---

## Error Handling

### Common Error Responses

**Invalid Amount**:
```json
{
  "error": "Invalid amount",
  "details": "Amount must be between 100 and 50000 PHP"
}
```

**Duplicate Reference**:
```json
{
  "error": "Reference code collision",
  "details": "Please try again"
}
```

**Top-Up Not Found**:
```json
{
  "error": "Top-up not found",
  "details": "Invalid top-up ID or user mismatch"
}
```

**Invalid Status Transition**:
```json
{
  "error": "Invalid status transition",
  "details": "Cannot confirm top-up in PENDING status"
}
```

---

## Testing

### Manual Testing Steps

1. **Initiate Top-Up**:
   ```
   POST /api/topups
   Amount: 500
   Payment Method: gcash
   ```

2. **Verify QR Code Generated**:
   - Check response includes `qr_code_data`
   - Decode QR code and verify content

3. **Submit Receipt**:
   ```
   POST /api/topups/123/submit
   Receipt Ref: TEST1234567890
   ```

4. **Admin Confirm**:
   ```
   POST /api/admin/topups/123/confirm
   ```

5. **Verify Credits Added**:
   ```
   GET /api/credits/balance
   Verify balance increased by 500
   ```

6. **Check Ledger Entry**:
   ```sql
   SELECT * FROM credit_ledger WHERE ref_id = 123;
   ```

---

## Monitoring and Analytics

### Key Metrics

Track via `v_user_topup_stats` view:

```sql
SELECT * FROM v_user_topup_stats 
WHERE user_id = ?;
```

Metrics to monitor:
- Total top-ups per user
- Confirmed vs rejected ratio
- Average top-up amount
- Confirmation time (from submission to confirmation)
- Rejection rate by reason

### Alert Conditions

Set up alerts for:
- High rejection rate (> 20%)
- Long pending queue (> 50 items)
- Failed credit ledger insertions
- Missing credit_transactions entries

---

## Future Enhancements

### Phase 2: Automated Verification

```javascript
// Future: Integrate with GCash/Maya APIs for automated verification
async function verifyPaymentAutomatically(topupId) {
  const topup = await getTopup(topupId);
  
  // Check payment gateway API
  const verification = await paymentGateway.verify({
    amount: topup.amount,
    reference: topup.generated_ref,
    receipt: topup.user_receipt_ref
  });
  
  if (verification.confirmed) {
    await confirmTopup(topupId, SYSTEM_USER_ID);
  }
}
```

### Phase 3: Real-Time Notifications

- Email confirmation when status changes
- SMS alerts for large top-ups
- Push notifications on mobile app
- WebSocket updates for instant status changes

### Phase 4: Fraud Detection

- Machine learning for pattern detection
- IP address tracking
- Device fingerprinting
- Velocity checks (too many top-ups too quickly)

---

## Migration from Legacy System

If migrating from existing `credit_transactions` top-ups:

1. Existing `credit_transactions` entries remain valid
2. New top-ups use `topups` + `credit_ledger` tables
3. `credit_ledger` links to `credit_transactions` via foreign key
4. No data loss during migration

**Backward Compatibility**:
- Old API endpoints (`POST /api/credits/purchase`) still supported
- New endpoints (`POST /api/topups`) recommended for new implementations
- Both systems write to `credit_transactions` table

---

## Troubleshooting

### Common Issues

**"Reference code already exists"**
- Solution: Rare but possible with high volume
- Retry with new code automatically generated

**"Top-up not appearing in admin queue"**
- Check `status` is 'UNDER_REVIEW'
- Verify admin user has proper permissions
- Check database connection

**"Credits not adding after confirmation"**
- Verify `sp_confirm_topup` stored procedure executed
- Check `credit_ledger` for entry
- Verify `credit_transactions` created
- Check for database transaction rollback

**"QR code not scanning"**
- Verify QR code data URL format
- Check QR code size (minimum 250x250 pixels)
- Test with multiple QR code readers

---

## Support

For issues or questions:
- **Technical**: Create issue in GitHub repository
- **Business**: Contact BidHub team lead
- **Database**: Check `sql/manual_topup_schema.sql`
- **API**: See OpenAPI spec in `/docs/openapi.yaml`

---

## References

- [Database Schema](./manual_topup_schema.sql)
- [API Documentation](./api.md)
- [Sequence Diagram](./manual_topup_sequence.md)
- [Integration Guide](./integration_guide.md)

---

**Document Version**: 1.0  
**Last Updated**: November 3, 2025  
**Maintained By**: BidHub Development Team

