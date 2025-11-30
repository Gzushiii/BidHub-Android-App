# Credit Balance Update Documentation

## Overview

This document describes how credit balance updates are handled across the BidHub Android app after successful top-up transactions. The system ensures that the user's credit balance is immediately updated and synchronized across all UI components.

## Architecture

### Data Flow

1. **User submits top-up reference** → Frontend calls `POST /api/topups/:id/submit`
2. **Backend processes top-up** → Updates database, returns `new_balance` in response
3. **Frontend receives response** → Immediately updates SharedPreferences and UI
4. **Background refresh** → Optionally confirms balance from backend API
5. **Other screens refresh** → ProfileFragment, CreditsActivity refresh on resume

### Key Components

#### 1. Backend API (`bidhub-backend/src/routes/topups.js`)

**Endpoint:** `POST /api/topups/:id/submit`

**Response Structure:**
```json
{
  "success": true,
  "status": "CONFIRMED",
  "new_balance": 1500.00,
  "message": "Top-up processed successfully. Credits have been added to your account."
}
```

**Key Fields:**
- `new_balance` (number): The updated credit balance after top-up processing
- `status` (string): Status of the top-up (CONFIRMED after successful processing)
- `success` (boolean): Indicates successful processing

**Processing Flow:**
1. Validates 13-digit reference number
2. Verifies top-up ownership and status
3. Updates user credits in database (atomic transaction)
4. Creates credit transaction record
5. Creates ledger entry
6. Returns updated balance immediately

#### 2. CreditBalanceManager (`bidhub/app/src/main/java/com/cc106/bidhub/utils/CreditBalanceManager.java`)

**Purpose:** Centralized utility for managing credit balance updates across the app.

**Key Methods:**

- `refreshBalance(Context, BalanceUpdateCallback)`: Fetches balance from `/api/credits/balance` and updates SharedPreferences
- `getCurrentBalance(Context)`: Returns cached balance from SharedPreferences
- `updateBalanceImmediately(Context, double)`: Updates SharedPreferences directly (for use with API responses)

**Balance Endpoint:** `GET /api/credits/balance`

**Response Structure:**
```json
{
  "credits": 1500.00,
  "recent_transactions": [...]
}
```

**Note:** CreditBalanceManager handles both `credits` and `balance` field names for compatibility.

#### 3. SharedPreferencesHelper (`bidhub/app/src/main/java/com/cc106/bidhub/utils/SharedPreferencesHelper.java`)

**Purpose:** Manages local storage of user data including credit balance.

**Key Methods:**
- `setCredits(double)`: Saves balance to SharedPreferences
- `getCredits()`: Retrieves balance from SharedPreferences

**Storage Key:** `credits` (stored as float)

## Implementation Details

### CreditsFragment (`bidhub/app/src/main/java/com/cc106/bidhub/fragments/CreditsFragment.java`)

**Top-Up Success Flow:**

1. **Parse API Response:**
   ```java
   JSONObject responseJson = new JSONObject(responseBody);
   double newBalance = responseJson.optDouble("new_balance", 0.0);
   ```

2. **Immediately Update SharedPreferences:**
   ```java
   prefsHelper.setCredits(newBalance);
   ```

3. **Update UI Immediately:**
   ```java
   balanceAmount.setText(creditManager.formatCurrency(newBalance));
   ```

4. **Background Refresh (Confirmation):**
   ```java
   CreditBalanceManager.refreshBalance(context, callback);
   ```

**Why This Approach:**
- Immediate UI update provides instant feedback to user
- SharedPreferences update ensures other screens see new balance
- Background refresh confirms consistency with backend

### ProfileFragment (`bidhub/app/src/main/java/com/cc106/bidhub/fragments/ProfileFragment.java`)

**Refresh Strategy:**

1. **On Resume:** Automatically refreshes balance from backend
2. **Fallback:** Shows cached value from SharedPreferences if refresh fails
3. **UI Update:** Updates `textViewCredits` with formatted balance

**Implementation:**
```java
@Override
public void onResume() {
    super.onResume();
    refreshCreditsFromBackend();
}
```

### CreditsActivity (`bidhub/app/src/main/java/com/cc106/bidhub/CreditsActivity.java`)

**Refresh Strategy:**

1. **On Resume:** Refreshes balance from backend
2. **Display:** Uses SharedPreferences as primary source, falls back to local manager
3. **Animation:** Provides visual feedback when balance updates

### ItemDetailActivity (`bidhub/app/src/main/java/com/cc106/bidhub/ItemDetailActivity.java`)

**Balance Refresh Points:**

1. **After Bid Placement:** Refreshes balance to reflect deduction
2. **After Buy Now:** Refreshes balance to reflect purchase

**Implementation:**
```java
CreditBalanceManager.refreshBalance(this, callback);
```

## Synchronization Strategy

### Immediate Updates

When a top-up is successfully processed:

1. **API Response** contains `new_balance` → Frontend updates SharedPreferences immediately
2. **UI Updates** happen synchronously on main thread
3. **No waiting** for additional API calls

### Background Confirmation

After immediate update:

1. **Background thread** calls `/api/credits/balance`
2. **SharedPreferences** updated again with confirmed value
3. **UI refreshed** if value differs (edge case handling)

### Cross-Screen Synchronization

All screens that display balance:

