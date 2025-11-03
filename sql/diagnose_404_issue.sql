-- Diagnostic: Find why item loads in UI but fails in bid/buy-now
-- Run this with your specific item ID

USE defaultdb;

SET @item_id = '0b9cb399-7aa3-4235-a3bd-9650f114358b';  -- Item from logs that's failing

SELECT '=======================================================' AS '';
SELECT 'DIAGNOSTIC: Item Status Mismatch Analysis' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';

-- 1. Check if item exists in items table
SELECT '1. Item in items table (raw):' AS '';
SELECT
    id,
    title,
    status,
    seller_id,
    starting_price,
    current_price,
    buy_now_price,
    created_at,
    CASE
        WHEN status IN ('active', 'draft') THEN 'WOULD MATCH bid/buy-now filter'
        ELSE 'WOULD NOT MATCH - this is the problem!'
    END as filter_result
FROM items
WHERE id = @item_id;

SELECT '' AS '';

-- 2. Check if v_active_items view exists
SELECT '2. Checking if v_active_items view exists:' AS '';
SELECT TABLE_NAME, TABLE_TYPE
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'defaultdb'
AND TABLE_NAME = 'v_active_items';

SELECT '' AS '';

-- 3. Check item in v_active_items view (what GET endpoint uses)
SELECT '3. Item in v_active_items view (what UI sees):' AS '';
SELECT *
FROM v_active_items
WHERE id = @item_id;

SELECT '' AS '';

-- 4. Show v_active_items view definition
SELECT '4. v_active_items view definition:' AS '';
SELECT VIEW_DEFINITION
FROM information_schema.VIEWS
WHERE TABLE_SCHEMA = 'defaultdb'
AND TABLE_NAME = 'v_active_items';

SELECT '' AS '';

-- 5. Show all possible status values in items table
SELECT '5. All status values currently in use:' AS '';
SELECT DISTINCT status, COUNT(*) as count
FROM items
GROUP BY status
ORDER BY count DESC;

SELECT '' AS '';

-- 6. Compare filters
SELECT '=======================================================' AS '';
SELECT 'COMPARISON OF FILTERS' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';

SELECT 'GET /api/items/:id uses:' AS explanation;
SELECT '  SELECT * FROM v_active_items WHERE id = ?' AS query;
SELECT '' AS '';

SELECT 'POST /api/bids/place uses:' AS explanation;
SELECT '  SELECT * FROM items WHERE id = ? AND status IN (''active'', ''draft'')' AS query;
SELECT '' AS '';

SELECT 'POST /api/items/:id/buy-now uses:' AS explanation;
SELECT '  SELECT * FROM items WHERE id = ?' AS query;
SELECT '  Then checks: if (status !== ''active'' && status !== ''draft'')' AS check_logic;
SELECT '' AS '';

-- 7. Recommendations
SELECT '=======================================================' AS '';
SELECT 'RECOMMENDATIONS' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';

SELECT
    CASE
        WHEN (SELECT COUNT(*) FROM items WHERE id = @item_id) = 0 THEN
            'ERROR: Item does not exist in items table at all!'
        WHEN (SELECT COUNT(*) FROM items WHERE id = @item_id AND status IN ('active', 'draft')) = 0 THEN
            CONCAT('FOUND ISSUE: Item exists but status is ''',
                   (SELECT status FROM items WHERE id = @item_id),
                   ''' which does not match bid/buy-now filters (active, draft)')
        WHEN (SELECT COUNT(*) FROM v_active_items WHERE id = @item_id) = 0 THEN
            'WARNING: Item matches bid/buy-now filter but not in v_active_items view (UI will show 404)'
        ELSE
            'OK: Item should work for bid and buy-now'
    END AS diagnostic_result;

SELECT '' AS '';

-- 8. Fix recommendations
SELECT 'If status is wrong, run this to fix it:' AS '';
SELECT CONCAT('UPDATE items SET status = ''active'' WHERE id = ''', @item_id, ''';') AS fix_query;
