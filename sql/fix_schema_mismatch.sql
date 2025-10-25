-- Fix Database Schema Mismatch
-- This script identifies the actual schema and creates a corrected v_active_items view

USE defaultdb;

SELECT '=======================================================' AS '';
SELECT 'FIXING DATABASE SCHEMA MISMATCH' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';

-- Step 1: Check actual items table structure
SELECT 'Step 1: Actual items table structure...' AS '';
DESCRIBE items;

SELECT '' AS '';

-- Step 2: Check what columns actually exist
SELECT 'Step 2: Existing columns in items table...' AS '';
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'items'
ORDER BY ORDINAL_POSITION;

SELECT '' AS '';

-- Step 3: Check if v_active_items view exists and drop it
SELECT 'Step 3: Dropping existing v_active_items view...' AS '';
DROP VIEW IF EXISTS v_active_items;

SELECT '' AS '';

-- Step 4: Create corrected v_active_items view using only existing columns
SELECT 'Step 4: Creating corrected v_active_items view...' AS '';

-- First, let's see what the actual items look like
SELECT 'Sample items in database:' AS '';
SELECT 
    id,
    uuid_id,
    title,
    status,
    seller_id,
    created_at
FROM items 
ORDER BY created_at DESC 
LIMIT 3;

SELECT '' AS '';

-- Create the view using only existing columns
CREATE VIEW v_active_items AS
SELECT 
    i.uuid_id as id,  -- Use uuid_id as the primary identifier
    i.title,
    i.description,
    i.category_id,
    i.seller_id,
    i.seller_email,
    i.starting_bid,
    i.current_bid,
    i.buy_now_price,
    i.status,
    i.created_at,
    i.updated_at,
    u.username as seller_username,
    u.email as seller_user_email,
    c.name as category_name,
    c.description as category_description
FROM items i
LEFT JOIN users u ON i.seller_id = u.id
LEFT JOIN categories c ON i.category_id = c.id
WHERE i.status = 'active' AND i.uuid_id IS NOT NULL;

SELECT 'v_active_items view created successfully' AS '';

SELECT '' AS '';

-- Step 5: Test the corrected view
SELECT 'Step 5: Testing corrected view...' AS '';
SELECT COUNT(*) as total_active_items FROM v_active_items;

SELECT 'Sample items from corrected view:' AS '';
SELECT id, title, status FROM v_active_items LIMIT 3;

SELECT '' AS '';

-- Step 6: Check if the specific failing item exists
SELECT 'Step 6: Checking specific failing item...' AS '';
SELECT 
    'Item lookup test' as test_name,
    COUNT(*) as found_in_items,
    (SELECT COUNT(*) FROM v_active_items WHERE id = '9168c105-0e2d-4eb1-84e5-319003bad57b') as found_in_view
FROM items 
WHERE uuid_id = '9168c105-0e2d-4eb1-84e5-319003bad57b';

SELECT '' AS '';

-- Step 7: Show the actual item if it exists
SELECT 'Step 7: Actual item details (if exists)...' AS '';
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
WHERE uuid_id = '9168c105-0e2d-4eb1-84e5-319003bad57b'
   OR id = '9168c105-0e2d-4eb1-84e5-319003bad57b';

SELECT '' AS '';

SELECT '=======================================================' AS '';
SELECT 'SCHEMA MISMATCH FIX COMPLETE' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';
SELECT 'Changes made:' AS '';
SELECT '1. Dropped existing v_active_items view' AS '';
SELECT '2. Created corrected view using only existing columns' AS '';
SELECT '3. View now uses uuid_id as primary identifier' AS '';
SELECT '4. View filters by status = "active" only' AS '';
SELECT '' AS '';
SELECT 'Next steps:' AS '';
SELECT '1. Update backend code to remove references to non-existent columns' AS '';
SELECT '2. Use only existing columns: id, uuid_id, title, description, status, etc.' AS '';
SELECT '3. Test item lookups with corrected schema' AS '';
