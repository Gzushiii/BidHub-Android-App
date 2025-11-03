# Manual Top-Up Sequence Flow

**Diagram**: Manual Top-Up Payment Flow  
**Version**: 1.0  
**Date**: November 3, 2025

---

## System Flow Diagram

### Complete User Journey

```
┌─────────┐         ┌──────────┐         ┌──────────┐         ┌─────────┐
│  User   │         │ Android  │         │  Backend │         │  Admin  │
│         │         │   App    │         │   API    │         │  Panel  │
└────┬────┘         └─────┬────┘         └─────┬────┘         └─────┬───┘
     │                    │                     │                    │
     │  1. Opens Credit Shop                    │                    │
     │────────────────────>│                    │                    │
     │                    │                     │                    │
     │  2. Selects ₱500 Package                 │                    │
     │────────────────────>│                    │                    │
     │                    │                     │                    │
     │                    │  3. POST /api/topups                    │
     │                    │────────────────────>│                    │
     │                    │                     │                    │
     │                    │  4. Generate Reference                  │
     │                    │     Code: TOPUP20241103...              │
     │                    │<────────────────────│                    │
     │                    │                     │                    │
     │                    │  5. Insert topups                       │
     │                    │     (status: PENDING)                   │
     │                    │                     │                    │
     │  6. Show Payment Modal                    │                    │
     │<────────────────────│                    │                    │
     │                    │                     │                    │
     │  ┌─────────────────────────────────┐    │                    │
     │  │ PAYMENT INSTRUCTIONS            │    │                    │
     │  │                                 │    │                    │
     │  │ [QR CODE]                       │    │                    │
     │  │                                 │    │                    │
     │  │ Pay to: +63 916 123 4567       │    │                    │
     │  │ Amount: ₱500.00                │    │                    │
     │  │ Reference: TOPUP202411030001   │    │                    │
     │  │                                 │    │                    │
     │  │ [Enter Receipt Number]         │    │                    │
     │  │ [Submit for Review]            │    │                    │
     │  └─────────────────────────────────┘    │                    │
     │                    │                     │                    │
     │  7. User Transfers via GCash             │                    │
     │     Gets Receipt: RECEIPT12345           │                    │
     │                    │                     │                    │
     │  8. Enters Receipt Number                │                    │
     │────────────────────>│                    │                    │
     │                    │                     │                    │
     │                    │  9. POST /api/topups/:id/submit         │
     │                    │────────────────────>│                    │
     │                    │                     │                    │
     │                    │ 10. Update topups                       │
     │                    │     (status: UNDER_REVIEW)              │
     │                    │<────────────────────│                    │
     │                    │                     │                    │
     │ 11. Show "Under Review" Status           │                    │
     │<────────────────────│                    │                    │
     │                    │                     │                    │
     │                    │                     │                    │
     │                    │                     │  12. Admin Reviews │
     │                    │                     │──────────────────->│
     │                    │                     │                    │
     │                    │                     │ 13. Confirms Payment│
     │                    │                     │<───────────────────│
     │                    │                     │                    │
     │                    │ 14. POST /api/admin/topups/:id/confirm  │
     │                    │────────────────────>│                    │
     │                    │                     │                    │
     │                    │ 15. BEGIN TRANSACTION                    │
     │                    │     ┌────────────────┐                  │
     │                    │     │ UPDATE users SET credits += 500   │
     │                    │     │ INSERT credit_transactions        │
     │                    │     │ INSERT credit_ledger entry        │
     │                    │     │ UPDATE topups (status: CONFIRMED) │
     │                    │     └────────────────┘                  │
     │                    │ 16. COMMIT TRANSACTION                   │
     │                    │                     │                    │
     │                    │ 17. Return New Balance                  │
     │                    │<────────────────────│                    │
     │                    │                     │                    │
     │ 18. Show Success + Refresh Credits       │                    │
     │<────────────────────│                    │                    │
     │                    │                     │                    │
     │ 19. User Sees New Balance: ₱500          │                    │
     │     Can Now Bid on Items                 │                    │
     │                    │                     │                    │
```

---

## Detailed Sequence Steps

### Phase 1: Top-Up Initiation

**Step 1**: User opens Credit Shop in Android app
- Navigates to Credits tab
- Sees available packages (₱500, ₱1,000, ₱5,000)

**Step 2**: User selects amount
- Clicks on ₱500 package button
- App shows confirmation dialog

**Step 3**: Backend generates top-up request
- Android app sends `POST /api/topups`
- Request body:
  ```json
  {
    "amount": 500.00,
    "payment_method": "gcash"
  }
  ```

