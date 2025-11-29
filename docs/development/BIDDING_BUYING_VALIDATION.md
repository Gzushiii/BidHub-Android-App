# Bidding & Buy-Now Validation Report

**Date:** November 3, 2025  
**Scope:** Android client ↔ Node.js API alignment for bidding (`/api/bids/place`) and buy-now (`/api/items/:id/buy-now`) flows.

---

## Architecture Overview

| Layer      | Component(s)                                          | Notes |
|------------|-------------------------------------------------------|-------|
| Android    | `BidApiClient`, `ItemDetailActivity`, `BiddingEngine` | Uses HTTPS JSON requests, handles cold start timeouts, exposes detailed logs. |
| Backend    | `routes/bids.js`, `routes/items.js`, `utils/itemHelpers.js`, MySQL stored procedures `PlaceBid`, `BuyNow` | Shared lookup helper resolves both numeric IDs and UUIDs to avoid legacy issues. |
| Database   | `items`, `bids`, `credit_transactions`, `users`, stored procedures (`PlaceBid`, `BuyNow`) | Requires consistent INT primary keys + UUID shadow column (`uuid_id`). |

---

## API Contract Alignment

### Bid Placement (`POST /api/bids/place`)

| Field        | Android Client                           | Backend Expectation | Status |
|--------------|------------------------------------------|---------------------|--------|
| `item_id`    | `String` UUID passed by `BidApiClient`   | Resolved via `getItemWithErrorInfo` → INT `id` | ✅ |
| `amount`     | `double`                                 | Numeric > current bid & starting bid | ✅ |
| Auth header  | `Bearer <JWT>`                           | Required | ✅ |
| Response     | `{ message, bid_amount }` on success or `{ error, details }` on failure | Consumption via `ApiResponse` | ✅ |

### Buy Now (`POST /api/items/:id/buy-now`)

| Field        | Android Client                               | Backend Expectation | Status |
|--------------|----------------------------------------------|---------------------|--------|
| Path `:id`   | UUID string (`currentItem.getItemId()`)       | Resolved to INT `id` | ✅ |
| Body         | `{ amount: buyNowPrice }`                    | Optional, defaults to stored `buy_now_price` | ✅ |
| Auth header  | `Bearer <JWT>`                               | Required | ✅ |
| Response     | `{ message, item_id, amount }` or structured error | UI handles success/error states | ✅ |

Both client and server now use defensive lookups that support both numeric IDs and `uuid_id`. The helpers (`getItemWithErrorInfo`, `validateItemForBidding`, `validateItemForBuyNow`) centralise validation, ensuring consistent rules.

---

## Database Dependencies

| Requirement                               | Status | Notes |
|-------------------------------------------|--------|-------|
| `items` table has `uuid_id`, `starting_bid`, `reserve_price`, `end_date`, `current_bid`, `buy_now_price` | ✅ `sql/fix_api_schema_compatibility.sql` |
| `bids` table uses INT `item_id` + optional `item_uuid_id` for auditing | ✅ |
| Stored procedures `PlaceBid`, `BuyNow` reference `credit_transactions.type` column (not `transaction_type`) | ✅ Fixed in this iteration |
| `credit_transactions` table includes enum type `('bid','buy_now',...)` | ✅ |
| `users.credits` column kept in sync through transactions | ✅ |

> **Action Applied:** Corrected `credit_transactions` column usage in stored procedures to reference `type`. This removes the last schema mismatch that prevented bid/buy logging.

---

## Manual Verification Checklist

### 1. Place Bid Flow
1. Ensure test account has sufficient credits (`GET /api/credits/balance`).
2. Select active item UUID from `/api/items`.
3. Submit POST payload:
   ```bash
   curl -X POST https://bidhub-android-app.onrender.com/api/bids/place \
     -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"item_id":"<uuid>", "amount": 10.0}'
   ```
4. Expect `200 OK` with confirmation JSON.
5. Verify:
   - `bids` table entry inserted with `status='winning'`.
   - `users.credits` deducted.
   - `credit_transactions.type='bid'` row recorded.

### 2. Buy Now Flow
1. Identify item with `buy_now_price` from `/api/items`.
2. Confirm user credits >= price.
3. Execute:
   ```bash
   curl -X POST https://bidhub-android-app.onrender.com/api/items/<uuid>/buy-now \
     -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"amount": 100.0}'
   ```
4. Expected outcomes:
   - Item `status` updates to `sold`.
   - All active bids for item are cancelled.
   - `credit_transactions` adds buyer and seller entries (`type='buy_now'`).
   - Ledger entries recorded when manual top-up schema is installed.

### 3. Android UX Validation
1. Trigger bid from UI (`ItemDetailActivity`).
2. Observe loading state, toasts, error handling for:
   - Network timeout (60s).
   - Outbid scenario (toast, refresh suggestions).
3. Trigger buy now:
   - Verify balance check (`fetchFreshBalanceFromBackend`).
   - Confirm success path navigates to `PaymentConfirmationActivity`.
   - Confirm insufficient credits redirects to `CreditsActivity`.

---

## Outstanding Risks & Recommendations

1. **Data Sync:** Android still uses cached SQLite credits. Implement scheduled refresh or real-time updates to avoid stale balances.
2. **Concurrency:** Consider optimistic locking on `bids` table to avoid race conditions in high-traffic scenarios.
3. **Automated Tests:** Extend `tests/apiSmokeTest.js` (with `BIDHUB_ALLOW_MUTATIONS=true`) against staging DB to continuously validate bid/buy flows.
4. **Notifications:** Add push/email confirmation after successful buy-now to improve user trust.

---

**Conclusion:** Bidding and buy-now implementations are aligned across client and backend layers once the schema fixes are applied. The new smoke tests and schema patches ensure the flows operate as designed. Remaining work centres on automated coverage and UX polish.***

