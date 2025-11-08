# How to Fix Database Schema Compatibility Issues

## Summary

Your API endpoints expect a database schema that doesn't match `complete_database_recreation.sql`. This will cause **runtime errors** when the API runs on Render.

## Issues Found

1. ❌ Missing `item_images` table - Item creation/updates will fail
2. ❌ Wrong table name: `transactions` vs `credit_transactions` - Credit operations will fail
3. ❌ Missing view: `v_active_items` - Item listing will fail
4. ❌ Missing columns in `items` table: `uuid_id`, `starting_bid`, `reserve_price`, `end_date`
5. ⚠️ Stored procedures need updates to use correct table/column names

## Quick Fix

Run this SQL script on your Render database:

```bash
# Connect to your Render MySQL database and run:
mysql -h [YOUR_RENDER_DB_HOST] -u [YOUR_USER] -p [YOUR_DATABASE] < sql/fix_api_schema_compatibility.sql
```

Or execute the script manually in MySQL Workbench/CLI connected to your Render database.

## What the Fix Script Does

1. **Adds missing columns** to `items` table:
   - `uuid_id` (VARCHAR(36)) - for UUID-based item identification
   - `starting_bid` (DECIMAL) - API uses this instead of `starting_price`
   - `reserve_price` (DECIMAL) - for reserve price functionality
   - `end_date` (DATETIME) - for auction deadlines

2. **Creates `item_images` table**:
   - Stores images for items
   - Required for item creation/updates

3. **Creates `credit_transactions` table**:
   - Stores credit purchase and transaction history
   - API expects this table name (not `transactions`)

4. **Creates `v_active_items` view**:
   - Provides unified view of active items
   - Uses `uuid_id` as primary identifier
   - Includes all columns API expects

5. **Updates stored procedures**:
   - `PlaceBid` - Now uses `credit_transactions` table
   - `BuyNow` - Now uses `credit_transactions` table
   - Both support both `starting_bid` and `starting_price` columns

## Testing After Fix

After applying the fix, test these endpoints:

### Health Check (should always work)
```bash
curl https://your-render-app.onrender.com/api/health
```

### Item Listing (was broken, should work now)
```bash
curl https://your-render-app.onrender.com/api/items
```

### Item Creation (was broken, should work now)
```bash
curl -X POST https://your-render-app.onrender.com/api/items \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Test Item",
    "description": "Test description",
    "category_id": 1,
    "starting_price": 100.00,
    "duration_days": 7
  }'
```

### Credits Balance (was broken, should work now)
```bash
curl https://your-render-app.onrender.com/api/credits/balance \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## Files Created

1. **API_SCHEMA_COMPATIBILITY_REPORT.md** - Detailed analysis of all issues
2. **sql/fix_api_schema_compatibility.sql** - SQL script to fix all issues
3. **FIX_SCHEMA_INSTRUCTIONS.md** - This file

## Important Notes

- The fix script is **idempotent** - it's safe to run multiple times
- Existing data will be preserved
- UUIDs will be auto-generated for items that don't have one
- The script migrates data from `starting_price` to `starting_bid` automatically

## If You Encounter Errors

1. Check Render logs for specific error messages
2. Verify database connection credentials are correct
3. Ensure you have proper permissions to alter tables
4. Check if any constraints are blocking the changes

## Need Help?

Refer to `API_SCHEMA_COMPATIBILITY_REPORT.md` for detailed information about each issue and how the API expects the schema to be structured.

