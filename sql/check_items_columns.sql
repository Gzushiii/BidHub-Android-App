-- CHECK EXACT COLUMNS IN ITEMS TABLE
-- This will show us exactly what columns exist in the items table

USE defaultdb;

-- 1. Show all columns in items table
SELECT '=== ITEMS TABLE COLUMNS ===' as section;
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
    AND TABLE_NAME = 'items'
ORDER BY ORDINAL_POSITION;

-- 2. Show column count
SELECT '=== ITEMS TABLE COLUMN COUNT ===' as section;
SELECT COUNT(*) as column_count 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
    AND TABLE_NAME = 'items';

-- 3. Test with only basic columns that definitely exist
SELECT '=== ITEMS TABLE BASIC DATA ===' as section;
SELECT 
    id,
    title,
    description,
    seller_id,
    starting_bid,
    current_bid,
    status,
    created_at
FROM items 
ORDER BY created_at DESC;

-- 4. Check if there are any items at all
SELECT '=== ITEMS COUNT ===' as section;
SELECT COUNT(*) as total_items FROM items;
