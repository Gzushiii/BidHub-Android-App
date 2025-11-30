# Top-Up Automation Documentation

## Overview

The credit top-up flow has been updated to automatically process transactions upon submission of a valid 13-digit GCash reference number. The manual admin confirmation step has been removed, and credits are now added to user accounts immediately after validation.

## Changes Summary

### 1. Backend Changes (`bidhub-backend/src/routes/topups.js`)

#### Submit Endpoint (`POST /api/topups/:id/submit`)

**Previous Behavior:**
- Accepted reference numbers with minimum 4 characters
- Set status to `UNDER_REVIEW`
- Required admin confirmation via `/admin/:id/confirm` endpoint

**New Behavior:**
- Validates reference number must be exactly 13 digits (numeric only)
- Automatically processes the top-up in a single transaction:
  - Updates user credits balance
  - Creates credit transaction record
  - Creates ledger entry
  - Sets status to `CONFIRMED`
- Returns new balance immediately in response

**Key Changes:**
- Added `validateReferenceNumber()` helper function for 13-digit validation
- Integrated credit processing logic directly into submit endpoint
- Uses database transaction to ensure atomicity
- Returns `new_balance` in success response

**Validation Rules:**
```javascript
function validateReferenceNumber(ref) {
  if (!ref || typeof ref !== 'string') {
    return false;
  }
  const trimmed = ref.trim();
  // Must be exactly 13 digits, numeric only
  return /^\d{13}$/.test(trimmed);
}
```

**Response Format:**
```json
{
  "success": true,
  "status": "CONFIRMED",
  "new_balance": 1500.00,
  "message": "Top-up processed successfully. Credits have been added to your account."
}
```

### 2. Frontend Changes (`bidhub/app/src/main/java/com/cc106/bidhub/fragments/CreditsFragment.java`)

#### Reference Number Validation

**Client-Side Validation:**
- Input field restricted to numeric input only
- Maximum length: 13 characters
- Real-time validation as user types
- Submit button disabled until valid 13-digit number entered

**Validation Method:**
```java
private boolean isValidReferenceNumber(String ref) {
    if (ref == null || ref.trim().isEmpty()) {
        return false;
    }
    String trimmed = ref.trim();
    return trimmed.length() == 13 && trimmed.matches("\\d+");
}
```

#### UI Updates

**Dialog Changes:**
- Input type changed to `number` (numeric keyboard)
- Added `maxLength="13"` attribute
- Updated helper text: "Enter the 13-digit reference number from your GCash transaction"
- Updated button text: "Submit & Process" (was "Submit Reference")

**Success Handling:**
- Updated `TopupSubmitCallback` interface to include `newBalance` parameter
- Balance updated immediately upon successful processing
- Success message: "Top-up processed successfully! Credits have been added to your account."
- Automatic balance refresh from backend for consistency

#### Error Messages

**New String Resources:**
- `reference_must_be_13_digits`: "Reference number must be exactly 13 digits"
- `reference_numbers_only`: "Reference number must contain only numbers"
- `topup_processed_successfully`: "Top-up processed successfully! Credits have been added to your account."
- Updated `invalid_reference`: "Please enter a valid 13-digit reference number"

### 3. Database Schema

**No Schema Changes Required:**
- Existing `topups` table structure supports the new flow
- Status field still uses `ENUM('PENDING', 'UNDER_REVIEW', 'CONFIRMED', 'REJECTED')`
- `confirmed_at` and `confirmed_by` fields are populated automatically
- `user_receipt_ref` stores the 13-digit reference number

**Status Flow:**
1. `PENDING` - Top-up initiated, waiting for reference submission
2. `CONFIRMED` - Reference submitted and validated, credits added (automatic)

**Note:** `UNDER_REVIEW` status is no longer used in the automated flow but remains in the schema for backward compatibility.

### 4. Admin Endpoints

**Status: Deprecated (Not Removed)**
- `/api/topups/admin/:id/confirm` - No longer needed for automated flow
- `/api/topups/admin/:id/reject` - Retained for manual intervention if needed
- `/api/topups/admin/pending` - May still be useful for monitoring

**Recommendation:** These endpoints can be removed in a future cleanup if manual admin intervention is not required.

## User Flow

### Complete Top-Up Process

1. **User selects credit package**
   - User navigates to Credits tab
   - Selects desired credit package
   - Clicks "Buy" button

2. **Payment initiation**
   - System creates top-up record with status `PENDING`
   - Displays GCash payment number
   - Shows generated reference code
   - Opens payment dialog

3. **User sends payment via GCash**
   - User sends payment to displayed GCash number
   - User receives 13-digit reference number from GCash

