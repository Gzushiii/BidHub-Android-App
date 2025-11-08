# Database Mismatch Remediation

**Date:** November 3, 2025  
**Focus:** Align MySQL schema with expectations from BidHub Node.js API and new manual top-up workflow.

---

## Changes Applied

1. **Stored Procedure Alignment**
   - Updated `sql/fix_api_schema_compatibility.sql` so that `PlaceBid` and `BuyNow` procedures write to `credit_transactions.type` (column used by the API) instead of the non-existent `transaction_type`.  
   - Prevents runtime SQL errors during bid placement and buy-now purchase logging.

2. **Manual Top-Up Schema Installer**
   - Added `sql/add_manual_topup_tables.sql` to provision:
     - `topups` table with full status workflow.
     - `credit_ledger` table for immutable audit trails.
     - Supporting views (`v_pending_topups`, `v_user_topup_stats`, `v_credit_ledger_summary`).
     - Stored procedures `sp_confirm_topup` and `sp_reject_topup`.
   - Script is idempotent and safe to run on production (`defaultdb`).

3. **Dependency Synchronisation**
   - Smoke tests verify `/api/credits/balance` and other endpoints against live schema to confirm fixes.

---

## Deployment Instructions

1. **Apply schema compatibility patch (if not already executed):**
   ```bash
   mysql -h <host> -P <port> -u <user> -p defaultdb < sql/fix_api_schema_compatibility.sql
   ```

2. **Install manual top-up objects:**
   ```bash
   mysql -h <host> -P <port> -u <user> -p defaultdb < sql/add_manual_topup_tables.sql
   ```

3. **(Optional) Verify objects:**
   ```sql
   SHOW TABLES LIKE 'topups';
   SHOW TABLES LIKE 'credit_ledger';
   SHOW PROCEDURE STATUS WHERE Db = 'defaultdb' AND Name LIKE 'sp_%topup%';
   SELECT * FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='defaultdb' AND TABLE_NAME='credit_transactions' AND COLUMN_NAME='type';
   ```

---

## Validation Checklist

- [x] `credit_transactions` inserts succeed during bid/buy operations.
- [x] Manual top-up endpoints can persist data without schema errors.
- [x] Views and procedures installed without conflicts.
- [x] API smoke tests (`npm run smoke`) complete with HTTP 200 responses.

---

**Outcome:** Remaining schema mismatches are resolved. The backend and database now expose all structures required by bidding, buy-now, and manual top-up flows.***

