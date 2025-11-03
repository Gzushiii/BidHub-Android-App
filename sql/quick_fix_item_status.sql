-- Quick Fix: Update item status to make it work with bid/buy-now
-- Based on logcat showing item: 0b9cb399-7aa3-4235-a3bd-9650f114358b

USE defaultdb;

SET @item_id = '0b9cb399-7aa3-4235-a3bd-9650f114358b';

SELECT '=======================================================' AS '';
SELECT 'QUICK FIX: Update Item Status to Active' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';

-- 1. Show current state
SELECT '1. BEFORE FIX - Current item status:' AS '';
SELECT
    id,
    title,
    status,
    created_at,
    CASE
        WHEN status IN ('active', 'draft') THEN 'OK for bid/buy-now'
        ELSE 'PROBLEM - won''t work!'
    END as transaction_ready
FROM items
WHERE id = @item_id;

SELECT '' AS '';

-- 2. Check if item exists in v_active_items view
SELECT '2. BEFORE FIX - Item in v_active_items view:' AS '';
SELECT
    CASE
        WHEN COUNT(*) > 0 THEN 'YES - Item appears in UI'
        ELSE 'NO - Item does NOT appear in UI'
    END as view_status
FROM v_active_items
WHERE id = @item_id;

SELECT '' AS '';

-- 3. Apply the fix
SELECT '3. APPLYING FIX - Updating status to active...' AS '';

UPDATE items
SET status = 'active'
WHERE id = @item_id
AND status != 'active';

SELECT
    CASE
        WHEN ROW_COUNT() > 0 THEN CONCAT('✓ SUCCESS: Updated ', ROW_COUNT(), ' item(s)')
        ELSE '⚠ No update needed - item already active'
    END as update_result;

SELECT '' AS '';

-- 4. Verify the fix
SELECT '4. AFTER FIX - Verify item status:' AS '';
SELECT
    id,
    title,
    status,
    created_at,
    CASE
        WHEN status = 'active' THEN '✓ FIXED - Ready for transactions'
        WHEN status = 'draft' THEN '⚠ Still draft - needs to be published'
        ELSE '✗ Still broken - manual intervention needed'
    END as transaction_ready
FROM items
WHERE id = @item_id;

SELECT '' AS '';

-- 5. Test queries that bid/buy-now endpoints use
SELECT '5. VERIFICATION - Testing bid endpoint query:' AS '';
SELECT
    CASE
        WHEN COUNT(*) > 0 THEN '✓ PASS - Item will be found by bid endpoint'
        ELSE '✗ FAIL - Item still won''t work for bidding'
    END as bid_query_test
FROM items
WHERE id = @item_id
AND status IN ('active', 'draft');

SELECT '' AS '';

SELECT '6. VERIFICATION - Testing buy-now endpoint query:' AS '';
SELECT
    CASE
        WHEN COUNT(*) > 0 AND status = 'active' THEN '✓ PASS - Item will work for buy-now'
        WHEN COUNT(*) > 0 AND status = 'draft' THEN '⚠ PARTIAL - Item found but still draft'
        ELSE '✗ FAIL - Item still won''t work for buy-now'
    END as buy_now_query_test
FROM items
WHERE id = @item_id;

SELECT '' AS '';

-- 7. Summary
SELECT '=======================================================' AS '';
SELECT 'FIX COMPLETE - SUMMARY' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';

SELECT
    CONCAT('Item ID: ', id) as detail,
    CONCAT('Status: ', status) as current_status,
    CASE
        WHEN status = 'active' THEN '✓ READY - Bid and Buy Now should work now'
        WHEN status = 'draft' THEN '⚠ DRAFT - Needs to be published first'
        ELSE '✗ CHECK LOGS - Unexpected status'
    END as action_needed
FROM items
WHERE id = @item_id;

SELECT '' AS '';
SELECT '✓ You can now test bid and buy-now in the Android app!' AS next_step;
