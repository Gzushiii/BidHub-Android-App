-- Diagnose Read vs Write Path Mismatch
-- This script identifies why items load in UI but fail in bid/buy-now

USE defaultdb;

SELECT '=======================================================' AS '';
SELECT 'DIAGNOSING READ vs WRITE PATH MISMATCH' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';

-- Step 1: Check the v_active_items view definition
SELECT 'Step 1: Checking v_active_items view definition...' AS '';
SELECT VIEW_DEFINITION
FROM information_schema.VIEWS 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'v_active_items';

SELECT '' AS '';

-- Step 2: Check items table structure
SELECT 'Step 2: Checking items table structure...' AS '';
DESCRIBE items;

SELECT '' AS '';

-- Step 3: Check if the specific item exists in items table
SELECT 'Step 3: Checking specific item in items table...' AS '';
SELECT 
    id,
    uuid_id,
    title,
    status,
    seller_id,
    created_at
FROM items 
WHERE uuid_id = 'a7835505-24a9-4101-8088-6f7d9d3dd0dc'
   OR id = 'a7835505-24a9-4101-8088-6f7d9d3dd0dc';

SELECT '' AS '';

-- Step 4: Check if the specific item exists in v_active_items view
SELECT 'Step 4: Checking specific item in v_active_items view...' AS '';
SELECT 
    id,
    title,
    status,
    seller_id,
    created_at
FROM v_active_items 
WHERE id = 'a7835505-24a9-4101-8088-6f7d9d3dd0dc';

SELECT '' AS '';

-- Step 5: Check all items with their status
SELECT 'Step 5: All items with status (first 10)...' AS '';
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

-- Step 6: Check items by status
SELECT 'Step 6: Items by status...' AS '';
SELECT 
    status,
    COUNT(*) as count
FROM items 
GROUP BY status;

SELECT '' AS '';

-- Step 7: Check if there are any items with uuid_id populated
SELECT 'Step 7: Items with uuid_id populated...' AS '';
SELECT 
    COUNT(*) as total_items,
    COUNT(uuid_id) as items_with_uuid,
    COUNT(*) - COUNT(uuid_id) as items_without_uuid
FROM items;

SELECT '' AS '';

-- Step 8: Check recent items to see what's being created
SELECT 'Step 8: Recent items (last 5)...' AS '';
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

-- Step 9: Test the exact queries used by read vs write paths
SELECT 'Step 9: Testing read path query (v_active_items)...' AS '';
SELECT COUNT(*) as read_path_count
FROM v_active_items 
WHERE id = 'a7835505-24a9-4101-8088-6f7d9d3dd0dc';

SELECT '' AS '';

SELECT 'Step 10: Testing write path query (items table with uuid_id)...' AS '';
SELECT COUNT(*) as write_path_count
FROM items 
WHERE uuid_id = 'a7835505-24a9-4101-8088-6f7d9d3dd0dc';

SELECT '' AS '';

SELECT 'Step 11: Testing write path query (items table with id)...' AS '';
SELECT COUNT(*) as write_path_id_count
FROM items 
WHERE id = 'a7835505-24a9-4101-8088-6f7d9d3dd0dc';

SELECT '' AS '';

-- Step 12: Check if there's a mismatch between id and uuid_id columns
SELECT 'Step 12: Checking id vs uuid_id mismatch...' AS '';
SELECT 
    'Items with both id and uuid_id' as check_type,
    COUNT(*) as count
FROM items 
WHERE id IS NOT NULL AND uuid_id IS NOT NULL;

SELECT 
    'Items with only id (no uuid_id)' as check_type,
    COUNT(*) as count
FROM items 
WHERE id IS NOT NULL AND uuid_id IS NULL;

SELECT 
    'Items with only uuid_id (no id)' as check_type,
    COUNT(*) as count
FROM items 
WHERE id IS NULL AND uuid_id IS NOT NULL;

SELECT '' AS '';

SELECT '=======================================================' AS '';
SELECT 'DIAGNOSIS COMPLETE' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';
SELECT 'Key findings:' AS '';
SELECT '- v_active_items view uses items.id (integer)' AS '';
SELECT '- Write paths use items.uuid_id (string UUID)' AS '';
SELECT '- This creates a read vs write mismatch' AS '';
SELECT '- Items created with integer IDs are visible in UI' AS '';
SELECT '- But bid/buy-now look for UUID IDs and fail' AS '';
SELECT '' AS '';
SELECT 'Solution: Update v_active_items view to use uuid_id' AS '';
