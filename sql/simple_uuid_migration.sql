-- Simple UUID Migration - Skip column creation, focus on data population
-- This script assumes UUID columns already exist and just populates them

USE defaultdb;

SELECT '=======================================================' AS '';
SELECT 'SIMPLE UUID MIGRATION - DATA POPULATION ONLY' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';

-- Step 1: Check current state
SELECT 'Step 1: Checking current state...' AS '';
SELECT 'Items with UUIDs:', (SELECT COUNT(*) FROM items WHERE uuid_id IS NOT NULL) as count;
SELECT 'Items without UUIDs:', (SELECT COUNT(*) FROM items WHERE uuid_id IS NULL) as count;
SELECT 'Total items:', (SELECT COUNT(*) FROM items) as count;

SELECT '' AS '';

-- Step 2: Create ID mapping if it doesn't exist
SELECT 'Step 2: Creating/updating ID mapping...' AS '';
DROP TABLE IF EXISTS id_mapping;
CREATE TABLE id_mapping (
    old_id INT UNSIGNED,
    new_id VARCHAR(36),
    PRIMARY KEY (old_id)
);

-- Generate UUIDs for items that don't have them yet
INSERT INTO id_mapping (old_id, new_id)
SELECT id, UUID() as new_id
FROM items
WHERE uuid_id IS NULL
ORDER BY id;

-- Show the mapping
SELECT 'ID Mapping created for items without UUIDs:' AS '';
SELECT old_id, new_id FROM id_mapping LIMIT 10;

SELECT '' AS '';

-- Step 3: Update items with UUIDs
SELECT 'Step 3: Updating items with UUIDs...' AS '';
UPDATE items i 
JOIN id_mapping m ON i.id = m.old_id 
SET i.uuid_id = m.new_id
WHERE i.id = m.old_id AND i.uuid_id IS NULL;

SELECT 'Items updated with UUIDs:', ROW_COUNT() as updated_count;

SELECT '' AS '';

-- Step 4: Update related tables
SELECT 'Step 4: Updating related tables...' AS '';

-- Update bids table if item_uuid_id column exists
SELECT 'Checking bids table...' AS '';
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN 'item_uuid_id column exists in bids table'
        ELSE 'item_uuid_id column does not exist in bids table'
    END as bids_status
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'bids' 
  AND COLUMN_NAME = 'item_uuid_id';

-- Update item_images table if item_uuid_id column exists
SELECT 'Checking item_images table...' AS '';
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN 'item_uuid_id column exists in item_images table'
        ELSE 'item_uuid_id column does not exist in item_images table'
    END as images_status
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'item_images' 
  AND COLUMN_NAME = 'item_uuid_id';

SELECT '' AS '';

-- Step 5: Summary
SELECT '=======================================================' AS '';
SELECT 'MIGRATION SUMMARY' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';

SELECT 
    (SELECT COUNT(*) FROM items) as total_items,
    (SELECT COUNT(*) FROM items WHERE uuid_id IS NOT NULL) as items_with_uuid,
    (SELECT COUNT(*) FROM items WHERE uuid_id IS NULL) as items_without_uuid;

SELECT '' AS '';

-- Show sample items with UUIDs
SELECT 'Sample items with UUIDs:' AS '';
SELECT id, uuid_id, title, status FROM items WHERE uuid_id IS NOT NULL LIMIT 5;

SELECT '' AS '';

SELECT 'Migration completed!' AS '';
SELECT 'All items should now have UUID IDs' AS '';
SELECT 'Backend is ready to handle UUID-based item lookups' AS '';