- **Read from SharedPreferences** as primary source
- **Refresh on resume** to get latest value
- **Handle errors gracefully** by showing cached value

## Error Handling

### Network Failures

- **Top-up submission fails:** Error shown, balance not updated
- **Balance refresh fails:** Silent fail, cached value displayed
- **Partial updates:** SharedPreferences always has last known good value

### Data Consistency

- **SharedPreferences** is single source of truth for UI
- **Backend** is authoritative source for transactions
- **Refresh mechanism** ensures eventual consistency

## Testing Requirements

### Test Scenarios

1. **Successful Top-Up:**
   - Submit valid 13-digit reference
   - Verify balance updates immediately in CreditsFragment
   - Navigate to ProfileFragment → Verify balance matches
   - Close and reopen app → Verify balance persists

2. **Multiple Top-Ups:**
   - Perform consecutive top-ups
   - Verify each update reflects correctly
   - Verify no duplicate or missing credits

3. **Tab Switching:**
   - Complete top-up in CreditsFragment
   - Immediately switch to ProfileFragment
   - Verify balance is updated without delay

4. **Network Interruption:**
   - Start top-up submission
   - Simulate network failure
   - Verify error handling and no partial updates

5. **App Lifecycle:**
   - Complete top-up
   - Background app
   - Return to app
   - Verify balance refresh on resume

### Validation Points

- ✅ Balance updates immediately after top-up success
- ✅ SharedPreferences contains correct value
- ✅ All screens show consistent balance
- ✅ Balance persists across app restarts
- ✅ Network failures handled gracefully

## API Response Fields

### Top-Up Submit Response

| Field | Type | Description |
|-------|------|-------------|
| `success` | boolean | Indicates successful processing |
| `status` | string | Top-up status (CONFIRMED) |
| `new_balance` | number | **Updated credit balance** |
| `message` | string | Success message |

### Credits Balance Response

| Field | Type | Description |
|-------|------|-------------|
| `credits` | number | Current credit balance |
| `recent_transactions` | array | Recent transaction history |

**Note:** CreditBalanceManager accepts both `credits` and `balance` field names.

## Modified Files

### Frontend (Java)

1. `bidhub/app/src/main/java/com/cc106/bidhub/fragments/CreditsFragment.java`
   - Updated `onSuccess` callback to immediately update SharedPreferences
   - Added logging for balance updates

2. `bidhub/app/src/main/java/com/cc106/bidhub/fragments/ProfileFragment.java`
   - Enhanced `refreshCreditsFromBackend` to show cached value first
   - Improved error handling

3. `bidhub/app/src/main/java/com/cc106/bidhub/CreditsActivity.java`
   - Added `onResume` to refresh balance
   - Updated `updateBalanceDisplay` to use SharedPreferences
   - Added `refreshBalanceFromBackend` method

4. `bidhub/app/src/main/java/com/cc106/bidhub/utils/CreditBalanceManager.java`
   - Added `updateBalanceImmediately` method for direct SharedPreferences updates

### Backend (JavaScript)

1. `bidhub-backend/src/routes/topups.js`
   - Already returns `new_balance` in submit response (verified)
   - No changes needed

2. `bidhub-backend/src/routes/credits.js`
   - Returns `credits` field in balance endpoint (verified)
   - No changes needed

## Troubleshooting

### Balance Not Updating

**Symptoms:** Balance doesn't change after top-up

**Check:**
1. Verify API response contains `new_balance` field
2. Check SharedPreferences contains updated value
3. Verify UI refresh is called on main thread
4. Check logs for error messages

**Fix:**
- Ensure `prefsHelper.setCredits(newBalance)` is called
- Verify callback runs on main thread
- Check network connectivity for backend refresh

### Inconsistent Balance Across Screens

**Symptoms:** Different screens show different balances

**Check:**
1. Verify all screens read from SharedPreferences
2. Check refresh mechanisms are called on resume
3. Verify no stale cached values

**Fix:**
- Ensure all screens use `CreditBalanceManager` or `SharedPreferencesHelper`
- Add refresh calls in `onResume` methods
- Clear app data if corruption suspected

### Balance Resets After App Restart

**Symptoms:** Balance returns to old value after restart

**Check:**
1. Verify SharedPreferences is being saved (not just in memory)
2. Check `setCredits` uses `apply()` or `commit()`
3. Verify balance refresh on app start

**Fix:**
- Ensure `SharedPreferencesHelper.setCredits` uses `apply()` (already implemented)
- Add balance refresh in app initialization
- Check for SharedPreferences clearing on logout

## Future Enhancements

1. **Real-time Updates:** Implement WebSocket or push notifications for instant balance updates
2. **Offline Support:** Queue balance updates when offline, sync when online
3. **Transaction History:** Show detailed transaction log in UI
4. **Balance Notifications:** Notify user when balance changes significantly
5. **Multi-device Sync:** Ensure balance consistency across devices

## Summary

The credit balance update system ensures:

✅ **Immediate Updates:** Balance updates instantly after top-up success  
✅ **Consistent Display:** All screens show the same balance  
✅ **Reliable Storage:** SharedPreferences persists across app restarts  
✅ **Error Resilience:** Graceful handling of network failures  
✅ **Background Sync:** Automatic refresh ensures data accuracy  

The implementation follows Android best practices for data synchronization and provides a smooth user experience with instant feedback and reliable data consistency.

