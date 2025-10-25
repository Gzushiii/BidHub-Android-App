-- Inspect Actual Database Schema
-- This script identifies the real columns in the items table and related tables

USE defaultdb;

SELECT '=======================================================' AS '';
SELECT 'INSPECTING ACTUAL DATABASE SCHEMA' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';

-- Step 1: Check items table structure
SELECT 'Step 1: Items table structure...' AS '';
DESCRIBE items;

SELECT '' AS '';

-- Step 2: Show CREATE TABLE statement for items
SELECT 'Step 2: Items table CREATE statement...' AS '';
SHOW CREATE TABLE items;

SELECT '' AS '';

-- Step 3: Check if v_active_items view exists and its definition
SELECT 'Step 3: v_active_items view definition...' AS '';
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

-- Step 4: Check for specific problematic columns
SELECT 'Step 4: Checking for problematic columns...' AS '';
SELECT 
    'state' as column_name,
    CASE 
        WHEN COUNT(*) > 0 THEN 'EXISTS'
        ELSE 'MISSING'
    END as status
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'items' 
  AND COLUMN_NAME = 'state';

SELECT 
    'is_draft' as column_name,
    CASE 
        WHEN COUNT(*) > 0 THEN 'EXISTS'
        ELSE 'MISSING'
    END as status
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'items' 
  AND COLUMN_NAME = 'is_draft';

SELECT 
    'deleted_at' as column_name,
    CASE 
        WHEN COUNT(*) > 0 THEN 'EXISTS'
        ELSE 'MISSING'
    END as status
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'items' 
  AND COLUMN_NAME = 'deleted_at';

SELECT 
    'start_time' as column_name,
    CASE 
        WHEN COUNT(*) > 0 THEN 'EXISTS'
        ELSE 'MISSING'
    END as status
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'items' 
  AND COLUMN_NAME = 'start_time';

SELECT 
    'end_time' as column_name,
    CASE 
        WHEN COUNT(*) > 0 THEN 'EXISTS'
        ELSE 'MISSING'
    END as status
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'items' 
  AND COLUMN_NAME = 'end_time';

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

-- Step 6: Check if specific item exists
SELECT 'Step 6: Checking specific item...' AS '';
SELECT 
    id,
    uuid_id,
    title,
    status,
    seller_id,
    created_at
FROM items 
WHERE uuid_id = '9168c105-0e2d-4eb1-84e5-319003bad57b'
   OR id = '9168c105-0e2d-4eb1-84e5-319003bad57b';

SELECT '' AS '';

-- Step 7: Check recent items to see what's actually in the database
SELECT 'Step 7: Recent items in database...' AS '';
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

-- Step 8: Check if there are any auctions table references
SELECT 'Step 8: Checking for auctions table...' AS '';
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN 'auctions table EXISTS'
        ELSE 'auctions table MISSING'
    END as auctions_status
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'auctions';

SELECT '' AS '';

SELECT '=======================================================' AS '';
SELECT 'SCHEMA INSPECTION COMPLETE' AS '';
SELECT '=======================================================' AS '';
