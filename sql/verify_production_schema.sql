-- Verify Production Schema and Diagnose 404 Issues
-- This script checks the actual production database schema and identifies mismatches

USE defaultdb;

SELECT '=======================================================' AS '';
SELECT 'PRODUCTION SCHEMA VERIFICATION' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';

-- ==============================================
-- STEP 1: Check items table schema
-- ==============================================

SELECT '1. Items table schema (checking id column type):' AS '';
SELECT '' AS '';

SELECT
    COLUMN_NAME,
    COLUMN_TYPE,
    DATA_TYPE,
    CHARACTER_MAXIMUM_LENGTH,
    IS_NULLABLE,
    COLUMN_KEY,
    COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'defaultdb'
AND TABLE_NAME = 'items'
ORDER BY ORDINAL_POSITION;

SELECT '' AS '';

-- ==============================================
-- STEP 2: Check status column specifically
-- ==============================================

SELECT '2. Status column definition:' AS '';
SELECT '' AS '';

SELECT
    COLUMN_NAME,
    COLUMN_TYPE,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'defaultdb'
AND TABLE_NAME = 'items'
AND COLUMN_NAME = 'status';

SELECT '' AS '';

-- ==============================================
-- STEP 3: Check all unique status values in database
-- ==============================================

SELECT '3. All status values currently in items table:' AS '';
SELECT '' AS '';

SELECT
    status,
    COUNT(*) as count,
    CASE
        WHEN status IN ('active', 'draft') THEN '✓ Compatible with bid/buy-now'
        ELSE '✗ Will cause 404 errors'
    END as compatibility
FROM items
GROUP BY status
ORDER BY count DESC;

SELECT '' AS '';

-- ==============================================
-- STEP 4: Check v_active_items view definition
-- ==============================================

SELECT '4. v_active_items view definition:' AS '';
SELECT '' AS '';

SELECT VIEW_DEFINITION
FROM information_schema.VIEWS
WHERE TABLE_SCHEMA = 'defaultdb'
AND TABLE_NAME = 'v_active_items';

SELECT '' AS '';

-- ==============================================
-- STEP 5: Check specific failing item
-- ==============================================

SET @failing_item_id = '0b9cb399-7aa3-4235-a3bd-9650f114358b';

SELECT '5. Checking failing item from logcat:' AS '';
SELECT CONCAT('Item ID: ', @failing_item_id) AS '';
SELECT '' AS '';

-- Check if item exists in items table
SELECT '5a. Item in items table (direct query):' AS '';
SELECT
    id,
    title,
    status,
    seller_id,
    starting_price,
    current_price,
    buy_now_price,
    end_date,
    created_at
FROM items
WHERE id = @failing_item_id;

SELECT '' AS '';

-- Check if item exists in v_active_items view
SELECT '5b. Item in v_active_items view (what UI sees):' AS '';
SELECT
    id,
    title,
    status,
    seller_id,
    current_price,
    buy_now_price
FROM v_active_items
WHERE id = @failing_item_id;

SELECT '' AS '';

-- ==============================================
-- STEP 6: Test bid endpoint query
-- ==============================================

SELECT '6. Test bid endpoint query (should return item if working):' AS '';
SELECT '' AS '';

SELECT
    id,
    title,
    status,
    CASE
        WHEN id IS NOT NULL THEN '✓ Item found - bid should work'
        ELSE '✗ Item not found - bid will fail with 404'
    END as bid_test_result
FROM items
WHERE id = @failing_item_id
AND status IN ('active', 'draft');

SELECT '' AS '';

-- If no results, show why
SELECT '6a. Diagnosis if bid query failed:' AS '';
SELECT
    CASE
        WHEN (SELECT COUNT(*) FROM items WHERE id = @failing_item_id) = 0 THEN
            'Item does not exist in items table at all'
        WHEN (SELECT status FROM items WHERE id = @failing_item_id) NOT IN ('active', 'draft') THEN
            CONCAT('Item exists but status is: ',
                   (SELECT status FROM items WHERE id = @failing_item_id),
                   ' (not active or draft)')
        ELSE
            'Item should work - check for other issues'
    END as diagnosis;

SELECT '' AS '';

-- ==============================================
-- STEP 7: Test buy-now endpoint query
-- ==============================================

SELECT '7. Test buy-now endpoint query:' AS '';
SELECT '' AS '';

SELECT
    id,
    title,
    status,
    seller_id,
    buy_now_price,
    CASE
        WHEN status NOT IN ('active', 'draft') THEN '✗ Status check will fail'
        WHEN buy_now_price IS NULL THEN '⚠ Buy now price not set'
        ELSE '✓ Buy now should work'
    END as buy_now_test_result
FROM items
WHERE id = @failing_item_id;

SELECT '' AS '';

-- ==============================================
-- STEP 8: Find all problematic items
-- ==============================================

SELECT '8. All items visible in UI but failing in transactions:' AS '';
SELECT '' AS '';

SELECT
    i.id,
    i.title,
    i.status,
    i.created_at,
    CASE
        WHEN v.id IS NOT NULL THEN 'In view (visible)'
        ELSE 'Not in view'
    END as view_status,
    CASE
        WHEN i.status IN ('active', 'draft') THEN 'Transactable'
        ELSE 'Will fail bid/buy-now'
    END as transaction_status
FROM items i
LEFT JOIN v_active_items v ON i.id = v.id
WHERE v.id IS NOT NULL  -- Item is visible in UI
AND i.status NOT IN ('active', 'draft')  -- But won't work for transactions
LIMIT 20;

SELECT '' AS '';

-- ==============================================
-- STEP 9: Check for whitespace/casing issues
-- ==============================================

SELECT '9. Check for whitespace or casing issues in status:' AS '';
SELECT '' AS '';

SELECT
    status,
    LENGTH(status) as status_length,
    CHAR_LENGTH(status) as char_length,
    HEX(status) as hex_value,
    COUNT(*) as count
FROM items
GROUP BY status, LENGTH(status), CHAR_LENGTH(status), HEX(status);

SELECT '' AS '';

-- ==============================================
-- STEP 10: Summary and recommendations
-- ==============================================

SELECT '=======================================================' AS '';
SELECT 'SUMMARY AND RECOMMENDATIONS' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';

-- Count items in view vs transactable
SET @items_in_view = (SELECT COUNT(*) FROM v_active_items);
SET @items_transactable = (
    SELECT COUNT(*)
    FROM items i
    JOIN v_active_items v ON i.id = v.id
    WHERE i.status IN ('active', 'draft')
);

SELECT
    @items_in_view as items_visible_in_ui,
    @items_transactable as items_transactable,
    (@items_in_view - @items_transactable) as items_with_problems,
    CASE
        WHEN @items_in_view = @items_transactable THEN '✓ All visible items are transactable'
        ELSE CONCAT('⚠ ', (@items_in_view - @items_transactable), ' items need fixing')
    END as status;

SELECT '' AS '';

SELECT '✓ Schema verification complete!' AS '';
SELECT 'Review the output above to identify the exact issue.' AS '';
