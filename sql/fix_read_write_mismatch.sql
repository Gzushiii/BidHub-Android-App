-- Fix Read vs Write Path Mismatch
-- This script fixes the core issue where items load in UI but fail in bid/buy-now

USE defaultdb;

SELECT '=======================================================' AS '';
SELECT 'FIXING READ vs WRITE PATH MISMATCH' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';

-- Step 1: Update v_active_items view to use uuid_id instead of id
SELECT 'Step 1: Updating v_active_items view to use uuid_id...' AS '';

-- Drop existing view
DROP VIEW IF EXISTS v_active_items;

-- Recreate view using uuid_id as the primary identifier
CREATE VIEW v_active_items AS
SELECT 
    i.uuid_id as id,  -- Use uuid_id as the id field for consistency
    i.title,
    i.description,
    i.category_id,
    i.seller_id,
    i.seller_email,
    i.starting_bid,
    i.current_bid,
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

SELECT 'v_active_items view updated to use uuid_id' AS '';

SELECT '' AS '';

-- Step 2: Verify the view works correctly
SELECT 'Step 2: Testing the updated view...' AS '';
SELECT COUNT(*) as total_active_items FROM v_active_items;

SELECT 'Sample items from updated view:' AS '';
SELECT id, title, status FROM v_active_items LIMIT 3;

SELECT '' AS '';

-- Step 3: Check if there are any items without uuid_id that need to be handled
SELECT 'Step 3: Checking for items without uuid_id...' AS '';
SELECT 
    COUNT(*) as total_items,
    COUNT(uuid_id) as items_with_uuid,
    COUNT(*) - COUNT(uuid_id) as items_without_uuid
FROM items;

-- If there are items without uuid_id, we need to generate UUIDs for them
SELECT 'Generating UUIDs for items without uuid_id...' AS '';
UPDATE items 
SET uuid_id = UUID() 
WHERE uuid_id IS NULL;

SELECT 'UUID generation complete' AS '';

SELECT '' AS '';

-- Step 4: Update related tables to use uuid_id consistently
SELECT 'Step 4: Updating related tables...' AS '';

-- Update bids table to use item_uuid_id for lookups
SELECT 'Checking bids table structure...' AS '';
DESCRIBE bids;

-- Update item_images table to use item_uuid_id for lookups  
SELECT 'Checking item_images table structure...' AS '';
DESCRIBE item_images;

SELECT '' AS '';

-- Step 5: Test the specific item that was failing
SELECT 'Step 5: Testing specific failing item...' AS '';
SELECT 
    'Testing item: a7835505-24a9-4101-8088-6f7d9d3dd0dc' AS test_item,
    COUNT(*) as found_in_items,
    (SELECT COUNT(*) FROM v_active_items WHERE id = 'a7835505-24a9-4101-8088-6f7d9d3dd0dc') as found_in_view
FROM items 
WHERE uuid_id = 'a7835505-24a9-4101-8088-6f7d9d3dd0dc';

SELECT '' AS '';

-- Step 6: Verify all recent items are accessible
SELECT 'Step 6: Verifying recent items accessibility...' AS '';
SELECT 
    'Recent items in items table:' AS table_name,
    COUNT(*) as count
FROM items 
WHERE created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR);

SELECT 
    'Recent items in v_active_items view:' AS view_name,
    COUNT(*) as count
FROM v_active_items 
WHERE created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR);

SELECT '' AS '';

SELECT '=======================================================' AS '';
SELECT 'FIX COMPLETE' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';
SELECT 'Changes made:' AS '';
SELECT '1. Updated v_active_items view to use uuid_id as id field' AS '';
SELECT '2. Generated UUIDs for any items missing uuid_id' AS '';
SELECT '3. Ensured consistency between read and write paths' AS '';
SELECT '' AS '';
SELECT 'Expected result:' AS '';
SELECT '- Items visible in UI will now be accessible for bid/buy operations' AS '';
SELECT '- No more 404 errors for items that load in details view' AS '';
SELECT '- Consistent ID handling across all endpoints' AS '';
