# 🚨 URGENT: Fix PlaceBid GROUP BY Error

## The Problem
You're getting this error when placing bids:
```
"In aggregated query without GROUP BY, expression #2 of SELECT list contains 
nonaggregated column 'defaultdb.items.seller_id'..."
```

## Quick Fix (Choose One Method)

### Method 1: Run Node.js Script (Easiest) ⭐

```bash
cd bidhub-backend
npm run fix:placebid
```

This will automatically fix the stored procedure in your database.

### Method 2: Manual SQL Fix

1. Go to Render Dashboard → Your Database
2. Open SQL Editor
3. Copy contents of `sql/fix_placebid_groupby_error_simple.sql`
4. Paste and execute

### Method 3: Auto-Fix on Server Start

Add this to your Render environment variables:
```
AUTO_FIX_PLACEBID=true
```

Then restart your server. It will automatically apply the fix on startup.

## What Gets Fixed

The stored procedure `PlaceBid` in your database has this buggy code:
```sql
SELECT COUNT(*), seller_id, starting_price FROM items ...
```

This is fixed to:
```sql
SELECT COUNT(*) INTO v_item_exists FROM items ...;
IF v_item_exists > 0 THEN
    SELECT seller_id, starting_price INTO ... FROM items ...;
END IF;
```

## Verification

After applying the fix, test by placing a bid. The error should be gone!

## Need Help?

If the script doesn't work:
1. Check your database connection settings
2. Make sure you have permissions to create/drop procedures
3. Try the manual SQL method instead