**Step 4**: Backend generates unique reference
- Creates `generated_ref`: "TOPUP202411030001"
- Inserts into `topups` table with `status = 'PENDING'`
- Generates QR code data
- Returns payment instructions

**Step 5**: Backend returns response
```json
{
  "success": true,
  "topup_id": 123,
  "generated_ref": "TOPUP202411030001",
  "qr_code_data": "data:image/png;base64,iVBORw0...",
  "payment_number": "+63 916 123 4567",
  "instructions": "Pay ₱500 to GCash +63 916 123 4567...",
  "status": "PENDING"
}
```

---

### Phase 2: Payment Execution

**Step 6**: User sees payment modal
- Modal displays:
  - QR code (scannable)
  - Payment number
  - Reference code
  - Amount
  - Input field for receipt number

**Step 7**: User transfers money
- Opens GCash app
- Scans QR code or enters manually
- Completes transfer
- Receives receipt: "GC20241103012345ABC"

**Step 8**: User enters receipt number
- Types receipt reference in app
- Clicks "Submit for Review"

**Step 9**: Backend receives submission
- Android app sends `POST /api/topups/123/submit`
- Request body:
  ```json
  {
    "user_receipt_ref": "GC20241103012345ABC"
  }
  ```

**Step 10**: Backend updates status
- Updates `topups` record
- Sets `status = 'UNDER_REVIEW'`
- Sets `submitted_at = NOW()`

**Step 11**: User sees confirmation
- App shows "Submitted for Review" toast
- Status changes to "Under Review"

---

### Phase 3: Admin Review

**Step 12**: Admin reviews in dashboard
- Opens admin panel
- Sees pending top-ups queue
- Views details:
  - User info
  - Amount
  - Generated reference
  - User receipt reference
  - Submission time

**Step 13**: Admin verifies payment
- Checks payment records
- Matches amounts and references
- Decides to approve or reject

**Step 14**: Backend confirms top-up
- Admin clicks "Confirm"
- Sends `POST /api/admin/topups/123/confirm`
- Includes admin user ID from JWT

**Step 15**: Atomic transaction execution
```
BEGIN TRANSACTION

1. Get current balance
   SELECT credits FROM users WHERE id = user_id
   → current_balance = 1000.00

2. Calculate new balance
   new_balance = 1000.00 + 500.00 = 1500.00

3. Update user credits
   UPDATE users SET credits = 1500.00 WHERE id = user_id

4. Insert credit_transactions entry
   INSERT INTO credit_transactions (
     user_id, type, amount, status, payment_method, 
     reference, transaction_id
   ) VALUES (
     123, 'purchase', 500.00, 'completed', 'gcash',
     'TOPUP202411030001', 'GC20241103012345ABC'
   )
   → transaction_id = 456

5. Insert credit_ledger audit entry
   INSERT INTO credit_ledger (
     user_id, user_email, delta, balance_before, 
     balance_after, reason, description, ref_id, 
     ref_type, credit_transaction_id, performed_by
   ) VALUES (
     123, 'user@example.com', 500.00, 1000.00, 1500.00,
     'TOPUP', 'Top-up confirmed: ₱500.00 via gcash',
     123, 'topup', 456, 999
   )

6. Update topup status
   UPDATE topups SET 
     status = 'CONFIRMED',
     confirmed_by = 999,
     confirmed_at = NOW()
   WHERE id = 123

COMMIT TRANSACTION
```

**Step 16**: Success response
```json
{
  "success": true,
  "new_balance": 1500.00,
  "message": "Top-up confirmed and credits added"
}
```

---

### Phase 4: User Notification

**Step 17**: Android app receives confirmation
- Status check returns `status: 'CONFIRMED'`
- Or WebSocket notification received
- Credentials refreshed

**Step 18**: User sees success
- Toast notification: "Top-up confirmed! +₱500"
- Credit balance updates in UI
- Returns to main credits screen

**Step 19**: User can use credits
- Navigates to Browse items
- Places bid or buys items
- Credits balance decrements as used

---

## Alternative: Auto-Confirmation Flow

For small amounts, auto-confirmation can be enabled:

```
User Submits Receipt
      ↓
Auto-Confirmation Check
      ├─ Amount < 1000 PHP? → YES → Auto-Confirm
      │                            ↓
      │                    Credits Added
      │                            ↓
      │                    User Notified
      │
      └─ Amount >= 1000 PHP? → NO → Admin Review
                                          ↓
                                   Manual Verification
```

