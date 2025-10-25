-- Verify Render Database Schema
-- This script checks the actual database schema in the Render environment

USE defaultdb;

SELECT '=======================================================' AS '';
SELECT 'VERIFYING RENDER DATABASE SCHEMA' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';

-- Step 1: Check database connection info
SELECT 'Step 1: Database connection info...' AS '';
SELECT DATABASE() AS current_database, @@hostname AS hostname;

SELECT '' AS '';

-- Step 2: Check items table structure
SELECT 'Step 2: Items table structure...' AS '';
DESCRIBE items;

SELECT '' AS '';

-- Step 3: Show CREATE TABLE statement for items
SELECT 'Step 3: Items table CREATE statement...' AS '';
SHOW CREATE TABLE items;

SELECT '' AS '';

-- Step 4: Check if v_active_items view exists and its definition
SELECT 'Step 4: v_active_items view definition...' AS '';
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN 'View exists'
        ELSE 'View does not exist'
    END as view_status
FROM information_schema.VIEWS 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'v_active_items';

-- If view exists, show its definition
SELECT VIEW_DEFINITION
FROM information_schema.VIEWS 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'v_active_items';

SELECT '' AS '';

-- Step 5: Check all columns in items table
SELECT 'Step 5: All columns in items table...' AS '';
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    COLUMN_KEY
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'items'
ORDER BY ORDINAL_POSITION;

SELECT '' AS '';

-- Step 6: Test the specific failing item
SELECT 'Step 6: Testing specific failing item...' AS '';
SELECT 
    id,
    uuid_id,
    title,
    status,
    seller_id,
    starting_bid,
    current_bid,
    buy_now_price,
    created_at
FROM items 
WHERE uuid_id = 'c1a4382f-5293-4903-b1b1-78576c5c9549';

SELECT '' AS '';

-- Step 7: Test v_active_items view with the specific item
SELECT 'Step 7: Testing v_active_items view with specific item...' AS '';
SELECT 
    id,
    title,
    status,
    seller_id
FROM v_active_items 
WHERE id = 'c1a4382f-5293-4903-b1b1-78576c5c9549';

SELECT '' AS '';

-- Step 8: Check recent items to see what's actually in the database
SELECT 'Step 8: Recent items in database...' AS '';
SELECT 
    id,
    uuid_id,
    title,
    status,
    seller_id,
    created_at
FROM items 
ORDER BY created_at DESC 
LIMIT 5;

SELECT '' AS '';

-- Step 9: Check items by status
SELECT 'Step 9: Items by status...' AS '';
SELECT 
    status,
    COUNT(*) as count
FROM items 
GROUP BY status;

SELECT '' AS '';

-- Step 10: Test direct item lookup queries (what the code should use)
SELECT 'Step 10: Testing direct item lookup queries...' AS '';

-- Test uuid_id lookup
SELECT 'UUID lookup test:' AS '';
SELECT COUNT(*) as uuid_lookup_count
FROM items 
WHERE uuid_id = 'c1a4382f-5293-4903-b1b1-78576c5c9549';

-- Test uuid_id + status lookup
SELECT 'UUID + status lookup test:' AS '';
SELECT COUNT(*) as uuid_status_lookup_count
FROM items 
WHERE uuid_id = 'c1a4382f-5293-4903-b1b1-78576c5c9549' 
  AND status = 'active';

SELECT '' AS '';

SELECT '=======================================================' AS '';
SELECT 'RENDER DB SCHEMA VERIFICATION COMPLETE' AS '';
SELECT '=======================================================' AS '';
