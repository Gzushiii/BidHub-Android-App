-- Check Existing Items in Database
-- This script will show us what items actually exist and their status

USE defaultdb;

SELECT '=======================================================' AS '';
SELECT 'CHECKING EXISTING ITEMS IN DATABASE' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';

-- Step 1: Count total items
SELECT 'Step 1: Total items count...' AS '';
SELECT COUNT(*) as total_items FROM items;

SELECT '' AS '';

-- Step 2: Items by status
SELECT 'Step 2: Items by status...' AS '';
SELECT 
    status,
    COUNT(*) as count
FROM items 
GROUP BY status;

SELECT '' AS '';

-- Step 3: Recent items (last 10)
SELECT 'Step 3: Recent items (last 10)...' AS '';
SELECT 
    id,
    uuid_id,
    title,
    status,
    seller_id,
    created_at
FROM items 
ORDER BY created_at DESC 
LIMIT 10;

SELECT '' AS '';

-- Step 4: Check v_active_items view
SELECT 'Step 4: Items in v_active_items view...' AS '';
SELECT COUNT(*) as active_items_count FROM v_active_items;

SELECT 'Sample active items:' AS '';
SELECT 
    id,
    title,
    status,
    seller_id
FROM v_active_items 
LIMIT 5;

SELECT '' AS '';

-- Step 5: Check if items have proper UUIDs
SELECT 'Step 5: Items with UUIDs...' AS '';
SELECT 
    COUNT(*) as total_items,
    COUNT(uuid_id) as items_with_uuid,
    COUNT(*) - COUNT(uuid_id) as items_without_uuid
FROM items;

SELECT '' AS '';

-- Step 6: Check items that should be visible
SELECT 'Step 6: Items that should be visible (active status)...' AS '';
SELECT 
    id,
    uuid_id,
    title,
    status,
    seller_id,
    starting_bid,
    current_bid,
    created_at
FROM items 
WHERE status = 'active' 
ORDER BY created_at DESC 
LIMIT 5;

SELECT '' AS '';

-- Step 7: Check if there are any items with different statuses
SELECT 'Step 7: All unique statuses in database...' AS '';
SELECT DISTINCT status FROM items;

SELECT '' AS '';

SELECT '=======================================================' AS '';
SELECT 'ITEMS CHECK COMPLETE' AS '';
SELECT '=======================================================' AS '';