**Configuration**:
```javascript
{
  autoConfirmEnabled: true,
  autoConfirmThreshold: 1000.00,
  requirePhoneVerification: true
}
```

---

## Error Flows

### Rejection Flow

```
Admin Reviews Top-Up
      ↓
Verification Fails
      ↓
POST /api/admin/topups/:id/reject
      ↓
Set status = 'REJECTED'
Set rejected_by = admin_user_id
Set rejection_reason = "..."
      ↓
Send Email to User
      ↓
User Sees "Rejected" Status
      ↓
Can Resubmit with Correct Info
```

### Timeout Flow

```
Top-Up in UNDER_REVIEW
      ↓
24 Hours Elapse
      ↓
Auto-Rejection Cron Job
      ↓
Status = 'REJECTED'
Reason = "Confirmation timeout"
      ↓
User Notification
```

### Duplicate Reference Flow

```
Generate Reference Code
      ↓
Check Database
      ├─ Exists? → YES → Retry Generation
      │
      └─ Exists? → NO → Use Code
```

---

## Status State Machine

```
┌─────────┐
│ PENDING │ ──────┬──> User Submits Receipt
└─────────┘       │    │
                  │    ↓
      ┌───────────┴───┐
      │ UNDER_REVIEW  │ ──────┬──> Admin Confirms
      └───────────────┘       │    │
                              │    ↓
                         ┌────┴───┐
                         │CONFIRMED│
                         └─────────┘
                              │
                              ↓
                    Credits Added ✓

              ┌───────────────┴──────┬────────────────┐
              │                      │                │
       Timeout (24h)          Admin Rejects    User Cancels
              │                      │                │
              ↓                      ↓                ↓
         REJECTED              REJECTED          CANCELLED
```

---

## Database Transaction Safety

### ACID Compliance

**Atomicity**: All credit updates happen in single transaction
- If any step fails, entire operation rolls back
- No partial credit additions

**Consistency**: Always maintain referential integrity
- Foreign keys enforced
- Balance always correct

**Isolation**: Concurrent top-ups don't interfere
- Row-level locking on user record
- Transaction isolation level: READ COMMITTED

**Durability**: Once confirmed, changes persist
- Data flushed to disk immediately
- No lost transactions after commit

### Concurrency Handling

**Scenario**: Two admins confirm same top-up

```
Admin A              Admin B              Database
   │                    │                    │
   │─LOCK topups row───>│                    │
   │                    │                    │
   │                    │─LOCK topups row─X─>│ (BLOCKED)
   │                    │                    │
   │─Check status──────>│                    │
   │  (UNDER_REVIEW)    │                    │
   │                    │                    │
   │─Confirm & Commit──>│                    │
   │─UNLOCK─────────────>│                    │
   │                    │                    │
   │                    │─Lock Success───X─>│ (row gone)
   │                    │                    │
   │                    │ Error: Already    │
   │                    │ confirmed         │
```

---

## API Timing Diagram

```
Time →

0s    Android: POST /api/topups
       └─> Backend: 200ms response
                  
1s    Android: Display Modal
       └─> User: Reads Instructions

10s   User: Completes Payment
       └─> GCash: 5s transfer time

15s   Android: POST /api/topups/:id/submit
       └─> Backend: 200ms update

16s   Android: Show "Under Review"
       └─> Backend: Status = UNDER_REVIEW

30m   Admin: Reviews in Dashboard
       └─> Admin: Confirms Payment

30m   Admin: POST /api/admin/topups/:id/confirm
       └─> Backend: 500ms transaction

31m   Backend: Send WebSocket Notification
       └─> Android: Receive & Update UI

31m   User: Sees "Confirmed" + New Balance
       └─> Can Now Use Credits
```

---

## Testing Scenarios

### Happy Path Test
1. ✅ User initiates top-up
2. ✅ Receives reference code
3. ✅ Submits receipt
4. ✅ Admin confirms
5. ✅ Credits added
6. ✅ Balance updated in UI

### Error Path Tests
1. ❌ Invalid amount (< 100)
2. ❌ Duplicate reference code
3. ❌ Missing receipt reference
4. ❌ Admin rejection
5. ❌ Timeout after 24h
6. ❌ Concurrent confirmation attempt

### Edge Cases
1. 🔍 Very large amount (50,000 PHP)
2. 🔍 Rapid successive top-ups
3. 🔍 Network interruption during confirmation
4. 🔍 Database deadlock recovery
5. 🔍 Multiple admins reviewing simultaneously

---

**Document Version**: 1.0  
**Last Updated**: November 3, 2025

