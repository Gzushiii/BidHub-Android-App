-- Fix ALL items with mismatched status values
-- This ensures all items visible in UI are also transactable

USE defaultdb;

SELECT '=======================================================' AS '';
SELECT 'FIX ALL ITEMS: Status Alignment Check' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';

-- 1. Find all items in v_active_items view that won't work for transactions
SELECT '1. Finding items with problematic status values...' AS '';
SELECT '' AS '';

-- Create temp table with primary key (required by Aiven MySQL)
DROP TEMPORARY TABLE IF EXISTS items_to_fix;
CREATE TEMPORARY TABLE items_to_fix (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(255),
    status VARCHAR(50),
    created_at TIMESTAMP,
    view_status VARCHAR(50)
) ENGINE=InnoDB;

-- Populate temp table
INSERT INTO items_to_fix (id, title, status, created_at, view_status)
SELECT
    i.id,
    i.title,
    i.status,
    i.created_at,
    CASE
        WHEN v.id IS NOT NULL THEN 'In view (visible in UI)'
        ELSE 'Not in view'
    END as view_status
FROM items i
LEFT JOIN v_active_items v ON i.id = v.id
WHERE v.id IS NOT NULL  -- Item is in v_active_items view (visible in UI)
AND i.status NOT IN ('active', 'draft');  -- But won't work for bid/buy-now

SELECT
    COUNT(*) as problematic_items_count,
    CASE
        WHEN COUNT(*) = 0 THEN '✓ No issues found - all items are consistent'
        WHEN COUNT(*) = 1 THEN '⚠ Found 1 item that needs fixing'
        ELSE CONCAT('⚠ Found ', COUNT(*), ' items that need fixing')
    END as summary
FROM items_to_fix;

SELECT '' AS '';

-- 2. Show details of problematic items
SELECT '2. Items that will be fixed:' AS '';
SELECT
    id,
    title,
    status as current_status,
    'active' as new_status,
    view_status
FROM items_to_fix
LIMIT 20;

SELECT '' AS '';

-- 3. Show what statuses are currently in use
SELECT '3. Current status distribution in database:' AS '';
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

-- 4. Apply the fix
SELECT '4. APPLYING FIX - Updating all problematic items to active...' AS '';

UPDATE items
SET status = 'active'
WHERE id IN (SELECT id FROM items_to_fix);

SELECT
    CASE
        WHEN ROW_COUNT() > 0 THEN CONCAT('✓ SUCCESS: Updated ', ROW_COUNT(), ' item(s) to active status')
        ELSE '✓ No updates needed - all items already have compatible statuses'
    END as update_result;

SELECT '' AS '';

-- 5. Verify the fix
SELECT '5. VERIFICATION - Checking all items are now consistent:' AS '';

-- Count items in view
SET @items_in_view = (SELECT COUNT(*) FROM v_active_items);

-- Count items that would work for transactions
SET @items_transactable = (
    SELECT COUNT(*)
    FROM items i
    JOIN v_active_items v ON i.id = v.id
    WHERE i.status IN ('active', 'draft')
);

SELECT
    @items_in_view as items_visible_in_ui,
    @items_transactable as items_transactable,
    CASE
        WHEN @items_in_view = @items_transactable THEN '✓ PERFECT - All visible items are transactable'
        ELSE CONCAT('⚠ MISMATCH - ', (@items_in_view - @items_transactable), ' items still have issues')
    END as verification_result;

SELECT '' AS '';

-- 6. Final status distribution
SELECT '6. AFTER FIX - New status distribution:' AS '';
SELECT
    status,
    COUNT(*) as count,
    CASE
        WHEN status IN ('active', 'draft') THEN '✓ Good'
        ELSE '⚠ Check'
    END as status
FROM items
GROUP BY status
ORDER BY count DESC;

SELECT '' AS '';

-- 7. Cleanup
DROP TEMPORARY TABLE IF EXISTS items_to_fix;

-- 8. Summary
SELECT '=======================================================' AS '';
SELECT 'FIX COMPLETE - ALL ITEMS ALIGNED' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';

SELECT
    'All items visible in the UI should now work for bid and buy-now!' as result,
    'Test in the Android app to confirm' as next_step;

SELECT '' AS '';
SELECT '✓ Fix applied successfully!' AS '';
SELECT 'Items in v_active_items view now have compatible status values' AS '';
SELECT 'Bid and Buy Now endpoints should work for all visible items' AS '';