4. **Reference submission**
   - User enters 13-digit reference number
   - System validates format (exactly 13 digits, numeric only)
   - Submit button enabled only when valid

5. **Automatic processing**
   - User clicks "Submit & Process"
   - System validates reference number server-side
   - If valid:
     - Credits added to user account
     - Status updated to `CONFIRMED`
     - Transaction and ledger records created
     - New balance returned to frontend
   - If invalid:
     - Error message displayed
     - User can correct and resubmit

6. **Confirmation**
   - Success message displayed
   - Balance updated immediately
   - Dialog closed
   - Credits available for use

## Error Handling

### Client-Side Validation Errors

- **Less than 13 digits**: "Reference number must be exactly 13 digits"
- **Non-numeric characters**: "Reference number must contain only numbers"
- **Empty input**: Submit button disabled

### Server-Side Validation Errors

- **Invalid format**: HTTP 400 - "Reference number must contain exactly 13 digits (numbers only)"
- **Top-up not found**: HTTP 404 - "Invalid top-up ID or you do not own this top-up"
- **Invalid status**: HTTP 400 - "Cannot submit receipt for top-up in {status} status"
- **Database errors**: HTTP 500 - Detailed error message (development) or generic message (production)

## Testing Checklist

### Valid Scenarios
- [ ] Submit valid 13-digit reference number
- [ ] Verify credits added immediately
- [ ] Verify balance updated in UI
- [ ] Verify transaction record created
- [ ] Verify ledger entry created
- [ ] Verify status set to CONFIRMED

### Invalid Scenarios
- [ ] Submit reference with less than 13 digits
- [ ] Submit reference with more than 13 digits
- [ ] Submit reference with non-numeric characters
- [ ] Submit reference for non-existent top-up
- [ ] Submit reference for top-up in wrong status
- [ ] Submit reference for top-up owned by different user

### Edge Cases
- [ ] Submit empty reference
- [ ] Submit reference with spaces
- [ ] Submit reference with special characters
- [ ] Network timeout during submission
- [ ] Server error during processing
- [ ] Concurrent submissions (deduplication)

## Security Considerations

1. **Server-Side Validation**: Client-side validation is for UX only; server enforces strict 13-digit requirement
2. **Transaction Atomicity**: All database operations wrapped in transaction to prevent partial updates
3. **Ownership Verification**: Users can only submit references for their own top-ups
4. **Status Validation**: Only `PENDING` top-ups can be processed
5. **Input Sanitization**: Reference numbers are trimmed and validated before processing

## Migration Notes

### For Existing Pending Top-Ups

- Top-ups in `PENDING` status can be processed using the new automated flow
- Top-ups in `UNDER_REVIEW` status require manual admin confirmation (legacy)
- Consider migrating `UNDER_REVIEW` top-ups to `PENDING` if needed

### Backward Compatibility

- Admin endpoints remain functional for manual intervention
- Database schema unchanged
- API response format enhanced but backward compatible

## Future Enhancements

1. **Reference Number Verification**: Integrate with GCash API to verify reference numbers
2. **Duplicate Detection**: Check if reference number has been used before
3. **Rate Limiting**: Prevent abuse with submission rate limits
4. **Auto-Retry**: Automatic retry for transient failures
5. **Webhook Notifications**: Notify users via push notification upon successful processing

## API Reference

### POST /api/topups/:id/submit

**Request:**
```json
{
  "user_receipt_ref": "1234567890123"
}
```

**Success Response (200):**
```json
{
  "success": true,
  "status": "CONFIRMED",
  "new_balance": 1500.00,
  "message": "Top-up processed successfully. Credits have been added to your account."
}
```

**Error Response (400):**
```json
{
  "error": "Invalid receipt reference",
  "details": "Reference number must contain exactly 13 digits (numbers only)"
}
```

**Error Response (404):**
```json
{
  "error": "Top-up not found",
  "details": "Invalid top-up ID or you do not own this top-up"
}
```

## Files Modified

### Backend
- `bidhub-backend/src/routes/topups.js` - Submit endpoint updated for automatic processing

### Frontend
- `bidhub/app/src/main/java/com/cc106/bidhub/fragments/CreditsFragment.java` - Validation and UI updates
- `bidhub/app/src/main/res/layout/dialog_gcash_payment.xml` - Input field constraints
- `bidhub/app/src/main/res/values/strings.xml` - Updated messages

### Documentation
- `TOPUP_AUTOMATION_DOCUMENTATION.md` - This file

## Support

For issues or questions regarding the automated top-up flow, please refer to:
- Backend logs: Check server console for processing details
- Frontend logs: Check Android Logcat for validation and network errors
- Database: Query `topups` table for transaction history

